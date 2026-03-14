import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_radius.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// The custom animated bottom tab bar with a sliding accent pill.
///
/// The pill background slides between tabs using [AnimationController].
/// Selected tab shows icon (accent coloured) + label.
/// Unselected tabs show icon only at muted opacity.
class AppTabBar extends StatefulWidget {
  const AppTabBar({
    super.key,
    required this.selectedIndex,
    required this.onTap,
    required this.items,
    this.height = 66,
  });

  final int selectedIndex;
  final ValueChanged<int> onTap;
  final List<AppTabItem> items;
  final double height;

  @override
  State<AppTabBar> createState() => _AppTabBarState();
}

class _AppTabBarState extends State<AppTabBar>
    with SingleTickerProviderStateMixin {
  late AnimationController _pillCtrl;
  late Animation<double> _pillPos;
  double _prevFraction = 0;

  @override
  void initState() {
    super.initState();
    _pillCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 150),
    );
    _prevFraction = widget.selectedIndex / (widget.items.length - 1);
    _pillPos = AlwaysStoppedAnimation(_prevFraction);
  }

  @override
  void didUpdateWidget(AppTabBar old) {
    super.didUpdateWidget(old);
    if (old.selectedIndex != widget.selectedIndex) {
      final targetFraction = widget.selectedIndex / (widget.items.length - 1);
      _pillPos = Tween<double>(
        begin: _prevFraction,
        end: targetFraction,
      ).animate(
        CurvedAnimation(parent: _pillCtrl, curve: Curves.easeOutCubic),
      );
      _prevFraction = targetFraction;
      _pillCtrl
        ..reset()
        ..forward();
    }
  }

  @override
  void dispose() {
    _pillCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final brightness = Theme.of(context).brightness;
    final accent = Theme.of(context).colorScheme.primary;
    final mutedColor = AppColors.textMutedFor(brightness);
    final count = widget.items.length;
    final textScale = MediaQuery.textScalerOf(context).scale(1);
    final resolvedHeight =
        widget.height + ((textScale - 1) * 18).clamp(0.0, 14.0);

    return GlassCard(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 6),
      borderRadius: AppRadius.xxl,
      child: SizedBox(
        height: resolvedHeight,
        child: LayoutBuilder(
          builder: (context, constraints) {
            final totalWidth = constraints.maxWidth;
            final itemWidth = totalWidth / count;

            return AnimatedBuilder(
              animation: _pillPos,
              builder: (context, _) {
                final pillLeft = _pillPos.value * (totalWidth - itemWidth);
                return Stack(
                  children: [
                    // Sliding pill background
                    Positioned(
                      left: pillLeft,
                      top: 0,
                      bottom: 0,
                      width: itemWidth,
                      child: Container(
                        decoration: BoxDecoration(
                          color: accent.withValues(alpha: 0.18),
                          borderRadius: BorderRadius.circular(AppRadius.xl),
                          border: Border.all(
                            color: accent.withValues(alpha: 0.30),
                            width: 1,
                          ),
                        ),
                      ),
                    ),
                    // Tab buttons
                    Row(
                      children: List.generate(count, (i) {
                        final item = widget.items[i];
                        final selected = i == widget.selectedIndex;
                        final iconColor = selected ? accent : mutedColor;
                        return Expanded(
                          child: GestureDetector(
                            behavior: HitTestBehavior.opaque,
                            onTap: () {
                              HapticFeedback.lightImpact();
                              widget.onTap(i);
                            },
                            child: AnimatedScale(
                              scale: selected ? 1.0 : 0.95,
                              duration: const Duration(milliseconds: 120),
                              curve: Curves.easeOutBack,
                              child: Column(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  SizedBox(
                                    height: 24,
                                    child: Center(
                                      child: AnimatedSwitcher(
                                        duration:
                                            const Duration(milliseconds: 120),
                                        child: Icon(
                                          selected
                                              ? item.selectedIcon
                                              : item.icon,
                                          key: ValueKey(selected),
                                          color: iconColor,
                                          size: 22,
                                        ),
                                      ),
                                    ),
                                  ),
                                  const SizedBox(height: 2),
                                  SizedBox(
                                    height: 13,
                                    child: AnimatedOpacity(
                                      duration:
                                          const Duration(milliseconds: 120),
                                      opacity: selected ? 1 : 0,
                                      child: SizedBox(
                                        width: itemWidth - 8,
                                        child: Text(
                                          item.label,
                                          style: TextStyle(
                                            fontSize: 10,
                                            fontWeight: FontWeight.w600,
                                            color: accent,
                                            height: 1.1,
                                          ),
                                          textAlign: TextAlign.center,
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                        ),
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        );
                      }),
                    ),
                  ],
                );
              },
            );
          },
        ),
      ),
    );
  }
}

class AppTabItem {
  const AppTabItem({
    required this.label,
    required this.icon,
    required this.selectedIcon,
  });

  final String label;
  final IconData icon;
  final IconData selectedIcon;
}
