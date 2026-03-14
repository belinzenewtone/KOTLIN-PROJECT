import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_motion.dart';
import 'package:beltech/core/theme/app_radius.dart';
import 'package:beltech/core/theme/app_spacing.dart';
import 'package:beltech/core/theme/app_typography.dart';
import 'package:beltech/core/widgets/app_button.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// The animated sign-in / sign-up form card.
///
/// All typography uses [AppTypography] tokens.
/// The submit button uses [AppButton] with the loading prop.
/// The mode-toggle row uses [AppButton(variant: ghost)].
class AuthFormCard extends StatelessWidget {
  const AuthFormCard({
    super.key,
    required this.formKey,
    required this.isSignUp,
    required this.isLoading,
    required this.nameController,
    required this.phoneController,
    required this.emailController,
    required this.passwordController,
    required this.confirmPasswordController,
    required this.hidePassword,
    required this.hideConfirmPassword,
    required this.onTogglePasswordVisibility,
    required this.onToggleConfirmPasswordVisibility,
    required this.onSubmit,
    required this.onToggleMode,
  });

  final GlobalKey<FormState> formKey;
  final bool isSignUp;
  final bool isLoading;
  final TextEditingController nameController;
  final TextEditingController phoneController;
  final TextEditingController emailController;
  final TextEditingController passwordController;
  final TextEditingController confirmPasswordController;
  final bool hidePassword;
  final bool hideConfirmPassword;
  final VoidCallback onTogglePasswordVisibility;
  final VoidCallback onToggleConfirmPasswordVisibility;
  final VoidCallback onSubmit;
  final VoidCallback onToggleMode;

