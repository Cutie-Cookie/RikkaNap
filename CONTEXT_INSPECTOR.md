# Context Inspector — Prompt Cache Diagnostics

**Status:** Feature research / design notes only. No application code has been changed.  
**Reference implementation:** [MiaoWuNYA/rikkahub-sillytavern-android](https://github.com/MiaoWuNYA/rikkahub-sillytavern-android)  
**Reference revision inspected:** `e5006f36cf50451c1598f922cc0eacca40b04ac6` (`huadeng`, 2026-11-26)  
**RikkaNap revision inspected:** `6b301ba64419e2a59f409892766e169579f98a68` (`master`)

## 1. What the fork's feature actually is

The fork's feature is a **prompt viewer with prefix-cache diagnostics**, not a semantic prompt editor or a fully attributed breakdown of every prompt source.

It answers two related questions:

1. What ordered message list did the app prepare for the model on the most recent request?
2. Compared with the immediately preceding request for this conversation, where did the common message prefix stop matching?

The second question is the main point. Prompt caching offered by providers such as Claude, DeepSeek, and Kimi is generally based on a stable request prefix. If an early message changes, the cacheable prefix after that point is invalidated. The viewer makes likely causes visible instead of leaving the user to infer them from provider usage numbers.

The fork's user-facing entry point is **Chat drawer → bottom action row → Menu (sparkles) → View sent prompt**. It is not a button attached to every individual message. The viewer opens the latest captured request for the currently selected conversation.

## 2. Reference implementation: data capture

### Capture location

Reference file: [`GenerationLoop.kt`](https://github.com/MiaoWuNYA/rikkahub-sillytavern-android/blob/e5006f36cf50451c1598f922cc0eacca40b04ac6/app/src/main/java/me/rerere/rikkahub/data/ai/GenerationLoop.kt)

The fork captures the request after it has:

- assembled the system prompt and conversation history;
- applied context limiting and the input-message transformer pipeline;
- applied its optional system-prompt/XML escaping;
- produced the final app-level `UIMessage` list.

It then calls `PromptDebugCache.setRedactions(...)` and `PromptDebugCache.store(conversationId, escapedMessages)` immediately before it builds provider parameters and calls the provider.

This is an important placement detail: the viewer is not rebuilding a prompt later from saved chat history. It records the already-assembled message list at the request boundary.

**Scope limitation:** this is the final message list at the app/provider interface, not a guaranteed byte-for-byte capture of the HTTP request received by the upstream model. Provider adapters may still serialize, normalize, omit, or add fields. The reference cache also stores messages, not the full `TextGenerationParams`, custom headers/body, tool schemas, or raw HTTP payload.

### Local snapshot cache

Reference file: [`PromptDebugCache.kt`](https://github.com/MiaoWuNYA/rikkahub-sillytavern-android/blob/e5006f36cf50451c1598f922cc0eacca40b04ac6/app/src/main/java/me/rerere/rikkahub/data/ai/PromptDebugCache.kt)

The cache is a process-memory singleton:

- Keyed by conversation ID.
- Stores the current and immediately previous captured message lists for each conversation.
- Uses a synchronized access-order `LinkedHashMap` as an LRU.
- Keeps at most **8 conversations**; least-recently-used entries are evicted.
- Is not persisted as structured prompt data. Process death clears these snapshots.
- Ignores empty message lists.

The two snapshots are two consecutive **provider requests**, not necessarily two user turns. A single assistant turn can call the model multiple times while handling tool calls. Since capture happens for each model request, the previous snapshot may be an earlier tool-loop step from the same user turn.

### Redaction

The fork allows the generation path to register text fragments that must not be exposed in the viewer (currently plugin-injected system prompt text). Matching fragments are replaced with a placeholder before the snapshot is cached.

Observed implementation details and caveats:

- Redaction is applied to text and reasoning parts.
- The fragment list is global/volatile rather than attached to each conversation snapshot; concurrent generations could theoretically race when registering different redaction lists.
- Tool input/result and non-text attachment parts are not covered by that text replacement.
- RikkaNap should not copy this global mutable redaction design blindly. Redaction metadata should travel with the specific captured request, and sensitive plugin/extension content should be considered at the point it enters the request.

## 3. Prefix comparison and diagnostic algorithm

The fork compares the previous and current snapshots from the first message forward.

### Message signature

Its message-level signature includes:

- message role;
- text parts;
- tool-call name and input;
- tool-result call ID and content.

It finds the first message index whose signature differs. Messages before that index are treated as the common prefix.

### Diagnostic cases

The fork classifies the first divergence into these cases:

| Case | Meaning |
|---|---|
| Append-only | Every previous message still matches; the current request only retains that prefix and adds later content. This is the ideal stable-prefix shape. |
| Prefix appended | The first differing message has the same role and its new text starts with the old text. Existing text may remain cacheable; the appended tail and following content are not. |
| Content changed | The first differing message changed in place. The viewer shows the first differing character and short old/new text windows. |
| Inserted / shifted | A message from the previous request appears a few positions later in the current request. This suggests content was inserted before it, invalidating the following prefix. |
| Removed | A previous message is missing at the divergence point, such as after truncation, branch selection, or history editing. |

For the common prefix, the viewer estimates tokens in the matching messages and divides that by the estimated total message tokens. It presents this as an **estimated maximum cacheable prefix**, not a provider-confirmed cache hit.

The reference token estimator is `estimateContextTokens` in [`RollingContext.kt`](https://github.com/MiaoWuNYA/rikkahub-sillytavern-android/blob/e5006f36cf50451c1598f922cc0eacca40b04ac6/app/src/main/java/me/rerere/rikkahub/data/ai/context/RollingContext.kt). It estimates message/part sizes and accounts for message overhead; it is not a provider's exact tokenizer or a measurement of actual cache usage.

### Important limitations of the reference comparison

The diagnostic is intentionally an approximation:

- It compares at **message boundaries**, not token boundaries. A change inside a message can invalidate caching from somewhere inside that message.
- The signature ignores several non-text part types. For example, changed image/audio/video/document content may not be represented in the signature. This can make the displayed common prefix too optimistic for multimodal requests.
- It does not compare provider-specific serialized payloads, tool schemas, headers, or custom request bodies.
- The insertion heuristic only searches a small forward range (up to 8 messages).
- Estimated tokens and the percentage are not the same as provider-reported cached-input tokens.

These limitations should be visible in RikkaNap's implementation/help text. A message-level match must never be presented as proof that the provider cache was hit.

## 4. Viewer presentation in the fork

The cache object renders an HTML document and the drawer opens it in the app's WebView.

The page includes:

- message count and approximate character count;
- a diagnostic banner for the prefix comparison;
- sequential message numbers;
- role labels and role-specific colors (system, user, assistant, tool);
- optional message name;
- text content;
- expandable reasoning content;
- expandable tool-call name and input;
- a green left edge for the stable prefix and a red edge from the first divergent message onward;
- old/new excerpts around the first changed character for in-place content changes.

It is therefore primarily a **role-and-order viewer**. It does not attribute every substring to a separate semantic source such as character description, lorebook entry, author note, memory, or developer prompt. Those may already have been combined into a system message by the time the capture occurs.

## 5. Two different caches are involved

Do not confuse these mechanisms:

1. **Provider prompt/prefix cache:** remote provider-side reuse of a stable request prefix. This is the optimization being diagnosed.
2. **PromptDebugCache:** the fork's local in-memory cache of the latest two request snapshots per conversation. It exists only to make comparison possible.
3. **WebViewContentCache:** the fork/RikkaNap HTML handoff cache used to open generated viewer content. In RikkaNap, [`WebViewContentCache.kt`](https://github.com/Cutie-Cookie/rikkanap/blob/6b301ba64419e2a59f409892766e169579f98a68/ui/src/main/java/me/rerere/ui/webview/WebViewContentCache.kt) stores HTML in the app cache directory under a SHA-256 content ID and expires files after 7 days. This is separate from the short-lived in-memory request snapshots.

The viewer can expose private conversation prompts and tool content. Treat the generated HTML as sensitive local data; avoid logging its contents, exporting it automatically, or retaining it longer than necessary.

## 6. RikkaNap: existing integration points

RikkaNap already has most of the structural pieces needed for a similar feature, but its generation path differs from the fork.

### Request assembly and capture point

Relevant file: [`ChatRequestFactory.kt`](https://github.com/Cutie-Cookie/rikkanap/blob/6b301ba64419e2a59f409892766e169579f98a68/app/src/main/java/me/rerere/rikkahub/data/ai/ChatRequestFactory.kt)

- `createAgent()` creates the `AgentRequest` and `AgentHooks`.
- `AgentHooks.prepareStep(messages)` calls `buildStep(...)`.
- `buildStep()` assembles the system message, applies context limiting, runs the input transformers, expands notices, and returns a `StepRequest(messages, params)`.

Relevant file: [`AgentLoop.kt`](https://github.com/Cutie-Cookie/rikkanap/blob/6b301ba64419e2a59f409892766e169579f98a68/ai/src/main/java/me/rerere/ai/agent/AgentLoop.kt)

- For each model step, `AgentLoop.generateStep()` awaits `hooks.prepareStep(...)`.
- It then passes the returned `step.messages` and parameters to `Provider.streamText()` or `Provider.generateText()`.
- Tool execution can cause another call to `prepareStep()` in the same assistant turn.

**Best capture seam:** immediately after `buildStep()` returns inside RikkaNap's `prepareStep()`, before returning the `StepRequest` to `AgentLoop`. This gives the inspector the final transformed message list that the agent loop is about to hand to the provider. Capture every call if matching the fork's per-provider-request behavior.

Compression requests are a separate path: `generateCompressionSummary()` calls `buildStep()` and then calls the provider directly. Decide explicitly whether compression requests belong in the inspector rather than assuming the normal `prepareStep()` hook sees them.

### Existing viewer/navigation infrastructure

Relevant files:

- [`ChatDrawer.kt`](https://github.com/Cutie-Cookie/rikkanap/blob/6b301ba64419e2a59f409892766e169579f98a68/app/src/main/java/me/rerere/rikkahub/ui/pages/chat/ChatDrawer.kt)
- [`WebViewContentCache.kt`](https://github.com/Cutie-Cookie/rikkanap/blob/6b301ba64419e2a59f409892766e169579f98a68/ui/src/main/java/me/rerere/ui/webview/WebViewContentCache.kt)
- [`WebViewPage.kt`](https://github.com/Cutie-Cookie/rikkanap/blob/6b301ba64419e2a59f409892766e169579f98a68/app/src/main/java/me/rerere/rikkahub/ui/pages/webview/WebViewPage.kt)

RikkaNap's chat drawer already has a bottom action-row menu. Its current menu contains Image Generation; the fork's equivalent menu contains View Sent Prompt. RikkaNap's drawer content already receives the current `Conversation`, so the selected conversation ID is available at the UI entry point.

RikkaNap also already has the HTML content cache and a WebView page that loads HTML by content ID. A first faithful port could reuse that route rather than introducing a new navigation system. The page styling can then be replaced with a native RikkaNap-themed Compose viewer as a separate UI decision.

### Current behavior that is not present in RikkaNap yet

At the inspected revision, RikkaNap has no `PromptDebugCache`, no previous/current prompt snapshot comparison, no prefix-divergence banner, and no drawer menu item for viewing the captured request.

## 7. RikkaNap implementation direction (for later approval)

To preserve the behavior that makes the fork useful while fitting RikkaNap:

1. Add a dedicated request-snapshot cache keyed by conversation ID, keeping current and previous provider-step snapshots with a small bounded LRU.
2. Capture the final `StepRequest.messages` at the `prepareStep()` seam. Do not reconstruct the request from persisted chat messages later.
3. Store request metadata separately where available (model/provider, generation step, capture time, stream mode, and whether this was a normal chat or compression request). Do not imply that message snapshots include raw HTTP details.
4. Port the prefix comparison cases, but improve signatures so multimodal parts, tool calls/results, message names, and all cache-relevant message data are represented. Where exact comparison is impossible, label the result as approximate.
5. Use the existing drawer menu as the entry point and show an empty-state message until a request has been captured for that conversation.
6. Reuse the existing WebView content handoff initially if useful; a native Compose page is preferable if it can present the same diagnostics cleanly and match RikkaNap's theme.
7. Make sensitive-content redaction request-scoped and explicit. Never silently claim the displayed content is byte-for-byte identical to the upstream HTTP payload.
8. Keep local snapshot retention bounded and in memory. The HTML handoff should have short, deliberate retention because it may contain private prompts, attachments metadata, and tool output.

### Behavior decisions to settle before implementation

- **Comparison unit:** match the fork exactly (previous provider request vs current provider request), or group all tool-loop steps into a user-turn snapshot and compare equivalent steps across turns?
- **Compression:** include compression-model requests, or restrict the viewer to normal chat generation?
- **Display:** use the existing WebView handoff or build a native Compose inspector?
- **Multimodal detail:** show attachment metadata/previews, or only indicate the part type and whether it changed?
- **Redaction policy:** which plugin/extension-provided content must be hidden, and should the user be able to reveal it?
- **Provider certainty:** clearly distinguish locally estimated cacheable prefix from actual provider-reported cached tokens when usage data provides them.

## 8. Suggested verification cases

- First captured request: show the request and explain that no previous snapshot exists.
- Stable append-only request: identify the stable prefix and appended tail.
- Edited earlier message: locate the first changed message and show a concise old/new diff.
- Dynamic system prompt or injection: identify the first changing system message.
- Inserted, removed, truncated, and branch-switched history.
- Multiple tool calls in one assistant turn: verify the intended previous/current comparison unit.
- Image/audio/video/document changes: ensure the signature does not falsely claim an unchanged prefix.
- Two conversations generating concurrently: ensure snapshots and redaction rules remain isolated.
- More than the LRU limit of conversations: verify deterministic eviction.
- Process restart: verify the in-memory snapshots correctly return to the empty state.
- HTML/text escaping and sensitive-content redaction.
- Provider usage with cached-input token counts: show provider-reported values separately from local estimates.

## 9. Reference source map

### Fork

- [`PromptDebugCache.kt`](https://github.com/MiaoWuNYA/rikkahub-sillytavern-android/blob/e5006f36cf50451c1598f922cc0eacca40b04ac6/app/src/main/java/me/rerere/rikkahub/data/ai/PromptDebugCache.kt) — in-memory snapshot cache, redaction, comparison, HTML rendering.
- [`GenerationLoop.kt`](https://github.com/MiaoWuNYA/rikkahub-sillytavern-android/blob/e5006f36cf50451c1598f922cc0eacca40b04ac6/app/src/main/java/me/rerere/rikkahub/data/ai/GenerationLoop.kt) — final-message capture point before provider invocation.
- [`ChatDrawer.kt`](https://github.com/MiaoWuNYA/rikkahub-sillytavern-android/blob/e5006f36cf50451c1598f922cc0eacca40b04ac6/app/src/main/java/me/rerere/rikkahub/ui/pages/chat/ChatDrawer.kt) — menu entry and WebView handoff.
- [`RollingContext.kt`](https://github.com/MiaoWuNYA/rikkahub-sillytavern-android/blob/e5006f36cf50451c1598f922cc0eacca40b04ac6/app/src/main/java/me/rerere/rikkahub/data/ai/context/RollingContext.kt) — token estimation used by the diagnostic.
- [Cache documentation](https://github.com/MiaoWuNYA/rikkahub-sillytavern-android/blob/e5006f36cf50451c1598f922cc0eacca40b04ac6/docs-site/memory/cache.md) — provider prefix-cache behavior and the intended use of the viewer.
- [Conversation UI documentation](https://github.com/MiaoWuNYA/rikkahub-sillytavern-android/blob/e5006f36cf50451c1598f922cc0eacca40b04ac6/docs-site/chat/conversations.md) — describes the drawer menu entry.

### RikkaNap

- [`ChatRequestFactory.kt`](https://github.com/Cutie-Cookie/rikkanap/blob/6b301ba64419e2a59f409892766e169579f98a68/app/src/main/java/me/rerere/rikkahub/data/ai/ChatRequestFactory.kt) — final request assembly and `prepareStep()` hook.
- [`AgentLoop.kt`](https://github.com/Cutie-Cookie/rikkanap/blob/6b301ba64419e2a59f409892766e169579f98a68/ai/src/main/java/me/rerere/ai/agent/AgentLoop.kt) — exact handoff from prepared step to provider.
- [`ChatDrawer.kt`](https://github.com/Cutie-Cookie/rikkanap/blob/6b301ba64419e2a59f409892766e169579f98a68/app/src/main/java/me/rerere/rikkahub/ui/pages/chat/ChatDrawer.kt) — existing menu and current-conversation context.
- [`WebViewContentCache.kt`](https://github.com/Cutie-Cookie/rikkanap/blob/6b301ba64419e2a59f409892766e169579f98a68/ui/src/main/java/me/rerere/ui/webview/WebViewContentCache.kt) — existing HTML cache handoff.
- [`WebViewPage.kt`](https://github.com/Cutie-Cookie/rikkanap/blob/6b301ba64419e2a59f409892766e169579f98a68/app/src/main/java/me/rerere/rikkahub/ui/pages/webview/WebViewPage.kt) — existing WebView screen for cached HTML.
