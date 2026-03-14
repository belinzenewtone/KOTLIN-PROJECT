import 'package:beltech/core/navigation/app_shell.dart';
import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/glass_styles.dart';
import 'package:beltech/core/widgets/app_feedback.dart';
import 'package:beltech/features/auth/presentation/providers/account_providers.dart';
import 'package:beltech/features/auth/presentation/widgets/auth_brand_header.dart';
import 'package:beltech/features/auth/presentation/widgets/auth_form_card.dart';
import 'package:beltech/features/auth/presentation/widgets/auth_loading_screen.dart';
import 'package:beltech/features/onboarding/presentation/onboarding_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

class AuthGate extends ConsumerStatefulWidget {
  const AuthGate({super.key});

  @override
  ConsumerState<AuthGate> createState() => _AuthGateState();
}

class _AuthGateState extends ConsumerState<AuthGate> {
  bool _checkingOnboarding = true;
  bool _onboardingDone = false;

  @override
  void initState() {
    super.initState();
    hasSeenOnboarding().then((done) {
      if (mounted) {
        setState(() {
          _onboardingDone = done;
          _checkingOnboarding = false;
        });
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_checkingOnboarding) {
      return const AuthLoadingScreen();
    }
    if (!_onboardingDone) {
      return OnboardingScreen(
        onDone: () => setState(() => _onboardingDone = true),
      );
    }
    final sessionState = ref.watch(accountSessionProvider);
    return sessionState.when(
      data: (session) =>
          session.isAuthenticated ? const AppShell() : const AuthScreen(),
      loading: () => const AuthLoadingScreen(),
      error: (_, __) => const AuthScreen(),
    );
  }
}

class AuthScreen extends ConsumerStatefulWidget {
  const AuthScreen({super.key});

  @override
  ConsumerState<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends ConsumerState<AuthScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _phoneController = TextEditingController();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();

  bool _isSignUp = false;
  bool _hideSignInPassword = true;
  bool _hideSignUpPassword = true;
  bool _hideConfirmPassword = true;

  @override
  void dispose() {
    _nameController.dispose();
    _phoneController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final writeState = ref.watch(accountAuthControllerProvider);
    ref.listen<AsyncValue<void>>(accountAuthControllerProvider,
        (previous, next) {
      if (next.hasError) {
        final message = _friendlyAuthError(next.error);
        AppFeedback.error(context, message);
      }
    });

    final brightness = Theme.of(context).brightness;

    return Stack(
      children: [
        // ── Background gradient ─────────────────────────────────────────
        DecoratedBox(
          decoration: BoxDecoration(
            gradient: GlassStyles.backgroundGradientFor(brightness),
          ),
          child: const SizedBox.expand(),
        ),

        // ── Top-right accent glow ───────────────────────────────────────
        Positioned(
          top: -80,
          right: -80,
          child: IgnorePointer(
            child: Container(
              width: 340,
              height: 340,
              decoration: const BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [AppColors.glowBlue, Colors.transparent],
                  radius: 0.8,
                ),
              ),
            ),
          ),
        ),

        // ── Bottom-left secondary glow ──────────────────────────────────
        Positioned(
          bottom: 60,
          left: -90,
          child: IgnorePointer(
            child: Container(
              width: 280,
              height: 280,
              decoration: const BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [AppColors.glowViolet, Colors.transparent],
                  radius: 0.8,
                ),
              ),
            ),
          ),
        ),

        // ── Main scaffold ───────────────────────────────────────────────
        Scaffold(
          backgroundColor: Colors.transparent,
          body: SafeArea(
            child: Center(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(20, 28, 20, 28),
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 420),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const AuthBrandHeader(),
                      const SizedBox(height: 28),
                      AuthFormCard(
                        formKey: _formKey,
                        isSignUp: _isSignUp,
                        isLoading: writeState.isLoading,
                        nameController: _nameController,
                        phoneController: _phoneController,
                        emailController: _emailController,
                        passwordController: _passwordController,
                        confirmPasswordController: _confirmPasswordController,
                        hidePassword: _isSignUp
                            ? _hideSignUpPassword
                            : _hideSignInPassword,
                        hideConfirmPassword: _hideConfirmPassword,
                        onTogglePasswordVisibility: () {
                          setState(() {
                            if (_isSignUp) {
                              _hideSignUpPassword = !_hideSignUpPassword;
                            } else {
                              _hideSignInPassword = !_hideSignInPassword;
                            }
                          });
                        },
                        onToggleConfirmPasswordVisibility: () {
                          setState(() {
                            _hideConfirmPassword = !_hideConfirmPassword;
                          });
                        },
                        onSubmit: _submit,
                        onToggleMode: () {
                          setState(() => _isSignUp = !_isSignUp);
                        },
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }

  Future<void> _submit() async {
    if (_formKey.currentState?.validate() != true) {
      return;
    }
    if (_isSignUp) {
      await ref.read(accountAuthControllerProvider.notifier).signUp(
            name: _nameController.text.trim(),
            phone: _phoneController.text.trim(),
            email: _emailController.text.trim(),
            password: _passwordController.text,
          );
      return;
    }
    await ref.read(accountAuthControllerProvider.notifier).signIn(
          email: _emailController.text.trim(),
          password: _passwordController.text,
        );
  }

  String _friendlyAuthError(Object? error) {
    if (error == null) {
      return 'Sign in failed. Please try again.';
    }
    if (error is AuthApiException) {
      final msg = error.message.toLowerCase();
      if (msg.contains('invalid login credentials') ||
          msg.contains('invalid_credentials')) {
        return 'Invalid email or password. Please try again.';
      }
      if (msg.contains('email not confirmed')) {
        return 'Please verify your email before signing in.';
      }
      return 'Authentication failed. Please try again.';
    }
    final raw = '$error';
    if (raw.contains('invalid login credentials') ||
        raw.contains('invalid_credentials')) {
      return 'Invalid email or password. Please try again.';
    }
    return 'Authentication failed. Please try again.';
  }
}
