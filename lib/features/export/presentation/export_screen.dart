import 'package:beltech/core/widgets/glass_card.dart';
import 'package:beltech/core/theme/app_spacing.dart';
import 'package:beltech/core/widgets/app_feedback.dart';
import 'package:beltech/features/export/domain/entities/export_result.dart';
import 'package:beltech/features/export/presentation/providers/export_providers.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:share_plus/share_plus.dart';

class ExportScreen extends ConsumerWidget {
  const ExportScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final exportState = ref.watch(exportControllerProvider);
    final latestResult = exportState.valueOrNull;

    ref.listen<AsyncValue<ExportResult?>>(exportControllerProvider,
        (previous, next) {
      if (next.hasError) {
        AppFeedback.error(
            context, '${next.error}'.replaceFirst('Exception: ', ''));
      } else if (previous is AsyncLoading && next.hasValue) {
        final result = next.valueOrNull;
        if (result != null) {
          AppFeedback.success(
              context, 'Export complete: ${result.rowsExported} row(s).');
        }
      }
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('Export Data'),
      ),
      body: SafeArea(
        child: ListView(
          padding: AppSpacing.sectionPadding(context),
          children: [
            const GlassCard(
              child: Text(
                'Create CSV exports for backup and sharing. Files are saved in the app documents directory.',
              ),
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 10,
              runSpacing: 10,
              children: ExportScope.values
                  .map(
                    (scope) => FilledButton(
                      onPressed: exportState.isLoading
                          ? null
                          : () async {
                              await ref
                                  .read(exportControllerProvider.notifier)
                                  .export(scope);
                            },
                      child: Text(_labelFor(scope)),
                    ),
                  )
                  .toList(),
            ),
            if (latestResult != null) ...[
              const SizedBox(height: 16),
              GlassCard(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('Latest Export'),
                    const SizedBox(height: 8),
                    Text('Scope: ${_labelFor(latestResult.scope)}'),
                    Text('Rows: ${latestResult.rowsExported}'),
                    const SizedBox(height: 10),
                    SizedBox(
                      width: double.infinity,
                      child: FilledButton.icon(
                        onPressed: exportState.isLoading
                            ? null
                            : () async {
                                await Share.shareXFiles(
                                  [XFile(latestResult.filePath)],
                                  subject:
                                      'BELTECH Export – ${_labelFor(latestResult.scope)}',
                                );
                              },
                        icon: const Icon(Icons.share_outlined),
                        label: const Text('Share File'),
                      ),
                    ),
                    const SizedBox(height: 6),
                    SelectableText(
                      latestResult.filePath,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  String _labelFor(ExportScope scope) {
    return switch (scope) {
      ExportScope.all => 'Export All',
      ExportScope.expenses => 'Expenses',
      ExportScope.incomes => 'Incomes',
      ExportScope.tasks => 'Tasks',
      ExportScope.events => 'Events',
      ExportScope.budgets => 'Budgets',
      ExportScope.recurring => 'Recurring',
    };
  }
}
