package cuchaz.kludge.imgui

import com.sun.jna.NativeLong

/*
    On some platforms (eg linux, osx), `long` values are 8 bytes wide.
    On other platforms (cough cough, windows), `long` values are only 4 bytes wide.
    JNA needs the `NativeLong` class to signal when to translate between widths in the JVM.

    Pointers, however, are always 8 bytes wide on 64-bit systems, and so can map to the `long` JVM type.
    So we'll need to distinguish between pointer longs and merely regular longs when porting to Windows.
*/

fun Int.toNative() = toLong().toNative()
fun Long.toNative() = NativeLong(this)

// NOTE: use -Djna.protected to enable stack traces for JNA crashes
