package com.example.data

import android.util.Log

object FormSecuritySanitizer {

    private const val TAG = "FormSecuritySanitizer"

    // Patterns for XSS detection and mitigation
    private val scriptTagPattern = Regex("<script\\b[^<]*(?:(?!</script>)<[^<]*)*</script>", RegexOption.IGNORE_CASE)
    private val htmlTagPattern = Regex("<[^>]*>", RegexOption.IGNORE_CASE)
    private val javascriptUriPattern = Regex("javascript:", RegexOption.IGNORE_CASE)
    private val eventHandlerPattern = Regex("on\\w+\\s*=\\s*[\"'][^\"']*[\"']|on\\w+\\s*=\\s*[^\\s>]+", RegexOption.IGNORE_CASE)

    // Patterns for SQL Injection protection (defense-in-depth)
    // Room uses Prepared Statements / bound parameters, which inherently protects against SQL injection.
    // However, to satisfy high security constraints, we sanitize text inputs against raw SQL patterns.
    private val sqlCommentPattern = Regex("--|/\\*|\\*/|;", RegexOption.IGNORE_CASE)
    private val sqlKeywordPattern = Regex("\\b(UNION|SELECT|DROP|TRUNCATE|DELETE|INSERT|UPDATE|ALTER|CREATE|EXEC|GRANT|REVOKE)\\b", RegexOption.IGNORE_CASE)

    /**
     * Sanitizes raw text input to prevent XSS (Cross-Site Scripting).
     * It strips script tags, HTML tags, event handlers, and javascript protocol URIs.
     */
    fun sanitizeXss(input: String?): String {
        if (input == null) return ""
        var cleaned = input

        // 1. Strip script tags entirely
        if (scriptTagPattern.containsMatchIn(cleaned)) {
            Log.w(TAG, "Security Alert: Script tag detected and blocked in input.")
            cleaned = cleaned.replace(scriptTagPattern, "[BLOCKED_SCRIPT]")
        }

        // 2. Strip standard event handlers (e.g. onload, onerror)
        if (eventHandlerPattern.containsMatchIn(cleaned)) {
            Log.w(TAG, "Security Alert: HTML Event Handler detected and blocked.")
            cleaned = cleaned.replace(eventHandlerPattern, "[BLOCKED_EVENT]")
        }

        // 3. Strip Javascript URIs (e.g. javascript:alert)
        if (javascriptUriPattern.containsMatchIn(cleaned)) {
            Log.w(TAG, "Security Alert: Javascript URI schema blocked.")
            cleaned = cleaned.replace(javascriptUriPattern, "blocked-uri:")
        }

        // 4. Strip any remaining HTML tags to prevent custom layout injection
        if (htmlTagPattern.containsMatchIn(cleaned)) {
            Log.w(TAG, "Security Alert: Raw HTML tag input stripped.")
            cleaned = cleaned.replace(htmlTagPattern, "")
        }

        // 5. Escape HTML metacharacters as entity encoding for safe UI rendering
        return cleaned
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .trim()
    }

    /**
     * Sanitizes inputs to prevent SQL Injection-style exploits or anomalies.
     * Escapes critical characters and sanitizes suspect SQL syntax patterns.
     */
    fun sanitizeSql(input: String?): String {
        if (input == null) return ""
        var cleaned = input

        // 1. Strip standard SQL comment and terminal delimiters (--, /*, */, ;)
        if (sqlCommentPattern.containsMatchIn(cleaned)) {
            Log.w(TAG, "Security Warning: Potential SQL Injection comment delimiter removed.")
            cleaned = cleaned.replace(sqlCommentPattern, " ")
        }

        // 2. Sanitize SQL Keywords if they appear alongside quotes or comparison operators
        if (sqlKeywordPattern.containsMatchIn(cleaned)) {
            val hasInjectionIndicators = cleaned.contains("'") || cleaned.contains("\"") || cleaned.contains("=") || cleaned.contains("<") || cleaned.contains(">")
            if (hasInjectionIndicators) {
                Log.w(TAG, "Security Alert: SQL Keyword query syntax detected in user input.")
                cleaned = cleaned.replace(sqlKeywordPattern) { match ->
                    "[CLEANED_${match.value.uppercase()}]"
                }
            }
        }

        return cleaned.trim()
    }

