import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/utils/currency_formatter.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:beltech/features/analytics/domain/entities/analytics_snapshot.dart';
import 'package:flutter/material.dart';

class AnalyticsCategoryBreakdown extends StatelessWidget {
  const AnalyticsCategoryBreakdown({
    super.key,
    required this.categories,
  });

  final List<AnalyticsCategoryShare> categories;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Category Breakdown', style: textTheme.titleMedium),
          const SizedBox(height: 12),
          if (categories.isEmpty)
            const Text('No spending data available.')
          else
            for (final entry in categories.take(8)) ...[
              _CategoryRow(entry: entry),
              const SizedBox(height: 10),
            ],
        ],
      ),
    );
  }
}

class _CategoryRow extends StatelessWidget {
  const _CategoryRow({required this.entry});

  final AnalyticsCategoryShare entry;

  @override
  Widget build(BuildContext context) {
    final visual = _categoryVisual(entry.category);
    final ratio = (entry.percentage / 100).clamp(0.0, 1.0).toDouble();
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        CircleAvatar(
          radius: 16,
          backgroundColor: visual.background,
          child: Icon(visual.icon, color: visual.foreground, size: 16),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                entry.category,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: 6),
              ClipRRect(
                borderRadius: BorderRadius.circular(99),
                child: SizedBox(
                  height: 4,
                  child: LinearProgressIndicator(
                    value: ratio,
                    backgroundColor:
                        AppColors.surfaceMuted.withValues(alpha: 0.7),
                    valueColor:
                        AlwaysStoppedAnimation<Color>(visual.foreground),
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(width: 10),
        SizedBox(
          width: 108,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(
                CurrencyFormatter.money(entry.totalKes),
                maxLines: 1,
                softWrap: false,
                overflow: TextOverflow.fade,
              ),
              Text(
                '${entry.percentage.toStringAsFixed(1)}%',
                maxLines: 1,
                softWrap: false,
                overflow: TextOverflow.fade,
              ),
            ],
          ),
        ),
      ],
    );
  }
}

({IconData icon, Color foreground, Color background}) _categoryVisual(
  String category,
) {
  final normalized = category.trim().toLowerCase();
  if (normalized.contains('food')) {
    return (
      icon: Icons.restaurant_outlined,
      foreground: AppColors.categoryFood,
      background: AppColors.categoryFoodBg,
    );
  }
  if (normalized.contains('airtime')) {
    return (
      icon: Icons.phone_android_outlined,
      foreground: AppColors.categoryAirtime,
      background: AppColors.categoryAirtimeBg,
    );
  }
  if (normalized.contains('bill')) {
    return (
      icon: Icons.receipt_long_outlined,
      foreground: AppColors.categoryAirtime,
      background: AppColors.categoryBillBg,
    );
  }
  if (normalized.contains('transport')) {
    return (
      icon: Icons.directions_bus_outlined,
      foreground: AppColors.categoryTransport,
      background: AppColors.categoryTransportBg,
    );
  }
  return (
    icon: Icons.more_horiz,
    foreground: AppColors.textSecondary,
    background: AppColors.accentSoft,
  );
}
