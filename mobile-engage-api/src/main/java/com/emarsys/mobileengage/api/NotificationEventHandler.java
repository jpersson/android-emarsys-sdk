package com.emarsys.mobileengage.api;

import android.content.Context;
import androidx.annotation.Nullable;

import org.json.JSONObject;

public interface NotificationEventHandler {
    void handleEvent(Context context, String eventName, @Nullable JSONObject payload);
}
