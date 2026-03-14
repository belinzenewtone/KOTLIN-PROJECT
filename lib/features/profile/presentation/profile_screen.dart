import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_spacing.dart';
import 'package:beltech/core/widgets/app_feedback.dart';
import 'package:beltech/core/widgets/error_message.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:beltech/core/widgets/loading_indicator.dart';
import 'package:beltech/core/widgets/page_header.dart';
import 'package:beltech/core/widgets/page_shell.dart';
import 'package:beltech/features/auth/presentation/providers/account_providers.dart';
import 'package:beltech/features/profile/presentation/providers/profile_providers.dart';
import 'package:beltech/features/profile/presentation/widgets/profile_content_section.dart';
import 'package:beltech/features/profile/presentation/widgets/profile_dialogs.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'package:path/path.dart' as p;

class ProfileScreen extends ConsumerWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final profileState = ref.watch(profileProvider);
    final authWriteState = ref.watch(accountAuthControllerProvider);

    ref.listen<AsyncValue<void>>(profileWriteControllerProvider,
        (previous, next) {
      if (previous is AsyncLoading && next is AsyncData<void>) {
        AppFeedback.success(context, 'Profile updated successfully.', ref: ref);
      } else if (next.hasError) {
        AppFeedback.error(
            context, '${next.error}'.replaceFirst('Exception: ', ''),
            ref: ref);
      }
    });
    ref.listen<AsyncValue<void>>(accountAuthControllerProvider,
        (previous, next) {
      if (next.hasError) {
        final message = '${next.error}'.replaceFirst('Exception: ', '');
        AppFeedback.error(context, message, ref: ref);
      }
    });

    return PageShell(
      scrollable: true,
      glowColor: AppColors.glowBlue,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          PageHeader(
            eyebrow: 'ACCOUNT',
            title: 'Profile',
          ),
          profileState.when(
            data: (profile) => Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                ProfileContentSection(
                  profile: profile,
                  onEdit: () => showEditProfileDialog(context, ref, profile),
                  onChangePassword: () => showPasswordDialog(context, ref),
                  onAvatarCameraTap: () async {
                    try {
                      final picker = ImagePicker();
                      final picked = await picker.pickImage(
                        source: ImageSource.gallery,
                        maxWidth: 1024,
                        maxHeight: 1024,
                        imageQuality: 88,
                      );
                      if (picked == null) {
                        return;
                      }
                      final bytes = await picked.readAsBytes();
                      final extension =
                          p.extension(picked.path).replaceFirst('.', '');
                      await ref
                          .read(profileWriteControllerProvider.notifier)
                          .updateAvatar(
                            bytes: bytes,
                            fileExtension:
                                extension.isEmpty ? 'jpeg' : extension,
                          );
                      if (!context.mounted) {
                        return;
                      }
                      final writeState =
                          ref.read(profileWriteControllerProvider);
                      if (!writeState.hasError) {
                        AppFeedback.success(
                            context, 'Profile photo updated successfully.',
                            ref: ref);
                      }
                    } catch (error) {
                      if (!context.mounted) {
                        return;
                      }
                      final message = '$error'.replaceFirst('Exception: ', '');
                      AppFeedback.error(context, message, ref: ref);
                    }
                  },
                  showSignOut: true,
                  signingOut: authWriteState.isLoading,
                  onSignOut: () async {
                    await ref
                        .read(accountAuthControllerProvider.notifier)
                        .signOut();
                  },
                ),
                const SizedBox(height: AppSpacing.sectionGap),
                _ToolHubGrid(ref: ref),
                const SizedBox(height: AppSpacing.sectionGap),
              ],
            ),
            loading: () => Center(child: LoadingIndicator()),
            error: (_, __) => ErrorMessage(
              label: 'Unable to load profile',
              onRetry: () => ref.invalidate(profileProvider),
            ),
          ),
        ],
      ),
    );
  }
}

class _ToolHubGrid extends StatelessWidget {
  const _ToolHubGrid({required this.ref});

  final WidgetRef ref;

  @override
  Widget build(BuildContext context) {
    final tools = [
      (label: 'Analytics', icon: Icons.query_stats_rounded, route: 'analytics'),
      (label: 'Recurring', icon: Icons.repeat_rounded, route: 'recurring'),
      (label: 'Export', icon: Icons.download_rounded, route: 'export'),
      (label: 'Search', icon: Icons.search_rounded, route: 'search'),
      (label: 'Settings', icon: Icons.settings_rounded, route: 'settings'),
      (label: 'Budget', icon: Icons.account_balance_outlined, route: 'budget'),
    ];

    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        crossAxisSpacing: AppSpacing.listGap,
        mainAxisSpacing: AppSpacing.listGap,
        childAspectRatio: 1.2,
      ),
      itemCount: tools.length,
      itemBuilder: (context, index) {
        final tool = tools[index];
        return GlassCard(
          tone: GlassCardTone.muted,
          onTap: () {
            context.pushNamed(tool.route);
          },
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(tool.icon, color: AppColors.accent, size: 28),
              const SizedBox(height: 8),
              Text(
                tool.label,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
              ),
            ],
          ),
        );
      },
    );
  }
}
