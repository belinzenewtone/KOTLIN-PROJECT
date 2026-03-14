part of 'expenses_snapshot_content.dart';

class _SummaryCard extends StatelessWidget {
  const _SummaryCard({
    required this.title,
    required this.amount,
    this.tone = GlassCardTone.standard,
    this.accentColor,
  });

  final String title;
  final String amount;
  final GlassCardTone tone;
  final Color? accentColor;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return GlassCard(
      tone: tone,
      accentColor: accentColor,
      child: SizedBox(
        height: 72,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: textTheme.bodyMedium),
            const SizedBox(height: 6),
            Expanded(
              child: Align(
                alignment: Alignment.centerLeft,
                child: FittedBox(
                  fit: BoxFit.scaleDown,
                  alignment: Alignment.centerLeft,
                  child: Text(
                    amount,
                    style: textTheme.titleMedium,
                    maxLines: 1,
                    softWrap: false,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _CategoryCard extends StatelessWidget {
  const _CategoryCard({required this.categories});

  final List<CategoryExpenseTotal> categories;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final total =
        categories.fold<double>(0, (sum, item) => sum + item.totalKes);

    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Categories', style: textTheme.titleMedium),
          const SizedBox(height: 12),
          for (final entry in categories.take(8)) ...[
            _CategoryRow(
              name: entry.category,
              amount: CurrencyFormatter.money(entry.totalKes),
              ratio: total <= 0 ? 0 : entry.totalKes / total,
            ),
            const SizedBox(height: 10),
          ],
        ],
      ),
    );
  }
}

class _CategoryRow extends StatelessWidget {
  const _CategoryRow({
    required this.name,
    required this.amount,
    required this.ratio,
  });

  final String name;
  final String amount;
  final double ratio;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final visual = _categoryVisual(name);

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
                name,
                style: textTheme.bodyLarge,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: 6),
              ClipRRect(
                borderRadius: BorderRadius.circular(100),
                child: SizedBox(
                  height: 4,
                  child: LinearProgressIndicator(
                    value: ratio.clamp(0, 1),
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
        Text(
          amount,
          style: textTheme.bodyLarge,
          maxLines: 1,
          softWrap: false,
          overflow: TextOverflow.fade,
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
