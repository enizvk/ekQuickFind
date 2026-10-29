package com.ekquickfind.app.shortcuts

import com.ekquickfind.app.search.ZeroQuerySource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Keeps launcher shortcuts in step with pins for as long as [scope] lives. */
class ShortcutSync(
    private val zeroQuery: ZeroQuerySource,
    private val publisher: ShortcutsPublisher,
) {
    fun start(scope: CoroutineScope): Job =
        scope.launch {
            zeroQuery.state
                .map { it.pinned }
                .distinctUntilChanged { a, b -> a.map { it.id to it.title } == b.map { it.id to it.title } }
                .collect { publisher.publish(it) }
        }
}
