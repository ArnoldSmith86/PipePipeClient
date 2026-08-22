package org.schabi.newpipe.settings;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;

import org.schabi.newpipe.R;
import org.schabi.newpipe.util.Constants;
import org.schabi.newpipe.util.StreamQuickActions;
import org.schabi.newpipe.util.ThemeHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AppearanceSettingsFragment extends BasePreferenceFragment {

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        addPreferencesFromResourceRegistry();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            removePreference(getString(R.string.player_notification_screen_key));
        }

        setUpQuickActionsPreference();

        final String themeKey = getString(R.string.theme_key);
        // the key of the active theme when settings were opened (or recreated after theme change)
        final String startThemeKey = defaultPreferences
                .getString(themeKey, getString(R.string.default_theme_value));
        final String autoDeviceThemeKey = getString(R.string.auto_device_theme_key);
        findPreference(themeKey).setOnPreferenceChangeListener((preference, newValue) -> {
            if (newValue.toString().equals(autoDeviceThemeKey)) {
                Toast.makeText(getContext(), getString(R.string.select_night_theme_toast),
                        Toast.LENGTH_LONG).show();
            }

            applyThemeChange(startThemeKey, themeKey, newValue);
            return false;
        });

        final String nightThemeKey = getString(R.string.night_theme_key);
        if (startThemeKey.equals(autoDeviceThemeKey)) {
            final String startNightThemeKey = defaultPreferences
                    .getString(nightThemeKey, getString(R.string.default_night_theme_value));

            findPreference(nightThemeKey).setOnPreferenceChangeListener((preference, newValue) -> {
                applyThemeChange(startNightThemeKey, nightThemeKey, newValue);
                return false;
            });
        } else {
            removePreference(nightThemeKey);
        }
    }

    private void removePreference(final String preferenceKey) {
        final Preference preference = findPreference(preferenceKey);
        if (preference != null) {
            getPreferenceScreen().removePreference(preference);
        }
    }

    private void applyThemeChange(final String beginningThemeKey,
                                  final String themeKey,
                                  final Object newValue) {
        defaultPreferences.edit().putBoolean(Constants.KEY_THEME_CHANGE, true).apply();
        defaultPreferences.edit().putString(themeKey, newValue.toString()).apply();

        ThemeHelper.setDayNightMode(getContext(), newValue.toString());

        if (!newValue.equals(beginningThemeKey) && getActivity() != null) {
            // if it's not the current theme
            ActivityCompat.recreate(getActivity());
        }
    }

    /**
     * Fills the "Buttons on list items" preference with whatever context menu entries this build
     * has, and keeps its summary showing the current choice. Building the list here rather than in
     * XML is what lets an entry from another feature - the offline cache's Cache/Uncache - appear
     * on its own once the branches are merged.
     */
    private void setUpQuickActionsPreference() {
        final MultiSelectListPreference preference =
                findPreference(getString(R.string.list_quick_actions_key));
        if (preference == null) {
            return;
        }

        final List<StreamQuickActions.Action> actions =
                StreamQuickActions.available(requireContext());
        final List<CharSequence> labels = new ArrayList<>(actions.size());
        final List<CharSequence> values = new ArrayList<>(actions.size());
        for (final StreamQuickActions.Action action : actions) {
            labels.add(action.title(requireContext()));
            values.add(action.name);
        }
        preference.setEntries(labels.toArray(new CharSequence[0]));
        preference.setEntryValues(values.toArray(new CharSequence[0]));

        updateQuickActionsSummary(preference, preference.getValues());
        preference.setOnPreferenceChangeListener((pref, newValue) -> {
            if (newValue instanceof Set) {
                //noinspection unchecked
                updateQuickActionsSummary((MultiSelectListPreference) pref,
                        (Set<String>) newValue);
            }
            return true;
        });
    }

    private void updateQuickActionsSummary(@NonNull final MultiSelectListPreference preference,
                                           @Nullable final Set<String> chosen) {
        if (chosen == null || chosen.isEmpty()) {
            preference.setSummary(R.string.list_quick_actions_summary_none);
            return;
        }
        final List<String> labels = new ArrayList<>(chosen.size());
        for (final StreamQuickActions.Action action
                : StreamQuickActions.available(requireContext())) {
            if (chosen.contains(action.name)) {
                labels.add(action.title(requireContext()));
            }
        }
        preference.setSummary(TextUtils.join(", ", labels));
    }
}
