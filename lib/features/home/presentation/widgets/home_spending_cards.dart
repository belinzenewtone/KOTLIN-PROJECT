import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_spacing.dart';
import 'package:beltech/core/theme/app_typography.dart';
import 'package:beltech/core/utils/currency_formatter.dart';
import 'package:beltech/core/widgets/app_capsule.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:beltech/features/home/domain/entities/home_overview.dart';
import 'package:flutter/material.dart';

part 'home_spending_cards_balance.dart';
part 'home_spending_cards_insights.dart';

/// Two-tile strip: Today spend + This Week spend.
class HomeSpendSnapshotStrip extends StatelessWidget {
  const HomeSpendSnapshotStrip({super.key, required this.overview});
  final HomeOverview overview;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: GlassCard(
            tone: GlassCardTone.accent,
            accentColor: AppColors.accent,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Today', style: AppTypography.bodySm(context)),
                const SizedBox(height: 4),
                FittedBox(
                  fit: BoxFit.scaleDown,
                  alignment: Alignment.centerLeft,
                  child: Text(
                    CurrencyFormatter.money(overview.todayKes),
                    style: AppTypography.amount(context),
                    maxLines: 1,
                    softWrap: false,
                  ),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(width: AppSpacing.cardGap),
        Expanded(
          child: GlassCard(
            tone: GlassCardTone.accent,
            accentColor: AppColors.violet,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('This Week', style: AppTypography.bodySm(context)),
                const SizedBox(height: 4),
                FittedBox(
                  fit: BoxFit.scaleDown,
                  alignment: Alignment.centerLeft,
                  child: Text(
                    CurrencyFormatter.money(overview.weekKes),
                    style: AppTypography.amount(context),
                    maxLines: 1,
                    softWrap: false,
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

/// Single transaction row used in the dashboard recent transactions list.
class HomeDashboardTransactionTile extends StatelessWidget {
  const HomeDashboardTransactionTile({super.key, required this.tx});
  final HomeTransaction tx;

  @override
  Widget build(BuildContext context) {
    final catColor = AppColors.categoryColorFor(tx.category);
    return GlassCard(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: Row(
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: catColor.withValues(alpha: 0.18),
              shape: BoxShape.circle,
            ),
            child: Icon(Icons.payments_outlined, color: catColor, size: 20),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(tx.title,
                    style: AppTypography.cardTitle(context),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis),
                Text(
                  tx.category,
                  style: AppTypography.bodySm(context),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
          Text(
            CurrencyFormatter.money(tx.amountKes),
            style: AppTypography.bodyMd(context)
                .copyWith(fontWeight: FontWeight.w600),
            maxLines: 1,
            softWrap: false,
            overflow: TextOverflow.fade,
          ),
        ],
      ),
    );
  }
}
