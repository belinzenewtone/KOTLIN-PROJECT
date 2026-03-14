import 'package:beltech/core/di/sync_providers.dart';
import 'package:beltech/core/sync/sync_status_notifier.dart';
import 'package:beltech/core/theme/app_colors.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// A non-intrusive status pill that surfaces foreground sync activity on the
/// home screen.
///
/// Visibility rules:
/// • **syncing** — always visible; shows a micro spinner + "Syncing…"
/// • **done**    — visible for [SyncStatusNotifier._resetDelay]; shows ✓ + "Up to date"
/// • **failed**  — visible briefly; shows ⚠ + "Sync failed · Retry" (tappable)
/// • **idle**    — completely invisible (zero-height, not in the layout)
///
/// The widget cross-fades between visible and invisible states using
/// [AnimatedOpacity] so transitions feel smooth rather than jarring.
class HomeSyncBanner extends ConsumerWidget {
  const HomeSyncBanner({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final status = ref.watch(syncStatusProvider);
    final phase = status.phase;
    final visible = phase != SyncPhase.idle;

    return AnimatedOpacity(
      opacity: visible ? 1.0 : 0.0,
      duration: const Duration(milliseconds: 300),
      curve: Curves.easeInOut,
      child: AnimatedSize(
        duration: const Duration(milliseconds: 260),
        curve: Curves.easeInOut,
        child: visible
            ? Padding(
                padding: const EdgeInsets.only(bottom: 10),
                child: _SyncPill(status: status, ref: ref),
              )
            : const SizedBox.shrink(),
      ),
    );
  }
}

// ── Internal pill widget ───────────────────────────────────────────────────────

class _SyncPill extends StatelessWidget {
  const _SyncPill({required this.status, required this.ref});

  final SyncStatus status;
  final WidgetRef ref;

  @override
  Widget build(BuildContext context) {
    final (icon, label, color) = _content(status.phase);

    final pill = Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 5),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: color.withValues(alpha: 0.25), width: 0.8),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          _buildLeading(status.phase, color),
          const SizedBox(width: 6),
          Text(
            label,
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w500,
              color: color,
              height: 1.2,
            ),
          ),
          if (status.phase == SyncPhase.failed) ...[
            const SizedBox(width: 6),
            Text(
              '·',
              style: TextStyle(color: color, fontSize: 12),
            ),
            const SizedBox(width: 6),
            Text(
              'Retry',
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w700,
                color: color,
                height: 1.2,
              ),
            ),
          ],
        ],
      ),
    );

    if (status.phase == SyncPhase.failed) {
      return GestureDetector(
        onTap: () => ref.read(syncStatusProvider.notifier).runSync(),
        child: pill,
      );
    }
    return pill;
  }

  Widget _buildLeading(SyncPhase phase, Color color) {
    if (phase == SyncPhase.syncing) {
      return SizedBox(
        width: 12,
        height: 12,
        child: CircularProgressIndicator(
          strokeWidth: 1.5,
          valueColor: AlwaysStoppedAnimation<Color>(color),
        ),
      );
    }
    IconData iconData;
    switch (phase) {
      case SyncPhase.done:
        iconData = Icons.check_rounded;
      case SyncPhase.failed:
        iconData = Icons.warning_amber_rounded;
      default:
        iconData = Icons.sync_rounded;
    }
    return Icon(iconData, size: 13, color: color);
  }

  (IconData, String, Color) _content(SyncPhase phase) {
    return switch (phase) {
      SyncPhase.syncing => (Icons.sync_rounded, 'Syncing…', AppColors.accent),
      SyncPhase.done => (Icons.check_rounded, 'Up to date', AppColors.success),
      SyncPhase.failed =>
        (Icons.warning_amber_rounded, 'Sync failed', AppColors.danger),
      SyncPhase.idle =>
        (Icons.sync_rounded, '', AppColors.accent), // never rendered
    };
  }
}
