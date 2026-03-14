import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_spacing.dart';
import 'package:beltech/core/theme/app_typography.dart';
import 'package:beltech/core/utils/currency_formatter.dart';
import 'package:beltech/core/widgets/app_capsule.dart';
import 'package:beltech/core/widgets/app_empty_state.dart';
import 'package:beltech/core/widgets/app_feedback.dart';
import 'package:beltech/core/widgets/app_skeleton.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:beltech/core/widgets/secondary_page_shell.dart';
import 'package:beltech/features/recurring/domain/entities/recurring_template.dart';
import 'package:beltech/features/search/domain/entities/global_search_result.dart';
import 'package:beltech/features/search/presentation/providers/global_search_providers.dart';
import 'package:intl/intl.dart';
import 'package:beltech/features/recurring/presentation/providers/recurring_providers.dart';
import 'package:beltech/features/recurring/presentation/widgets/recurring_dialogs.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class RecurringScreen extends ConsumerWidget {
  const RecurringScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final templatesState = ref.watch(recurringTemplatesProvider);
    final writeState = ref.watch(recurringWriteControllerProvider);

    ref.listen<AsyncValue<void>>(recurringWriteControllerProvider,
        (previous, next) {
      if (next.hasError) {
        AppFeedback.error(
            context, 'Recurring action failed. Please try again.');
      }
    });

    return SecondaryPageShell(
      title: 'Recurring',
      glowColor: AppColors.glowTeal,
      actions: [
        IconButton(
          tooltip: 'Add template',
          icon: const Icon(Icons.add_rounded),
          onPressed: writeState.isLoading
              ? null
              : () async {
                  final input = await showRecurringTemplateDialog(context);
                  if (input == null) return;
                  await ref
                      .read(recurringWriteControllerProvider.notifier)
                      .addTemplate(
                        kind: input.kind,
                        title: input.title,
                        description: input.description,
                        category: input.category,
                        amountKes: input.amountKes,
                        priority: input.priority,
                        cadence: input.cadence,
                        nextRunAt: input.nextRunAt,
                      );
                  if (context.mounted &&
                      !ref.read(recurringWriteControllerProvider).hasError) {
                    AppFeedback.success(context, 'Template added');
                  }
                },
        ),
        IconButton(
          tooltip: 'Run now',
          icon: const Icon(Icons.play_arrow_rounded),
          onPressed: writeState.isLoading
              ? null
              : () async {
                  HapticFeedback.lightImpact();
                  final count = await ref
                      .read(recurringWriteControllerProvider.notifier)
                      .materializeNow();
                  if (context.mounted) {
                    AppFeedback.info(context, 'Generated $count item(s).');
                  }
                },
        ),
      ],
      child: templatesState.when(
        data: (templates) {
          _consumeSearchTarget(context, ref, templates);
          if (templates.isEmpty) {
            return AppEmptyState(
              icon: Icons.repeat_rounded,
              title: 'No recurring items',
              subtitle:
                  'Templates auto-generate tasks and expenses at specified intervals',
            );
          }
          return Column(
            children: List.generate(
              templates.length,
              (index) {
                final template = templates[index];
                return Padding(
                  padding: EdgeInsets.only(
                    bottom:
                        index < templates.length - 1 ? AppSpacing.listGap : 0,
                  ),
                  child: _RecurringRow(
                    template: template,
                    busy: writeState.isLoading,
                    onEdit: () async {
                      await _editTemplate(context, ref, template);
                    },
                    onDelete: () async {
                      await ref
                          .read(recurringWriteControllerProvider.notifier)
                          .deleteTemplate(template.id);
                      if (context.mounted &&
                          !ref
                              .read(recurringWriteControllerProvider)
                              .hasError) {
                        AppFeedback.success(context, 'Template deleted');
                      }
                    },
                  ),
                );
              },
            ),
          );
        },
        loading: () => Column(
          children: List.generate(
            4,
            (index) => Padding(
              padding: EdgeInsets.only(
                bottom: index < 3 ? AppSpacing.listGap : 0,
              ),
              child: AppSkeleton.card(context, height: 100),
            ),
          ),
        ),
        error: (_, __) => AppEmptyState(
          icon: Icons.error_outline,
          title: 'Unable to load templates',
          subtitle: 'Please try again',
          action: TextButton(
            onPressed: () => ref.invalidate(recurringTemplatesProvider),
            child: const Text('Retry'),
          ),
        ),
      ),
    );
  }

  void _consumeSearchTarget(
    BuildContext context,
    WidgetRef ref,
    List<RecurringTemplate> templates,
  ) {
    final target = ref.read(globalSearchDeepLinkTargetProvider);
    if (target?.kind != GlobalSearchKind.recurring) {
      return;
    }

    ref.read(globalSearchDeepLinkTargetProvider.notifier).state = null;

    final recordId = target?.recordId;
    if (recordId == null) {
      return;
    }

    final template = templates.where((item) => item.id == recordId).firstOrNull;
    if (template == null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (context.mounted) {
          AppFeedback.info(
              context, 'This recurring template no longer exists.');
        }
      });
      return;
    }

    WidgetsBinding.instance.addPostFrameCallback((_) async {
      if (!context.mounted) {
        return;
      }
      await _editTemplate(context, ref, template);
    });
  }

  Future<void> _editTemplate(
    BuildContext context,
    WidgetRef ref,
    RecurringTemplate template,
  ) async {
    final input = await showRecurringTemplateDialog(
      context,
      initial: template,
    );
    if (input == null) {
      return;
    }
    await ref.read(recurringWriteControllerProvider.notifier).updateTemplate(
          templateId: template.id,
          kind: input.kind,
          title: input.title,
          description: input.description,
          category: input.category,
          amountKes: input.amountKes,
          priority: input.priority,
          cadence: input.cadence,
          nextRunAt: input.nextRunAt,
          enabled: template.enabled,
        );
    if (context.mounted &&
        !ref.read(recurringWriteControllerProvider).hasError) {
      AppFeedback.success(context, 'Template updated');
    }
  }
}

