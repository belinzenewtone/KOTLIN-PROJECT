import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_spacing.dart';
import 'package:beltech/core/theme/app_typography.dart';
import 'package:beltech/core/widgets/app_feedback.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:beltech/features/auth/domain/entities/auth_state.dart';
import 'package:beltech/features/auth/presentation/providers/auth_providers.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class SettingsSecurityCard extends ConsumerWidget {
  const SettingsSecurityCard({super.key, required this.state});

  final AuthState state;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Column(
      children: [
        GlassCard(
          tone: GlassCardTone.muted,
          child: Row(
            children: [
              Icon(Icons.fingerprint_outlined, color: AppColors.accent),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Biometric Lock',
                        style: AppTypography.cardTitle(context)),
                    const SizedBox(height: 4),
                    Text(
                      state.biometricSupported
                          ? 'Use fingerprint/face to unlock secure actions'
                          : 'Biometrics not supported on this device',
                      style: AppTypography.bodySm(context),
                    ),
                  ],
                ),
              ),
              Switch.adaptive(
                value: state.biometricEnabled,
                onChanged: state.biometricSupported
                    ? (value) async {
                        await ref
                            .read(authProvider.notifier)
                            .setBiometricEnabled(value);
                      }
                    : null,
              ),
            ],
          ),
        ),
        const SizedBox(height: AppSpacing.listGap),
        GlassCard(
          tone: GlassCardTone.muted,
          child: SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: state.isAuthenticating
                  ? null
                  : () async {
                      final ok = await ref
                          .read(authProvider.notifier)
                          .authenticateNow();
                      if (context.mounted) {
                        if (ok) {
                          AppFeedback.success(
                              context, 'Authentication successful.');
                        } else {
                          AppFeedback.error(context, 'Authentication failed.');
                        }
                      }
                    },
              icon: state.isAuthenticating
                  ? const SizedBox(
                      width: 14,
                      height: 14,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.fingerprint),
              label: const Text('Authenticate Now'),
            ),
          ),
        ),
      ],
    );
  }
}
