package com.gan4x4.smslocator;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

/* Trick for Mi and Huaway devices
    https://stackoverflow.com/questions/41524459/broadcast-receiver-not-working-after-device-reboot-in-android/41627296
*/

public class FakeAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override
    public void onInterrupt() {

    }
}
