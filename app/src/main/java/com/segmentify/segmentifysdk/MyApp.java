package com.segmentify.segmentifysdk;

import android.app.Application;

import com.segmentify.segmentifyandroidsdk.SegmentifyManager;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // if you are using push module make sure to call SegmentifyManager.Instance.setPushConfig first
        SegmentifyManager.INSTANCE.setPushConfig("https://push-notification-api.preprod.cloud.unifonic.com", "ZDYwOTA5YzgtMzM5ZS00NGUxLWFiNjgtZDAzMzVmOThiZDRiOkNTNzVkQzdlbXVPYk9rMWcybGs2UEFJUVVpRDNSdnRS");
        SegmentifyManager.INSTANCE.config(this, "2031ce11-59e2-aec3-39f2-3adc60252339", "https://push-notification-api.preprod.cloud.unifonic.com", "push-sfy-web.int.oci.ruh.dev.unifonic.com", "ZTc5NmJlNGUtNjExNi00Y2Y4LTgyYjgtNDIxMGEzNjNkMWJlOlhtU09WbjJhMjNVOGhjV0xDNVlraDd3S0ZYblBpZUhx");
        SegmentifyManager.INSTANCE.setSessionKeepSecond(86400);
        SegmentifyManager.INSTANCE.logStatus(true);
    }
}




