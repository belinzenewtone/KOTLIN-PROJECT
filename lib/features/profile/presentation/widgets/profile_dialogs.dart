import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/widgets/app_dialog.dart';
import 'package:beltech/features/profile/domain/entities/user_profile.dart';
import 'package:beltech/features/profile/presentation/providers/profile_providers.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

Future<void> showEditProfileDialog(
  BuildContext context,
  WidgetRef ref,
  UserProfile profile,
) async {
  final nameCtrl = TextEditingController(text: profile.name);
  final emailCtrl = TextEditingController(text: profile.email);
  final phoneCtrl = TextEditingController(text: profile.phone);
  final formKey = GlobalKey<FormState>();

  await showAppDialog<void>(
    context: context,
    builder: (context) {
      return AlertDialog(
        backgroundColor: AppColors.surface,
        title: const Text('Edit Profile'),
        content: Form(
          key: formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                controller: nameCtrl,
                decoration: const InputDecoration(labelText: 'Name'),
                validator: (v) =>
                    (v == null || v.trim().isEmpty) ? 'Name is required' : null,
              ),
              TextFormField(
                controller: emailCtrl,
                decoration: const InputDecoration(labelText: 'Email'),
                validator: (v) => (v == null || !v.contains('@'))
                    ? 'Valid email required'
                    : null,
              ),
              TextFormField(
                controller: phoneCtrl,
                keyboardType: TextInputType.phone,
                inputFormatters: [
                  FilteringTextInputFormatter.digitsOnly,
                  LengthLimitingTextInputFormatter(10),
                ],
                decoration: const InputDecoration(labelText: 'Phone'),
                validator: (v) {
                  final phone = v?.trim() ?? '';
                  if (phone.isEmpty) {
                    return 'Phone is required';
                  }
                  if (!RegExp(r'^\d{10}$').hasMatch(phone)) {
                    return 'Phone must be exactly 10 digits';
                  }
                  return null;
                },
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () async {
              if (formKey.currentState?.validate() != true) {
                return;
              }
              await ref
                  .read(profileWriteControllerProvider.notifier)
                  .updateProfile(
                    name: nameCtrl.text.trim(),
                    email: emailCtrl.text.trim(),
                    phone: phoneCtrl.text.trim(),
                  );
              final writeState = ref.read(profileWriteControllerProvider);
              if (context.mounted && !writeState.hasError) {
                Navigator.pop(context);
              }
            },
            child: const Text('Save'),
          ),
        ],
      );
    },
  );
}

Future<void> showPasswordDialog(BuildContext context, WidgetRef ref) async {
  final currentCtrl = TextEditingController();
  final newCtrl = TextEditingController();
  final formKey = GlobalKey<FormState>();

  await showAppDialog<void>(
    context: context,
    builder: (context) {
      return AlertDialog(
        backgroundColor: AppColors.surface,
        title: const Text('Change Password'),
        content: Form(
          key: formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                controller: currentCtrl,
                obscureText: true,
                decoration:
                    const InputDecoration(labelText: 'Current password'),
                validator: (v) => (v == null || v.isEmpty)
                    ? 'Current password required'
                    : null,
              ),
              TextFormField(
                controller: newCtrl,
                obscureText: true,
                decoration: const InputDecoration(labelText: 'New password'),
                validator: (v) =>
                    (v == null || v.length < 6) ? 'Minimum 6 characters' : null,
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () async {
              if (formKey.currentState?.validate() != true) {
                return;
              }
              await ref
                  .read(profileWriteControllerProvider.notifier)
                  .changePassword(
                    currentPassword: currentCtrl.text,
                    newPassword: newCtrl.text,
                  );
              final writeState = ref.read(profileWriteControllerProvider);
              if (context.mounted && !writeState.hasError) {
                Navigator.pop(context);
              }
            },
            child: const Text('Update'),
          ),
        ],
      );
    },
  );
}
