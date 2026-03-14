import 'dart:ui';

import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/glass_styles.dart';
import 'package:flutter/material.dart';

enum GlassCardTone { standard, accent, muted }

class GlassCard extends StatelessWidget {
  const GlassCard({
    super.key,
    required this.child,
    this.padding = GlassStyles.cardPadding,
    this.margin,
    this.borderRadius = GlassStyles.borderRadius,
    this.tone = GlassCardTone.standard,
    this.accentColor,
    this.onTap,
  });

  final Widget child;
  final EdgeInsetsGeometry padding;
  final EdgeInsetsGeometry? margin;
  final double borderRadius;
  final GlassCardTone tone;
  final Color? accentColor;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final brightness = Theme.of(context).brightness;
    final effectiveAccent = accentColor ?? colorScheme.primary;

    final gradient = switch (tone) {
      GlassCardTone.accent =>
        GlassStyles.accentGlassGradientFor(brightness, effectiveAccent),
      GlassCardTone.muted => LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: brightness == Brightness.light
              ? [const Color(0xFFF5F8FF), const Color(0xFFEEF3FB)]
              : [const Color(0x180D1B30), const Color(0x100A1424)],
        ),
      GlassCardTone.standard => GlassStyles.glassGradientFor(brightness),
    };

    final borderColor = switch (tone) {
      GlassCardTone.muted => brightness == Brightness.light
          ? const Color(0xFFCFDEF0).withValues(alpha: 0.5)
          : AppColors.border.withValues(alpha: 0.18),
      _ => brightness == Brightness.light
          ? AppColors.borderFor(brightness).withValues(alpha: 0.68)
          : colorScheme.outline.withValues(alpha: 0.35),
    };

    final double shadowAlpha =
        tone == GlassCardTone.muted ? 0.06 : (brightness == Brightness.light ? 0.12 : 0.22);
    final shadowColor = brightness == Brightness.light
        ? const Color(0xFF4D6487).withValues(alpha: shadowAlpha)
        : Colors.black.withValues(alpha: shadowAlpha);

    final glowBase =
        tone == GlassCardTone.accent ? effectiveAccent : AppColors.teal;
    final glowColor = tone == GlassCardTone.muted
        ? Colors.transparent
        : brightness == Brightness.light
            ? glowBase.withValues(alpha: 0.08)
            : glowBase.withValues(alpha: 0.14);

    final blurSigma = tone == GlassCardTone.muted
        ? GlassStyles.blurSigmaFor(brightness) * 0.5
        : GlassStyles.blurSigmaFor(brightness);

    Widget inner = Container(
      margin: margin,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(borderRadius),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: blurSigma, sigmaY: blurSigma),
          child: Container(
            decoration: BoxDecoration(
              gradient: gradient,
              borderRadius: BorderRadius.circular(borderRadius),
              border: Border.all(color: borderColor),
              boxShadow: [
                BoxShadow(
                  color: shadowColor,
                  blurRadius: brightness == Brightness.light ? 18 : 28,
                  offset: Offset(0, brightness == Brightness.light ? 8 : 14),
                ),
                if (tone != GlassCardTone.muted)
                  BoxShadow(
                    color: glowColor,
                    blurRadius: 16,
                    offset: const Offset(0, 2),
                  ),
              ],
            ),
            padding: padding,
            child: child,
          ),
        ),
      ),
    );

    if (onTap != null) {
      return Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(borderRadius),
          child: inner,
        ),
      );
    }

    return inner;
  }
}
