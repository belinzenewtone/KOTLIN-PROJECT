part of 'settings_screen.dart';

enum _DataModeSwitchDecision {
  cancel,
  switchOnly,
  migrateAndSwitch,
}

Future<void> _applyDataMode({
  required BuildContext context,
  required WidgetRef ref,
  required DataModePreference mode,
  required bool cloudAvailable,
}) async {
  final currentMode = ref.read(preferredDataModeProvider);
  if (currentMode == mode) {
    return;
  }

  if (mode == DataModePreference.cloud && !cloudAvailable) {
    AppFeedback.error(
      context,
      'Cloud mode is unavailable on this build. Configure Supabase first.',
    );
    return;
  }

  final fromMode =
      currentMode == DataModePreference.cloud ? DataStoreMode.cloud : DataStoreMode.local;
  final toMode = mode == DataModePreference.cloud ? DataStoreMode.cloud : DataStoreMode.local;
  final migrationService = ref.read(dataModeMigrationServiceProvider);

  DataMigrationPreview preview;
  try {
    preview = await migrationService.preview(
      fromMode: fromMode,
      toMode: toMode,
      cloudAvailable: cloudAvailable,
    );
  } catch (error) {
    if (context.mounted) {
      AppFeedback.error(context, 'Unable to prepare mode switch: $error');
    }
    return;
  }

  if (!context.mounted) {
    return;
  }

  final decision = await _showModeSwitchDialog(
    context: context,
    fromMode: currentMode,
    toMode: mode,
    preview: preview,
  );

  if (!context.mounted ||
      decision == null ||
      decision == _DataModeSwitchDecision.cancel) {
    return;
  }

  ref.read(_dataModeSwitchBusyProvider.notifier).state = true;
  try {
    DataMigrationResult? result;
    if (decision == _DataModeSwitchDecision.migrateAndSwitch) {
      result = await migrationService.migrate(
        fromMode: fromMode,
        toMode: toMode,
        cloudAvailable: cloudAvailable,
      );
    }

    await persistDataModePreference(mode);
    ref.read(preferredDataModeProvider.notifier).state = mode;
    _invalidateModeBoundProviders(ref);

    if (!context.mounted) {
      return;
    }

    if (result != null && result.changed) {
      AppFeedback.success(
        context,
        'Switched to ${_modeLabel(mode)} and migrated ${result.insertedTotal + result.updatedTotal} records.',
      );
    } else {
      AppFeedback.success(context, 'Switched to ${_modeLabel(mode)}.');
    }
  } catch (error) {
    if (context.mounted) {
      AppFeedback.error(context, 'Failed to switch mode: $error');
    }
  } finally {
    ref.read(_dataModeSwitchBusyProvider.notifier).state = false;
  }
}

void _invalidateModeBoundProviders(WidgetRef ref) {
  ref.invalidate(useSupabaseProvider);
  ref.invalidate(homeRepositoryProvider);
  ref.invalidate(calendarRepositoryProvider);
  ref.invalidate(expensesRepositoryProvider);
  ref.invalidate(incomeRepositoryProvider);
  ref.invalidate(budgetRepositoryProvider);
  ref.invalidate(recurringRepositoryProvider);
  ref.invalidate(globalSearchRepositoryProvider);
  ref.invalidate(exportRepositoryProvider);
  ref.invalidate(tasksRepositoryProvider);
  ref.invalidate(accountRepositoryProvider);
  ref.invalidate(accountSessionProvider);
  ref.invalidate(accountAuthControllerProvider);
  ref.invalidate(assistantRepositoryProvider);
  ref.invalidate(analyticsRepositoryProvider);
  ref.invalidate(profileRepositoryProvider);
  ref.invalidate(backgroundSyncCoordinatorProvider);
  ref.invalidate(syncStatusProvider);
  ref.invalidate(appUpdateServiceProvider);
}

Future<_DataModeSwitchDecision?> _showModeSwitchDialog({
  required BuildContext context,
  required DataModePreference fromMode,
  required DataModePreference toMode,
  required DataMigrationPreview preview,
}) {
  final canMigrate = preview.canMigrate && preview.sourceTotal > 0;
  final sourceSummary = _scopeSummary(preview.sourceCounts);
  final destinationSummary = _scopeSummary(preview.destinationCounts);

  return showAppDialog<_DataModeSwitchDecision>(
    context: context,
    builder: (_) {
      return AlertDialog(
        title: Text('Switch to ${_modeLabel(toMode)}?'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'You are switching from ${_modeLabel(fromMode)} to ${_modeLabel(toMode)}.',
                style: AppTypography.bodySm(context),
              ),
              const SizedBox(height: 10),
              Text(
                '${_modeLabel(fromMode)} records: ${preview.sourceTotal} ($sourceSummary)',
                style: AppTypography.bodySm(context),
              ),
              const SizedBox(height: 4),
              Text(
                '${_modeLabel(toMode)} records: ${preview.destinationTotal} ($destinationSummary)',
                style: AppTypography.bodySm(context),
              ),
              if (preview.blockerMessage != null) ...[
                const SizedBox(height: 10),
                Text(
                  preview.blockerMessage!,
                  style: AppTypography.bodySm(context)
                      .copyWith(color: AppColors.warning),
                ),
              ],
              const SizedBox(height: 10),
              Text(
                canMigrate
                    ? 'You can migrate data during the switch, or switch modes without migration.'
                    : 'You can still switch modes without migration.',
                style: AppTypography.bodySm(context),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(_DataModeSwitchDecision.cancel),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () =>
                Navigator.of(context).pop(_DataModeSwitchDecision.switchOnly),
            child: const Text('Switch Only'),
          ),
          if (canMigrate)
            FilledButton(
              onPressed: () => Navigator.of(context)
                  .pop(_DataModeSwitchDecision.migrateAndSwitch),
              child: const Text('Migrate + Switch'),
            ),
        ],
      );
    },
  );
}

String _modeLabel(DataModePreference mode) {
  return mode == DataModePreference.cloud ? 'Cloud mode' : 'Offline mode';
}

String _scopeSummary(Map<String, int> counts) {
  final chunks = <String>[];
  for (final entry in counts.entries) {
    if (entry.value <= 0) {
      continue;
    }
    chunks.add('${entry.key}:${entry.value}');
    if (chunks.length == 3) {
      break;
    }
  }
  if (chunks.isEmpty) {
    return 'empty';
  }
  return chunks.join(', ');
}
