import 'dart:async';
import 'package:beltech/core/di/update_providers.dart';
import 'package:beltech/core/di/sync_providers.dart';
import 'package:beltech/core/di/repository_providers.dart';
import 'package:beltech/core/navigation/app_shell_helpers.dart';
import 'package:beltech/core/navigation/shell_providers.dart';
import 'package:beltech/core/navigation/widgets/app_tab_bar.dart';
import 'package:beltech/core/navigation/widgets/biometric_lock_overlay.dart';
import 'package:beltech/core/navigation/widgets/shell_body_switcher.dart';
import 'package:beltech/core/theme/app_motion.dart';
import 'package:beltech/core/theme/app_spacing.dart';
import 'package:beltech/core/theme/glass_styles.dart';
import 'package:beltech/core/update/presentation/app_update_dialog.dart';
import 'package:beltech/core/widgets/app_dialog.dart';
import 'package:beltech/core/widgets/app_toast.dart';
import 'package:beltech/core/widgets/offline_banner.dart';
import 'package:beltech/features/assistant/presentation/assistant_screen.dart';
import 'package:beltech/features/calendar/presentation/calendar_screen.dart';
import 'package:beltech/features/expenses/presentation/expenses_screen.dart';
import 'package:beltech/features/home/presentation/home_screen.dart';
import 'package:beltech/features/profile/presentation/profile_screen.dart';
import 'package:beltech/features/tasks/presentation/tasks_screen.dart';
import 'package:beltech/core/sync/background_sync_coordinator.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class AppShell extends ConsumerStatefulWidget {
  const AppShell({super.key});

  static const List<Widget> _screens = [
    HomeScreen(),
    CalendarScreen(),
    ExpensesScreen(),
    TasksScreen(),
    AssistantScreen(),
    ProfileScreen(),
  ];

  static const List<AppTabItem> _tabs = [
    AppTabItem(
      label: 'Home',
      icon: Icons.grid_view_outlined,
      selectedIcon: Icons.grid_view_rounded,
    ),
    AppTabItem(
      label: 'Calendar',
      icon: Icons.calendar_today_outlined,
      selectedIcon: Icons.calendar_today_rounded,
    ),
    AppTabItem(
      label: 'Finance',
      icon: Icons.account_balance_wallet_outlined,
      selectedIcon: Icons.account_balance_wallet_rounded,
    ),
    AppTabItem(
      label: 'Tasks',
      icon: Icons.task_alt_outlined,
      selectedIcon: Icons.task_alt_rounded,
    ),
    AppTabItem(
      label: 'AI',
      icon: Icons.auto_awesome_outlined,
      selectedIcon: Icons.auto_awesome_rounded,
    ),
    AppTabItem(
      label: 'Profile',
      icon: Icons.person_outline_rounded,
      selectedIcon: Icons.person_rounded,
    ),
  ];

  @override
  ConsumerState<AppShell> createState() => _AppShellState();
}

