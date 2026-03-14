import 'package:beltech/data/local/drift/app_drift_store.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

part 'data_mode_migration_service_cloud_gateway.dart';
part 'data_mode_migration_service_codec.dart';
part 'data_mode_migration_service_local_gateway.dart';
part 'data_mode_migration_service_payload.dart';

enum DataStoreMode { local, cloud }

class DataMigrationPreview {
  const DataMigrationPreview({
    required this.fromMode,
    required this.toMode,
    required this.sourceCounts,
    required this.destinationCounts,
    required this.canMigrate,
    this.blockerMessage,
  });

  final DataStoreMode fromMode;
  final DataStoreMode toMode;
  final Map<String, int> sourceCounts;
  final Map<String, int> destinationCounts;
  final bool canMigrate;
  final String? blockerMessage;

  int get sourceTotal =>
      sourceCounts.values.fold<int>(0, (sum, value) => sum + value);
  int get destinationTotal =>
      destinationCounts.values.fold<int>(0, (sum, value) => sum + value);
}

class DataMigrationResult {
  const DataMigrationResult({
    required this.insertedCounts,
    required this.updatedCounts,
  });

  final Map<String, int> insertedCounts;
  final Map<String, int> updatedCounts;

  int get insertedTotal =>
      insertedCounts.values.fold<int>(0, (sum, value) => sum + value);
  int get updatedTotal =>
      updatedCounts.values.fold<int>(0, (sum, value) => sum + value);

  bool get changed => insertedTotal > 0 || updatedTotal > 0;
}

class DataModeMigrationService {
  DataModeMigrationService({
    required AppDriftStore localStore,
    required SupabaseClient? supabaseClient,
  })  : _localStore = localStore,
        _supabaseClient = supabaseClient;

  final AppDriftStore _localStore;
  final SupabaseClient? _supabaseClient;
  final _codec = _MigrationCodec();

  static const String scopeExpenses = 'expenses';
  static const String scopeIncome = 'income';
  static const String scopeTasks = 'tasks';
  static const String scopeEvents = 'events';
  static const String scopeBudgets = 'budgets';
  static const String scopeRecurring = 'recurring';

  static const List<String> orderedScopes = [
    scopeExpenses,
    scopeIncome,
    scopeTasks,
    scopeEvents,
    scopeBudgets,
    scopeRecurring,
  ];

  late final _LocalMigrationGateway _localGateway = _LocalMigrationGateway(
    store: _localStore,
    codec: _codec,
    orderedScopes: orderedScopes,
  );

  late final _CloudMigrationGateway _cloudGateway = _CloudMigrationGateway(
    client: _supabaseClient,
    codec: _codec,
    orderedScopes: orderedScopes,
  );

  Future<DataMigrationPreview> preview({
    required DataStoreMode fromMode,
    required DataStoreMode toMode,
    required bool cloudAvailable,
  }) async {
    final blocker = _resolveBlocker(
      fromMode: fromMode,
      toMode: toMode,
      cloudAvailable: cloudAvailable,
    );
    final userId = _currentCloudUserId();

    final sourceCounts = await _safeLoadCounts(
      mode: fromMode,
      userId: userId,
      blocked: blocker != null,
    );
    final destinationCounts = await _safeLoadCounts(
      mode: toMode,
      userId: userId,
      blocked: blocker != null,
    );

    return DataMigrationPreview(
      fromMode: fromMode,
      toMode: toMode,
      sourceCounts: sourceCounts,
      destinationCounts: destinationCounts,
      canMigrate: blocker == null,
      blockerMessage: blocker,
    );
  }

  Future<DataMigrationResult> migrate({
    required DataStoreMode fromMode,
    required DataStoreMode toMode,
    required bool cloudAvailable,
  }) async {
    if (fromMode == toMode) {
      final empty = _codec.emptyScopeCounts(orderedScopes);
      return DataMigrationResult(insertedCounts: empty, updatedCounts: empty);
    }

    final blocker = _resolveBlocker(
      fromMode: fromMode,
      toMode: toMode,
      cloudAvailable: cloudAvailable,
    );
    if (blocker != null) {
      throw Exception(blocker);
    }

    final userId = _requireCloudUserId();
    final payload = fromMode == DataStoreMode.local
        ? await _localGateway.readPayload()
        : await _cloudGateway.readPayload(userId);

    if (toMode == DataStoreMode.local) {
      return _localGateway.writePayload(payload);
    }
    return _cloudGateway.writePayload(payload, userId);
  }

  String? _resolveBlocker({
    required DataStoreMode fromMode,
    required DataStoreMode toMode,
    required bool cloudAvailable,
  }) {
    if (fromMode == toMode) {
      return 'Source and destination are the same.';
    }
    if ((fromMode == DataStoreMode.cloud || toMode == DataStoreMode.cloud) &&
        !cloudAvailable) {
      return 'Cloud mode is unavailable on this build.';
    }
    if (fromMode == DataStoreMode.cloud || toMode == DataStoreMode.cloud) {
      if (_supabaseClient == null) {
        return 'Cloud mode is unavailable on this build.';
      }
      if (_currentCloudUserId() == null) {
        return 'Sign in to your cloud account to migrate data.';
      }
    }
    return null;
  }

  Future<Map<String, int>> _safeLoadCounts({
    required DataStoreMode mode,
    required String? userId,
    required bool blocked,
  }) async {
    if (blocked) {
      if (mode == DataStoreMode.cloud) {
        return _codec.emptyScopeCounts(orderedScopes);
      }
      return _localGateway.loadCounts();
    }
    return _loadCounts(mode: mode, userId: userId);
  }

  Future<Map<String, int>> _loadCounts({
    required DataStoreMode mode,
    required String? userId,
  }) {
    if (mode == DataStoreMode.local) {
      return _localGateway.loadCounts();
    }
    if (userId == null || userId.isEmpty) {
      return Future.value(_codec.emptyScopeCounts(orderedScopes));
    }
    return _cloudGateway.loadCounts(userId);
  }

  String? _currentCloudUserId() {
    return _supabaseClient?.auth.currentUser?.id;
  }

  String _requireCloudUserId() {
    final userId = _currentCloudUserId();
    if (userId == null || userId.isEmpty) {
      throw Exception('Sign in to your cloud account to migrate data.');
    }
    return userId;
  }
}
