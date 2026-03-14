class AssistantMessage {
  const AssistantMessage({
    required this.id,
    required this.text,
    required this.isUser,
    required this.createdAt,
  });

  final String id;
  final String text;
  final bool isUser;
  final DateTime createdAt;
}

class AssistantSuggestion {
  const AssistantSuggestion(this.prompt);

  final String prompt;
}
