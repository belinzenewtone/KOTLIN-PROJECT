import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_motion.dart';
import 'package:beltech/core/theme/app_spacing.dart';
import 'package:flutter/material.dart';

/// The unified page wrapper every tab screen and secondary screen uses.
///
/// Provides:
/// - Atmospheric radial glow at top-right (accent) + soft bottom-left (secondary)
/// - Optional scroll mode vs fixed Column
/// - Staggered fade+slide reveal on first load
/// - Safe-area aware top padding
/// - Consistent horizontal padding via [AppSpacing.screenHorizontal]
///
/// Usage:
/// ```dart
/// PageShell(
///   glowColor: AppColors.glowBlue,
///   child: Column(children: [...]),
/// )
/// ```
class PageShell extends StatefulWidget {
  const PageShell({
    super.key,
    required this.child,
    this.scrollable = true,
    this.glowColor,
    this.secondaryGlowColor,
    this.horizontalPadding = AppSpacing.screenHorizontal,
    this.topPadding = AppSpacing.screenTop,
    this.bottomPadding = AppSpacing.contentBottomSafe,
    this.controller,
  });

  final Widget child;
  final bool scrollable;
  final Color? glowColor;
  final Color? secondaryGlowColor;
  final double horizontalPadding;
  final double topPadding;
  final double bottomPadding;
  final ScrollController? controller;

  @override
  State<PageShell> createState() => _PageShellState();
}

class _PageShellState extends State<PageShell>
    with SingleTickerProviderStateMixin {
  late final AnimationController _revealCtrl;
  late final Animation<double> _opacity;
  late final Animation<Offset> _slide;

  @override
  void initState() {
    super.initState();
    _revealCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 240),
    );
    _opacity = CurvedAnimation(parent: _revealCtrl, curve: Curves.easeOut);
    _slide = Tween<Offset>(
      begin: const Offset(0, 0.03),
      end: Offset.zero,
    ).animate(CurvedAnimation(parent: _revealCtrl, curve: Curves.easeOutCubic));

    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) _revealCtrl.forward();
    });
  }

  @override
  void dispose() {
    _revealCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final reduceMotion = AppMotion.reduceMotion(context);
    final safeBottom = MediaQuery.paddingOf(context).bottom;
    final effectiveBottom =
        widget.bottomPadding + (safeBottom > 0 ? safeBottom * 0.6 : 0);

    final glow = widget.glowColor ?? AppColors.glowBlue;
    final secondaryGlow = widget.secondaryGlowColor ??
        glow.withValues(alpha: glow.a * 0.5);

    Widget content = widget.scrollable
        ? SingleChildScrollView(
            controller: widget.controller,
            padding: EdgeInsets.fromLTRB(
              widget.horizontalPadding,
              widget.topPadding,
              widget.horizontalPadding,
              effectiveBottom,
            ),
            child: widget.child,
          )
        : Padding(
            padding: EdgeInsets.fromLTRB(
              widget.horizontalPadding,
              widget.topPadding,
              widget.horizontalPadding,
              0,
            ),
            child: widget.child,
          );

    if (!reduceMotion) {
      content = FadeTransition(
        opacity: _opacity,
        child: SlideTransition(position: _slide, child: content),
      );
    }

    return Stack(
      children: [
        // Top-right accent glow
        Positioned(
          top: -60,
          right: -60,
          child: IgnorePointer(
            child: Container(
              width: 300,
              height: 300,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [glow, Colors.transparent],
                  radius: 0.8,
                ),
              ),
            ),
          ),
        ),
        // Bottom-left secondary glow
        Positioned(
          bottom: 100,
          left: -80,
          child: IgnorePointer(
            child: Container(
              width: 240,
              height: 240,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [secondaryGlow, Colors.transparent],
                  radius: 0.8,
                ),
              ),
            ),
          ),
        ),
        SafeArea(bottom: false, child: content),
      ],
    );
  }
}
