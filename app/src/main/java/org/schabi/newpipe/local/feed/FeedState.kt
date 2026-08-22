package org.schabi.newpipe.local.feed

import androidx.annotation.StringRes
import org.schabi.newpipe.local.feed.item.StreamItem
import java.time.OffsetDateTime

sealed class FeedState {
    data class ProgressState(
        val currentProgress: Int = -1,
        val maxProgress: Int = -1,
        @StringRes val progressMessage: Int = 0
    ) : FeedState()

    data class LoadedState(
        val items: List<StreamItem>,
        val oldestUpdate: OffsetDateTime? = null,
        val notLoadedCount: Long,
        val itemsErrors: List<Throwable> = emptyList(),
        /**
         * Whether this list is the answer to a filter the user just changed, rather than the same
         * list with fresher data. Those two want opposite things from the scroll position: a
         * filter change should show the top of the new list, an update should stay where the user
         * was reading.
         */
        val filterChanged: Boolean = false
    ) : FeedState()

    data class ErrorState(
        val error: Throwable? = null
    ) : FeedState()
}
