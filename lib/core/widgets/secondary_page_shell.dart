import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_spacing.dart';
import 'package:beltech/core/theme/app_typography.dart';
import 'package:beltech/core/theme/glass_styles.dart';
import 'package:flutter/material.dart';

/// Consistent wrapper for all secondary (non-tab) screens.
///
/// Renders a back-arrow header, the same atmospheric glow as [PageShell],
/// and consistent horizontal padding.
class SecondaryPageShell extends StatelessWidget {
  const SecondaryPageShell({
    super.key,
    required this.title,
    required this.child,
    this.scrollable = true,
    this.actions,
    this.glowColor,
  });

  final String title;
  final Widget child;
  final bool scrollable;
  final List<Widget>? actions;
  final Color? glowColor;

  @override
  Widget build(BuildContext context) {
    final brightness = Theme.of(context).brightness;
    final glow = glowColor ?? AppColors.glowBlue;

    return DecoratedBox(
      decoration: BoxDecoration(
        gradient: GlassStyles.backgroundGradientFor(brightness),
      ),
      child: Stack(
        children: [
          // Atmosphere glow
          Positioned(
            top: -40,
            right: -40,
            child: IgnorePointer(
              child: Container(
                width: 260,
                height: 260,
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
          Scaffold(
            backgroundColor: Colors.transparent,
            appBar: AppBar(
              backgroundColor: Colors.transparent,
              elevation: 0,
              leading: IconButton(
                icon: const Icon(Icons.arrow_back_ios_new_rounded, size: 20),
                onPressed: () => Navigator.of(context).maybePop(),
              ),
              title: Text(title, style: AppTypography.sectionTitle(context)),
              actions: actions,
            ),
            body: scrollable
                ? SingleChildScrollView(
                    padding: EdgeInsets.fromLTRB(
                      AppSpacing.screenHorizontal,
                      8,
                      AppSpacing.screenHorizontal,
                      AppSpacing.contentBottomSafe,
                    ),
                    child: child,
                  )
                : Padding(
                    padding: EdgeInsets.fromLTRB(
                      AppSpacing.screenHorizontal,
                      8,
                      AppSpacing.screenHorizontal,
                      0,
                    ),
                    child: child,
                  ),
          ),
        ],
      ),
    );
  }
}
