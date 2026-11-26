package com.segmentify.segmentifysdk;

import android.app.Application;

import com.segmentify.segmentifyandroidsdk.SegmentifyManager;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Push module: setPushConfig can be called before or after config.
        SegmentifyManager.INSTANCE.setPushConfig("https://psh2.segmentify.com");
        SegmentifyManager.INSTANCE.config(this, "3c9e211a-d049-43d5-aa4a-f98b7e66e482", "https://gandalf.segmentify.com", "demosfy.com");
        SegmentifyManager.INSTANCE.setSessionKeepSecond(86400);
        SegmentifyManager.INSTANCE.logStatus(true);
    }
}




