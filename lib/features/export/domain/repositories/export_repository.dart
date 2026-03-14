import 'package:beltech/features/export/domain/entities/export_result.dart';

abstract class ExportRepository {
  Future<ExportResult> exportCsv({
    required ExportScope scope,
  });
}
