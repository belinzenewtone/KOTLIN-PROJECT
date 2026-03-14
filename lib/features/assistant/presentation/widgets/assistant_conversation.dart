import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:beltech/features/assistant/domain/entities/assistant_message.dart';
import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';

class AssistantConversationList extends StatelessWidget {
  const AssistantConversationList({required this.messages, super.key});

  final List<AssistantMessage> messages;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: messages
          .map((message) => Padding(
                padding: const EdgeInsets.only(bottom: 10),
                child: AssistantMessageBubble(message: message),
              ))
          .toList(),
    );
  }
}

class AssistantMessageBubble extends StatelessWidget {
  const AssistantMessageBubble({required this.message, super.key});

  final AssistantMessage message;

  @override
  Widget build(BuildContext context) {
    final onSurface = Theme.of(context).colorScheme.onSurface;
    final alignment =
        message.isUser ? Alignment.centerRight : Alignment.centerLeft;
    final screenWidth = MediaQuery.of(context).size.width;

    if (message.isUser) {
      return Align(
        alignment: alignment,
        child: ConstrainedBox(
          constraints: BoxConstraints(maxWidth: screenWidth * 0.82),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                colors: [AppColors.accent, AppColors.accentStrong],
              ),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Text(
              message.text.trim(),
              style: const TextStyle(
                fontSize: 16,
                color: AppColors.textPrimary,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
        ),
      );
    }

    return Align(
      alignment: alignment,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const CircleAvatar(
            backgroundColor: AppColors.accentSoft,
            child: Icon(
              Icons.smart_toy,
              color: AppColors.accent,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Align(
              alignment: Alignment.centerLeft,
              child: ConstrainedBox(
                constraints: BoxConstraints(maxWidth: screenWidth * 0.84),
                child: GlassCard(
                  borderRadius: 20,
                  padding:
                      const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                  child: MarkdownBody(
                    data: message.text.trim(),
                    selectable: false,
                    styleSheet: MarkdownStyleSheet(
                      p: TextStyle(
                        fontSize: 16,
                        color: onSurface,
                        height: 1.35,
                      ),
                      strong: TextStyle(
                        fontSize: 16,
                        color: onSurface,
                        fontWeight: FontWeight.w700,
                      ),
                      listBullet: TextStyle(color: onSurface),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
