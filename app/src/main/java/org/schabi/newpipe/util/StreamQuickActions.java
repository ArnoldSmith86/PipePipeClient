package org.schabi.newpipe.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import org.schabi.newpipe.MainActivity;
import org.schabi.newpipe.R;
import org.schabi.newpipe.error.ErrorInfo;
import org.schabi.newpipe.error.ErrorUtil;
import org.schabi.newpipe.error.UserAction;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.info_list.dialog.StreamDialogDefaultEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The context-menu entries a user can promote to icon-only buttons on every stream in a list
 * (Settings -&gt; Appearance -&gt; "Buttons on list items").
 *
 * <p>Each button runs the very same {@link StreamDialogDefaultEntry} action the long-press menu
 * runs, so there is one implementation of "share this", "download this", "cache this" and the
 * button is only a second way to reach it.</p>
 *
 * <p>The catalogue is keyed by the enum constant's <em>name</em> rather than the constant itself,
 * and the icons are looked up by resource name - which is also why an icon here can name a
 * drawable another feature branch ships, such as the cache's {@code ic_cached_offline}. That is what lets a feature branch add an entry -
 * the offline cache adds {@code CACHE} - and have it appear here automatically once the branches
 * are merged, without this class having to know that the entry exists.</p>
 */
public final class StreamQuickActions {

    private static final String TAG = "StreamQuickActions";

    /** An entry that can be promoted to a button, with the icon to show for it. */
    public static final class Action {
        @NonNull public final String name;
        @NonNull public final String iconName;

        private Action(@NonNull final String name, @NonNull final String iconName) {
            this.name = name;
            this.iconName = iconName;
        }

        /**
         * The entry's own label, which is also the button's tooltip and content description. Read
         * from the entry rather than repeated here, so an entry from another feature branch needs
         * no string of its own in this class.
         */
        @NonNull
        public String title(@NonNull final Context context) {
            final StreamDialogDefaultEntry entry = entryOrNull(name);
            return entry == null ? name : context.getString(entry.resource);
        }
    }

    /**
     * Every entry that makes sense on its own, in the order the buttons appear.
     *
     * <p>Deliberately not "all of them": {@code DELETE}, {@code SET_AS_PLAYLIST_THUMBNAIL},
     * {@code NAVIGATE_TO} and {@code SHOW_STREAM_DETAILS} throw unless the dialog that shows them
     * supplies the action itself, so a button could only ever crash.</p>
     */
    private static final Action[] CATALOGUE = {
            new Action("ENQUEUE", "ic_playlist_add"),
            new Action("ENQUEUE_NEXT", "ic_next"),
            new Action("START_HERE_ON_BACKGROUND", "ic_headset"),
            new Action("START_HERE_ON_POPUP", "ic_picture_in_picture"),
            new Action("CACHE", "ic_cached_offline"),
            new Action("DOWNLOAD", "ic_file_download"),
            new Action("APPEND_PLAYLIST", "ic_playlist_add_check"),
            new Action("MARK_AS_WATCHED", "ic_done"),
            new Action("SHARE", "ic_share"),
            new Action("OPEN_IN_BROWSER", "ic_language"),
            new Action("PLAY_WITH_KODI", "ic_cast"),
            new Action("SHOW_CHANNEL_DETAILS", "ic_channels"),
            new Action("ADD_TO_FILTER_LIST", "ic_blocking"),
    };

    private StreamQuickActions() {
        // no instance
    }

    /**
     * The actions this build can actually offer: an entry whose enum constant or icon is missing
     * belongs to a feature that is not in this build, and is silently left out.
     */
    @NonNull
    public static List<Action> available(@NonNull final Context context) {
        final List<Action> actions = new ArrayList<>(CATALOGUE.length);
        for (final Action action : CATALOGUE) {
            if (entryOrNull(action.name) != null && iconOf(context, action) != 0) {
                actions.add(action);
            }
        }
        return actions;
    }

