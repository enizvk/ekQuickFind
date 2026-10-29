package com.ekquickfind.core.search

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

class ModuleIsolationTest {
    @Test
    fun `android framework classes are not on the classpath`() {
        assertThrows<ClassNotFoundException> { Class.forName("android.content.Context") }
    }

    @Test
    fun `main sources never import android packages`() {
        val offenders =
            File("src/main")
                .walkTopDown()
                .filter { it.extension == "kt" }
                .filter { file -> file.readLines().any { it.startsWith("import android") || it.startsWith("import androidx") } }
                .map { it.name }
                .toList()
        assertTrue(offenders.isEmpty(), "Android imports found in: $offenders")
    }
}
