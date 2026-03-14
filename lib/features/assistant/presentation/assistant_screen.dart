import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_spacing.dart';
import 'package:beltech/core/theme/app_typography.dart';
import 'package:beltech/core/widgets/app_button.dart';
import 'package:beltech/core/widgets/app_feedback.dart';
import 'package:beltech/core/widgets/error_message.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:beltech/core/widgets/loading_indicator.dart';
import 'package:beltech/core/widgets/page_header.dart';
import 'package:beltech/core/widgets/page_shell.dart';
import 'package:beltech/features/assistant/presentation/providers/assistant_providers.dart';
import 'package:beltech/features/assistant/presentation/widgets/assistant_conversation.dart';
import 'package:beltech/features/assistant/presentation/widgets/assistant_prompt_grid.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class AssistantScreen extends ConsumerStatefulWidget {
  const AssistantScreen({super.key});

  @override
  ConsumerState<AssistantScreen> createState() => _AssistantScreenState();
}

class _AssistantScreenState extends ConsumerState<AssistantScreen> {
  final TextEditingController _messageController = TextEditingController();

  @override
  void dispose() {
    _messageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final brightness = Theme.of(context).brightness;
    final secondaryText = AppColors.textSecondaryFor(brightness);
    final messagesState = ref.watch(assistantMessagesProvider);
    final suggestions = ref.watch(assistantSuggestionsProvider);
    final writeState = ref.watch(assistantWriteControllerProvider);
    final conversationState =
        ref.watch(assistantConversationControllerProvider);

    ref.listen<AsyncValue<void>>(assistantWriteControllerProvider,
        (previous, next) {
      if (next.hasError) {
        AppFeedback.error(context, 'Message failed to send.', ref: ref);
      }
    });
    ref.listen<AsyncValue<void>>(assistantConversationControllerProvider,
        (previous, next) {
      if (next.hasError) {
        AppFeedback.error(context, 'Unable to clear chat history.', ref: ref);
      } else if (previous?.isLoading == true && next.hasValue) {
        AppFeedback.success(context, 'Chat history cleared.', ref: ref);
      }
    });

    final hasMessages = messagesState.valueOrNull?.isNotEmpty ?? false;

    return PageShell(
      glowColor: AppColors.glowViolet,
      scrollable: false,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          PageHeader(
            eyebrow: 'AI COACH',
            title: 'Assistant',
            subtitle: 'Ask me anything',
            action: hasMessages
                ? IconButton(
                    tooltip: 'Clear chats',
                    onPressed:
                        conversationState.isLoading ? null : _confirmClearChats,
                    icon: conversationState.isLoading
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.delete_sweep_outlined),
                  )
                : null,
          ),
          Expanded(
            child: ListView(
              padding: EdgeInsets.fromLTRB(
                0,
                0,
                0,
                AppSpacing.contentBottomSafe,
              ),
              children: [
                Padding(
                  padding: EdgeInsets.symmetric(
                    horizontal: AppSpacing.screenHorizontal,
                  ),
                  child: Text(
                    'Quick prompts',
                    style: AppTypography.sectionTitle(context),
                  ),
                ),
                const SizedBox(height: 10),
                Padding(
                  padding: EdgeInsets.symmetric(
                    horizontal: AppSpacing.screenHorizontal,
                  ),
                  child: AssistantPromptGrid(
                    prompts: suggestions.map((item) => item.prompt).toList(),
                    onPromptTap: _sendMessage,
                  ),
                ),
                const SizedBox(height: 14),
                messagesState.when(
                  data: (messages) =>
                      AssistantConversationList(messages: messages),
                  loading: () => Center(child: LoadingIndicator()),
                  error: (_, __) => ErrorMessage(
                    label: 'Unable to load assistant',
                    onRetry: () => ref.invalidate(assistantMessagesProvider),
                  ),
                ),
              ],
            ),
          ),
          AnimatedPadding(
            duration: const Duration(milliseconds: 180),
            curve: Curves.easeOut,
            padding: EdgeInsets.fromLTRB(
              AppSpacing.screenHorizontal,
              12,
              AppSpacing.screenHorizontal,
              12,
            ),
            child: GlassCard(
              borderRadius: 24,
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _messageController,
                      onSubmitted: (_) => _sendMessage(_messageController.text),
                      decoration: InputDecoration(
                        hintText: 'Message BELTECH...',
                        border: InputBorder.none,
                        hintStyle: TextStyle(color: secondaryText),
                      ),
                    ),
                  ),
                  Container(
                    decoration: const BoxDecoration(
                      color: AppColors.accent,
                      shape: BoxShape.circle,
                    ),
                    child: IconButton(
                      onPressed: writeState.isLoading
                          ? null
                          : () => _sendMessage(_messageController.text),
                      icon: writeState.isLoading
                          ? const SizedBox(
                              width: 16,
                              height: 16,
                              child: CircularProgressIndicator(
                                color: AppColors.textPrimary,
                                strokeWidth: 2,
                              ),
                            )
                          : const Icon(Icons.send,
                              color: AppColors.textPrimary),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _sendMessage(String text) async {
    final payload = text.trim();
    if (payload.isEmpty) {
      return;
    }
    _messageController.clear();
    await ref
        .read(assistantWriteControllerProvider.notifier)
        .sendMessage(payload);
  }

  Future<void> _confirmClearChats() async {
    final shouldClear = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Clear chats'),
        content: const Text(
          'This will remove previous assistant messages from this account.',
        ),
        actions: [
          AppButton(
            label: 'Cancel',
            onPressed: () => Navigator.of(context).pop(false),
            variant: AppButtonVariant.secondary,
            size: AppButtonSize.sm,
          ),
          AppButton(
            label: 'Clear',
            onPressed: () => Navigator.of(context).pop(true),
            variant: AppButtonVariant.danger,
            size: AppButtonSize.sm,
          ),
        ],
      ),
    );
    if (shouldClear != true || !mounted) {
      return;
    }
    await ref
        .read(assistantConversationControllerProvider.notifier)
        .clearConversation();
  }
}
