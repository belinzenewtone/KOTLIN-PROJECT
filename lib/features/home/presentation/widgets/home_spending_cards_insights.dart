part of 'home_spending_cards.dart';

/// Teaser card prompting the user to open the AI assistant.
class HomeAiInsightCard extends StatelessWidget {
  const HomeAiInsightCard({super.key});

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      tone: GlassCardTone.muted,
      padding: const EdgeInsets.all(14),
      child: Row(
        children: [
          Container(
            width: 36,
            height: 36,
            decoration: BoxDecoration(
              color: AppColors.violet.withValues(alpha: 0.18),
              shape: BoxShape.circle,
            ),
            child: const Icon(
              Icons.auto_awesome_rounded,
              color: AppColors.violet,
              size: 18,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        'AI Insight',
                        style: AppTypography.cardTitle(context),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    const SizedBox(width: 6),
                    AppCapsule(
                      label: 'NEW',
                      color: AppColors.violet,
                      variant: AppCapsuleVariant.subtle,
                      size: AppCapsuleSize.sm,
                    ),
                  ],
                ),
                const SizedBox(height: 3),
                Text(
                  'Ask your assistant for spending tips, task priorities, or weekly summaries.',
                  style: AppTypography.bodySm(context),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
          const Icon(
            Icons.chevron_right_rounded,
            color: AppColors.textMuted,
            size: 20,
          ),
        ],
      ),
    );
  }
}

/// Task completion progress bar card.
class HomeProductivityCard extends StatelessWidget {
  const HomeProductivityCard({super.key, required this.overview});
  final HomeOverview overview;

  @override
  Widget build(BuildContext context) {
    final total = overview.completedCount + overview.pendingCount;
    final progress = total == 0 ? 0.0 : overview.completedCount / total;
    final brightness = Theme.of(context).brightness;

    return GlassCard(
      child: Row(
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              color: AppColors.teal.withValues(alpha: 0.18),
              shape: BoxShape.circle,
            ),
            child: const Icon(
              Icons.check_circle_rounded,
              color: AppColors.teal,
              size: 22,
            ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Productivity', style: AppTypography.cardTitle(context)),
                const SizedBox(height: 4),
                ClipRRect(
                  borderRadius: BorderRadius.circular(4),
                  child: LinearProgressIndicator(
                    value: progress,
                    minHeight: 4,
                    backgroundColor:
                        AppColors.borderFor(brightness).withValues(alpha: 0.3),
                    valueColor:
                        const AlwaysStoppedAnimation<Color>(AppColors.teal),
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  '${overview.completedCount} done · ${overview.pendingCount} pending',
                  style: AppTypography.bodySm(context),
                  maxLines: 1,
                  softWrap: false,
                  overflow: TextOverflow.fade,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
