import 'package:beltech/core/di/repository_providers.dart';
import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/glass_styles.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

// ── Convenience helpers used by the app router ────────────────────────────────
//
// The router runs outside of a widget tree and cannot inject a provider,
// so it calls the repository directly via a one-shot ProviderContainer.
// Keeping these as package-level functions preserves the existing router
// call-site while the actual persistence is delegated to the repository.

/// Returns `true` if the user has already completed onboarding.
///
/// Uses a temporary [ProviderContainer] so the router can call this without
/// a widget context.
Future<bool> hasSeenOnboarding() async {
  final container = ProviderContainer();
  try {
    final repo = container.read(onboardingRepositoryProvider);
    return await repo.hasSeenOnboarding();
  } finally {
    container.dispose();
  }
}

/// Marks onboarding as complete.  See [hasSeenOnboarding] for rationale.
Future<void> markOnboardingDone() async {
  final container = ProviderContainer();
  try {
    final repo = container.read(onboardingRepositoryProvider);
    await repo.markOnboardingDone();
  } finally {
    container.dispose();
  }
}

class _OnboardingPage {
  const _OnboardingPage({
    required this.icon,
    required this.title,
    required this.body,
    required this.color,
  });
  final IconData icon;
  final String title;
  final String body;
  final Color color;
}

const _pages = [
  _OnboardingPage(
    icon: Icons.home_outlined,
    title: 'Welcome to BELTECH',
    body:
        'Your all-in-one personal finance and productivity companion. Track spending, manage tasks, and stay on top of your schedule — all in one place.',
    color: AppColors.accent,
  ),
  _OnboardingPage(
    icon: Icons.receipt_long_outlined,
    title: 'Track Every Expense',
    body:
        'Log expenses manually or let BELTECH auto-import from your SMS messages. Set monthly budget targets and get notified before you overspend.',
    color: AppColors.teal,
  ),
  _OnboardingPage(
    icon: Icons.check_circle_outline,
    title: 'Stay Productive',
    body:
        'Manage tasks with priority levels and due dates. Schedule calendar events. Set up recurring items so nothing falls through the cracks.',
    color: AppColors.violet,
  ),
  _OnboardingPage(
    icon: Icons.smart_toy_outlined,
    title: 'Your AI Assistant',
    body:
        'Ask BELTECH about your spending, pending tasks, or upcoming events. Get instant insights powered by your real data.',
    color: AppColors.accent,
  ),
];

class OnboardingScreen extends ConsumerStatefulWidget {
  const OnboardingScreen({super.key, required this.onDone});

  final VoidCallback onDone;

  @override
  ConsumerState<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends ConsumerState<OnboardingScreen> {
  final _controller = PageController();
  int _page = 0;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _next() {
    if (_page < _pages.length - 1) {
      _controller.nextPage(
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeInOut,
      );
    } else {
      _finish();
    }
  }

  Future<void> _finish() async {
    await ref.read(onboardingRepositoryProvider).markOnboardingDone();
    widget.onDone();
  }

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final brightness = Theme.of(context).brightness;

    return DecoratedBox(
      decoration: BoxDecoration(
        gradient: GlassStyles.backgroundGradientFor(brightness),
      ),
      child: Scaffold(
        backgroundColor: Colors.transparent,
        body: SafeArea(
          child: Column(
            children: [
              Expanded(
                child: PageView.builder(
                  controller: _controller,
                  onPageChanged: (i) => setState(() => _page = i),
                  itemCount: _pages.length,
                  itemBuilder: (context, index) {
                    final page = _pages[index];
                    return Padding(
                      padding: const EdgeInsets.fromLTRB(28, 40, 28, 24),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Container(
                            width: 100,
                            height: 100,
                            decoration: BoxDecoration(
                              color: page.color.withValues(alpha: 0.18),
                              shape: BoxShape.circle,
                            ),
                            child: Icon(
                              page.icon,
                              size: 52,
                              color: page.color,
                            ),
                          ),
                          const SizedBox(height: 36),
                          Text(
                            page.title,
                            style: textTheme.titleLarge,
                            textAlign: TextAlign.center,
                          ),
                          const SizedBox(height: 16),
                          GlassCard(
                            child: Text(
                              page.body,
                              style: textTheme.bodyLarge,
                              textAlign: TextAlign.center,
                            ),
                          ),
                        ],
                      ),
                    );
                  },
                ),
              ),
              // Page indicator dots
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: List.generate(
                  _pages.length,
                  (i) => AnimatedContainer(
                    duration: const Duration(milliseconds: 200),
                    margin: const EdgeInsets.symmetric(horizontal: 4),
                    width: i == _page ? 20 : 8,
                    height: 8,
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(4),
                      color: i == _page
                          ? AppColors.accent
                          : AppColors.accent.withValues(alpha: 0.3),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 28),
              Padding(
                padding: const EdgeInsets.fromLTRB(28, 0, 28, 28),
                child: Row(
                  children: [
                    if (_page > 0) ...[
                      TextButton(
                        onPressed: () => _controller.previousPage(
                          duration: const Duration(milliseconds: 300),
                          curve: Curves.easeInOut,
                        ),
                        child: const Text('Back'),
                      ),
                      const SizedBox(width: 12),
                    ],
                    Expanded(
                      child: FilledButton(
                        onPressed: _next,
                        child: Text(
                          _page == _pages.length - 1
                              ? 'Get Started'
                              : 'Next',
                        ),
                      ),
                    ),
                    if (_page < _pages.length - 1) ...[
                      const SizedBox(width: 12),
                      TextButton(
                        onPressed: _finish,
                        child: const Text('Skip'),
                      ),
                    ],
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
