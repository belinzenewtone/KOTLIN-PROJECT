import 'dart:io';

import 'package:beltech/data/local/drift/app_drift_store.dart';
import 'package:beltech/features/export/data/services/csv_builder.dart';
import 'package:beltech/features/export/domain/entities/export_result.dart';
import 'package:beltech/features/export/domain/repositories/export_repository.dart';
import 'package:flutter/foundation.dart';
import 'package:path_provider/path_provider.dart';

class ExportRepositoryImpl implements ExportRepository {
  ExportRepositoryImpl(
    this._store, {
    CsvBuilder csvBuilder = const CsvBuilder(),
  }) : _csvBuilder = csvBuilder;

  final AppDriftStore _store;
  final CsvBuilder _csvBuilder;

  @override
  Future<ExportResult> exportCsv({
    required ExportScope scope,
  }) async {
    if (kIsWeb) {
      throw Exception('CSV export is not supported on web builds.');
    }
    await _store.ensureInitialized();
    final exports = await _buildScopedExports(scope);
    final sections = <String>[];
    var totalRows = 0;
    for (final export in exports) {
      sections.add('## ${export.name}');
      sections.add(export.csv);
      totalRows += export.rows;
    }
    final content = sections.join('\n');
    final dir = await getApplicationDocumentsDirectory();
    final stamp = DateTime.now().millisecondsSinceEpoch;
    final file =
        File('${dir.path}${Platform.pathSeparator}dart2_export_$stamp.csv');
    await file.writeAsString(content);
    return ExportResult(
      filePath: file.path,
      rowsExported: totalRows,
      scope: scope,
    );
  }

  Future<List<_ExportChunk>> _buildScopedExports(ExportScope scope) async {
    final chunks = <_ExportChunk>[];
    if (scope == ExportScope.all || scope == ExportScope.expenses) {
      chunks.add(await _buildChunk(
        name: 'expenses',
        query:
            'SELECT id, title, category, amount, occurred_at, source FROM transactions ORDER BY occurred_at DESC',
        headers: const [
          'id',
          'title',
          'category',
          'amount',
          'occurred_at',
          'source'
        ],
      ));
    }
    if (scope == ExportScope.all || scope == ExportScope.incomes) {
      chunks.add(await _buildChunk(
        name: 'incomes',
        query:
            'SELECT id, title, amount, received_at, source FROM incomes ORDER BY received_at DESC',
        headers: const ['id', 'title', 'amount', 'received_at', 'source'],
      ));
    }
    if (scope == ExportScope.all || scope == ExportScope.tasks) {
      chunks.add(await _buildChunk(
        name: 'tasks',
        query:
            'SELECT id, title, description, completed, due_at, priority FROM tasks ORDER BY id DESC',
        headers: const [
          'id',
          'title',
          'description',
          'completed',
          'due_at',
          'priority'
        ],
      ));
    }
    if (scope == ExportScope.all || scope == ExportScope.events) {
      chunks.add(await _buildChunk(
        name: 'events',
        query:
            'SELECT id, title, start_at, end_at, note FROM events ORDER BY start_at DESC',
        headers: const ['id', 'title', 'start_at', 'end_at', 'note'],
      ));
    }
    if (scope == ExportScope.all || scope == ExportScope.budgets) {
      chunks.add(await _buildChunk(
        name: 'budgets',
        query:
            'SELECT id, category, monthly_limit FROM budgets ORDER BY category',
        headers: const ['id', 'category', 'monthly_limit'],
      ));
    }
    if (scope == ExportScope.all || scope == ExportScope.recurring) {
      chunks.add(await _buildChunk(
        name: 'recurring_templates',
        query:
            'SELECT id, kind, title, description, category, amount, priority, cadence, next_run_at, enabled FROM recurring_templates ORDER BY id DESC',
        headers: const [
          'id',
          'kind',
          'title',
          'description',
          'category',
          'amount',
          'priority',
          'cadence',
          'next_run_at',
          'enabled'
        ],
      ));
    }
    return chunks;
  }

  Future<_ExportChunk> _buildChunk({
    required String name,
    required String query,
    required List<String> headers,
  }) async {
    final rows = await _store.executor.runSelect(query, const []);
    final values = rows
        .map((row) => headers.map((header) => row[header]).toList())
        .toList();
    return _ExportChunk(
      name: name,
      csv: _csvBuilder.build(headers: headers, rows: values),
      rows: rows.length,
    );
  }
}

class _ExportChunk {
  const _ExportChunk({
    required this.name,
    required this.csv,
    required this.rows,
  });

  final String name;
  final String csv;
  final int rows;
}
