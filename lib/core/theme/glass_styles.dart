import 'package:beltech/core/theme/app_colors.dart';
import 'package:flutter/material.dart';

class GlassStyles {
  GlassStyles._();

  static const double blurSigma = 12;
  static const double borderRadius = 24;
  static const EdgeInsets cardPadding = EdgeInsets.all(16);

  static double blurSigmaFor(Brightness brightness) {
    if (brightness == Brightness.light) {
      return 6;
    }
    return blurSigma;
  }

  static LinearGradient backgroundGradientFor(Brightness brightness) {
    if (brightness == Brightness.light) {
      return const LinearGradient(
        begin: Alignment.topCenter,
        end: Alignment.bottomCenter,
        colors: [
          Color(0xFFF9FBFF),
          Color(0xFFF0F5FC),
          Color(0xFFEAF1FB),
        ],
      );
    }
    return const LinearGradient(
      begin: Alignment.topCenter,
      end: Alignment.bottomCenter,
      colors: [AppColors.backgroundTop, AppColors.backgroundBottom],
    );
  }

  static LinearGradient glassGradientFor(Brightness brightness) {
    if (brightness == Brightness.light) {
      return const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: [
          Color(0xF7FFFFFF),
          Color(0xE7F2FAFF),
        ],
      );
    }
    return const LinearGradient(
      begin: Alignment.topLeft,
      end: Alignment.bottomRight,
      colors: [
        Color(0x3B1E3A67),
        Color(0x1C102542),
      ],
    );
  }

  static LinearGradient accentGlassGradientFor(
    Brightness brightness,
    Color accent,
  ) {
    if (brightness == Brightness.light) {
      return LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: [
          accent.withValues(alpha: 0.2),
          const Color(0xF7FFFFFF),
        ],
      );
    }
    return LinearGradient(
      begin: Alignment.topLeft,
      end: Alignment.bottomRight,
      colors: [
        accent.withValues(alpha: 0.24),
        const Color(0x1C102542),
      ],
    );
  }
}
