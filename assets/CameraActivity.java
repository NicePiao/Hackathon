
package com.vuzix.sg.device.app.camera;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.CameraProfile;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.Toast;

import com.vuzix.sg.shared.utilities.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * CameraActivity handles the camera/video functionality.
 */
public class CameraActivity extends Activity implements SurfaceHolder.Callback, SensorEventListener, AutoFocusCallback {

    public final static String MSG_PARTNER_COMMANDS = "com.vuzix.message.partner_commands";
    public final static String APP = "app";
    public final static String CAMERA_RES = "camera_res";

    public static final String PREFERENCE_NAME = "cam_settings";
    public static final String PICTURE_RESOLUTION_SETTING = "picture_resolution";
    public static final String VIDEO_FPS_SETTING = "video_resolution";
    public static final String CAMERA_BRIGHTNESS_SETTING = "brightness";
    public static final String CAMERA_SHARPNESS_SETTING = "sharpness";
    public static final String CAMERA_CONTRAST_SETTING = "contrast";
    public static final String CAMERA_SATURATION_SETTING = "saturation";
    public static final String CAMERA_WHITE_BALANCE_SETTING = "white balance";
    public static final String CAMERA_EXPOSURE_SETTING = "exposure";
    public static final String CAMERA_ZOOM_SETTING = "zoom";
    public static final String CAMERA_COLOR_FILTER_SETTING = "color filter";
    public static final String CAMERA_FOCUS_AREA_SETTING = "focus";

    public final static String VIDEO_FPS = "video_fps";
    private final static String EXTRA_COMMAND_PROCESSED = "command_processed";
    private static final String TAG = "CameraActivity";
    private final static String COMMAND = "command";
    private final static String MSG_CAMERA_COMMANDS = "com.vuzix.sg.device.app.camera.camera_commands";
    private final static String COMMAND_LAUNCH_CAMERA = "com.vuzix.sg.device.app.camera.command.launch_camera";
    private final static String COMMAND_LAUNCH_CAMERA_SETTINGS = "com.vuzix.sg.device.app.camera.command.launch_camera_settings";
    private final static String COMMAND_TAKE_PICTURE = "com.vuzix.sg.device.app.camera.command.take_picture";
    private final static String COMMAND_LAUNCH_VIDEO = "com.vuzix.sg.device.app.camera.command.launch_video";
    private final static String COMMAND_START_VIDEO = "com.vuzix.sg.device.app.camera.command.start_video";
    private final static String COMMAND_STOP_VIDEO = "com.vuzix.sg.device.app.camera.command.stop_video";

    private final static String MSG_VIEW_GAINED_FOCUS = "com.vuzix.sg.device.app.view_gained_focus";
    private final static String MSG_APP_GAINED_FOCUS = "com.vuzix.sg.device.app.gained_focus";
    private final static String MSG_APP_LOST_FOCUS = "com.vuzix.sg.device.app.lost_focus";
    private final static String MSG_PICTURE_TAKEN = "com.vuzix.message.picture_taken";
    private final static String MSG_PICTURE_FAILED = "com.vuzix.message.picture_failed";
    private final static String MSG_RECORDING_STARTED = "com.vuzix.message.recording_started";
    private final static String MSG_RECORDING_FAILED = "com.vuzix.message.recording_failed";
    private final static String MSG_RECORDING_ENDED = "com.vuzix.message.recording_stopped";

    protected static final int REQUEST_SETTINGS = 101;
    public static final String CAMERA_PICTURE_RESOLUTION = "picture_resolution";
    public static final String CAMERA_VIDEO_QUALITY = "video_quality";
    private MediaRecorder mediaRecorder;
    private SurfaceHolder surfaceHolder;
    private SurfaceView surfaceView;
    private Camera camera;
    private View overlayView;
    private View imgFocus;
    private boolean isCameraReady = false;
    private boolean isRecordingMode;
    private boolean isRecording;
    private SensorManager mSensorManager;
    private int currentDisplayRotation;
    private boolean launchVideo;

