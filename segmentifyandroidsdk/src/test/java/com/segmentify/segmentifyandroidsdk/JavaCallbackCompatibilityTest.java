package com.segmentify.segmentifyandroidsdk;

import static org.junit.Assert.assertEquals;

import com.segmentify.segmentifyandroidsdk.utils.SegmentifyCallback;
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyException;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * SFY-28788: integrations written in Java before onError existed implement only
 * onDataLoaded. They must still compile and must not fail when the SDK reports
 * an error to them.
 */
public class JavaCallbackCompatibilityTest {

    @Test
    public void callbackWithoutOnErrorStillCompilesAndIgnoresErrors() {
        final List<String> loaded = new ArrayList<>();
        SegmentifyCallback<String> legacy = new SegmentifyCallback<String>() {
            @Override
            public void onDataLoaded(String data) {
                loaded.add(data);
            }
        };

        legacy.onError(new SegmentifyException("offline", null, null));
        legacy.onDataLoaded("ok");

        assertEquals(1, loaded.size());
    }
}
