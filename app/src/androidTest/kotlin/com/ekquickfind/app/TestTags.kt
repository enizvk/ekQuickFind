package androidx.compose.ui.test

import androidx.compose.ui.semantics.SemanticsProperties

/** Matches nodes whose test tag starts with [prefix] (for tagged families like "tile:…"). */
fun hasTestTagStartingWith(prefix: String): SemanticsMatcher =
    SemanticsMatcher("TestTag starts with '$prefix'") { node ->
        node.config.getOrElse(SemanticsProperties.TestTag) { "" }.startsWith(prefix)
    }
