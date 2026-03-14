import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// Semantic text style helpers.
///
/// Use these instead of raw `Theme.of(context).textTheme.*` calls so that
/// intent is self-documenting and consistent across every screen.
///
/// Example:
/// ```dart
/// Text('Good Morning', style: AppTypography.pageTitle(context))
/// Text('YOUR DAY', style: AppTypography.eyebrow(context))
/// Text('KES 4,200', style: AppTypography.amountLg(context))
/// ```
class AppTypography {
  AppTypography._();

  // ── Page-level ───────────────────────────────────────────────────────────────

  /// 28px w700 — screen main title
  static TextStyle pageTitle(BuildContext context) => GoogleFonts.inter(
        fontSize: 28,
        fontWeight: FontWeight.w700,
        height: 34 / 28,
        color: Theme.of(context).colorScheme.onSurface,
      );

  /// 11px w600 uppercase + letter-spacing — label above a title
  static TextStyle eyebrow(BuildContext context) => GoogleFonts.inter(
        fontSize: 11,
        fontWeight: FontWeight.w600,
        letterSpacing: 0.9,
        height: 1.4,
        color: Theme.of(context).brightness == Brightness.light
            ? const Color(0xFF8AA0BF)
            : const Color(0xFF74839A),
      ).copyWith(
        // uppercase via a workaround: use the text itself — callers should
        // pass UPPER text, or wrap with .toUpperCase()
      );

  // ── Section-level ────────────────────────────────────────────────────────────

  /// 17px w600 — section label
  static TextStyle sectionTitle(BuildContext context) => GoogleFonts.inter(
        fontSize: 17,
        fontWeight: FontWeight.w600,
        height: 24 / 17,
        color: Theme.of(context).colorScheme.onSurface,
      );

  // ── Card-level ───────────────────────────────────────────────────────────────

  /// 15px w600 — card heading
  static TextStyle cardTitle(BuildContext context) => GoogleFonts.inter(
        fontSize: 15,
        fontWeight: FontWeight.w600,
        height: 22 / 15,
        color: Theme.of(context).colorScheme.onSurface,
      );

  // ── Body ─────────────────────────────────────────────────────────────────────

  /// 15px w400 — default body copy
  static TextStyle bodyMd(BuildContext context) => GoogleFonts.inter(
        fontSize: 15,
        fontWeight: FontWeight.w400,
        height: 22 / 15,
        color: Theme.of(context).brightness == Brightness.light
            ? const Color(0xFF516584)
            : const Color(0xFFA8B9D6),
      );

  /// 13px w400 — small supporting text, metadata
  static TextStyle bodySm(BuildContext context) => GoogleFonts.inter(
        fontSize: 13,
        fontWeight: FontWeight.w400,
        height: 20 / 13,
        color: Theme.of(context).brightness == Brightness.light
            ? const Color(0xFF8AA0BF)
            : const Color(0xFF74839A),
      );

  // ── Numeric / financial ──────────────────────────────────────────────────────

  /// 22px w700 — inline amounts on cards
  static TextStyle amount(BuildContext context) => GoogleFonts.inter(
        fontSize: 22,
        fontWeight: FontWeight.w700,
        height: 28 / 22,
        color: Theme.of(context).colorScheme.onSurface,
      );

  /// 30px w700 — hero amounts (balance, total)
  static TextStyle amountLg(BuildContext context) => GoogleFonts.inter(
        fontSize: 30,
        fontWeight: FontWeight.w700,
        height: 36 / 30,
        color: Theme.of(context).colorScheme.onSurface,
      );

  // ── Utility ──────────────────────────────────────────────────────────────────

  /// Copy a style and apply a specific color without losing the rest.
  static TextStyle withColor(TextStyle style, Color color) =>
      style.copyWith(color: color);

  /// Copy a style and reduce opacity (for disabled / muted states).
  static TextStyle muted(TextStyle style) =>
      style.copyWith(
        color: style.color?.withValues(alpha: 0.5),
      );
}
