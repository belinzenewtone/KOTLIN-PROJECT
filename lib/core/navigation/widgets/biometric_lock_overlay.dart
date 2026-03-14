import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:flutter/material.dart';

class BiometricLockOverlay extends StatelessWidget {
  const BiometricLockOverlay({
    super.key,
    required this.busy,
    required this.message,
    required this.onUnlock,
  });

  final bool busy;
  final String? message;
  final Future<void> Function() onUnlock;

  @override
  Widget build(BuildContext context) {
    return Positioned.fill(
      child: ColoredBox(
        color: AppColors.background.withValues(alpha: 0.8),
        child: SafeArea(
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 360),
              child: GlassCard(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(
                      Icons.fingerprint,
                      color: AppColors.accent,
                      size: 56,
                    ),
                    const SizedBox(height: 12),
                    Text(
                      'Unlock BELTECH',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Use your fingerprint or face to continue.',
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.bodyMedium,
                    ),
                    if (message != null) ...[
                      const SizedBox(height: 10),
                      Text(
                        message!,
                        textAlign: TextAlign.center,
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              color: AppColors.warning,
                            ),
                      ),
                    ],
                    const SizedBox(height: 16),
                    SizedBox(
                      width: double.infinity,
                      child: FilledButton.icon(
                        onPressed: busy ? null : () => onUnlock(),
                        icon: busy
                            ? const SizedBox(
                                width: 14,
                                height: 14,
                                child:
                                    CircularProgressIndicator(strokeWidth: 2),
                              )
                            : const Icon(Icons.lock_open_rounded),
                        label: Text(busy ? 'Authenticating...' : 'Unlock'),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
