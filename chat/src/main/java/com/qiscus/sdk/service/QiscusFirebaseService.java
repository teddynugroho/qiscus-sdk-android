/*
 * Copyright (c) 2016 Qiscus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qiscus.sdk.service;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.qiscus.sdk.Qiscus;
import com.qiscus.sdk.data.model.QiscusComment;
import com.qiscus.sdk.data.remote.QiscusPusherApi;
import com.qiscus.sdk.event.QiscusCommentReceivedEvent;
import com.qiscus.sdk.util.QiscusAndroidUtil;
import com.qiscus.sdk.util.QiscusPushNotificationUtil;

import org.greenrobot.eventbus.EventBus;

public class QiscusFirebaseService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (handleMessageReceived(remoteMessage)) {
            return;
        }
    }

    /**
     * Handle remote message from FCM to display push notification
     *
     * @param remoteMessage The message from firebase
     * @return true if the message is for Qiscus SDK, false if the message is not for Qiscus SDK
     */
    public static boolean handleMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().containsKey("qiscus_sdk")) {
            if (Qiscus.hasSetupUser()) {
                if (!QiscusPusherApi.getInstance().isConnected()) {
                    QiscusPusherApi.getInstance().restartConnection();
                }
                if (remoteMessage.getData().containsKey("payload")) {
                    QiscusComment qiscusComment = QiscusPusherApi.jsonToComment(remoteMessage.getData().get("payload"));
                    if (qiscusComment == null) {
                        return true;
                    }
                    if (!qiscusComment.getSenderEmail().equals(Qiscus.getQiscusAccount().getEmail())) {
                        QiscusPusherApi.getInstance()
                                .setUserDelivery(qiscusComment.getRoomId(), qiscusComment.getTopicId(),
                                        qiscusComment.getId(), qiscusComment.getUniqueId());
                    }
                    QiscusPushNotificationUtil.handlePushNotification(Qiscus.getApps(), qiscusComment);
                    QiscusAndroidUtil.runOnUIThread(() -> EventBus.getDefault()
                            .post(new QiscusCommentReceivedEvent(qiscusComment)));
                }
            }
            return true;
        }
        return false;
    }
}