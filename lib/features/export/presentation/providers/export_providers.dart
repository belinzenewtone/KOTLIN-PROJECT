import 'dart:async';

import 'package:beltech/core/di/repository_providers.dart';
import 'package:beltech/features/export/domain/entities/export_result.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class ExportController extends AutoDisposeAsyncNotifier<ExportResult?> {
  @override
  FutureOr<ExportResult?> build() => null;

  Future<ExportResult> export(ExportScope scope) async {
    state = const AsyncLoading();
    final result = await AsyncValue.guard(
      () => ref.read(exportRepositoryProvider).exportCsv(scope: scope),
    );
    state = result;
    if (result.hasError) {
      throw result.error!;
    }
    return result.valueOrNull!;
  }
}

final exportControllerProvider =
    AutoDisposeAsyncNotifierProvider<ExportController, ExportResult?>(
  ExportController.new,
);
