package org.wikipedia.history;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;

import org.wikipedia.views.SearchActionProvider;

import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.core.view.MenuItemCompat;

public abstract class SearchActionModeCallback implements ActionMode.Callback {
    public static final String ACTION_MODE_TAG = "searchActionMode";

    public static boolean is(@Nullable ActionMode mode) {
        return mode != null && ACTION_MODE_TAG.equals(mode.getTag());
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTag(ACTION_MODE_TAG);
        MenuItem menuItem = menu.add(getSearchHintString());
        // Manually setup a action provider to be able to adjust the left margin of the search field.
        MenuItemCompat.setActionProvider(menuItem, new SearchActionProvider(getParentContext(), getSearchHintString(), new SearchActionProvider.Callback() {
            @Override
            public void onQueryTextChange(String s) {
                onQueryChange(s);
            }

            @Override
            public void onQueryTextFocusChange() {
                if (finishActionModeIfKeyboardHiding()) {
                    mode.finish();
                }
            }
        }));

        return true;
    }

    protected abstract String getSearchHintString();

    protected abstract void onQueryChange(String s);

    protected abstract boolean finishActionModeIfKeyboardHiding();

    protected abstract Context getParentContext();

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
    }
}
