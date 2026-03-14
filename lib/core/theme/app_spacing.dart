import 'package:flutter/material.dart';

class AppSpacing {
  AppSpacing._();

  // ── Layout ───────────────────────────────────────────────────────────────────
  static const double screenHorizontal = 20;
  static const double screenTop = 16; // slightly more breathing room
  static const double shellHorizontal = 12;
  static const double contentBottomSafe = 120;
  static const double sectionBottom = 24;
  static const double navBottomMargin = 6;
  static const double fabBottomOffset = 104;

  // ── Gaps ─────────────────────────────────────────────────────────────────────
  /// Between two sibling section blocks
  static const double sectionGap = 24;

  /// Between a section header and its first card
  static const double sectionHeaderGap = 12;

  /// Between adjacent cards in the same section
  static const double cardGap = 14;

  /// Between adjacent list items (tight)
  static const double listGap = 10;

  static EdgeInsets screenPadding(
    BuildContext context, {
    double bottom = contentBottomSafe,
  }) {
    return EdgeInsets.fromLTRB(
      screenHorizontal,
      screenTop,
      screenHorizontal,
      bottom + _safeBottomContribution(context),
    );
  }

  static EdgeInsets sectionPadding(
    BuildContext context, {
    double bottom = sectionBottom,
  }) {
    return EdgeInsets.fromLTRB(
      screenHorizontal,
      screenTop,
      screenHorizontal,
      bottom + _safeBottomContribution(context),
    );
  }

  static double fabBottom(BuildContext context) {
    return fabBottomOffset + _safeBottomContribution(context);
  }

  static double navBottom(BuildContext context) {
    final safeBottom = MediaQuery.paddingOf(context).bottom;
    final reservedBottom = safeBottom > 0 ? safeBottom : 8;
    return navBottomMargin + reservedBottom;
  }

  static double _safeBottomContribution(BuildContext context) {
    final safeBottom = MediaQuery.paddingOf(context).bottom;
    return safeBottom > 0 ? safeBottom * 0.6 : 0;
  }
}