    /**
     * Listens to the various commands from the partner
     */
    private BroadcastReceiver commandReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String command = intent.getStringExtra(COMMAND);
            if (command.equalsIgnoreCase(COMMAND_LAUNCH_CAMERA)) {
                stopRecording(true);
            } else if (command.equalsIgnoreCase(COMMAND_LAUNCH_VIDEO)) {
                if (isCameraReady) {
                    prepareRecording();
                } else {
                    launchVideo = true;
                }
            } else if (command.equalsIgnoreCase(COMMAND_TAKE_PICTURE)) {
                launchVideo = false;
                takePicture();
            } else if (command.equalsIgnoreCase(COMMAND_START_VIDEO)) {
                if (!isRecording) {
                    startRecording();
                }
            } else if (command.equalsIgnoreCase(COMMAND_STOP_VIDEO)) {
                if (isRecording) {
                    stopRecording(false);
                }
            } else if (command.equalsIgnoreCase(COMMAND_LAUNCH_CAMERA_SETTINGS)) {
                startActivityForResult(
                        new Intent(CameraActivity.this, CameraSettingsActivity.class)
                                .putExtra(CAMERA_RES, resStrings)
                                .putExtra(VIDEO_FPS, vidFpsStrings)
                        ,
                        REQUEST_SETTINGS);
            }
            Bundle results = getResultExtras(true);
            results.putBoolean(EXTRA_COMMAND_PROCESSED, true);
        }
    };

    /**
     * Listens to the state of the wifi
     */
    private BroadcastReceiver wifiMonitor = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                invalidateOptionsMenu();
            }
        }
    };
    private List<Size> sizes;
    private String[] resStrings;
    private String[] vidFpsStrings;
    private int cameraResSelected = 0;
    private int vidQualitySelected = 0;
    private List<int[]> fpss;
    private List<Size> previewSizes;
    private List<Size> videoSizes;
    private File videoFile;
    private int maxZoomLevel;
    private int currentZoom;
    private int currentBrightness;
    private int currentSharpness;
    private int currentContrast;
    private int currentSaturation;
    private int currentExposure;
    private int currentWhiteBalance;
    private int currentColorFilter;
    private Rect currentCameraFocus;

    private String[] wbOptions = new String[] {
            "Auto", "Daylight", "Cloudy-Daylight", "Tungsten", "Fluorescent", "Incandescent", "Horizon", "Sunset", "Shade", "Twilight", "Warm-Fluorescent"
    };
    private String[] colorFilterOptions = new String[] {
            "none", "negative", "solarize", "sepia", "mono", "vivid", "blackwhite", "aqua", "posterize", "natural", "color-swap"
            // , "whiteboard", "blackboard"
    };
    private ImageView cameraIcon;
    private AlphaAnimation blinkingAnimation;

    /**
     * commandReceiver used to receive the commands sent from partner.
     */
    private BroadcastReceiver settingsCommandReceiver = new BroadcastReceiver() {

        private JSONObject settingJsonObject;

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                settingJsonObject = new JSONObject(intent.getStringExtra(COMMAND));
                if (settingJsonObject.has("picture_resolution")) {
                    saveCameraSettings(PICTURE_RESOLUTION_SETTING, settingJsonObject.getInt("picture_resolution"));
                }
                if (settingJsonObject.has("video_fps")) {
                    saveCameraSettings(VIDEO_FPS_SETTING, settingJsonObject.getInt("video_fps"));
                }
                if (settingJsonObject.has("brightness")) {
                    saveCameraSettings(CAMERA_BRIGHTNESS_SETTING, settingJsonObject.getInt("brightness"));
                    currentBrightness = settingJsonObject.getInt("brightness");
                }
                if (settingJsonObject.has("contrast")) {
                    saveCameraSettings(CAMERA_CONTRAST_SETTING, settingJsonObject.getInt("contrast"));
                    currentContrast = settingJsonObject.getInt("contrast");
                }
                if (settingJsonObject.has("sharpness")) {
                    saveCameraSettings(CAMERA_SHARPNESS_SETTING, settingJsonObject.getInt("sharpness"));
                    currentSharpness = settingJsonObject.getInt("sharpness");
                }
                if (settingJsonObject.has("saturation")) {
                    saveCameraSettings(CAMERA_SATURATION_SETTING, settingJsonObject.getInt("saturation"));
                    currentSaturation = settingJsonObject.getInt("saturation");
                }
                if (settingJsonObject.has("exposure")) {
                    saveCameraSettings(CAMERA_EXPOSURE_SETTING, settingJsonObject.getInt("exposure"));
                    currentExposure = settingJsonObject.getInt("exposure");
                }
                if (settingJsonObject.has("white balance")) {
                    saveCameraSettings(CAMERA_WHITE_BALANCE_SETTING, settingJsonObject.getInt("white balance"));
                    currentWhiteBalance = settingJsonObject.getInt("white balance");
                }
                if (settingJsonObject.has("zoom")) {
                    saveCameraSettings(CAMERA_ZOOM_SETTING, settingJsonObject.getInt("zoom"));
                    currentZoom = settingJsonObject.getInt("zoom");
                }
                if (settingJsonObject.has("color filter")) {
                    saveCameraSettings(CAMERA_COLOR_FILTER_SETTING, settingJsonObject.getInt("color filter"));
                    currentColorFilter = settingJsonObject.getInt("color filter");
                }
                if (settingJsonObject.has("focus")) {
                    JSONObject focusRectJsonObject = settingJsonObject.getJSONObject("focus");
                    Rect rect = new Rect(focusRectJsonObject.getInt("left"),
                            focusRectJsonObject.getInt("top"),
                            focusRectJsonObject.getInt("right"),
                            focusRectJsonObject.getInt("bottom"));
                    saveCameraSettings(CAMERA_FOCUS_AREA_SETTING, rectToString(rect));
                    currentCameraFocus = rect;
                }
            } catch (JSONException e) {
            }
            if (camera != null)
                setCameraParams(camera.getParameters());
            Bundle results = getResultExtras(true);
            results.putBoolean(EXTRA_COMMAND_PROCESSED, true);
        }
    };

    /**
     * Release the media recorder and camera objects when activity is being
     * destroyed
     */
    @Override
    protected void onDestroy() {
        unregisterReceiver(commandReceiver);
        unregisterReceiver(settingsCommandReceiver);
        unregisterReceiver(wifiMonitor);

        sendBroadcast(new Intent(MSG_PARTNER_COMMANDS).putExtra(APP,
                getApplicationContext().getPackageName()).putExtra(MSG_PARTNER_COMMANDS,
                MSG_APP_LOST_FOCUS));

        releaseMediaRecorder();
        releaseCamera();
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        sendBroadcast(new Intent(MSG_PARTNER_COMMANDS).putExtra(APP,
                getApplicationContext().getPackageName()).putExtra(MSG_PARTNER_COMMANDS,
                MSG_APP_GAINED_FOCUS));

        getActionBar().setTitle(getDeviceName());

        surfaceView = (SurfaceView) findViewById(R.id.cameraSurfaceView);
        cameraIcon = (ImageView) findViewById(R.id.cameraIcon);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceView.setKeepScreenOn(true);

        overlayView = findViewById(R.id.pnlOverlay);
        imgFocus = findViewById(R.id.imgFocus);

        imgFocus.setVisibility(View.VISIBLE);

        // SG Commands receiver
        registerReceiver(commandReceiver, new IntentFilter(MSG_CAMERA_COMMANDS));
        registerReceiver(settingsCommandReceiver, new IntentFilter(CameraSettingsActivity.MSG_CAMERA_SETTINGS));

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        registerReceiver(wifiMonitor, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
        initBlinkingAnimation();
        fetchCameraSettings();
    }

    /**
     * Fetch the camera photo/video quality from the preferences
     */
    private void fetchCameraSettings() {
        final Uri uri = new Uri.Builder().scheme("content").authority(getPackageName()).appendPath("preferences").build();
        final Cursor prefs = getContentResolver().query(uri, null, null, null, null);
        prefs.moveToFirst();

        final int picResIndex = prefs.getColumnIndex(PICTURE_RESOLUTION_SETTING);
        final int vidFpsIndex = prefs.getColumnIndex(VIDEO_FPS_SETTING);
        final int brightnessIndex = prefs.getColumnIndex(CAMERA_BRIGHTNESS_SETTING);
        final int sharpnessIndex = prefs.getColumnIndex(CAMERA_SHARPNESS_SETTING);
        final int contrastIndex = prefs.getColumnIndex(CAMERA_CONTRAST_SETTING);
        final int saturationIndex = prefs.getColumnIndex(CAMERA_SATURATION_SETTING);
        final int exposureIndex = prefs.getColumnIndex(CAMERA_EXPOSURE_SETTING);
        final int whiteBalanceIndex = prefs.getColumnIndex(CAMERA_WHITE_BALANCE_SETTING);
        final int zoomIndex = prefs.getColumnIndex(CAMERA_ZOOM_SETTING);
        final int colorFilterIndex = prefs.getColumnIndex(CAMERA_COLOR_FILTER_SETTING);
        final int focusAreaIntex = prefs.getColumnIndex(CAMERA_FOCUS_AREA_SETTING);

        cameraResSelected = (picResIndex >= 0) ? prefs.getInt(picResIndex) : 0;
        vidQualitySelected = (vidFpsIndex >= 0) ? prefs.getInt(vidFpsIndex) : 0;
        currentBrightness = (brightnessIndex >= 0) ? prefs.getInt(brightnessIndex) : 50;
        currentSharpness = (sharpnessIndex >= 0) ? prefs.getInt(sharpnessIndex) : 100;
        currentContrast = (contrastIndex >= 0) ? prefs.getInt(contrastIndex) : 100;
        currentSaturation = (saturationIndex >= 0) ? prefs.getInt(saturationIndex) : 100;
        currentExposure = (exposureIndex >= 0) ? prefs.getInt(exposureIndex) : 0;
        currentWhiteBalance = (whiteBalanceIndex >= 0) ? prefs.getInt(whiteBalanceIndex) : 0;
        currentZoom = (zoomIndex >= 0) ? prefs.getInt(zoomIndex) : 0;
        currentColorFilter = (colorFilterIndex >= 0) ? prefs.getInt(colorFilterIndex) : 0;
        currentCameraFocus = stringToRect((focusAreaIntex >= 0) ? prefs.getString(focusAreaIntex) : "0,0,0,0");

        prefs.close();
    }

    /**
     * If app already running and another command is obtained to launch the app
     * then stop the recording that was already in progress and send a focus
     * gained event
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        stopRecording(true);
        sendBroadcast(new Intent(MSG_PARTNER_COMMANDS).putExtra(APP,
                getApplicationContext().getPackageName()).putExtra(MSG_PARTNER_COMMANDS,
                MSG_APP_GAINED_FOCUS));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.wifi_menu, menu);
        menu.findItem(R.id.mainbar_wifi).setEnabled(false);
        menu.add(isRecordingMode ? "Camera" : "Video").setOnMenuItemClickListener(new MenuItem
                .OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem)
                    {
                        if (isRecordingMode) {
                            sendBroadcast(new Intent(MSG_PARTNER_COMMANDS).putExtra(APP,
                                    COMMAND_LAUNCH_CAMERA).putExtra(MSG_PARTNER_COMMANDS,
                                    MSG_VIEW_GAINED_FOCUS));
                            stopRecording(true);
                        } else {
                            if (isCameraReady) {
                                sendBroadcast(new Intent(MSG_PARTNER_COMMANDS).putExtra(APP,
                                        COMMAND_LAUNCH_VIDEO).putExtra(MSG_PARTNER_COMMANDS,
                                        MSG_VIEW_GAINED_FOCUS));
                                prepareRecording();
                            }
                        }
                        return true;
                    }
                });
        menu.add("Settings").setOnMenuItemClickListener(new MenuItem
                .OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem)
                    {
                        startActivityForResult(
                                new Intent(CameraActivity.this, CameraSettingsActivity.class)
                                        .putExtra(CAMERA_RES, resStrings)
                                        .putExtra(VIDEO_FPS, vidFpsStrings)
                                ,
                                REQUEST_SETTINGS);
                        return true;
                    }
                });
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        checkWifiItem(menu);
        menu.getItem(1).setTitle(isRecordingMode ? "Camera" : "Video");
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Check the state of the wifi icon to be shown in the action bar
     * 
     * @param menu
     */
    private void checkWifiItem(Menu menu) {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (!mWifi.isConnected())
            menu.findItem(R.id.mainbar_wifi).setVisible(false);
        else
            menu.findItem(R.id.mainbar_wifi).setVisible(true);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN)
            return super.dispatchKeyEvent(event);

        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                zoom();
                return true;

            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                unZoom();
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (isRecordingMode) {
                    if (isRecording)
                        stopRecording(false);
                    else
                        startRecording();
                } else
                    takePicture();
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    /**
     * Takes the picture
     */
    public void takePicture() {
        if (isCameraReady) {
            isCameraReady = false;

            overlayView.setVisibility(View.VISIBLE);
            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    boolean saved = false;
                    File pictureFile = getOutputFile(true);
                    if (pictureFile != null) {

                        try {
                            FileOutputStream fos = new FileOutputStream(pictureFile);
                            fos.write(data);
                            fos.close();

                            MediaScannerConnection.scanFile(CameraActivity.this,
                                    new String[] {
                                        pictureFile.toString()
                                    },
                                    new String[] {
                                        "image/jpeg"
                                    }, null);
                            saved = true;
                        } catch (Exception e) {
                            saved = false;
                            Log.d(TAG, "Error accessing file: " + e.getMessage());
                        }
                    }

                    // Restart the preview and re-enable the shutter button so
                    // that we can take another picture
                    camera.startPreview();

                    isCameraReady = true;
                    overlayView.setVisibility(View.GONE);
                    if (launchVideo) {
                        launchVideo = false;
                        prepareRecording();
                    }

                    final String partnerCommand = (saved) ? MSG_PICTURE_TAKEN : MSG_PICTURE_FAILED;
                    sendBroadcast(new Intent(MSG_PARTNER_COMMANDS).putExtra(APP,
                            MSG_CAMERA_COMMANDS).putExtra(
                            MSG_PARTNER_COMMANDS, partnerCommand));
                }
            });
        }

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("XXX", "Surface created");
        camera = Camera.open();
        if (camera != null) {
            Camera.Parameters params = camera.getParameters();
            setCameraParams(params);
            // Log.d("XXX",
            // "Resolution = " + camera.getParameters().getPictureSize().width +
            // " - "
            // + camera.getParameters().getPictureSize().height);
        } else {
            Toast.makeText(getApplicationContext(), "Camera not available!", Toast.LENGTH_LONG)
                    .show();
            finish();
        }
    }

    /**
     * Set the Video FPS camera setting
     * 
     * @param params
     */
    private void setVideoFPS(Camera.Parameters params) {
        if (fpss == null || vidFpsStrings == null) {
            fpss = params.getSupportedPreviewFpsRange();
            if (fpss != null && !fpss.isEmpty()) {
                Collections.sort(fpss, new Comparator<int[]>() {
                    @Override
                    public int compare(int[] lhs, int[] rhs) {
                        if ((lhs[0] > rhs[0]) && (lhs[1] >= rhs[1]))
                            return -1;
                        else if ((rhs[0] > lhs[0]) && (rhs[1] >= lhs[1]))
                            return 1;
                        else if ((lhs[0] == rhs[0])) {
                            if (lhs[1] > rhs[1])
                                return -1;
                            else if (rhs[1] > lhs[1])
                                return 1;
                            else
                                return 0;
                        } else
                            return 0;
                    }
                });

                int i = 0;
                vidFpsStrings = new String[fpss.size()];
                for (int[] temp : fpss) {
                    if (temp[0] == temp[1])
                        vidFpsStrings[i] = String.valueOf(temp[0] / 1000);
                    else
                        vidFpsStrings[i] = String.valueOf(temp[0] / 1000) + " - "
                                + String.valueOf(temp[1] / 1000);
                    i++;
                }
                params.setPreviewFpsRange(fpss.get(getVideoFpsAtPosition(vidQualitySelected))[0],
                        fpss.get(getVideoFpsAtPosition(vidQualitySelected))[1]);
            }
        } else {
            params.setPreviewFpsRange(fpss.get(getVideoFpsAtPosition(vidQualitySelected))[0],
                    fpss.get(getVideoFpsAtPosition(vidQualitySelected))[1]);
        }
    }

    /**
     * Fetches the appropriate video camera settings . The index of the setting
     * is saved in the preferences
     * 
     * @param option
     * @return
     */
    private int getVideoFpsAtPosition(int option) {
        if (option == 0)
            return 0; // 27- 27 Very High
        else if (option == 1)
            return 2; // 20 � 20 High
        else if (option == 2)
            return 4; // 10 � 10 Medium
        else if (option == 3)
            return 6; // 5 � 5 Low
        return 0;
    }

    /**
     * Set the picture resolution to the camera
     * 
     * @param params
     */
    private void setPictureSize(Camera.Parameters params) {
        if (sizes == null || resStrings == null) {
            sizes = params.getSupportedPictureSizes();
            if (sizes != null && !sizes.isEmpty()) {
                Collections.sort(sizes, new Comparator<Camera.Size>() {
                    @Override
                    public int compare(Camera.Size lhs, Camera.Size rhs) {
                        if ((lhs.width > rhs.width) && (lhs.height >= rhs.height))
                            return -1;
                        else if ((rhs.width > lhs.width) && (rhs.height >= lhs.height))
                            return 1;
                        else if ((lhs.width == rhs.width)) {
                            if (lhs.height > rhs.height)
                                return -1;
                            else if (rhs.height > lhs.height)
                                return 1;
                            else
                                return 0;
                        } else
                            return 0;
                    }
                });

                int i = 0;
                resStrings = new String[sizes.size()];
                for (Camera.Size temp : sizes) {
                    resStrings[i] = temp.width + " x " + temp.height;
                    i++;
                }

                params.setPictureSize(sizes.get(getCameraResAtPosition(cameraResSelected)).width,
                        sizes.get(getCameraResAtPosition(cameraResSelected)).height);
            }
        } else {
            params.setPictureSize(sizes.get(getCameraResAtPosition(cameraResSelected)).width,
                    sizes.get(getCameraResAtPosition(cameraResSelected)).height);
        }
    }

    private void setPreviewSize(Camera.Parameters params) {
        if (previewSizes == null) {
            previewSizes = params.getSupportedPreviewSizes();
            if (previewSizes != null && !previewSizes.isEmpty()) {
                Collections.sort(previewSizes, new Comparator<Camera.Size>() {
                    @Override
                    public int compare(Camera.Size lhs, Camera.Size rhs) {
                        if ((lhs.width > rhs.width) && (lhs.height >= rhs.height))
                            return -1;
                        else if ((rhs.width > lhs.width) && (rhs.height >= lhs.height))
                            return 1;
                        else if ((lhs.width == rhs.width)) {
                            if (lhs.height > rhs.height)
                                return -1;
                            else if (rhs.height > lhs.height)
                                return 1;
                            else
                                return 0;
                        } else
                            return 0;
                    }
                });
            }
        }

        params.setPreviewSize(
                previewSizes.get(getPreviewSizeAtPosition(cameraResSelected)).width,
                previewSizes.get(getPreviewSizeAtPosition(cameraResSelected)).height);

    }

    private Size getVideoSize(Camera.Parameters params) {
        if (videoSizes == null) {
            videoSizes = getSupportedVideoResolutions(params);
            if (videoSizes != null && !videoSizes.isEmpty()) {
                Collections.sort(videoSizes, new Comparator<Camera.Size>() {
                    @Override
                    public int compare(Camera.Size lhs, Camera.Size rhs) {
                        if ((lhs.width > rhs.width) && (lhs.height >= rhs.height))
                            return -1;
                        else if ((rhs.width > lhs.width) && (rhs.height >= lhs.height))
                            return 1;
                        else if ((lhs.width == rhs.width)) {
                            if (lhs.height > rhs.height)
                                return -1;
                            else if (rhs.height > lhs.height)
                                return 1;
                            else
                                return 0;
                        } else
                            return 0;
                    }
                });
            }
        }
        if (videoSizes != null)
            Log.d("XXX", "Video sizes not null");
        else
            Log.d("XXX", "Video sizes were null");

        int index = vidQualitySelected;
        if (index > videoSizes.size()) {
            index = videoSizes.size() - 1;
        } else if (index < 0) {
            index = 0;
        }

        Log.d("XXX", "Index = " + index + " " + vidQualitySelected);

        return videoSizes.get(getVideoSizeAtPosition(index));
    }

    private int getVideoSizeAtPosition(int option) {
        if (option == 0)
            return 0; // 1920x1080 Very High
        else if (option == 1)
            return 1; // 1280x720 High
        else if (option == 2)
            return 8; // 720x480 Medium
        else if (option == 3)
            return 16; // 432x240 Low
        return 0;
    }

    /**
     * Fetches the appropriate camera photo preview size setting
     * 
     * @param option
     * @return
     */
    private int getPreviewSizeAtPosition(int option) {
        if (option == 0)
            return 0; // 1920 - 1080 Very High
        else if (option == 1)
            return 0; // 1920 - 1080 High
        else if (option == 2)
            return 1; // 1280 - 720 Medium
        else if (option == 3)
            return 18; // 320 - 240 Low
        return 0;
    }

    /**
     * Fetches the appropriate camera photo resolution setting
     * 
     * @param option
     * @return
     */
    private int getCameraResAtPosition(int option) {
        if (option == 0)
            return 0; // 2592x1944 Very High
        else if (option == 1)
            return 7; // 2048x1536 High
        else if (option == 2)
            return 13; // 1280x960 Medium
        else if (option == 3)
            return 18; // 640x480 Low
        return 0;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("XXX", "Surface changed");
        if (isCameraReady) {
            camera.stopPreview();
        }
        Camera.Parameters params = camera.getParameters();
        setVideoFPS(params);
        setCameraParams(params);
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
            isCameraReady = true;
        } catch (IOException e) {
            Log.e("XXX", e.getMessage());
            e.printStackTrace();
        }
    }

    private void setCameraParams(Parameters params) {
        params.set("iso", "auto");
        params.set("contrast", currentContrast);
        params.set("brightness", currentBrightness);
        params.set("saturation", currentSaturation);
        params.set("sharpness", currentSharpness);
        params.setExposureCompensation(currentExposure);
        params.setWhiteBalance(wbOptions[currentWhiteBalance].toLowerCase());
        params.setAntibanding("auto");
        params.setPictureFormat(ImageFormat.JPEG);
        params.setFlashMode(Parameters.FLASH_MODE_OFF);
        params.set("jpeg-quality", 100);
        params.setJpegQuality(CameraProfile.getJpegEncodingQualityParameter(0,
                CameraProfile.QUALITY_HIGH));
        if (params.isZoomSupported()) {
            maxZoomLevel = params.getMaxZoom();
            params.setZoom(currentZoom);
        }
        params.setColorEffect(colorFilterOptions[currentColorFilter]);
        setPreviewSize(params);
        setPictureSize(params);
        submitFocusAreaRect(params, currentCameraFocus);
        setFocusMode(params);
        camera.setParameters(params);
    }

    private void setFocusMode(Parameters params) {
        List<String> focusModes = params.getSupportedFocusModes();

        if (isRecordingMode) {
            if (focusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        else {
            if (focusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
                params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
    }

    /**
     * @param isImage
     * @return the file to which the image would be captured
     */
    private File getOutputFile(boolean isImage) {
        String IMG_PATTERN = "yyyyMMdd_HHmmss";
        String VUZIX_CAMERA = "Camera";

        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File("/mnt/ext_sdcard/DCIM/" + VUZIX_CAMERA);
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            mediaStorageDir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    VUZIX_CAMERA);
        }

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs())
            return null;

        // Create a media file name
        String timeStamp = new SimpleDateFormat(IMG_PATTERN, Locale.getDefault())
                .format(new Date());
        if (isImage)
            return new File(mediaStorageDir.getPath() + File.separator + "img_" + timeStamp
                    + ".jpeg");
        else
            return new File(mediaStorageDir.getPath() + File.separator + "vid_" + timeStamp
                    + ".mp4");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        isCameraReady = false;
        camera.release();
        camera = null;
    }

    /**
     * Prepare the views for video recording
     */
    private void prepareRecording() {
        imgFocus.setVisibility(View.INVISIBLE);

        imgFocus.requestLayout();
        cameraIcon.setImageResource(R.drawable.video_grey);
        cameraIcon.clearAnimation();
        cameraIcon.requestLayout();

        isRecordingMode = true;
        camera.stopPreview();
        camera.setParameters(camera.getParameters());
        camera.startPreview();
    }

    /**
     * Start the video recording
     * 
     * @return
     */
    private void startRecording() {
        mediaRecorder = new MediaRecorder();
        new StartRecording().execute(mediaRecorder);
    }

    /**
     * Starts recording on a Thread. When the media recorder is finished, the
     * state of the View will be in the "recording" state. If there is an error
     * in preparing for the recording, then "stopRecording" will be called and
     * the View will revert back to the "prepare recording" state.
     */
    private class StartRecording extends AsyncTask<MediaRecorder, Integer, Boolean> {
        @Override
        protected void onPreExecute() {
            isRecording = true;
        }

        @Override
        public Boolean doInBackground(MediaRecorder... player) {
            if (player == null || player.length == 0) {
                return false;
            }

            publishProgress(0);

            final Size cameraSize = getVideoSize(camera.getParameters());
            mediaRecorder = new MediaRecorder();
            // Step 1: Unlock and set camera to MediaRecorder
            setVideoFocusParams();
            camera.stopPreview();
            try {
                camera.unlock();
            } catch (RuntimeException e) {
                Log.d("DEBUG", "RuntimeException unlocking camera: " + e.getMessage());
                releaseMediaRecorder();
                return false;
            }
            mediaRecorder.setCamera(camera);

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
            mediaRecorder.setOutputFormat(getCamcorderProfile().fileFormat);
            mediaRecorder.setVideoSize(cameraSize.width, cameraSize.height);
            mediaRecorder.setVideoEncodingBitRate(getCamcorderProfile().videoBitRate);
            mediaRecorder.setAudioEncoder(getCamcorderProfile().audioCodec);
            mediaRecorder.setVideoEncoder(getCamcorderProfile().videoCodec);

            // Step 4: Set output file
            videoFile = getOutputFile(false);
            mediaRecorder.setOutputFile(videoFile.getPath());

            // Step 5: Set the preview output
            mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
            // Step 6: Prepare configured MediaRecorder
            try {

                mediaRecorder.prepare();
                mediaRecorder.start();
            } catch (IllegalStateException e) {
                Log.d("DEBUG", "IllegalStateException preparing MediaRecorder: " + e.getMessage());
                releaseMediaRecorder();
                return false;
            } catch (IOException e) {
                Log.d("DEBUG", "IOException preparing MediaRecorder: " + e.getMessage());
                releaseMediaRecorder();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean recording) {
            if (!recording) {
                stopRecording(false);
            } else {
                imgFocus.requestLayout();
                cameraIcon.setImageResource(R.drawable.video_green);
                cameraIcon.requestLayout();
                cameraIcon.startAnimation(blinkingAnimation);
            }

            final String partnerCommand = (recording) ? MSG_RECORDING_STARTED : MSG_RECORDING_FAILED;
            sendBroadcast(new Intent(MSG_PARTNER_COMMANDS)
                    .putExtra(APP, MSG_CAMERA_COMMANDS)
                    .putExtra(MSG_PARTNER_COMMANDS, partnerCommand));
        }
    }

    private void initBlinkingAnimation() {
        blinkingAnimation = new AlphaAnimation(0.0f, 1.0f);
        blinkingAnimation.setDuration(500);
        blinkingAnimation.setStartOffset(20);
        blinkingAnimation.setRepeatMode(Animation.REVERSE);
        blinkingAnimation.setRepeatCount(Animation.INFINITE);
    }

    /**
     * Stop the video media recording
     * 
     * @param showCamera
     */
    protected void stopRecording(boolean showCamera) {
        new StopRecording(showCamera).execute();
    }

    /**
     * This asynctask will prepare the media recorder to stop recording if it
     * is. It will also set the View to the proper state. If "showCamera" is
     * true, then the state will be shifted to the camera app. If the
     * "showCamera" is false, then the state will be shifted to the prepare for
     * recording state.
     */
    private class StopRecording extends AsyncTask<Void, Void, Void> {
        boolean showCamera;

        public StopRecording(boolean showCamera) {
            this.showCamera = showCamera;
        }

        @Override
        public void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (mediaRecorder != null) {
                try {
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    mediaRecorder = null;
                    camera.startPreview();
                    if (videoFile != null)
                        MediaScannerConnection.scanFile(
                                CameraActivity.this, new String[] {
                                    videoFile.toString()
                                },
                                new String[] {
                                    "video/mp4"
                                }, null);

                } catch (Exception ex) {
                    // Doesn't much matter if this state fails. Put the View in
                    // to the state it
                    // should be in.
                    mediaRecorder = null;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            cameraIcon.setImageResource(showCamera ? R.drawable.camera_grey : R.drawable.video_grey);
            imgFocus.setVisibility(showCamera ? View.VISIBLE : View.INVISIBLE);
            cameraIcon.clearAnimation();
            imgFocus.requestLayout();
            cameraIcon.requestLayout();
            isRecordingMode = !showCamera;
            isRecording = false;

            if (isRecordingMode)
                setVideoFocusParams();
            else
                setPictureFocusParams();

            final String partnerCommand = MSG_RECORDING_ENDED;
            sendBroadcast(new Intent(MSG_PARTNER_COMMANDS)
                    .putExtra(APP, MSG_CAMERA_COMMANDS)
                    .putExtra(MSG_PARTNER_COMMANDS, partnerCommand));
        }
    }

    private void setVideoFocusParams() {
        if (camera != null) {
            Parameters parms = camera.getParameters();
            List<String> focusModes = parms.getSupportedFocusModes();
            if (focusModes != null && focusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                parms.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            camera.setParameters(parms);
        }
    }

    private void setPictureFocusParams() {
        if (camera != null) {
            Parameters parms = camera.getParameters();
            if (parms != null) {
                List<String> focusModes = parms.getSupportedFocusModes();
                if (focusModes != null && focusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
                    parms.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                camera.setParameters(parms);
            }
        }
    }

    /**
     * Nullify and Release the media recorder object
     */
    private void releaseMediaRecorder() {

        if (mediaRecorder != null) {
            mediaRecorder.reset(); // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
        }
    }

    /**
     * Nullify and Release the camera object
     */
    private void releaseCamera() {
        if (camera != null) {
            camera.release(); // release the camera for other applications
            camera = null;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();

        if (currentDisplayRotation != rotation && !isRecording)
            setCameraOrientation(camera);
    }

    /**
     * Set the camera orientation. Only landscape sensor modes supported on the
     * SG
     * 
     * @param camera
     */
    private void setCameraOrientation(Camera camera) {
        if (camera != null) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(0, info);

            currentDisplayRotation = getWindowManager().getDefaultDisplay().getRotation();
            int degrees = 0;
            switch (currentDisplayRotation) {
                case Surface.ROTATION_0:
                    degrees = 0;
                    break;
                case Surface.ROTATION_90:
                    degrees = 90;
                    break;
                case Surface.ROTATION_180:
                    degrees = 180;
                    break;
                case Surface.ROTATION_270:
                    degrees = 270;
                    break;
                default:
                    degrees = 0;
            }

            int result = (info.orientation + degrees) % 360;
            if (Build.VERSION.SDK_INT < 14)
                camera.stopPreview();
            camera.setDisplayOrientation(result);
            if (Build.VERSION.SDK_INT < 14)
                camera.startPreview();
        }
    }

    @Override
    protected void onResume() {
        super.onResume(); // To change body of overridden methods use File |
                          // Settings | File Templates.
        invalidateOptionsMenu();
        overlayView.setVisibility(View.GONE);
    }

    /**
     * @return the bluetooth device name
     */
    private String getDeviceName() {
        String name = BluetoothAdapter.getDefaultAdapter().getName();
        return TextUtils.isEmpty(name) ? getString(R.string.app_name) : name;
    }

    /**
     * Fetching the selected options from the Camera Settings Activity and
     * storing in the preferences
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("XXX", "On activity created");
        fetchCameraSettings();
        super.onActivityResult(requestCode, resultCode, data);
    }

    private List<Size> getSupportedVideoResolutions(Parameters param) {
        List<Size> sizes = param.getSupportedVideoSizes();
        if (sizes == null) {
            // If the sizes list is null, then it indicates that they are the
            // same as the preview sizes.
            sizes = param.getSupportedPreviewSizes();
        }
        return sizes;
    }

    public CamcorderProfile getCamcorderProfile() {
        CamcorderProfile profile = null;
        if (vidQualitySelected == 0)
            profile = CamcorderProfile.get(0, CamcorderProfile.QUALITY_1080P);
        else if (vidQualitySelected == 1)
            profile = CamcorderProfile.get(0, CamcorderProfile.QUALITY_720P);
        else if (vidQualitySelected == 2)
            profile = CamcorderProfile.get(0, CamcorderProfile.QUALITY_480P);
        else if (vidQualitySelected == 3)
            profile = CamcorderProfile.get(0, CamcorderProfile.QUALITY_LOW);
        return profile;
    }

    private void zoom() {
        if (currentZoom < maxZoomLevel) {
            currentZoom += 3;
            // mCamera.startSmoothZoom(currentZoomLevel);
            Parameters params = camera.getParameters();
            params.setZoom(currentZoom);
            camera.setParameters(params);
        }
    }

    private void unZoom() {
        if (currentZoom > 0) {
            currentZoom -= 3;
            Parameters params = camera.getParameters();
            params.setZoom(currentZoom);
            camera.setParameters(params);
        }
    }

    /**
     * Save the camera settings in the preferences
     * 
     * @param key
     * @param value
     */
    private void saveCameraSettings(String key, int value) {
        final ContentValues cv = new ContentValues();
        cv.put(key, value);
        final Uri uri = new Uri.Builder().scheme("content").authority(getPackageName()).appendPath("preferences").build();
        getContentResolver().update(uri, cv, null, null);
    }

    /**
     * Save the camera settings in the preferences
     * 
     * @param key
     * @param value
     */
    private void saveCameraSettings(String key, String value) {
        final ContentValues cv = new ContentValues();
        cv.put(key, value);
        final Uri uri = new Uri.Builder().scheme("content").authority(getPackageName()).appendPath("preferences").build();
        getContentResolver().update(uri, cv, null, null);
    }

    /**
     * Will update the current camera parameters with the focus area then
     * resubmit the parameters.
     * 
     * @param touchRect Touch area to focus
     */
    private void submitFocusAreaRect(final Rect touchRect) {
        Camera.Parameters cameraParameters = camera.getParameters();
        submitFocusAreaRect(cameraParameters, touchRect);
        camera.setParameters(cameraParameters);
        // Start the autofocus operation
        camera.autoFocus(this);
    }

    /**
     * Will update the supplied camera parameters with the focus touch rect.
     * This will not submit the camera parameters to the camera
     * 
     * @param touchRect Touch area to focus
     */
    private void submitFocusAreaRect(Camera.Parameters cameraParameters, final Rect touchRect)
    {
        if (cameraParameters.getMaxNumFocusAreas() == 0)
        {
            return;
        }

        // Convert from View's width and height to +/- 1000
        Rect focusArea = new Rect();
        focusArea.set(touchRect.left * 2000 / surfaceView.getWidth() - 1000,
                touchRect.top * 2000 / surfaceView.getHeight() - 1000,
                touchRect.right * 2000 / surfaceView.getWidth() - 1000,
                touchRect.bottom * 2000 / surfaceView.getHeight() - 1000);
        // The rect can not be < -1000 or > 1000 and the sides can *not* cross
        Rect max = new Rect(-1000, -1000, 1000, 1000);
        if (!max.contains(focusArea)) {
            Logger.e(TAG, "Could not focus. Rectangle " + focusArea.toString() + " is out of bounds.");
            return;
        } else if (focusArea.width() <= 0 || focusArea.height() <= 0) {
            Logger.e(TAG, "Could not focus. Rectangle " + focusArea.toString() + " has negative dimensions.");
            return;
        }
        // Submit focus area to camera
        ArrayList<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
        focusAreas.add(new Camera.Area(focusArea, 1000));

        cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        cameraParameters.setFocusAreas(focusAreas);
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        Log.d("XXX", "Focussed? " + success);
        // TODO: What happens when this doesn't succeed?
    }

    /**
     * Converts Rect to a comma deliminated string. Rect already has it's own
     * toString method, but this is just quicker to convert back. Like the
     * toString, the method returned is in the format left,top,right,bottom
     * 
     * @param rect Rect to convert. Can be null which will result in a 0,0,0,0
     *            rect
     */
    private String rectToString(Rect rect) {
        if (rect == null) {
            return "0,0,0,0";
        }

        return new StringBuilder()
                .append(rect.left)
                .append(",")
                .append(rect.top)
                .append(",")
                .append(rect.right)
                .append(",")
                .append(rect.bottom).toString();
    }

    /**
     * Converts the string from rectToString back to a Rect
     * 
     * @param string String in the same format rectToString returns
     * @return
     */
    private Rect stringToRect(String string) {
        String[] values = TextUtils.split(string, ",");
        if (values.length != 4) {
            throw new IllegalArgumentException("Invalid format");
        }
        return new Rect(
                Integer.valueOf(values[0]),
                Integer.valueOf(values[1]),
                Integer.valueOf(values[2]),
                Integer.valueOf(values[3]));
    }
}
