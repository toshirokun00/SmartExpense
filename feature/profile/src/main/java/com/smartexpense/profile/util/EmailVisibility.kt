package com.smartexpense.profile.util

/** Fixed-width mask so the email's content and length stay private. */
const val HIDDEN_EMAIL = "************"

/**
 * Returns the email to show on screen: the real address when the user allows
 * it, otherwise an asterisk mask. A blank email is left blank so callers can
 * still fall back to their own placeholder text.
 */
fun maskEmail(email: String, isVisible: Boolean): String {
    if (isVisible || email.isBlank()) return email
    return HIDDEN_EMAIL
}