class _RecurringRow extends StatelessWidget {
  const _RecurringRow({
    required this.template,
    required this.busy,
    required this.onEdit,
    required this.onDelete,
  });

  final RecurringTemplate template;
  final bool busy;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    final date = template.nextRunAt;
    final typeColor = AppColors.categoryColorFor(template.category ?? 'other');

    return GlassCard(
      tone: GlassCardTone.muted,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      template.title,
                      style: AppTypography.cardTitle(context),
                    ),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        AppCapsule(
                          label: template.kind.name,
                          color: typeColor,
                          variant: AppCapsuleVariant.subtle,
                          size: AppCapsuleSize.sm,
                        ),
                        const SizedBox(width: 8),
                        AppCapsule(
                          label: template.cadence.name,
                          color: AppColors.slate,
                          variant: AppCapsuleVariant.subtle,
                          size: AppCapsuleSize.sm,
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              if (template.amountKes != null)
                Text(
                  CurrencyFormatter.money(template.amountKes!),
                  style: AppTypography.amount(context),
                  maxLines: 1,
                  softWrap: false,
                  overflow: TextOverflow.fade,
                ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: Text(
                  'Next: ${DateFormat('MMM d, yyyy HH:mm').format(date)}',
                  style: AppTypography.bodySm(context),
                ),
              ),
              IconButton(
                onPressed: busy ? null : onEdit,
                icon: const Icon(Icons.edit_outlined),
                iconSize: 20,
              ),
              IconButton(
                onPressed: busy ? null : onDelete,
                icon: const Icon(Icons.delete_outline),
                iconSize: 20,
              ),
            ],
          ),
        ],
      ),
    );
  }
}