  @override
  Widget build(BuildContext context) {
    final brightness = Theme.of(context).brightness;

    final resizeDuration = AppMotion.duration(
      context,
      normalMs: 180,
      reducedMs: 0,
    );
    final sectionDuration = AppMotion.duration(
      context,
      normalMs: 160,
      reducedMs: 0,
    );

    return GlassCard(
      borderRadius: AppRadius.xxl,
      padding: const EdgeInsets.fromLTRB(20, 22, 20, 18),
      child: Form(
        key: formKey,
        child: AnimatedSize(
          duration: resizeDuration,
          curve: Curves.easeOutCubic,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              // ── Form title ────────────────────────────────────────────────
              Text(
                isSignUp ? 'Create Account' : 'Welcome Back',
                style: AppTypography.sectionTitle(context).copyWith(
                  fontSize: 20,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 5),
              Text(
                isSignUp ? 'Join BELTECH today' : 'Sign in to your account',
                style: AppTypography.bodySm(context),
              ),
              const SizedBox(height: AppSpacing.sectionHeaderGap),

              // ── Sign-up-only fields (name + phone) ───────────────────────
              AnimatedSwitcher(
                duration: sectionDuration,
                switchInCurve: Curves.easeOutCubic,
                switchOutCurve: Curves.easeInCubic,
                transitionBuilder: (child, animation) => SizeTransition(
                  sizeFactor: animation,
                  child: FadeTransition(opacity: animation, child: child),
                ),
                child: isSignUp
                    ? Column(
                        key: const ValueKey<String>('signup-fields'),
                        children: [
                          TextFormField(
                            controller: nameController,
                            textInputAction: TextInputAction.next,
                            decoration: const InputDecoration(
                              hintText: 'Username',
                              prefixIcon:
                                  Icon(Icons.person_outline_rounded),
                            ),
                            validator: (value) {
                              if (!isSignUp) return null;
                              if (value == null || value.trim().isEmpty) {
                                return 'Name is required';
                              }
                              return null;
                            },
                          ),
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: phoneController,
                            textInputAction: TextInputAction.next,
                            keyboardType: TextInputType.phone,
                            inputFormatters: [
                              FilteringTextInputFormatter.digitsOnly,
                              LengthLimitingTextInputFormatter(10),
                            ],
                            decoration: const InputDecoration(
                              hintText: 'Phone',
                              prefixIcon:
                                  Icon(Icons.phone_outlined),
                            ),
                            validator: (value) {
                              if (!isSignUp) return null;
                              final phone = value?.trim() ?? '';
                              if (phone.isEmpty) {
                                return 'Phone is required';
                              }
                              if (!RegExp(r'^\d{10}$').hasMatch(phone)) {
                                return 'Phone must be exactly 10 digits';
                              }
                              return null;
                            },
                          ),
                          const SizedBox(height: 12),
                        ],
                      )
                    : const SizedBox.shrink(
                        key: ValueKey<String>('signin-fields'),
                      ),
              ),

              // ── Email ────────────────────────────────────────────────────
              TextFormField(
                controller: emailController,
                textInputAction: TextInputAction.next,
                keyboardType: TextInputType.emailAddress,
                decoration: const InputDecoration(
                  hintText: 'Email',
                  prefixIcon: Icon(Icons.mail_outline_rounded),
                ),
                validator: (value) {
                  if (value == null || !value.contains('@')) {
                    return 'Valid email is required';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 12),

              // ── Password ─────────────────────────────────────────────────
              TextFormField(
                controller: passwordController,
                textInputAction:
                    isSignUp ? TextInputAction.next : TextInputAction.done,
                obscureText: hidePassword,
                onFieldSubmitted: (_) {
                  if (!isSignUp && !isLoading) onSubmit();
                },
                decoration: InputDecoration(
                  hintText: 'Password',
                  prefixIcon: const Icon(Icons.lock_outline_rounded),
                  suffixIcon: IconButton(
                    onPressed: onTogglePasswordVisibility,
                    icon: Icon(
                      hidePassword
                          ? Icons.visibility_off_outlined
                          : Icons.visibility_outlined,
                    ),
                  ),
                ),
                validator: (value) {
                  if (value == null || value.length < 6) {
                    return 'Use at least 6 characters';
                  }
                  return null;
                },
              ),

              // ── Confirm password (sign-up only) ──────────────────────────
              AnimatedSwitcher(
                duration: sectionDuration,
                switchInCurve: Curves.easeOutCubic,
                switchOutCurve: Curves.easeInCubic,
                transitionBuilder: (child, animation) => SizeTransition(
                  sizeFactor: animation,
                  child: FadeTransition(opacity: animation, child: child),
                ),
                child: isSignUp
                    ? Column(
                        key: const ValueKey<String>('confirm-password'),
                        children: [
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: confirmPasswordController,
                            textInputAction: TextInputAction.done,
                            obscureText: hideConfirmPassword,
                            onFieldSubmitted: (_) {
                              if (!isLoading) onSubmit();
                            },
                            decoration: InputDecoration(
                              hintText: 'Confirm Password',
                              prefixIcon:
                                  const Icon(Icons.lock_outline_rounded),
                              suffixIcon: IconButton(
                                onPressed: onToggleConfirmPasswordVisibility,
                                icon: Icon(
                                  hideConfirmPassword
                                      ? Icons.visibility_off_outlined
                                      : Icons.visibility_outlined,
                                ),
                              ),
                            ),
                            validator: (value) {
                              if (!isSignUp) return null;
                              if (value != passwordController.text) {
                                return 'Passwords do not match';
                              }
                              return null;
                            },
                          ),
                        ],
                      )
                    : const SizedBox.shrink(
                        key: ValueKey<String>('no-confirm-password'),
                      ),
              ),

              // ── Submit button ─────────────────────────────────────────────
              const SizedBox(height: AppSpacing.cardGap),
              AppButton(
                label: isSignUp ? 'Create Account' : 'Sign In',
                onPressed: isLoading ? null : onSubmit,
                loading: isLoading,
                variant: AppButtonVariant.primary,
                size: AppButtonSize.lg,
                fullWidth: true,
              ),

              // ── Mode toggle ───────────────────────────────────────────────
              const SizedBox(height: 8),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    isSignUp
                        ? 'Already have an account?'
                        : "Don't have an account?",
                    style: AppTypography.bodySm(context).copyWith(
                      color: AppColors.textMutedFor(brightness),
                    ),
                  ),
                  AppButton(
                    label: isSignUp ? 'Sign In' : 'Sign Up',
                    onPressed: onToggleMode,
                    variant: AppButtonVariant.ghost,
                    size: AppButtonSize.sm,
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
