package org.wikipedia.page;

import android.content.Context;
import android.os.Bundle;
import android.widget.FrameLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.wikipedia.WikipediaApp;

import androidx.annotation.NonNull;

/**
 * Descendant of BottomSheetDialog that prevents the background from being dimmed.
 */
public class NoDimBottomSheetDialog extends BottomSheetDialog {
    public NoDimBottomSheetDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setDimAmount(0f);
    }

    @Override
    public void onStop() {
        super.onStop();
        WikipediaApp.getInstance().getRefWatcher().watch(this);
    }

    protected void startExpanded() {
        /*
        HACK: We'd like some of our bottom sheets to be fully expanded when opened (as
        opposed to expanded to the peek height). In order to do this, however, we have to
        call setState() only *after* the dialog is created and laid out.
        https://code.google.com/p/android/issues/detail?id=202174
        TODO: remove when this is improved in the library.
        */
        getWindow().getDecorView().post(() -> {
            FrameLayout bottomSheet = getWindow().getDecorView().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            BottomSheetBehavior<?> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });
    }
}
