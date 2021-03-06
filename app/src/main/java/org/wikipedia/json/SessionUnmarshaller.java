package org.wikipedia.json;

import org.wikipedia.analytics.SessionData;
import org.wikipedia.crash.RemoteLogException;
import org.wikipedia.util.log.L;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class SessionUnmarshaller {
    @NonNull public static SessionData unmarshal(@Nullable String json) {
        SessionData sessionData = null;
        try {
            sessionData = GsonUnmarshaller.unmarshal(SessionData.class, json);
        } catch (Exception e) {
            // Catch all. Any Exception can be thrown when unmarshalling.
            L.logRemoteErrorIfProd(new RemoteLogException(e).put("json", json));
        }
        if (sessionData == null) {
            sessionData = new SessionData();
        }
        return sessionData;
    }

    private SessionUnmarshaller() { }
}
