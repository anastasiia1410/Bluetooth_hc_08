package com.example.presentation.utils

import android.content.Context
import android.content.pm.PackageManager
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.presentation.R
import com.example.presentation.screens.connected_device.entity.Params
import java.io.ByteArrayOutputStream
import java.lang.IllegalArgumentException

val newline_crlf = "\r\n"
val newline_lf = "\n"

fun TextView?.inputText(): String {
    return this?.text?.toString() ?: ""
}

val String.toEditText: Editable get() = Editable.Factory.getInstance().newEditable(this)

fun fromHexString(s: CharSequence): ByteArray {
    val buf = ByteArrayOutputStream()
    var b: Byte = 0
    var nibble = 0

    for (pos in 0 until s.length) {
        if (nibble == 2) {
            buf.write(b.toInt())
            nibble = 0
            b = 0
        }
        val c = s[pos].toInt()
        when {
            c in '0'.toInt()..'9'.toInt() -> {
                nibble++
                b = (b * 16 + (c - '0'.toInt())).toByte()
            }

            c in 'A'.toInt()..'F'.toInt() -> {
                nibble++
                b = (b * 16 + (c - 'A'.toInt() + 10)).toByte()
            }

            c in 'a'.toInt()..'f'.toInt() -> {
                nibble++
                b = (b * 16 + (c - 'a'.toInt() + 10)).toByte()
            }
        }
    }

    if (nibble > 0) {
        buf.write(b.toInt())
    }

    return buf.toByteArray()
}


fun toHexString(sb: StringBuilder, buf: ByteArray) {
    toHexString(sb, buf, 0, buf.size)
}

fun toHexString(sb: StringBuilder, buf: ByteArray, begin: Int, end: Int) {
    for (pos in begin until end) {
        if (sb.isNotEmpty()) {
            sb.append(' ')
        }
        var c = (buf[pos].toInt() and 0xff) / 16
        c = if (c >= 10) c + 'A'.toInt() - 10 else c + '0'.toInt()
        sb.append(c.toChar())
        c = (buf[pos].toInt() and 0xff) % 16
        c = if (c >= 10) c + 'A'.toInt() - 10 else c + '0'.toInt()
        sb.append(c.toChar())
    }
}

fun toCaretString(s: CharSequence, keepNewline: Boolean, caretBackgroundColor: Int): CharSequence {
    return toCaretString(s, keepNewline, s.length, caretBackgroundColor)
}

fun toCaretString(
    s: CharSequence,
    keepNewline: Boolean,
    length: Int,
    caretBackgroundColor: Int,
): CharSequence {
    var found = false
    for (pos in 0 until length) {
        if (s[pos] < 32.toChar() && (!keepNewline || s[pos] != '\n')) {
            found = true
            break
        }
    }
    if (!found) {
        return s
    }
    val sb = SpannableStringBuilder()
    for (pos in 0 until length) {
        if (s[pos] < 32.toChar() && (!keepNewline || s[pos] != '\n')) {
            sb.append('^')
            sb.append((s[pos] + 64))
            sb.setSpan(
                BackgroundColorSpan(caretBackgroundColor),
                sb.length - 2,
                sb.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } else {
            sb.append(s[pos])
        }
    }
    return sb
}


fun hasPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}

fun setDividerColor(params: Params): Int {
    return when (params.name) {
        R.string.maximum_voltage -> R.color.voltage_back
        R.string.frequency_maximum -> R.color.frequency_back
        R.string.start_frequency -> R.color.start_frequency_back
        R.string.boost -> R.color.boost_back
        R.string.time_of_dispersal -> R.color.dispersal_back
        R.string.braking_time -> R.color.braking_time_back
        R.string.braking -> R.color.braking_back
        else -> throw IllegalArgumentException()
    }
}
