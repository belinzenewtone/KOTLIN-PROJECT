import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_radius.dart';
import 'package:flutter/material.dart';

enum AppButtonVariant { primary, secondary, ghost, danger }
enum AppButtonSize { sm, md, lg }

class AppButton extends StatelessWidget {
  const AppButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.variant = AppButtonVariant.primary,
    this.size = AppButtonSize.md,
    this.icon,
    this.loading = false,
    this.fullWidth = false,
  });

  final String label;
  final VoidCallback? onPressed;
  final AppButtonVariant variant;
  final AppButtonSize size;
  final IconData? icon;
  final bool loading;
  final bool fullWidth;

  @override
  Widget build(BuildContext context) {
    final brightness = Theme.of(context).brightness;
    final isEnabled = onPressed != null && !loading;

    final (height, hPad, fontSize) = switch (size) {
      AppButtonSize.sm => (38.0, 14.0, 13.0),
      AppButtonSize.md => (46.0, 18.0, 15.0),
      AppButtonSize.lg => (54.0, 22.0, 16.0),
    };

    final accent = brightness == Brightness.light
        ? const Color(0xFF2A6FE8)
        : AppColors.accent;

    final (bgColor, fgColor, borderColor) = switch (variant) {
      AppButtonVariant.primary => (
          isEnabled ? accent : accent.withValues(alpha: 0.5),
          Colors.white,
          Colors.transparent,
        ),
      AppButtonVariant.secondary => (
          Colors.transparent,
          accent,
          accent.withValues(alpha: isEnabled ? 0.8 : 0.4),
        ),
      AppButtonVariant.ghost => (
          Colors.transparent,
          accent,
          Colors.transparent,
        ),
      AppButtonVariant.danger => (
          isEnabled ? AppColors.danger : AppColors.danger.withValues(alpha: 0.5),
          Colors.white,
          Colors.transparent,
        ),
    };

    final content = loading
        ? SizedBox(
            width: 18,
            height: 18,
            child: CircularProgressIndicator(
              strokeWidth: 2,
              color: fgColor,
            ),
          )
        : Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (icon != null) ...[
                Icon(icon, size: fontSize + 2, color: fgColor),
                const SizedBox(width: 6),
              ],
              Text(
                label,
                style: TextStyle(
                  fontSize: fontSize,
                  fontWeight: FontWeight.w600,
                  color: fgColor,
                  height: 1.2,
                ),
              ),
            ],
          );

    final shape = RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(AppRadius.lg),
      side: borderColor == Colors.transparent
          ? BorderSide.none
          : BorderSide(color: borderColor),
    );

    Widget button = SizedBox(
      height: height,
      width: fullWidth ? double.infinity : null,
      child: variant == AppButtonVariant.secondary || variant == AppButtonVariant.ghost
          ? OutlinedButton(
              onPressed: isEnabled ? onPressed : null,
              style: OutlinedButton.styleFrom(
                backgroundColor: bgColor,
                foregroundColor: fgColor,
                side: BorderSide(color: borderColor),
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(AppRadius.lg)),
                padding: EdgeInsets.symmetric(horizontal: hPad),
              ),
              child: content,
            )
          : FilledButton(
              onPressed: isEnabled ? onPressed : null,
              style: FilledButton.styleFrom(
                backgroundColor: bgColor,
                foregroundColor: fgColor,
                shape: shape,
                padding: EdgeInsets.symmetric(horizontal: hPad),
                disabledBackgroundColor: bgColor.withValues(alpha: 0.5),
              ),
              child: content,
            ),
    );

    return button;
  }
}
