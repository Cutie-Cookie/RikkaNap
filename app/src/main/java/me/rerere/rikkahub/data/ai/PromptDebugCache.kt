package me.rerere.rikkahub.data.ai

import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import java.text.DateFormat
import java.util.Date
import kotlin.uuid.Uuid

/**
 * Keeps the latest two prepared provider requests per conversation so the chat drawer can
 * explain where a stable prompt-cache prefix diverged.
 *
 * This is a local diagnostic snapshot, not a capture of the provider's serialized HTTP body.
 */
object PromptDebugCache {
    private data class Snapshot(
        val messages: List<UIMessage>,
        val modelName: String,
        val modelId: String,
        val capturedAtMillis: Long,
    )

    private data class Entry(
        val current: Snapshot,
        val previous: Snapshot?,
    )

    private const val MAX_ENTRIES = 8
    private val cache = object : LinkedHashMap<String, Entry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>): Boolean =
            size > MAX_ENTRIES
    }

    fun store(
        conversationId: Uuid,
        messages: List<UIMessage>,
        modelName: String,
        modelId: String,
    ) {
        if (messages.isEmpty()) return
        synchronized(cache) {
            val key = conversationId.toString()
            val previous = cache[key]?.current
            cache[key] = Entry(
                current = Snapshot(messages.toList(), modelName, modelId, System.currentTimeMillis()),
                previous = previous,
            )
        }
    }

    fun get(conversationId: Uuid): String? {
        val entry = synchronized(cache) { cache[conversationId.toString()] } ?: return null
        return buildHtml(entry.current, entry.previous)
    }

    private fun messageSignature(message: UIMessage): String = buildString {
        append(message.role.name)
        append('\u001f')
        // toString() includes the complete data-class payload for text, tools, results and
        // multimodal parts. It is intentionally conservative: uncertain changes break the prefix.
        message.parts.forEach { part ->
            append(part::class.simpleName.orEmpty())
            append(':')
            append(part.toString())
            append('\u001e')
        }
    }

    private fun commonPrefixCount(previous: List<UIMessage>, current: List<UIMessage>): Int {
        val limit = minOf(previous.size, current.size)
        for (index in 0 until limit) {
            if (messageSignature(previous[index]) != messageSignature(current[index])) return index
        }
        return limit
    }

    private fun estimateTokens(messages: List<UIMessage>): Int = messages.sumOf { message ->
        // A lightweight character-based estimate, not a provider tokenizer.
        val characters = message.parts.sumOf { it.toString().length }
        ((characters + 3) / 4) + 4
    }

    private fun fullText(message: UIMessage): String =
        message.parts.filterIsInstance<UIMessagePart.Text>()
            .joinToString("\n") { it.text }
            .ifBlank { "(no text content)" }

    private fun firstDiffIndex(old: String, new: String): Int {
        val limit = minOf(old.length, new.length)
        for (index in 0 until limit) if (old[index] != new[index]) return index
        return limit
    }

    private fun diffWindow(value: String, index: Int): String {
        if (value.isBlank()) return "(empty)"
        val start = (index - 52).coerceAtLeast(0)
        val end = (index + 112).coerceAtMost(value.length)
        return (if (start > 0) "…" else "") + value.substring(start, index.coerceIn(start, end)) +
            "▶" + value.substring(index.coerceIn(start, end), end) + (if (end < value.length) "…" else "")
    }

    private fun escapeHtml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    private fun roleName(role: MessageRole): String = when (role) {
        MessageRole.SYSTEM -> "System"
        MessageRole.USER -> "User"
        MessageRole.ASSISTANT -> "Assistant"
        else -> "Tool"
    }

    private fun diagnostic(current: Snapshot, previous: Snapshot?): String {
        if (previous == null) {
            return """
                <section class="diagnostic info">
                  <div class="diag-title">Waiting for a comparison</div>
                  <p>This is the first captured request for this conversation. Send another message and inspect again to compare the shared prefix.</p>
                </section>
            """.trimIndent()
        }

        val old = previous.messages
        val now = current.messages
        val common = commonPrefixCount(old, now)
        val prefixTokens = estimateTokens(now.take(common))
        val totalTokens = estimateTokens(now)
        val percent = if (totalTokens > 0) (prefixTokens * 100 / totalTokens).coerceIn(0, 100) else 0

        val detail = when {
            common >= old.size && now.size >= old.size ->
                """<section class="diagnostic good"><div class="diag-title">Stable prefix</div><p>The previous request is preserved as a prefix. New content was appended after it, which is the ideal shape for prefix caching.</p></section>"""
            common < old.size && common < now.size && old[common].role == now[common].role -> {
                val oldText = fullText(old[common])
                val newText = fullText(now[common])
                if (oldText.isNotBlank() && newText.startsWith(oldText)) {
                    """<section class="diagnostic warn"><div class="diag-title">Content appended inside a message</div><p>The first changed message is #${common + 1}. Its old text remains at the beginning, but the appended tail and following messages may not reuse the cache.</p><pre>${escapeHtml(newText.take(260))}</pre></section>"""
                } else {
                    val diff = firstDiffIndex(oldText, newText)
                    """<section class="diagnostic bad"><div class="diag-title">Prefix changed at message #${common + 1}</div><p>The first changed message has role ${roleName(now[common].role)}. Its change can invalidate the cached prefix after this point.</p><div class="diff-label">Previous</div><pre>${escapeHtml(diffWindow(oldText, diff))}</pre><div class="diff-label">Current</div><pre>${escapeHtml(diffWindow(newText, diff))}</pre><p class="hint">The marker shows the first differing character. Message-level comparison is approximate; providers cache serialized token prefixes.</p></section>"""
                }
            }
            else -> {
                val moved = (1..8).firstOrNull { shift ->
                    common + shift < now.size &&
                        messageSignature(old[common]) == messageSignature(now[common + shift])
                }
                if (moved != null) {
                    """<section class="diagnostic warn"><div class="diag-title">Messages inserted before the prefix</div><p>${moved} message(s) appear before the previous message #${common + 1}. The prefix after that insertion may need to be sent again.</p></section>"""
                } else {
                    """<section class="diagnostic warn"><div class="diag-title">Previous context is missing</div><p>The previous message #${common + 1} is no longer at this position. Context truncation, branch switching, or edited history may explain the change.</p></section>"""
                }
            }
        }

        return """
            <section class="cache-stats">
              <div><strong>$common</strong><span>shared messages</span></div>
              <div><strong>$prefixTokens</strong><span>estimated prefix tokens</span></div>
              <div><strong>$percent%</strong><span>estimated prefix share</span></div>
            </section>
            $detail
        """.trimIndent()
    }

    private fun renderPart(part: UIMessagePart): String = when (part) {
        is UIMessagePart.Text -> ""
        is UIMessagePart.Reasoning -> if (part.reasoning.isBlank()) "" else
            """<details class="part reasoning"><summary>Reasoning</summary><pre>${escapeHtml(part.reasoning)}</pre></details>"""
        is UIMessagePart.Tool ->
            """<details class="part tool"><summary>Tool call · ${escapeHtml(part.toolName)}</summary><pre>${escapeHtml(part.input)}</pre></details>"""
        is UIMessagePart.ToolResult ->
            """<details class="part result"><summary>Tool result · ${escapeHtml(part.toolCallId)}</summary><pre>${escapeHtml(part.content.toString())}</pre></details>"""
        else ->
            """<details class="part attachment"><summary>${escapeHtml(part::class.simpleName ?: "Content part")}</summary><pre>${escapeHtml(part.toString())}</pre></details>"""
    }

    private fun buildHtml(current: Snapshot, previous: Snapshot?): String {
        val messages = current.messages
        val common = previous?.let { commonPrefixCount(it.messages, messages) } ?: messages.size
        val totalChars = messages.sumOf { fullText(it).length }
        val totalTokens = estimateTokens(messages)
        val capturedAt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(current.capturedAtMillis))
        val cards = buildString {
            messages.forEachIndexed { index, message ->
                val role = roleName(message.role)
                val status = when {
                    previous == null -> "uncompared"
                    index < common -> "stable"
                    else -> "changed"
                }
                val text = message.parts.filterIsInstance<UIMessagePart.Text>()
                    .joinToString("\n") { it.text }
                val parts = message.parts.joinToString("") { renderPart(it) }
                append(
                    """
                    <article class="message $status">
                      <header class="message-head">
                        <span class="role role-${role.lowercase()}">$role</span>
                        <span class="message-index">#${index + 1}</span>
                        <span class="message-tokens">~${estimateTokens(listOf(message))} tokens</span>
                      </header>
                      ${if (text.isNotBlank()) """<pre class="message-text">${escapeHtml(text)}</pre>""" else """<div class="empty-content">No plain-text body</div>"""}
                      $parts
                    </article>
                    """.trimIndent()
                )
            }
        }

        return """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
              <meta name="color-scheme" content="light dark">
              <title>Context Inspector</title>
              <style>
                :root { color-scheme: light dark; --bg:#f5f5f7; --surface:#fff; --surface2:#f0f0f4; --text:#191a20; --muted:#70717c; --line:#dedfe5; --accent:#6750a4; --green:#23845b; --red:#c24c52; --amber:#a76b19; }
                @media(prefers-color-scheme:dark) { :root { --bg:#101014; --surface:#1b1b21; --surface2:#25252d; --text:#eeeef4; --muted:#aaaab6; --line:#383842; --accent:#c7b8f2; --green:#72d5a4; --red:#ff8b91; --amber:#f0bd6d; } }
                * { box-sizing:border-box; }
                body { margin:0; padding:16px 14px 28px; background:var(--bg); color:var(--text); font-family:Roboto,"Noto Sans",system-ui,sans-serif; font-size:14px; line-height:1.5; }
                .top { display:flex; align-items:flex-start; justify-content:space-between; gap:12px; margin:4px 2px 16px; }
                h1 { margin:0; font-size:22px; font-weight:700; letter-spacing:-.3px; }
                .subtitle { color:var(--muted); font-size:12px; margin-top:3px; }
                .model { background:var(--surface2); border-radius:999px; padding:6px 10px; font-size:11px; font-weight:600; max-width:44%; overflow-wrap:anywhere; }
                .overview { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:8px; margin-bottom:12px; }
                .metric { background:var(--surface); border:1px solid var(--line); border-radius:16px; padding:12px 10px; min-width:0; }
                .metric strong { display:block; font-size:18px; letter-spacing:-.4px; overflow-wrap:anywhere; }
                .metric span { display:block; color:var(--muted); font-size:10px; line-height:1.25; margin-top:3px; }
                .meta { color:var(--muted); font-size:11px; margin:0 3px 14px; }
                .cache-stats { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:8px; margin:0 0 10px; }
                .cache-stats div { background:var(--surface2); border-radius:13px; padding:10px; min-width:0; }
                .cache-stats strong { display:block; font-size:17px; }
                .cache-stats span { color:var(--muted); font-size:10px; }
                .diagnostic { border-radius:16px; padding:13px 14px; margin:0 0 16px; border:1px solid var(--line); background:var(--surface); }
                .diagnostic.good { border-color:color-mix(in srgb,var(--green) 45%,var(--line)); }
                .diagnostic.warn { border-color:color-mix(in srgb,var(--amber) 45%,var(--line)); }
                .diagnostic.bad { border-color:color-mix(in srgb,var(--red) 45%,var(--line)); }
                .diagnostic.info { border-color:color-mix(in srgb,var(--accent) 45%,var(--line)); }
                .diag-title { font-size:14px; font-weight:700; margin-bottom:4px; }
                p { margin:4px 0; color:var(--muted); font-size:12px; }
                .hint { font-size:11px; }
                .section-title { font-size:13px; font-weight:700; margin:18px 2px 9px; display:flex; justify-content:space-between; }
                .message { background:var(--surface); border:1px solid var(--line); border-radius:15px; overflow:hidden; margin-bottom:9px; }
                .message.stable { border-left:3px solid var(--green); }
                .message.changed { border-left:3px solid var(--red); }
                .message.uncompared { border-left:3px solid var(--line); }
                .message-head { display:flex; align-items:center; flex-wrap:wrap; gap:7px; padding:10px 12px; background:var(--surface2); }
                .role { font-size:10px; font-weight:800; text-transform:uppercase; letter-spacing:.6px; }
                .role-system { color:var(--amber); } .role-user { color:#4288d8; } .role-assistant { color:var(--green); } .role-tool { color:var(--accent); }
                .message-index { color:var(--muted); font-size:11px; }
                .message-tokens { margin-left:auto; color:var(--muted); font-size:10px; }
                pre { white-space:pre-wrap; overflow-wrap:anywhere; word-break:break-word; font:12px/1.55 ui-monospace,monospace; margin:0; }
                .message-text { padding:12px; color:var(--text); }
                .empty-content { padding:10px 12px; color:var(--muted); font-size:11px; font-style:italic; }
                .part { border-top:1px solid var(--line); }
                .part summary { cursor:pointer; padding:9px 12px; color:var(--muted); font-size:11px; font-weight:600; }
                .part pre { padding:0 12px 12px; color:var(--muted); }
                .diff-label { color:var(--muted); font-size:10px; text-transform:uppercase; font-weight:700; margin-top:8px; }
                .foot { color:var(--muted); font-size:10px; text-align:center; padding:16px 8px 0; }
              </style>
            </head>
            <body>
              <div class="top">
                <div><h1>Context Inspector</h1><div class="subtitle">Prompt cache diagnostics · latest prepared request</div></div>
                <div class="model">${escapeHtml(current.modelName)}</div>
              </div>
              <section class="overview">
                <div class="metric"><strong>${messages.size}</strong><span>messages sent</span></div>
                <div class="metric"><strong>${totalTokens}</strong><span>estimated tokens</span></div>
                <div class="metric"><strong>${totalChars}</strong><span>text characters</span></div>
              </section>
              <div class="meta">Model ID: ${escapeHtml(current.modelId)} · Captured: ${escapeHtml(capturedAt)}</div>
              ${diagnostic(current, previous)}
              <div class="section-title"><span>Request messages</span><span>${messages.size} items</span></div>
              $cards
              <div class="foot">Snapshots stay in memory and are cleared when the app process ends. Token counts are rough text-based estimates; attachments, provider framing and provider-side cache hits may differ.</div>
            </body>
            </html>
        """.trimIndent()
    }
}
