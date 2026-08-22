package org.schabi.newpipe.util;

import androidx.recyclerview.widget.RecyclerView;

public abstract class OnClickGesture<T> {

    public abstract void selected(T selectedItem);

    public void held(final T selectedItem) {
        // Optional gesture
    }

    public void drag(final T selectedItem, final RecyclerView.ViewHolder viewHolder) {
        // Optional gesture
    }

    /**
     * One of the item's quick-action buttons was tapped (see
     * {@link StreamQuickActions}). {@code actionName} is the name of the
     * {@code StreamDialogDefaultEntry} to run for this item.
     */
    public void quickAction(final T selectedItem, final String actionName) {
        // Optional gesture
    }
}
