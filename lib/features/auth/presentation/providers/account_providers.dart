import 'dart:async';

import 'package:beltech/core/di/repository_providers.dart';
import 'package:beltech/features/auth/domain/entities/account_session.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final accountSessionProvider = StreamProvider<AccountSession>(
  (ref) => ref.watch(accountRepositoryProvider).watchSession(),
);

class AccountAuthController extends AutoDisposeAsyncNotifier<void> {
  @override
  FutureOr<void> build() {}

  Future<void> signIn({
    required String email,
    required String password,
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      await ref.read(accountRepositoryProvider).signIn(
            email: email,
            password: password,
          );
    });
  }

  Future<void> signUp({
    required String name,
    required String email,
    required String phone,
    required String password,
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      await ref.read(accountRepositoryProvider).signUp(
            name: name,
            email: email,
            phone: phone,
            password: password,
          );
    });
  }

  Future<void> signOut() async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      await ref.read(accountRepositoryProvider).signOut();
    });
  }
}

final accountAuthControllerProvider =
    AutoDisposeAsyncNotifierProvider<AccountAuthController, void>(
  AccountAuthController.new,
);
