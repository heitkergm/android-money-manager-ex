/*
 * Copyright (C) 2012-2016 The Android Money Manager Ex Project Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.money.manager.ex.settings;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.money.manager.ex.MoneyManagerApplication;

import javax.inject.Inject;

import dagger.Lazy;
import timber.log.Timber;

public class SettingsActivity
    extends BaseSettingsFragmentActivity {

    public static final String EXTRA_FRAGMENT = "extraFragment";

    @Inject Lazy<AppSettings> appSettingsLazy;
    private String initialTheme;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        MoneyManagerApplication.getApp().iocComponent.inject(this);

        showFragment();

        // store the current theme
        initialTheme = appSettingsLazy.get().getLookAndFeelSettings().getTheme();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // apply theme if changed.
        String currentTheme = appSettingsLazy.get().getLookAndFeelSettings().getTheme();
        if (!currentTheme.equals(initialTheme)) {
            recreate();
        }
    }

    private void showFragment() {
        // figure out which fragment to show
        PreferenceFragment fragment = null;

        Intent intent = getIntent();

        String fragmentName = intent.getStringExtra(EXTRA_FRAGMENT);
        if (fragmentName != null && fragmentName.equals(PerDatabaseFragment.class.getSimpleName())) {
            fragment = new PerDatabaseFragment();
        }

        // default
        if (fragment == null) {
            fragment = new SettingsFragment();
        }

        setSettingFragment(fragment);
    }
}
