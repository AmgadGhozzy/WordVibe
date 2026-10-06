package com.venom.harness

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Permanent mechanical guard for the container rule at the top of VmSessionHarness.kt
 * (ticket 11, PM round-7): the shared fakes are written from repository consumers on
 * Dispatchers.IO and read from test threads, so the file must only ever build
 * thread-safe containers — ConcurrentHashMap and CopyOnWriteArrayList.
 *
 * The check is deliberately blunt and file-wide: it reads VmSessionHarness.kt from
 * disk and fails on any mutableMapOf / mutableListOf / raw ArrayList constructor
 * outside comment lines. Both the generic and the bare call forms are caught
 * (mutableListOf<X>() and mutableListOf() alike), while the "ArrayList" substring
 * inside CopyOnWriteArrayList is explicitly excluded — that is the sanctioned
 * container. Each violation is reported with its line number and full line text.
 */
class HarnessContainerHygieneTest {

    @Test
    fun `harness source bans thread-unsafe mutable container constructors`() {
        val source = locateHarnessSource()
            ?: error(
                "VmSessionHarness.kt not found on disk from working dir " +
                    "'${System.getProperty("user.dir")}' — the container rule cannot be enforced"
            )
        // mutableMapOf[<(] / mutableListOf[<(]: catches both mutableListOf<X>() and
        // mutableListOf() forms. (?<![A-Za-z])ArrayList<: flags raw ArrayList while
        // never flagging CopyOnWriteArrayList, the sanctioned container.
        val banned = Regex("mutableMapOf[<(]|mutableListOf[<(]|(?<![A-Za-z])ArrayList<")
        val violations = mutableListOf<Pair<Int, String>>()
        var inBlockComment = false
        source.useLines { lines ->
            lines.forEachIndexed { index, raw ->
                val trimmed = raw.trim()
                val isComment = if (inBlockComment) {
                    if (trimmed.contains("*/")) inBlockComment = false
                    true
                } else {
                    when {
                        trimmed.startsWith("//") -> true
                        trimmed.startsWith("/*") -> {
                            if (!trimmed.contains("*/")) inBlockComment = true
                            true
                        }
                        else -> false
                    }
                }
                if (!isComment && banned.containsMatchIn(raw)) {
                    violations += (index + 1) to trimmed
                }
            }
        }
        assertTrue(
            violations.isEmpty(),
            "Thread-unsafe containers are banned in VmSessionHarness.kt (PM round-7 " +
                "container rule) — use ConcurrentHashMap / CopyOnWriteArrayList instead. " +
                "Offending lines:\n" +
                violations.joinToString("\n") { "  line ${it.first}: ${it.second}" }
        )
    }

    /**
     * Resolves the harness source on disk: first the two Gradle-typical relative
     * layouts (module dir and repo root as the working dir), then an upward walk
     * from user.dir so the guard survives any working-directory convention.
     */
    private fun locateHarnessSource(): File? {
        val moduleRelative = "src/test/java/com/venom/harness/VmSessionHarness.kt"
        val candidates = mutableListOf(File(moduleRelative), File("app/$moduleRelative"))
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        repeat(8) {
            val current = dir ?: return@repeat
            candidates += File(current, moduleRelative)
            candidates += File(current, "app/$moduleRelative")
            dir = current.parentFile
        }
        return candidates.firstOrNull { it.isFile }
    }
}
