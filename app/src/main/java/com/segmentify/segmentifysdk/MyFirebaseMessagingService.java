package com.segmentify.segmentifysdk;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.segmentify.segmentifyandroidsdk.SegmentifyManager;
import com.segmentify.segmentifyandroidsdk.model.NotificationModel;
import com.segmentify.segmentifyandroidsdk.model.NotificationType;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import android.net.Uri;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (!remoteMessage.getData().isEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            Map<String, String> data = remoteMessage.getData();

            String title = data.get("title");
            String body = data.get("message");
            String deepLink = data.get("deeplink");
            String image = data.get("image");
            String icon = "https://img.segmentify.com/52f397e3-505c-409c-acb8-67076ecbb664/u/ca6845b0-8842-4e71-92e5-36a0be5548ef.png";

            if (title != null || body != null) {
                sendNotification(body, title, deepLink, image, icon);
            }

            if (data.containsKey("instanceId")) {
                String instanceId = data.get("instanceId");

                // Resolve interactionId: utm_content from deeplink, fallback to instanceId
                String interactionId = null;
                if (deepLink != null && !deepLink.isEmpty()) {
                    try {
                        interactionId = Uri.parse(deepLink).getQueryParameter("utm_content");
                    } catch (Exception ignored) {
                    }
                }
                if (interactionId == null || interactionId.isEmpty()) {
                    interactionId = instanceId;
                }

                NotificationModel model = new NotificationModel();
                model.setType(NotificationType.VIEW);
                model.setInstanceId(instanceId);
                model.setInteractionId(interactionId);
                SegmentifyManager.INSTANCE.sendNotificationInteraction(model);
            }
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        sendRegistrationToServer(token);
    }

    private void scheduleJob() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(MyWorker.class).build();
        WorkManager.getInstance(this).beginWith(work).enqueue();
    }

    private void handleNow() {
        Log.d(TAG, "Short lived task is done.");
    }

    private void sendRegistrationToServer(String token) {
        if (SegmentifyManager.INSTANCE.getClientPreferences() != null) {
            SegmentifyManager.INSTANCE.getClientPreferences().setDeviceToken(token);
        }
    }

    private void sendNotification(String messageBody, String title, String deepLink, String image, String icon) {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

            if (deepLink != null && !deepLink.isEmpty()) {
                intent.putExtra("deeplink", deepLink);
            }

            if (image != null && !image.isEmpty()) {
                intent.putExtra("pushimage", image);
            }

            int requestCode = (int) System.currentTimeMillis();

            int flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
            PendingIntent pendingIntent = PendingIntent.getActivity(this, requestCode, intent, flags);

            String channelId = "segmentify_push_channel_v2";

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            Log.d(TAG, "Icon URL: " + icon);
            Log.d(TAG, "Image URL: " + image);
            Bitmap iconBitmap = getBitmapFromUrl(icon);
            Log.d(TAG, "Icon bitmap downloaded: " + (iconBitmap != null ? iconBitmap.getWidth() + "x" + iconBitmap.getHeight() : "null"));
            Bitmap imageBitmap = getBitmapFromUrl(image);
            Log.d(TAG, "Image bitmap downloaded: " + (imageBitmap != null ? imageBitmap.getWidth() + "x" + imageBitmap.getHeight() : "null"));

            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this, channelId)
                            .setContentTitle(title != null ? title : "Notification")
                            .setContentText(messageBody)
                            .setSmallIcon(R.drawable.ic_stat_ic_notification)
                            .setAutoCancel(true)
                            .setSound(defaultSoundUri)
                            .setContentIntent(pendingIntent)
                            .setPriority(NotificationCompat.PRIORITY_HIGH);

            if (iconBitmap != null) {
                notificationBuilder.setLargeIcon(iconBitmap);
            }

            if (imageBitmap != null) {
                NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle()
                        .bigPicture(imageBitmap);
                if (iconBitmap != null) {
                    bigPictureStyle.bigLargeIcon(iconBitmap);
                }
                notificationBuilder.setStyle(bigPictureStyle);
            }

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        channelId,
                        "Segmentify Bildirimleri",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Uygulama bildirimleri");
                channel.enableVibration(true);
                notificationManager.createNotificationChannel(channel);
            }

            notificationManager.notify(requestCode, notificationBuilder.build());
            Log.d(TAG, "Notification sent. ID: " + requestCode);
        } catch (Exception e) {
            Log.e("MyFirebaseMsgService", "Bildirim oluşturulurken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Bitmap getBitmapFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            Log.w(TAG, "getBitmapFromUrl: URL is null or empty");
            return null;
        }
        try {
            Log.d(TAG, "getBitmapFromUrl: downloading " + imageUrl);
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.connect();
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "getBitmapFromUrl: response code " + responseCode);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "getBitmapFromUrl: HTTP error " + responseCode);
                return null;
            }
            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            Log.d(TAG, "getBitmapFromUrl: bitmap=" + (bitmap != null ? bitmap.getWidth() + "x" + bitmap.getHeight() : "null (decode failed)"));
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "getBitmapFromUrl: failed for " + imageUrl, e);
            return null;
        }
    }


    public static class MyWorker extends Worker {
        public MyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            return Result.success();
        }
    }
}