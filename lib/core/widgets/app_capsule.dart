import 'package:flutter/material.dart';

enum AppCapsuleVariant { solid, subtle, outline }
enum AppCapsuleSize { sm, md, lg }

/// A compact pill badge/chip used for category labels, priority levels,
/// status indicators, and countdown badges. Optionally tappable when
/// [onTap] is provided.
class AppCapsule extends StatelessWidget {
  const AppCapsule({
    super.key,
    required this.label,
    required this.color,
    this.variant = AppCapsuleVariant.subtle,
    this.size = AppCapsuleSize.sm,
    this.icon,
    this.onTap,
  });

  final String label;
  final Color color;
  final AppCapsuleVariant variant;
  final AppCapsuleSize size;
  final IconData? icon;
  /// If non-null, wraps the chip in a [GestureDetector].
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final (hPad, vPad, fontSize) = switch (size) {
      AppCapsuleSize.sm => (7.0, 3.0, 11.0),
      AppCapsuleSize.md => (10.0, 5.0, 13.0),
      AppCapsuleSize.lg => (12.0, 6.0, 14.0),
    };

    final bgColor = switch (variant) {
      AppCapsuleVariant.solid => color,
      AppCapsuleVariant.subtle => color.withValues(alpha: 0.16),
      AppCapsuleVariant.outline => Colors.transparent,
    };

    final textColor = switch (variant) {
      AppCapsuleVariant.solid => Colors.white,
      _ => color,
    };

    final border = variant == AppCapsuleVariant.outline
        ? Border.all(color: color.withValues(alpha: 0.7))
        : null;

    final pill = Container(
      padding: EdgeInsets.symmetric(horizontal: hPad, vertical: vPad),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(999),
        border: border,
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, size: fontSize, color: textColor),
            const SizedBox(width: 3),
          ],
          Text(
            label,
            style: TextStyle(
              fontSize: fontSize,
              fontWeight: FontWeight.w600,
              color: textColor,
              height: 1.2,
            ),
          ),
        ],
      ),
    );

    if (onTap == null) return pill;
    return GestureDetector(onTap: onTap, child: pill);
  }
}