    /** The actions the user picked, in catalogue order. Empty means the feature is off. */
    @NonNull
    public static List<Action> selected(@NonNull final Context context) {
        final SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        final Set<String> chosen = preferences.getStringSet(
                context.getString(R.string.list_quick_actions_key), Collections.emptySet());
        if (chosen == null || chosen.isEmpty()) {
            return Collections.emptyList();
        }
        final List<Action> actions = new ArrayList<>(chosen.size());
        for (final Action action : available(context)) {
            if (chosen.contains(action.name)) {
                actions.add(action);
            }
        }
        return actions;
    }

    /**
     * Fills a list item's button strip with the chosen actions, or hides it when there are none.
     * Buttons are reused across binds, so scrolling doesn't rebuild them.
     *
     * @param container the strip in the item layout; missing in layouts that have no room for one
     * @param onAction  invoked with the entry name of whichever button was tapped
     */
    public static void bind(@Nullable final ViewGroup container,
                            @NonNull final Consumer<String> onAction) {
        if (container == null) {
            return;
        }
        final Context context = container.getContext();
        final List<Action> actions = selected(context);
        if (actions.isEmpty()) {
            container.setVisibility(View.GONE);
            container.removeAllViews();
            return;
        }

        container.setVisibility(View.VISIBLE);
        while (container.getChildCount() > actions.size()) {
            container.removeViewAt(container.getChildCount() - 1);
        }
        while (container.getChildCount() < actions.size()) {
            container.addView(newButton(context));
        }

        for (int i = 0; i < actions.size(); i++) {
            final Action action = actions.get(i);
            final ImageButton button = (ImageButton) container.getChildAt(i);
            final String title = action.title(context);
            button.setImageResource(iconOf(context, action));
            button.setContentDescription(title);
            TooltipCompat.setTooltipText(button, title);
            button.setOnClickListener(view -> onAction.accept(action.name));
        }
    }

    /**
     * Runs the entry the way the long-press menu would.
     *
     * @param fragment the fragment showing the list: the entries open dialogs and read the queue
     *                 through it, exactly as they do from {@code InfoItemDialog}
     */
    public static void run(@NonNull final Fragment fragment,
                           @NonNull final StreamInfoItem item,
                           @NonNull final String entryName) {
        final StreamDialogDefaultEntry entry = entryOrNull(entryName);
        if (entry == null) {
            return;
        }
        try {
            entry.action.onClick(fragment, item);
        } catch (final Exception e) {
            if (MainActivity.DEBUG) {
                Log.e(TAG, "quick action " + entryName + " failed on " + item.getUrl(), e);
            }
            ErrorUtil.showSnackbar(fragment, new ErrorInfo(e, UserAction.UI_ERROR,
                    "Quick action " + entryName + " on " + item.getUrl(),
                    item.getServiceId()));
        }
    }

    @NonNull
    private static ImageButton newButton(@NonNull final Context context) {
        final ImageButton button = new ImageButton(context);
        final int size = dp(context, 40);
        final int padding = dp(context, 8);
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        button.setLayoutParams(params);
        button.setPadding(padding, padding, padding, padding);
        button.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        button.setBackgroundResource(resolveAttribute(context,
                androidx.appcompat.R.attr.selectableItemBackgroundBorderless));
        ImageViewCompat.setImageTintList(button,
                android.content.res.ColorStateList.valueOf(secondaryTextColor(context)));
        return button;
    }

    @Nullable
    private static StreamDialogDefaultEntry entryOrNull(@NonNull final String name) {
        try {
            return StreamDialogDefaultEntry.valueOf(name);
        } catch (final IllegalArgumentException e) {
            // An entry from a feature this build does not have.
            return null;
        }
    }

    private static int iconOf(@NonNull final Context context, @NonNull final Action action) {
        return context.getResources().getIdentifier(
                action.iconName, "drawable", context.getPackageName());
    }

    private static int dp(@NonNull final Context context, final int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics()));
    }

    private static int resolveAttribute(@NonNull final Context context, final int attribute) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attribute, value, true);
        return value.resourceId;
    }

    private static int secondaryTextColor(@NonNull final Context context) {
        final TypedArray array = context.getTheme().obtainStyledAttributes(
                new int[]{android.R.attr.textColorSecondary});
        try {
            return array.getColor(0, 0xFF888888);
        } finally {
            array.recycle();
        }
    }
}
