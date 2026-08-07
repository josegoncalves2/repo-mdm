package com.hmdm.launcher.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.hmdm.launcher.Const;
import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.util.RemoteLogger;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;

/**
 * <p>Captures one frame of the screen and uploads it to the server, in response to the
 * "screenshot" push command.</p>
 *
 * <p>The agent had no handler for that push type at all, so the panel's request was silently
 * dropped and <code>files/screenshots/&lt;number&gt;.png</code> never existed -- which is why
 * "Ver ultima captura de tela" answered 404. The server side
 * (com.hmdm.rest.resource.PublicScreenshotResource) already accepted the upload.</p>
 *
 * <p>Screen capture on Android always requires a MediaProjection consent token; not even a
 * device owner may capture silently. {@link ScreenshotConsentActivity} obtains it once and
 * caches it here, so only the first capture after a reboot prompts the user.</p>
 */
public class ScreenshotService extends Service {

    private static final String CHANNEL_ID = "hmdm_screenshot";
    private static final int NOTIFICATION_ID = 0xC5;

    /** Consent token, cached so repeated captures do not prompt again. */
    private static int consentResultCode = 0;
    private static Intent consentData = null;

    public static void cacheConsent(int resultCode, Intent data) {
        consentResultCode = resultCode;
        consentData = data;
    }

    public static boolean hasConsent() {
        return consentData != null;
    }

    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundCompat();
        if (!hasConsent()) {
            RemoteLogger.log(this, Const.LOG_WARN, "Screenshot failed: no screen capture consent");
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            capture();
        } catch (Throwable t) {
            RemoteLogger.log(this, Const.LOG_WARN, "Screenshot failed: " + t.getMessage());
            cleanup();
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void startForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(new NotificationChannel(
                        CHANNEL_ID, "Screen capture", NotificationManager.IMPORTANCE_MIN));
            }
        }
        Notification notification = new androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("MDM")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MIN)
                .build();
        // Android 14 refuses to create a MediaProjection unless the service is already in the
        // foreground with the mediaProjection type declared.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void capture() {
        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (manager == null) {
            RemoteLogger.log(this, Const.LOG_WARN, "Screenshot failed: no projection manager");
            stopSelf();
            return;
        }
        projection = manager.getMediaProjection(consentResultCode, consentData);
        if (projection == null) {
            // A token is single-use once revoked; drop it so the next request asks again.
            consentData = null;
            RemoteLogger.log(this, Const.LOG_WARN, "Screenshot failed: consent no longer valid");
            stopSelf();
            return;
        }

        final Handler handler = new Handler(Looper.getMainLooper());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Required from Android 14 on: registering a callback is mandatory.
            projection.registerCallback(new MediaProjection.Callback() {
                @Override
                public void onStop() {
                }
            }, handler);
        }

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealMetrics(metrics);
        final int width = metrics.widthPixels;
        final int height = metrics.heightPixels;

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        virtualDisplay = projection.createVirtualDisplay("hmdm-screenshot",
                width, height, metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, handler);

        imageReader.setOnImageAvailableListener(reader -> {
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                if (image == null) {
                    return;
                }
                final Bitmap bitmap = toBitmap(image, width, height);
                // Only the first frame is wanted; stop listening before uploading.
                reader.setOnImageAvailableListener(null, null);
                new Thread(() -> {
                    upload(bitmap);
                    cleanup();
                    stopSelf();
                }).start();
            } catch (Throwable t) {
                RemoteLogger.log(ScreenshotService.this, Const.LOG_WARN,
                        "Screenshot failed while reading the frame: " + t.getMessage());
                cleanup();
                stopSelf();
            } finally {
                if (image != null) {
                    image.close();
                }
            }
        }, handler);
    }

    /**
     * ImageReader rows are padded to a stride, so the raw buffer is usually wider than the
     * screen; the padding has to be cropped out or the capture comes out skewed.
     */
    private Bitmap toBitmap(Image image, int width, int height) {
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int padding = rowStride - pixelStride * width;
        Bitmap padded = Bitmap.createBitmap(width + padding / pixelStride, height,
                Bitmap.Config.ARGB_8888);
        padded.copyPixelsFromBuffer(buffer);
        if (padding == 0) {
            return padded;
        }
        Bitmap cropped = Bitmap.createBitmap(padded, 0, 0, width, height);
        padded.recycle();
        return cropped;
    }

    private void upload(Bitmap bitmap) {
        HttpURLConnection connection = null;
        try {
            SettingsHelper settings = SettingsHelper.getInstance(this);
            final String number = settings.getDeviceId();
            if (number == null || number.trim().isEmpty()) {
                RemoteLogger.log(this, Const.LOG_WARN, "Screenshot not sent: no device number");
                return;
            }

            ByteArrayOutputStream png = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, png);
            bitmap.recycle();
            final byte[] body = png.toByteArray();

            connection = (HttpURLConnection) new URL(buildUrl(settings, number)).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Content-Type", "image/png");
            connection.setFixedLengthStreamingMode(body.length);
            OutputStream out = connection.getOutputStream();
            out.write(body);
            out.flush();
            out.close();

            final int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                RemoteLogger.log(this, Const.LOG_INFO, "Screenshot sent (" + body.length + " bytes)");
            } else {
                RemoteLogger.log(this, Const.LOG_WARN, "Screenshot upload rejected: HTTP " + status);
            }
        } catch (Throwable t) {
            RemoteLogger.log(this, Const.LOG_WARN, "Screenshot upload failed: " + t.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String buildUrl(SettingsHelper settings, String number) {
        StringBuilder url = new StringBuilder(settings.getBaseUrl());
        final String project = settings.getServerProject();
        if (project != null && !project.trim().isEmpty()) {
            url.append("/").append(project);
        }
        url.append("/rest/public/screenshot/").append(android.net.Uri.encode(number));
        return url.toString();
    }

    private void cleanup() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        if (projection != null) {
            projection.stop();
            projection = null;
        }
    }

    @Override
    public void onDestroy() {
        cleanup();
        super.onDestroy();
    }
}
