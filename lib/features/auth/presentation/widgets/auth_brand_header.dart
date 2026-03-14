import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_typography.dart';
import 'package:beltech/core/widgets/beltech_logo.dart';
import 'package:flutter/material.dart';

/// The brand hero block shown above the auth form.
///
/// Renders the BELTECH logo inside a soft radial glow halo,
/// followed by the wordmark and tagline using design tokens.
class AuthBrandHeader extends StatelessWidget {
  const AuthBrandHeader({super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // Logo + atmospheric glow halo
        Stack(
          alignment: Alignment.center,
          children: [
            // Radial glow behind logo
            IgnorePointer(
              child: Container(
                width: 130,
                height: 130,
                decoration: const BoxDecoration(
                  shape: BoxShape.circle,
                  gradient: RadialGradient(
                    colors: [AppColors.glowBlue, Colors.transparent],
                    radius: 0.75,
                  ),
                ),
              ),
            ),
            const BeltechLogo(size: 80, borderRadius: 20),
          ],
        ),
        const SizedBox(height: 18),
        // Wordmark
        Text(
          'BELTECH',
          style: AppTypography.pageTitle(context).copyWith(
            letterSpacing: 4,
            fontWeight: FontWeight.w800,
          ),
        ),
        const SizedBox(height: 5),
        // Tagline
        Text(
          'Innovate and Create',
          style: AppTypography.bodySm(context),
        ),
      ],
    );
  }
}
