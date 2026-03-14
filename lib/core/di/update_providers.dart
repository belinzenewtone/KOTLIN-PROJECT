import 'package:beltech/core/di/repository_providers.dart';
import 'package:beltech/core/update/data/app_update_service.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final appUpdateServiceProvider = Provider<AppUpdateService>((ref) {
  if (ref.watch(useSupabaseProvider)) {
    return AppUpdateService(supabaseClient: ref.watch(supabaseClientProvider));
  }
  return AppUpdateService();
});
