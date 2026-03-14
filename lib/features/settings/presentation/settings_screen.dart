import 'package:beltech/core/di/repository_providers.dart';
import 'package:beltech/core/di/notification_providers.dart';
import 'package:beltech/core/di/sync_providers.dart';
import 'package:beltech/core/di/update_providers.dart';
import 'package:beltech/core/sync/data_mode_migration_service.dart';
import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_spacing.dart';
import 'package:beltech/core/theme/app_typography.dart';
import 'package:beltech/core/widgets/app_capsule.dart';
import 'package:beltech/core/widgets/app_dialog.dart';
import 'package:beltech/core/widgets/app_feedback.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:beltech/core/widgets/loading_indicator.dart';
import 'package:beltech/core/widgets/secondary_page_shell.dart';
import 'package:beltech/core/widgets/section_header.dart';
import 'package:beltech/features/auth/domain/entities/auth_state.dart';
import 'package:beltech/features/auth/presentation/providers/account_providers.dart';
import 'package:beltech/features/auth/presentation/providers/auth_providers.dart';
import 'package:beltech/features/settings/presentation/widgets/notification_preferences_section.dart';
import 'package:beltech/features/settings/presentation/widgets/settings_about_card.dart';
import 'package:beltech/features/settings/presentation/widgets/settings_appearance_card.dart';
import 'package:beltech/features/settings/presentation/widgets/settings_security_card.dart';
import 'package:beltech/features/settings/presentation/widgets/settings_tools_card.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

part 'settings_screen_data_mode.dart';

final _dataModeSwitchBusyProvider = StateProvider<bool>((_) => false);

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authProvider);
    final accountWriteState = ref.watch(accountAuthControllerProvider);
    final useSupabase = ref.watch(useSupabaseProvider);
    final cloudAvailable = ref.watch(cloudModeAvailableProvider);
    final preferredDataMode = ref.watch(preferredDataModeProvider);
    final dataModeSwitchBusy = ref.watch(_dataModeSwitchBusyProvider);

    ref.listen<AsyncValue<AuthState>>(authProvider, (previous, next) {
      if (next.hasError) {
        AppFeedback.error(context, '${next.error}');
      }
    });
    ref.listen<AsyncValue<void>>(notificationPreferenceControllerProvider,
        (previous, next) {
      if (previous is AsyncLoading && next is AsyncData<void>) {
        AppFeedback.success(context, 'Notification preference updated.');
      } else if (next.hasError) {
        AppFeedback.error(context, 'Unable to update notification settings.');
      }
    });

    return SecondaryPageShell(
      title: 'Settings',
      glowColor: AppColors.glowBlue,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SectionHeader('Data Mode', topPadding: 0),
          GlassCard(
            tone: GlassCardTone.muted,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  useSupabase
                      ? 'Cloud mode is active'
                      : 'Offline mode is active',
                  style: AppTypography.cardTitle(context),
                ),
                const SizedBox(height: 4),
                Text(
                  useSupabase
                      ? 'Data syncs with your cloud account.'
                      : 'Data stays on this device only.',
                  style: AppTypography.bodySm(context),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 12),
                SegmentedButton<DataModePreference>(
                  showSelectedIcon: false,
                  segments: [
                    const ButtonSegment<DataModePreference>(
                      value: DataModePreference.local,
                      icon: Icon(Icons.phone_android_outlined, size: 18),
                      label: Text('Offline'),
                    ),
                    ButtonSegment<DataModePreference>(
                      value: DataModePreference.cloud,
                      enabled: cloudAvailable,
                      icon: const Icon(Icons.cloud_done_outlined, size: 18),
                      label: Text(cloudAvailable ? 'Cloud' : 'Cloud (Setup)'),
                    ),
                  ],
                  selected: {preferredDataMode},
                  onSelectionChanged: dataModeSwitchBusy
                      ? null
                      : (selection) async {
                          final mode = selection.first;
                          await _applyDataMode(
                            context: context,
                            ref: ref,
                            mode: mode,
                            cloudAvailable: cloudAvailable,
                          );
                        },
                ),
                if (dataModeSwitchBusy) ...[
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      const SizedBox(
                        width: 14,
                        height: 14,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          'Applying mode and syncing data...',
                          style: AppTypography.bodySm(context),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ),
                ],
              ],
            ),
          ),
          SizedBox(height: AppSpacing.sectionGap),

          // Account banner — sign out or local-mode notice
          if (useSupabase)
            GlassCard(
              tone: GlassCardTone.muted,
              child: SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: accountWriteState.isLoading
                      ? null
                      : () async {
                          await ref
                              .read(accountAuthControllerProvider.notifier)
                              .signOut();
                        },
                  icon: accountWriteState.isLoading
                      ? const SizedBox(
                          width: 14,
                          height: 14,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.logout_rounded),
                  label: const Text('Sign Out'),
                ),
              ),
            )
          else
            GlassCard(
              tone: GlassCardTone.muted,
              child: Row(
                children: [
                  Icon(Icons.cloud_off_outlined,
                      color: AppColors.textMuted, size: 20),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Local mode',
                            style: AppTypography.cardTitle(context)),
                        const SizedBox(height: 2),
                        Text(
                          'Data is stored on this device only',
                          style: AppTypography.bodySm(context),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                  AppCapsule(
                    label: 'Offline',
                    color: AppColors.textMuted,
                    variant: AppCapsuleVariant.outline,
                    size: AppCapsuleSize.sm,
                  ),
                ],
              ),
            ),
          SizedBox(height: AppSpacing.sectionGap),

          // Appearance
          SectionHeader('Appearance', topPadding: 0),
          const SettingsAppearanceCard(),
          SizedBox(height: AppSpacing.sectionGap),

          // Security
          SectionHeader('Security'),
          authState.when(
            data: (state) => SettingsSecurityCard(state: state),
            loading: () => GlassCard(
              tone: GlassCardTone.muted,
              child: SizedBox(
                height: 200,
                child: const Center(
                  child: LoadingIndicator(),
                ),
              ),
            ),
            error: (_, __) => GlassCard(
              tone: GlassCardTone.muted,
              child: Column(
                children: [
                  Icon(Icons.error_outline, color: AppColors.danger),
                  const SizedBox(height: 8),
                  Text(
                    'Unable to load security settings',
                    style: AppTypography.bodySm(context),
                  ),
                  const SizedBox(height: 12),
                  TextButton(
                    onPressed: () => ref.invalidate(authProvider),
                    child: const Text('Retry'),
                  ),
                ],
              ),
            ),
          ),
          SizedBox(height: AppSpacing.sectionGap),

          // Notifications
          SectionHeader('Notifications'),
          GlassCard(
            tone: GlassCardTone.muted,
            child: const NotificationPreferencesSection(),
          ),
          SizedBox(height: AppSpacing.sectionGap),

          // Workspace Tools
          SectionHeader('Workspace Tools'),
          const SettingsToolsCard(),
          SizedBox(height: AppSpacing.sectionGap),

          // About
          SectionHeader('About'),
          const SettingsAboutCard(),
        ],
      ),
    );
  }
}
