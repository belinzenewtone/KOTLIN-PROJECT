import 'dart:async';
import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_radius.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

// ── Model ────────────────────────────────────────────────────────────────────

enum ToastType { success, error, info, warning }

class ToastMessage {
  const ToastMessage({
    required this.id,
    required this.message,
    required this.type,
  });
  final int id;
  final String message;
  final ToastType type;
}

// ── Provider ─────────────────────────────────────────────────────────────────

class ToastNotifier extends Notifier<List<ToastMessage>> {
  int _nextId = 0;

  @override
  List<ToastMessage> build() => [];

  void show(String message, {ToastType type = ToastType.info}) {
    final id = _nextId++;
    state = [...state, ToastMessage(id: id, message: message, type: type)];
    Future.delayed(const Duration(milliseconds: 3400), () {
      dismiss(id);
    });
  }

  void success(String message) => show(message, type: ToastType.success);
  void error(String message) => show(message, type: ToastType.error);
  void info(String message) => show(message, type: ToastType.info);
  void warning(String message) => show(message, type: ToastType.warning);

  void dismiss(int id) {
    state = state.where((t) => t.id != id).toList();
  }
}

final toastProvider =
    NotifierProvider<ToastNotifier, List<ToastMessage>>(ToastNotifier.new);

// ── Overlay widget ────────────────────────────────────────────────────────────

/// Place this once at the top of AppShell's Stack to display toast messages.
class AppToastOverlay extends ConsumerWidget {
  const AppToastOverlay({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final toasts = ref.watch(toastProvider);
    if (toasts.isEmpty) return const SizedBox.shrink();

    return Positioned(
      top: MediaQuery.paddingOf(context).top + 12,
      left: 16,
      right: 16,
      child: Column(
        children: toasts
            .map((t) => _ToastItem(key: ValueKey(t.id), toast: t))
            .toList(),
      ),
    );
  }
}

class _ToastItem extends ConsumerStatefulWidget {
  const _ToastItem({super.key, required this.toast});
  final ToastMessage toast;

  @override
  ConsumerState<_ToastItem> createState() => _ToastItemState();
}

class _ToastItemState extends ConsumerState<_ToastItem>
    with SingleTickerProviderStateMixin {
  late final AnimationController _ctrl;
  late final Animation<double> _opacity;
  late final Animation<Offset> _slide;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 240),
    );
    _opacity = CurvedAnimation(parent: _ctrl, curve: Curves.easeOut);
    _slide = Tween<Offset>(
      begin: const Offset(0, -0.3),
      end: Offset.zero,
    ).animate(CurvedAnimation(parent: _ctrl, curve: Curves.easeOutCubic));
    _ctrl.forward();
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  Color get _barColor => switch (widget.toast.type) {
    ToastType.success => AppColors.success,
    ToastType.error => AppColors.danger,
    ToastType.warning => AppColors.warning,
    ToastType.info => AppColors.accent,
  };

  IconData get _icon => switch (widget.toast.type) {
    ToastType.success => Icons.check_circle_outline_rounded,
    ToastType.error => Icons.error_outline_rounded,
    ToastType.warning => Icons.warning_amber_rounded,
    ToastType.info => Icons.info_outline_rounded,
  };

  @override
  Widget build(BuildContext context) {
    final brightness = Theme.of(context).brightness;
    final bg = brightness == Brightness.light
        ? const Color(0xFFF3F8FF)
        : AppColors.surface;

    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: FadeTransition(
        opacity: _opacity,
        child: SlideTransition(
          position: _slide,
          child: Material(
            color: Colors.transparent,
            child: Container(
              decoration: BoxDecoration(
                color: bg,
                borderRadius: BorderRadius.circular(AppRadius.lg),
                border: Border.all(
                  color: AppColors.borderFor(brightness).withValues(alpha: 0.4),
                ),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.12),
                    blurRadius: 16,
                    offset: const Offset(0, 6),
                  ),
                ],
              ),
              child: Row(
                children: [
                  Container(
                    width: 4,
                    height: 48,
                    decoration: BoxDecoration(
                      color: _barColor,
                      borderRadius: const BorderRadius.only(
                        topLeft: Radius.circular(AppRadius.lg),
                        bottomLeft: Radius.circular(AppRadius.lg),
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Icon(_icon, color: _barColor, size: 20),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Padding(
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      child: Text(
                        widget.toast.message,
                        maxLines: 3,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w500,
                          color: AppColors.textPrimaryFor(brightness),
                          height: 1.4,
                        ),
                      ),
                    ),
                  ),
                  IconButton(
                    icon: Icon(
                      Icons.close,
                      size: 16,
                      color: AppColors.textMutedFor(brightness),
                    ),
                    onPressed: () =>
                        ref.read(toastProvider.notifier).dismiss(widget.toast.id),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
