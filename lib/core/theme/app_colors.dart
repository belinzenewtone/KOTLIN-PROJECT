import 'package:flutter/material.dart';

class AppColors {
  AppColors._();

  // ── Background ──────────────────────────────────────────────────────────────
  static const Color background = Color(0xFF020B1E);
  static const Color backgroundTop = Color(0xFF0A1B3E);
  static const Color backgroundBottom = Color(0xFF030713);

  // ── Surfaces ─────────────────────────────────────────────────────────────────
  static const Color surface = Color(0xFF16243D);
  static const Color surfaceMuted = Color(0xFF101C31);
  static const Color surfaceSubtle = Color(0xFF0C1628); // deepest surface
  static const Color border = Color(0xFF2E4A78);

  // ── Accent ───────────────────────────────────────────────────────────────────
  static const Color accent = Color(0xFF2F82FF);
  static const Color accentStrong = Color(0xFF175FFF);
  static const Color accentSoft = Color(0x663486FF);

  // ── Semantic ─────────────────────────────────────────────────────────────────
  static const Color success = Color(0xFF4AC36B);
  static const Color warning = Color(0xFFF2A63A);
  static const Color danger = Color(0xFFE45C5C);
  static const Color orange = Color(0xFFE4895E); // warm orange (e.g. health events)

  // ── Semantic muted (swipe-action / status backgrounds) ───────────────────────
  static const Color successMuted = Color(0xFF1E5C2A); // complete swipe bg
  static const Color dangerMuted  = Color(0xFF612226); // delete swipe bg
  static const Color warningMuted = Color(0xFF57411D); // edit swipe bg

  // ── Tooltip / chart overlay ───────────────────────────────────────────────────
  static const Color tooltipBackground = Color(0xDD0F1C34);

  // ── Text (three levels) ──────────────────────────────────────────────────────
  static const Color textPrimary = Color(0xFFF3F7FF);
  static const Color textSecondary = Color(0xFFA8B9D6);
  static const Color textMuted = Color(0xFF74839A); // tertiary / metadata

  // ── Extended palette ─────────────────────────────────────────────────────────
  static const Color teal = Color(0xFF2AAE9D);
  static const Color violet = Color(0xFF6D77E8);
  static const Color slate = Color(0xFF5F7395);
  static const Color azure = Color(0xFF3A8AE8);   // tab accent (expenses)
  static const Color sky   = Color(0xFF3E91D6);   // tab accent (profile)

  // ── Glow colors (radial background atmosphere) ───────────────────────────────
  static const Color glowBlue = Color(0x3D4D9AFF);   // 24% alpha
  static const Color glowTeal = Color(0x2E26C4B6);   // 18% alpha
  static const Color glowViolet = Color(0x2E6D77E8); // 18% alpha
  static const Color glowAmber = Color(0x2EF2A63A);  // 18% alpha

  // ── Category colors (foreground) ─────────────────────────────────────────────
  static const Color categoryWork      = Color(0xFF4F8CFF);
  static const Color categoryGrowth    = Color(0xFF8B6DFF);
  static const Color categoryPersonal  = Color(0xFF2DCF91);
  static const Color categoryBill      = Color(0xFFF39A4D);
  static const Color categoryHealth    = Color(0xFFE45C5C);
  static const Color categoryOther     = Color(0xFF5F7395);
  static const Color categoryFood      = Color(0xFFFF8C5A);
  static const Color categoryAirtime   = Color(0xFFC66BFF);
  static const Color categoryTransport = Color(0xFF57B3FF);

  // ── Category muted backgrounds (for chips/avatars) ───────────────────────────
  static const Color categoryFoodBg      = Color(0xFF4B2F2B);
  static const Color categoryAirtimeBg   = Color(0xFF3B284F);
  static const Color categoryBillBg      = Color(0xFF3C2A4D);
  static const Color categoryTransportBg = Color(0xFF233A4F);

  /// Returns the foreground color for a named category (case-insensitive).
  static Color categoryColorFor(String category) {
    return switch (category.toLowerCase()) {
      'work' => categoryWork,
      'growth' || 'personal growth' => categoryGrowth,
      'personal' => categoryPersonal,
      'bill' || 'bills' || 'utilities' => categoryBill,
      'health' || 'medical' => categoryHealth,
      'food' || 'restaurant' || 'groceries' => categoryFood,
      'airtime' || 'mobile' || 'data' => categoryAirtime,
      'transport' || 'transit' || 'fuel' => categoryTransport,
      _ => categoryOther,
    };
  }

  // ── Brightness-aware helpers ─────────────────────────────────────────────────
  static Color backgroundFor(Brightness brightness) =>
      brightness == Brightness.light ? const Color(0xFFF2F6FC) : background;

  static Color surfaceFor(Brightness brightness) =>
      brightness == Brightness.light ? const Color(0xFFFFFFFF) : surface;

  static Color surfaceMutedFor(Brightness brightness) =>
      brightness == Brightness.light ? const Color(0xFFF1F5FB) : surfaceMuted;

  static Color surfaceSubtleFor(Brightness brightness) =>
      brightness == Brightness.light ? const Color(0xFFEBF1FA) : surfaceSubtle;

  static Color borderFor(Brightness brightness) =>
      brightness == Brightness.light ? const Color(0xFFB7C6DC) : border;

  static Color textPrimaryFor(Brightness brightness) =>
      brightness == Brightness.light ? const Color(0xFF11233F) : textPrimary;

  static Color textSecondaryFor(Brightness brightness) =>
      brightness == Brightness.light ? const Color(0xFF516584) : textSecondary;

  static Color textMutedFor(Brightness brightness) =>
      brightness == Brightness.light ? const Color(0xFF8AA0BF) : textMuted;
}
