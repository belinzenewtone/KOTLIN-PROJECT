import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_motion.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:flutter/material.dart';

class ExpenseTransactionRow extends StatelessWidget {
  const ExpenseTransactionRow({
    super.key,
    required this.dismissKey,
    required this.title,
    required this.subtitle,
    required this.amount,
    required this.onEdit,
    required this.onDelete,
    required this.busy,
  });

  final String dismissKey;
  final String title;
  final String subtitle;
  final String amount;
  final VoidCallback onEdit;
  final VoidCallback onDelete;
  final bool busy;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final swipeDuration = AppMotion.swipe(context);
    final resizeDuration = AppMotion.resize(context);
    return Dismissible(
      key: ValueKey(dismissKey),
      direction: busy ? DismissDirection.none : DismissDirection.horizontal,
      movementDuration: swipeDuration,
      resizeDuration: resizeDuration,
      dismissThresholds: const {
        DismissDirection.startToEnd: 0.4,
        DismissDirection.endToStart: 0.4,
      },
      confirmDismiss: (direction) async {
        if (direction == DismissDirection.startToEnd) {
          onEdit();
          return false;
        }
        if (direction == DismissDirection.endToStart) {
          onDelete();
          return false;
        }
        return false;
      },
      background: const _ExpenseSwipeBackground(
        color: AppColors.warningMuted,
        icon: Icons.edit_outlined,
        alignment: Alignment.centerLeft,
      ),
      secondaryBackground: const _ExpenseSwipeBackground(
        color: AppColors.dangerMuted,
        icon: Icons.delete_outline,
        alignment: Alignment.centerRight,
      ),
      child: GlassCard(
        child: Row(
          children: [
            const CircleAvatar(
              backgroundColor: AppColors.accentSoft,
              child: Icon(Icons.more_horiz),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: textTheme.bodyLarge,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  Text(
                    subtitle,
                    style: textTheme.bodyMedium,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
            ConstrainedBox(
              constraints: const BoxConstraints(minWidth: 68, maxWidth: 104),
              child: FittedBox(
                fit: BoxFit.scaleDown,
                alignment: Alignment.centerRight,
                child: Text(
                  amount,
                  style: textTheme.bodyLarge,
                  maxLines: 1,
                  softWrap: false,
                  overflow: TextOverflow.fade,
                  textAlign: TextAlign.right,
                ),
              ),
            ),
            IconButton(
              onPressed: busy ? null : onEdit,
              icon: const Icon(Icons.edit_outlined),
            ),
            IconButton(
              onPressed: busy ? null : onDelete,
              icon: const Icon(Icons.delete_outline, color: AppColors.danger),
            ),
          ],
        ),
      ),
    );
  }
}

class _ExpenseSwipeBackground extends StatelessWidget {
  const _ExpenseSwipeBackground({
    required this.color,
    required this.icon,
    required this.alignment,
  });

  final Color color;
  final IconData icon;
  final Alignment alignment;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(22),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 22),
      alignment: alignment,
      child: Icon(icon, color: Colors.white, size: 28),
    );
  }
}
