const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

type AssistantRequest = {
  prompt?: string;
  context?: string;
};

type ProviderResult =
  | {
      reply: string;
      errorCode?: never;
    }
  | {
      reply?: never;
      errorCode: string;
    };

const FALLBACK_MODEL = "local-fallback";
const OPENAI_RESPONSES_URL = "https://api.openai.com/v1/responses";
const GEMINI_GENERATE_BASE_URL =
  "https://generativelanguage.googleapis.com/v1beta/models";

function jsonResponse(payload: unknown, status = 200): Response {
  return new Response(JSON.stringify(payload), {
    status,
    headers: {
      ...corsHeaders,
      "Content-Type": "application/json",
    },
  });
}

function sanitizeText(value: string, maxLength: number): string {
  return value.trim().replace(/\s+/g, " ").slice(0, maxLength);
}

function buildFallbackReply(prompt: string): string {
  return [
    `I heard you: "${prompt}".`,
    "I can help you break this into clear next actions.",
    "If you want, ask me to create a task or log an expense from this message.",
  ].join(" ");
}

async function callOpenAI(
  apiKey: string,
  model: string,
  prompt: string,
  context: string,
): Promise<ProviderResult> {
  const systemPrompt = [
    "You are Personal OS Assistant.",
    "Be concise, practical, and safe for personal productivity and finance workflows.",
    "Never claim to have committed app data changes.",
    "Propose actions clearly and keep responses short.",
  ].join(" ");

  const input = [
    {
      role: "system",
      content: [{ type: "input_text", text: systemPrompt }],
    },
    {
      role: "user",
      content: [
        {
          type: "input_text",
          text: `User message: ${prompt}\n\nContext:\n${context || "No additional context."}`,
        },
      ],
    },
  ];

  const response = await fetch(OPENAI_RESPONSES_URL, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model,
      input,
      max_output_tokens: 280,
    }),
  });

  if (!response.ok) {
    try {
      const errorBody = await response.json();
      const code =
        typeof errorBody?.error?.code === "string"
          ? errorBody.error.code
          : `http_${response.status}`;
      return { errorCode: code };
    } catch {
      return { errorCode: `http_${response.status}` };
    }
  }

  const body = await response.json();
  const directOutput = typeof body?.output_text === "string" ? body.output_text : null;
  if (directOutput && directOutput.trim().length > 0) {
    return { reply: sanitizeText(directOutput, 2_000) };
  }

  const flattenedOutput = Array.isArray(body?.output) ? body.output : [];
  const segments = flattenedOutput
    .flatMap((item: unknown) => {
      const content = (item as { content?: unknown[] })?.content;
      return Array.isArray(content) ? content : [];
    })
    .map((segment: unknown) => {
      const text = (segment as { text?: string })?.text;
      return typeof text === "string" ? text : "";
    })
    .filter((text: string) => text.trim().length > 0);

  if (segments.length == 0) {
    return { errorCode: "empty_output" };
  }
  return { reply: sanitizeText(segments.join("\n"), 2_000) };
}

async function callGemini(
  apiKey: string,
  model: string,
  prompt: string,
  context: string,
): Promise<ProviderResult> {
  const systemPrompt = [
    "You are Personal OS Assistant.",
    "Be concise, practical, and safe for personal productivity and finance workflows.",
    "Never claim to have committed app data changes.",
    "Propose actions clearly and keep responses short.",
  ].join(" ");

  const url =
    `${GEMINI_GENERATE_BASE_URL}/${encodeURIComponent(model)}:generateContent?key=` +
    encodeURIComponent(apiKey);

  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      contents: [
        {
          role: "user",
          parts: [
            {
              text:
                `${systemPrompt}\n\nUser message: ${prompt}\n\nContext:\n` +
                `${context || "No additional context."}`,
            },
          ],
        },
      ],
      generationConfig: {
        maxOutputTokens: 280,
      },
    }),
  });

  if (!response.ok) {
    try {
      const errorBody = await response.json();
      const code =
        typeof errorBody?.error?.status === "string"
          ? errorBody.error.status
          : `http_${response.status}`;
      return { errorCode: code.toLowerCase() };
    } catch {
      return { errorCode: `http_${response.status}` };
    }
  }

  const body = await response.json();
  const candidates = Array.isArray(body?.candidates) ? body.candidates : [];
  const parts = candidates
    .flatMap((candidate: unknown) => {
      const content = (candidate as { content?: { parts?: unknown[] } })?.content;
      return Array.isArray(content?.parts) ? content.parts : [];
    })
    .map((part: unknown) => {
      const text = (part as { text?: string })?.text;
      return typeof text === "string" ? text : "";
    })
    .filter((text: string) => text.trim().length > 0);

  if (parts.length == 0) {
    return { errorCode: "empty_output" };
  }

  return { reply: sanitizeText(parts.join("\n"), 2_000) };
}

Deno.serve(async (request: Request): Promise<Response> => {
  if (request.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (request.method !== "POST") {
    return jsonResponse({ error: "Method not allowed." }, 405);
  }

  let payload: AssistantRequest;
  try {
    payload = (await request.json()) as AssistantRequest;
  } catch {
    return jsonResponse({ error: "Invalid JSON body." }, 400);
  }

  const prompt = sanitizeText(payload.prompt ?? "", 1_200);
  const context = sanitizeText(payload.context ?? "", 6_000);
  if (!prompt) {
    return jsonResponse({ error: "Field 'prompt' is required." }, 400);
  }

  const openAiApiKey = (Deno.env.get("OPENAI_API_KEY") ?? "").trim();
  const openAiModel = (Deno.env.get("OPENAI_MODEL") ?? "gpt-4o-mini").trim();
  const geminiApiKey = (Deno.env.get("GEMINI_API_KEY") ?? "").trim();
  const geminiModel = (Deno.env.get("GEMINI_MODEL") ?? "gemini-1.5-flash").trim();
  const fallbackReasons: string[] = [];

  if (openAiApiKey) {
    try {
      const openAiResult = await callOpenAI(
        openAiApiKey,
        openAiModel,
        prompt,
        context,
      );
      if (openAiResult.reply) {
        return jsonResponse({
          reply: openAiResult.reply,
          source: "openai",
          model: openAiModel,
        });
      }
      fallbackReasons.push(`openai_${openAiResult.errorCode}`);
    } catch {
      fallbackReasons.push("openai_exception");
    }
  } else {
    fallbackReasons.push("openai_missing_key");
  }

  if (geminiApiKey) {
    try {
      const geminiResult = await callGemini(
        geminiApiKey,
        geminiModel,
        prompt,
        context,
      );
      if (geminiResult.reply) {
        return jsonResponse({
          reply: geminiResult.reply,
          source: "gemini",
          model: geminiModel,
        });
      }
      fallbackReasons.push(`gemini_${geminiResult.errorCode}`);
    } catch {
      fallbackReasons.push("gemini_exception");
    }
  } else {
    fallbackReasons.push("gemini_missing_key");
  }

  return jsonResponse({
    reply: buildFallbackReply(prompt),
    source: FALLBACK_MODEL,
    model: FALLBACK_MODEL,
    fallback_reason: fallbackReasons.join("|"),
  });
});
