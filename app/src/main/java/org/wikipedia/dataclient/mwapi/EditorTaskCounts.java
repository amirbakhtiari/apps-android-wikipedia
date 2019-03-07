package org.wikipedia.dataclient.mwapi;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

@SuppressWarnings("unused")
public class EditorTaskCounts {
    @Nullable private Counts counts;
    @Nullable private Object targetsPassed;

    public class Counts {
        @Nullable @SerializedName("app_description_edits") private Map<String, Integer> appDescriptionEdits;
    }
}