class _AppShellState extends ConsumerState<AppShell>
    with WidgetsBindingObserver {
  late BackgroundSyncCoordinator _backgroundSyncCoordinator;
  bool _updateChecked = false;
  bool _biometricConfigured = false;
  bool _appLocked = false;
  bool _biometricUnlockInProgress = false;
  String? _biometricLockMessage;
  DateTime? _lastPausedAt;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _backgroundSyncCoordinator = ref.read(backgroundSyncCoordinatorProvider);
    unawaited(_startBackgroundSync());
    unawaited(_initializeBiometricLock());
    unawaited(cleanupNotificationReminders(ref));
    WidgetsBinding.instance.addPostFrameCallback((_) {
      unawaited(_checkForAppUpdate());
    });
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _backgroundSyncCoordinator.stop();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.inactive ||
        state == AppLifecycleState.paused) {
      _lastPausedAt = DateTime.now();
      return;
    }
    if (state == AppLifecycleState.resumed) {
      unawaited(_syncNow());
      unawaited(_materializeRecurringNow());
      unawaited(_runNotificationSweep());
      unawaited(cleanupNotificationReminders(ref));
      unawaited(_applyBiometricLockOnResume());
    }
  }

  @override
  Widget build(BuildContext context) {
    ref.listen<bool>(useSupabaseProvider, (previous, next) {
      if (previous == null || previous == next) {
        return;
      }
      unawaited(_rebindBackgroundSyncForModeChange());
    });

    final currentIndex = ref.watch(shellTabIndexProvider);
    final accent = accentForTab(currentIndex);
    final brightness = Theme.of(context).brightness;
    final reduceMotion = AppMotion.reduceMotion(context);
    final overlayDuration =
        AppMotion.duration(context, normalMs: 260, reducedMs: 0);
    final keyboardVisible = MediaQuery.viewInsetsOf(context).bottom > 0;

    return DecoratedBox(
      decoration: BoxDecoration(
        gradient: GlassStyles.backgroundGradientFor(brightness),
      ),
      child: Stack(
        children: [
          // Per-tab accent radial glow
          IgnorePointer(
            child: AnimatedContainer(
              duration: overlayDuration,
              curve: Curves.easeOutCubic,
              decoration: BoxDecoration(
                gradient: RadialGradient(
                  center: const Alignment(0.75, -0.9),
                  radius: 1.05,
                  colors: [
                    accent.withValues(alpha: 0.22),
                    Colors.transparent,
                  ],
                ),
              ),
            ),
          ),
          IgnorePointer(
            ignoring: _appLocked,
            child: Scaffold(
              backgroundColor: Colors.transparent,
              body: ShellBodySwitcher(
                currentIndex: currentIndex,
                reduceMotion: reduceMotion,
                children: AppShell._screens,
              ),
              bottomNavigationBar: keyboardVisible
                  ? null
                  : Padding(
                      padding: EdgeInsets.fromLTRB(
                        AppSpacing.shellHorizontal,
                        0,
                        AppSpacing.shellHorizontal,
                        AppSpacing.navBottom(context),
                      ),
                      child: AppTabBar(
                        selectedIndex: currentIndex,
                        items: AppShell._tabs,
                        onTap: (index) {
                          ref.read(shellTabIndexProvider.notifier).state =
                              index;
                        },
                      ),
                    ),
            ),
          ),
          // Offline banner
          const Positioned(
            top: 0,
            left: 0,
            right: 0,
            child: OfflineBanner(),
          ),
          // Toast overlay
          const AppToastOverlay(),
          // Biometric lock
          if (_appLocked)
            BiometricLockOverlay(
              busy: _biometricUnlockInProgress,
              message: _biometricLockMessage,
              onUnlock: _unlockWithBiometrics,
            ),
        ],
      ),
    );
  }

  Future<void> _startBackgroundSync() async =>
      _backgroundSyncCoordinator.start();
  Future<void> _materializeRecurringNow() async =>
      _backgroundSyncCoordinator.materializeNow();
  Future<void> _syncNow() async => _backgroundSyncCoordinator.syncNow();
  Future<void> _runNotificationSweep() async =>
      _backgroundSyncCoordinator.runNotificationSweep();

  Future<void> _rebindBackgroundSyncForModeChange() async {
    await _backgroundSyncCoordinator.stop();
    ref.invalidate(backgroundSyncCoordinatorProvider);
    ref.invalidate(syncStatusProvider);
    _backgroundSyncCoordinator = ref.read(backgroundSyncCoordinatorProvider);
    await _backgroundSyncCoordinator.start();
    await cleanupNotificationReminders(ref);
  }

  Future<void> _checkForAppUpdate() async {
    if (_updateChecked || !mounted) return;
    _updateChecked = true;
    final service = ref.read(appUpdateServiceProvider);
    final update = await service.fetchAvailableUpdate();
    if (update == null || !mounted) return;
    await showAppDialog<void>(
      context: context,
      barrierDismissible: !update.forceUpdate,
      builder: (context) => AppUpdateDialog(update: update, service: service),
    );
  }

  Future<void> _initializeBiometricLock() async =>
      _refreshBiometricConfiguration(lockNow: true);

  Future<void> _applyBiometricLockOnResume() async {
    if (_biometricUnlockInProgress) return;
    final pausedAt = _lastPausedAt;
    _lastPausedAt = null;
    if (pausedAt == null) return;
    if (DateTime.now().difference(pausedAt) < const Duration(seconds: 2)) {
      return;
    }
    await _refreshBiometricConfiguration(lockNow: true);
  }

  Future<void> _refreshBiometricConfiguration({required bool lockNow}) async {
    final authRepository = ref.read(authRepositoryProvider);
    final enabled = await authRepository.isBiometricEnabled();
    final supported = await authRepository.isBiometricSupported();
    final configured = enabled && supported;
    if (!mounted) return;
    setState(() {
      _biometricConfigured = configured;
      if (!configured) {
        _appLocked = false;
        _biometricLockMessage = null;
        return;
      }
      if (lockNow) _appLocked = true;
    });
  }

  Future<void> _unlockWithBiometrics() async {
    if (_biometricUnlockInProgress || !_biometricConfigured) return;
    setState(() {
      _biometricUnlockInProgress = true;
      _biometricLockMessage = null;
    });
    final authenticated = await ref.read(authRepositoryProvider).authenticate();
    if (!mounted) return;
    setState(() {
      _biometricUnlockInProgress = false;
      _appLocked = !authenticated;
      _biometricLockMessage =
          authenticated ? null : 'Authentication was not completed.';
      if (authenticated) _lastPausedAt = null;
    });
  }
}