    /**
     * Runs both XSS and SQL injection sanitizers sequentially for maximum security.
     */
    fun sanitizeAll(input: String?): String {
        if (input.isNullOrBlank()) return ""
        return sanitizeSql(sanitizeXss(input))
    }

    /**
     * Validates and sanitizes a post caption.
     * Enforces size boundaries and strips injection strings.
     */
    fun sanitizePostCaption(caption: String): String {
        val truncated = if (caption.length > 500) {
            Log.w(TAG, "Validation Notice: Post caption truncated to 500 characters limit.")
            caption.take(500)
        } else {
            caption
        }
        return sanitizeAll(truncated)
    }

    /**
     * Validates and sanitizes a chat message.
     */
    fun sanitizeChatMessage(message: String): String {
        val truncated = if (message.length > 1000) {
            Log.w(TAG, "Validation Notice: Chat message truncated to 1000 characters limit.")
            message.take(1000)
        } else {
            message
        }
        return sanitizeAll(truncated)
    }

    /**
     * Validates and sanitizes marketplace product names.
     */
    fun sanitizeProductName(name: String): String {
        val truncated = if (name.length > 100) {
            name.take(100)
        } else {
            name
        }
        return sanitizeAll(truncated)
    }

    /**
     * Validates and sanitizes marketplace product descriptions.
     */
    fun sanitizeProductDescription(description: String): String {
        val truncated = if (description.length > 800) {
            description.take(800)
        } else {
            description
        }
        return sanitizeAll(truncated)
    }

    /**
     * Validates and sanitizes creator profile names.
     */
    fun sanitizeProfileName(name: String): String {
        val filtered = name.filter { it.isLetterOrDigit() || it.isWhitespace() || it == '.' || it == '_' || it == '-' }
        val truncated = if (filtered.length > 50) filtered.take(50) else filtered
        return sanitizeAll(truncated)
    }

    /**
     * Validates and sanitizes creator handles (no spaces, alpha-numeric/underscore only).
     */
    fun sanitizeProfileHandle(handle: String): String {
        val filtered = handle.lowercase().filter { it.isLetterOrDigit() || it == '_' || it == '-' }
        val truncated = if (filtered.length > 30) filtered.take(30) else filtered
        return sanitizeAll(truncated)
    }

    /**
     * Validates and sanitizes external URIs/URLs to ensure safe schemes.
     * Prevents javascript: or data: URL-based XSS attacks in hyperlinks.
     */
    fun validateUrl(url: String, defaultUrl: String): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return defaultUrl

        val isSafeScheme = trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)

        return if (isSafeScheme) {
            // Strip raw HTML tags or whitespace to be extra safe
            trimmed.replace(htmlTagPattern, "").filter { !it.isWhitespace() }
        } else {
            Log.e(TAG, "Security Alert: Malicious or unsupported URL scheme rejected.")
            defaultUrl
        }
    }

    /**
     * Validates currency amounts and pricing to prevent fraud, integer overflows, or negative values.
     */
    fun validatePrice(price: Double): Double {
        return when {
            price.isNaN() || price.isInfinite() -> 0.0
            price < 0.0 -> 0.0
            price > 10000.0 -> 10000.0 // upper safeguard limit
            else -> price
        }
    }

    /**
     * Prevents emulator double typing keyboard issue by detecting if a single edit event
     * has inserted two identical characters at the cursor point, and deduplicating it.
     */
    fun preventDoubleTyping(newValue: String, oldValue: String): String {
        if (newValue.length != oldValue.length + 2) {
            return newValue
        }
        // Find where the difference starts from left
        var prefixLen = 0
        while (prefixLen < oldValue.length && prefixLen < newValue.length && oldValue[prefixLen] == newValue[prefixLen]) {
            prefixLen++
        }
        // Find where the difference ends from right
        var suffixLen = 0
        while (suffixLen < oldValue.length - prefixLen && 
               suffixLen < newValue.length - prefixLen && 
               oldValue[oldValue.length - 1 - suffixLen] == newValue[newValue.length - 1 - suffixLen]) {
            suffixLen++
        }
        
        // Extract the inserted segment
        val inserted = newValue.substring(prefixLen, newValue.length - suffixLen)
        if (inserted.length == 2 && inserted[0] == inserted[1]) {
            // Deduplicate the double typed character
            return newValue.substring(0, prefixLen) + inserted[0] + newValue.substring(newValue.length - suffixLen)
        }
        return newValue
    }
}
