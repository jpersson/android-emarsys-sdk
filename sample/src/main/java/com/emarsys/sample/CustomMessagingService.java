package com.emarsys.sample;

import com.emarsys.Emarsys;
import com.emarsys.service.EmarsysMessagingServiceUtils;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class CustomMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);

        Emarsys.Push.setPushToken(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        boolean handledByEmarsysSDK = EmarsysMessagingServiceUtils.handleMessage(
                this,
                remoteMessage);

        if (!handledByEmarsysSDK) {
            //handle your custom push message here
        }
    }
}
