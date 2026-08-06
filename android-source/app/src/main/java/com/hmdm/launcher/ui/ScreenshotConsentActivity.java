package com.hmdm.launcher.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

import com.hmdm.launcher.Const;
import com.hmdm.launcher.service.ScreenshotService;
import com.hmdm.launcher.util.RemoteLogger;

/**
 * <p>Obtains the MediaProjection consent that Android requires before any screen capture.
 * There is no way around this prompt -- device owner privileges do not exempt an app from
 * it -- so the token is cached in {@link ScreenshotService} and reused until reboot.</p>
 *
 * <p>The activity is invisible and finishes as soon as the user answers.</p>
 */
public class ScreenshotConsentActivity extends Activity {

    private static final int REQUEST_CODE = 0xC5;

    /**
     * Captures the screen, asking for consent first only when there is no cached token.
     */
    public static void requestScreenshot(Context context) {
        if (ScreenshotService.hasConsent()) {
            Intent service = new Intent(context, ScreenshotService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
            return;
        }
        Intent consent = new Intent(context, ScreenshotConsentActivity.class);
        consent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(consent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (manager == null) {
            RemoteLogger.log(this, Const.LOG_WARN, "Screenshot failed: no projection manager");
            finish();
            return;
        }
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                ScreenshotService.cacheConsent(resultCode, data);
                Intent service = new Intent(this, ScreenshotService.class);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(service);
                } else {
                    startService(service);
                }
            } else {
                RemoteLogger.log(this, Const.LOG_WARN, "Screenshot refused: consent denied");
            }
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
