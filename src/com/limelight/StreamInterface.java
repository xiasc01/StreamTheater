package com.limelight;

import com.vrmatter.streamtheater.MainActivity;
import com.vrmatter.streamtheater.R;

import com.limelight.binding.PlatformBinding;
import com.limelight.binding.input.ControllerHandler;
import com.limelight.binding.input.KeyboardTranslator;
import com.limelight.binding.input.NvMouseHelper;
import com.limelight.binding.input.TouchContext;
import com.limelight.binding.input.driver.UsbDriverService;
import com.limelight.binding.input.evdev.EvdevHandler;
import com.limelight.binding.input.evdev.EvdevListener;
import com.limelight.binding.video.EnhancedDecoderRenderer;
import com.limelight.binding.video.MediaCodecDecoderRenderer;
import com.limelight.binding.video.MediaCodecHelper;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.NvConnectionListener;
import com.limelight.nvstream.StreamConfiguration;
import com.limelight.nvstream.av.video.VideoDecoderRenderer;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.input.KeyboardPacket;
import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.nvstream.input.ControllerPacket;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.ui.GameGestures;
import com.limelight.utils.Dialog;
import com.limelight.utils.SpinnerDialog;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import java.util.Locale;


public class StreamInterface implements SurfaceHolder.Callback,
    NvConnectionListener, EvdevListener, GameGestures
{
	private MainActivity activity;

    private int lastMouseX = Integer.MIN_VALUE;
    private int lastMouseY = Integer.MIN_VALUE;
    private int lastButtonState = 0;

    // Only 2 touches are supported
    private final TouchContext[] touchContextMap = new TouchContext[2];
    private long threeFingerDownTime = 0;

    private static final double REFERENCE_HORIZ_RES = 1280;
    private static final double REFERENCE_VERT_RES = 720;

    private static final int THREE_FINGER_TAP_THRESHOLD = 300;

    private ControllerHandler controllerHandler;
    private KeyboardTranslator keybTranslator;

    private PreferenceConfiguration prefConfig;
    private final Point screenSize = new Point((int)REFERENCE_HORIZ_RES, (int)REFERENCE_VERT_RES);

    private NvConnection conn;
    private SpinnerDialog spinner;
    private boolean displayedFailureDialog = false;
    private boolean connecting = false;
    private boolean connected = false;
    private boolean deferredSurfaceResize = false;

    private EvdevHandler evdevHandler;
    private int modifierFlags = 0;
    private boolean grabbedInput = true;
    private boolean grabComboDown = false;

    private EnhancedDecoderRenderer decoderRenderer;

    private WifiManager.WifiLock wifiLock;

    private int drFlags = 0;

    private boolean connectedToUsbDriverService = false;
    private ServiceConnection usbDriverServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            UsbDriverService.UsbDriverBinder binder = (UsbDriverService.UsbDriverBinder) iBinder;
            binder.setListener(controllerHandler);
            connectedToUsbDriverService = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            connectedToUsbDriverService = false;
        }
    };

    public static final String EXTRA_HOST = "Host";
    public static final String EXTRA_APP_NAME = "AppName";
    public static final String EXTRA_APP_ID = "AppId";
    public static final String EXTRA_UNIQUEID = "UniqueId";
    public static final String EXTRA_STREAMING_REMOTE = "Remote";

    public long getLastFrameTimestamp() {
    	if(decoderRenderer != null)
    		return decoderRenderer.getLastFrameTimestamp();
    	return 0;
    }

	public StreamInterface(MainActivity creatingActivity, String compUUID, String appName, int appId, String uniqueId, SurfaceHolder sh, int width, int height, int fps, boolean hostAudio, boolean remote) {
		activity = creatingActivity;
		
		ComputerDetails computer = activity.pcSelector.findByUUID(compUUID);

        String locale = PreferenceConfiguration.readPreferences(activity).language;
        if (!locale.equals(PreferenceConfiguration.DEFAULT_LANGUAGE)) {
            Configuration config = new Configuration(activity.getResources().getConfiguration());
            config.locale = new Locale(locale);
            activity.getResources().updateConfiguration(config, activity.getResources().getDisplayMetrics());
        }

        // Read the stream preferences
        prefConfig = PreferenceConfiguration.readPreferences(activity);

        // Overrid prefs with our settings
        prefConfig.width = width;
        prefConfig.height = height;
        prefConfig.fps = fps;
        prefConfig.playHostAudio = hostAudio;

        // 1080p30 and 720p60 are 10
        prefConfig.bitrate = 10;
        if(height == 1080 && fps == 60)
        {
        	prefConfig.bitrate = 20;
        }
        if(height == 720 && fps == 30)
        {
        	prefConfig.bitrate = 5;
        }
        if(height == 480 && fps == 60)
        {
        	prefConfig.bitrate = 4;
        }
        if(height == 480 && fps== 30)
        {
        	prefConfig.bitrate = 2;        	
        }


        if (prefConfig.stretchVideo) {
            drFlags |= VideoDecoderRenderer.FLAG_FILL_SCREEN;
        }

        // Warn the user if they're on a metered connection
        checkDataConnection();

        // Make sure Wi-Fi is fully powered up
        WifiManager wifiMgr = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiMgr.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Limelight");
        wifiLock.setReferenceCounted(false);
        wifiLock.acquire();

        if (appId == StreamConfiguration.INVALID_APP_ID) {
        	//TODO: This needs to go back to app selection
            return;
        }

        // Initialize the MediaCodec helper before creating the decoder
        MediaCodecHelper.initializeWithContext(activity);

        decoderRenderer = new MediaCodecDecoderRenderer(prefConfig.videoFormat);

        // Display a message to the user if H.265 was forced on but we still didn't find a decoder
        if (prefConfig.videoFormat == PreferenceConfiguration.FORCE_H265_ON && !decoderRenderer.isHevcSupported()) {
//            Toast.makeText(this, "No H.265 decoder found.\nFalling back to H.264.", Toast.LENGTH_LONG).show();
        }

        if (!decoderRenderer.isAvcSupported()) {
            if (spinner != null) {
                spinner.dismiss();
                spinner = null;
            }

            // If we can't find an AVC decoder, we can't proceed
            Dialog.displayDialog(activity, activity.getResources().getString(R.string.conn_error_title),
                    "This device or ROM doesn't support hardware accelerated H.264 playback.", true);
            return;
        }
        
        StreamConfiguration config = new StreamConfiguration.Builder()
                .setResolution(width, height)
                .setRefreshRate(fps)
                .setApp(new NvApp(appName, appId))
                .setBitrate(prefConfig.bitrate * 1000)
                .setEnableSops(prefConfig.enableSops)
                .enableAdaptiveResolution((decoderRenderer.getCapabilities() &
                        VideoDecoderRenderer.CAPABILITY_ADAPTIVE_RESOLUTION) != 0)
                .enableLocalAudioPlayback(hostAudio)
                .setMaxPacketSize(remote ? 1024 : 1292)
                .setRemote(remote)
                .setHevcSupported(decoderRenderer.isHevcSupported())
                .setAudioConfiguration(prefConfig.enable51Surround ?
                        StreamConfiguration.AUDIO_CONFIGURATION_5_1 :
                        StreamConfiguration.AUDIO_CONFIGURATION_STEREO)
                .build();

        // Initialize the connection
        String ip = computer.reachability == ComputerDetails.Reachability.LOCAL ?
                computer.localIp.getHostAddress() : computer.remoteIp.getHostAddress();
        conn = new NvConnection(ip, uniqueId, StreamInterface.this, config, PlatformBinding.getCryptoProvider(activity));
        keybTranslator = new KeyboardTranslator(conn);
        controllerHandler = new ControllerHandler(conn, this, prefConfig.multiController, prefConfig.deadzonePercentage);

        InputManager inputManager = (InputManager) activity.getSystemService(Context.INPUT_SERVICE);
        inputManager.registerInputDeviceListener(controllerHandler, null);

        boolean aspectRatioMatch = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            // On KitKat and later (where we can use the whole screen via immersive mode), we'll
            // calculate whether we need to scale by aspect ratio or not. If not, we'll use
            // setFixedSize so we can handle 4K properly. The only known devices that have
            // >= 4K screens have exactly 4K screens, so we'll be able to hit this good path
            // on these devices. On Marshmallow, we can start changing to 4K manually but no
            // 4K devices run 6.0 at the moment.
            double screenAspectRatio = ((double)screenSize.y) / screenSize.x;
            double streamAspectRatio = ((double)prefConfig.height) / prefConfig.width;
            if (Math.abs(screenAspectRatio - streamAspectRatio) < 0.001) {
                LimeLog.info("Stream has compatible aspect ratio with output display");
                aspectRatioMatch = true;
            }
        }

        if (prefConfig.stretchVideo || aspectRatioMatch) {
            // Set the surface to the size of the video
            sh.setFixedSize(prefConfig.width, prefConfig.height);
        }
        else {
            deferredSurfaceResize = true;
        }

        if (LimelightBuildProps.ROOT_BUILD) {
            // Start watching for raw input
            evdevHandler = new EvdevHandler(activity, this);
            evdevHandler.start();
        }

        if (prefConfig.usbDriver) {
            // Start the USB driver
        	activity.bindService(new Intent(activity, UsbDriverService.class),
                    usbDriverServiceConnection, Service.BIND_AUTO_CREATE);
        }

        // The connection will be started when the surface gets created
        sh.addCallback(this);
    }

    private void resizeSurfaceWithAspectRatio(SurfaceView sv, double vidWidth, double vidHeight)
    {
        // Get the visible width of the activity
        double visibleWidth = activity.getWindow().getDecorView().getWidth();

        ViewGroup.LayoutParams lp = sv.getLayoutParams();

        // Calculate the new size of the SurfaceView
        lp.width = (int) visibleWidth;
        lp.height = (int) ((vidHeight / vidWidth) * visibleWidth);

        // Apply the size change
        sv.setLayoutParams(lp);
    }

    private void checkDataConnection()
    {
        ConnectivityManager mgr = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mgr.isActiveNetworkMetered()) {
            displayTransientMessage(activity.getResources().getString(R.string.conn_metered));
        }
    }

    @SuppressLint("InlinedApi")
    private final Runnable hideSystemUi = new Runnable() {
            @Override
            public void run() {
                // Use immersive mode on 4.4+ or standard low profile on previous builds
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                	activity.getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
                else {
                	activity.getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LOW_PROFILE);
                }
            }
    };

    private void hideSystemUi(int delay) {
        Handler h = activity.getWindow().getDecorView().getHandler();
        if (h != null) {
            h.removeCallbacks(hideSystemUi);
            h.postDelayed(hideSystemUi, delay);
        }
    }

    public void stop() {
        SpinnerDialog.closeDialogs(activity);
        Dialog.closeDialogs();

		if (controllerHandler != null) {
        InputManager inputManager = (InputManager) activity.getSystemService(Context.INPUT_SERVICE);
        inputManager.unregisterInputDeviceListener(controllerHandler);
		}

        wifiLock.release();

        if (connectedToUsbDriverService) {
            // Unbind from the discovery service
        	activity.unbindService(usbDriverServiceConnection);
        }

        if (conn != null) {
        VideoDecoderRenderer.VideoFormat videoFormat = conn.getActiveVideoFormat();

        displayedFailureDialog = true;
        stopConnection();

        int averageEndToEndLat = decoderRenderer.getAverageEndToEndLatency();
        int averageDecoderLat = decoderRenderer.getAverageDecoderLatency();
        String message = null;
        if (averageEndToEndLat > 0) {
            message = activity.getResources().getString(R.string.conn_client_latency)+" "+averageEndToEndLat+" ms";
            if (averageDecoderLat > 0) {
                message += " ("+activity.getResources().getString(R.string.conn_client_latency_hw)+" "+averageDecoderLat+" ms)";
            }
        }
        else if (averageDecoderLat > 0) {
            message = activity.getResources().getString(R.string.conn_hardware_latency)+" "+averageDecoderLat+" ms";
        }

        // Add the video codec to the post-stream toast
        if (message != null && videoFormat != VideoDecoderRenderer.VideoFormat.Unknown) {
            if (videoFormat == VideoDecoderRenderer.VideoFormat.H265) {
                message += " [H.265]";
            }
            else {
                message += " [H.264]";
            }
        }

        if (message != null) {
        	// TODO: Need a non-error message passing.  Doesn't display right now anyway, so commenting out to stop crashes
            // MainActivity.nativeShowError(activity.getAppPtr(), message);
        }
        }

    }

    private final Runnable toggleGrab = new Runnable() {
        @Override
        public void run() {
            if (grabbedInput) {
                NvMouseHelper.setCursorVisibility(activity, true);
            if (evdevHandler != null) {
                    evdevHandler.ungrabAll();
                }
            }
                else {
                NvMouseHelper.setCursorVisibility(activity, false);
                if (evdevHandler != null) {
                    evdevHandler.regrabAll();
                }
            }

            grabbedInput = !grabbedInput;
        }
    };

    // Returns true if the key stroke was consumed
    private boolean handleSpecialKeys(short translatedKey, boolean down) {
        int modifierMask = 0;

        // Mask off the high byte
        translatedKey &= 0xff;

        if (translatedKey == KeyboardTranslator.VK_CONTROL) {
            modifierMask = KeyboardPacket.MODIFIER_CTRL;
        }
        else if (translatedKey == KeyboardTranslator.VK_SHIFT) {
            modifierMask = KeyboardPacket.MODIFIER_SHIFT;
        }
        else if (translatedKey == KeyboardTranslator.VK_ALT) {
            modifierMask = KeyboardPacket.MODIFIER_ALT;
        }

        if (down) {
            this.modifierFlags |= modifierMask;
        }
        else {
            this.modifierFlags &= ~modifierMask;
        }

        // Check if Ctrl+Shift+Z is pressed
        if (translatedKey == KeyboardTranslator.VK_Z &&
            (modifierFlags & (KeyboardPacket.MODIFIER_CTRL | KeyboardPacket.MODIFIER_SHIFT)) ==
                (KeyboardPacket.MODIFIER_CTRL | KeyboardPacket.MODIFIER_SHIFT))
        {
            if (down) {
                // Now that we've pressed the magic combo
                // we'll wait for one of the keys to come up
                grabComboDown = true;
            }
            else {
                // Toggle the grab if Z comes up
                Handler h = activity.getWindow().getDecorView().getHandler();
                if (h != null) {
                    h.postDelayed(toggleGrab, 250);
                }

                grabComboDown = false;
            }

            return true;
        }
        // Toggle the grab if control or shift comes up
        else if (grabComboDown) {
            Handler h = activity.getWindow().getDecorView().getHandler();
            if (h != null) {
                h.postDelayed(toggleGrab, 250);
            }

            grabComboDown = false;
            return true;
        }

        // Not a special combo
        return false;
    }

    private static byte getModifierState(KeyEvent event) {
        byte modifier = 0;
        if (event.isShiftPressed()) {
            modifier |= KeyboardPacket.MODIFIER_SHIFT;
        }
        if (event.isCtrlPressed()) {
            modifier |= KeyboardPacket.MODIFIER_CTRL;
        }
        if (event.isAltPressed()) {
            modifier |= KeyboardPacket.MODIFIER_ALT;
        }
        return modifier;
    }

    private byte getModifierState() {
        return (byte) modifierFlags;
    }

//    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Pass-through virtual navigation keys
        if ((event.getFlags() & KeyEvent.FLAG_VIRTUAL_HARD_KEY) != 0) {
            return false;
        }

        // Try the controller handler first
        boolean handled = controllerHandler.handleButtonDown(event);
        if (!handled) {
            // Try the keyboard handler
            short translated = keybTranslator.translate(event.getKeyCode());
            if (translated == 0) {
                return false;
            }

            // Let this method take duplicate key down events
            if (handleSpecialKeys(translated, true)) {
                return true;
            }

            // Eat repeat down events
            if (event.getRepeatCount() > 0) {
                return true;
            }

            // Pass through keyboard input if we're not grabbing
            if (!grabbedInput) {
                return false;
            }

            keybTranslator.sendKeyDown(translated,
                    getModifierState(event));
        }

        return true;
    }

//    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Pass-through virtual navigation keys
        if ((event.getFlags() & KeyEvent.FLAG_VIRTUAL_HARD_KEY) != 0) {
        	return false;
        }

        // Try the controller handler first
        boolean handled = controllerHandler.handleButtonUp(event);
        if (!handled) {
            // Try the keyboard handler
            short translated = keybTranslator.translate(event.getKeyCode());
            if (translated == 0) {
            	return false;
            }

            if (handleSpecialKeys(translated, false)) {
                return true;
            }

            // Pass through keyboard input if we're not grabbing
            if (!grabbedInput) {
                return false;
            }

            keybTranslator.sendKeyUp(translated,
                    getModifierState(event));
        }

        return true;
    }

    private TouchContext getTouchContext(int actionIndex)
    {
        if (actionIndex < touchContextMap.length) {
            return touchContextMap[actionIndex];
        }
        else {
            return null;
        }
    }

    @Override
    public void showKeyboard() {
        LimeLog.info("Showing keyboard overlay");
//        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    // Returns true if the event was consumed
    public boolean handleMotionEvent(MotionEvent event) {
        // Pass through keyboard input if we're not grabbing
        if (!grabbedInput) {
            return false;
        }

        if ((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
            if (controllerHandler.handleMotionEvent(event)) {
                return true;
            }
        }
        else if ((event.getSource() & InputDevice.SOURCE_CLASS_POINTER) != 0)
        {
            // This case is for mice
            if (event.getSource() == InputDevice.SOURCE_MOUSE ||
                    (event.getPointerCount() >= 1 &&
                            event.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE))
            {
                int changedButtons = event.getButtonState() ^ lastButtonState;

                if (event.getActionMasked() == MotionEvent.ACTION_SCROLL) {
                    // Send the vertical scroll packet
                    byte vScrollClicks = (byte) event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                    conn.sendMouseScroll(vScrollClicks);
                }

                if ((changedButtons & MotionEvent.BUTTON_PRIMARY) != 0) {
                    if ((event.getButtonState() & MotionEvent.BUTTON_PRIMARY) != 0) {
                        conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT);
                    }
                    else {
                        conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
                    }
                }

                if ((changedButtons & MotionEvent.BUTTON_SECONDARY) != 0) {
                    if ((event.getButtonState() & MotionEvent.BUTTON_SECONDARY) != 0) {
                        conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_RIGHT);
                    }
                    else {
                        conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_RIGHT);
                    }
                }

                if ((changedButtons & MotionEvent.BUTTON_TERTIARY) != 0) {
                    if ((event.getButtonState() & MotionEvent.BUTTON_TERTIARY) != 0) {
                        conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_MIDDLE);
                    }
                    else {
                        conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_MIDDLE);
                    }
                }

                // Get relative axis values if we can
                if (NvMouseHelper.eventHasRelativeMouseAxes(event)) {
                    // Send the deltas straight from the motion event
                    conn.sendMouseMove((short)NvMouseHelper.getRelativeAxisX(event),
                            (short)NvMouseHelper.getRelativeAxisY(event));

                    // We have to also update the position Android thinks the cursor is at
                    // in order to avoid jumping when we stop moving or click.
                    lastMouseX = (int)event.getX();
                    lastMouseY = (int)event.getY();
                }
                else {
                // First process the history
                for (int i = 0; i < event.getHistorySize(); i++) {
                    updateMousePosition((int)event.getHistoricalX(i), (int)event.getHistoricalY(i));
                }

                // Now process the current values
                updateMousePosition((int)event.getX(), (int)event.getY());
                }

                lastButtonState = event.getButtonState();
            }
            // This case is for touch-based input devices
            else
            {
//                if (virtualController != null &&
//                        virtualController.getControllerMode() == VirtualController.ControllerMode.Configuration) {
                    // Ignore presses when the virtual controller is in configuration mode
//                    return true;
//                }

                int actionIndex = event.getActionIndex();

                int eventX = (int)event.getX(actionIndex);
                int eventY = (int)event.getY(actionIndex);

                // Special handling for 3 finger gesture
                if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN &&
                        event.getPointerCount() == 3) {
                    // Three fingers down
                    threeFingerDownTime = SystemClock.uptimeMillis();

                    // Cancel the first and second touches to avoid
                    // erroneous events
                    for (TouchContext aTouchContext : touchContextMap) {
                        aTouchContext.cancelTouch();
                    }

                    return true;
                }

                TouchContext context = getTouchContext(actionIndex);
                if (context == null) {
                    return false;
                }

                switch (event.getActionMasked())
                {
                case MotionEvent.ACTION_POINTER_DOWN:
                case MotionEvent.ACTION_DOWN:
                    context.touchDownEvent(eventX, eventY);
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_UP:
                    if (event.getPointerCount() == 1) {
                        // All fingers up
                        if (SystemClock.uptimeMillis() - threeFingerDownTime < THREE_FINGER_TAP_THRESHOLD) {
                            // This is a 3 finger tap to bring up the keyboard
                            showKeyboard();
                            return true;
                        }
                    }
                    context.touchUpEvent(eventX, eventY);
                    if (actionIndex == 0 && event.getPointerCount() > 1 && !context.isCancelled()) {
                        // The original secondary touch now becomes primary
                        context.touchDownEvent((int)event.getX(1), (int)event.getY(1));
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    // ACTION_MOVE is special because it always has actionIndex == 0
                    // We'll call the move handlers for all indexes manually

                    // First process the historical events
                    for (int i = 0; i < event.getHistorySize(); i++) {
                        for (TouchContext aTouchContextMap : touchContextMap) {
                            if (aTouchContextMap.getActionIndex() < event.getPointerCount())
                            {
                                aTouchContextMap.touchMoveEvent(
                                        (int)event.getHistoricalX(aTouchContextMap.getActionIndex(), i),
                                        (int)event.getHistoricalY(aTouchContextMap.getActionIndex(), i));
                            }
                        }
                    }

                    // Now process the current values
                    for (TouchContext aTouchContextMap : touchContextMap) {
                        if (aTouchContextMap.getActionIndex() < event.getPointerCount())
                        {
                            aTouchContextMap.touchMoveEvent(
                                    (int)event.getX(aTouchContextMap.getActionIndex()),
                                    (int)event.getY(aTouchContextMap.getActionIndex()));
                        }
                    }
                    break;
                default:
                    return false;
                }
            }

            // Handled a known source
            return true;
        }

        // Unknown class
        return false;
    }

    private void updateMousePosition(int eventX, int eventY) {
        // Send a mouse move if we already have a mouse location
        // and the mouse coordinates change
        if (lastMouseX != Integer.MIN_VALUE &&
            lastMouseY != Integer.MIN_VALUE &&
            !(lastMouseX == eventX && lastMouseY == eventY))
        {
            int deltaX = eventX - lastMouseX;
            int deltaY = eventY - lastMouseY;

            // Scale the deltas if the device resolution is different
            // than the stream resolution
            deltaX = (int)Math.round((double)deltaX * (REFERENCE_HORIZ_RES / (double)screenSize.x));
            deltaY = (int)Math.round((double)deltaY * (REFERENCE_VERT_RES / (double)screenSize.y));

            conn.sendMouseMove((short)deltaX, (short)deltaY);
        }

        // Update pointer location for delta calculation next time
        lastMouseX = eventX;
        lastMouseY = eventY;
    }

    @Override
    public void stageStarting(Stage stage) {
        if (spinner != null) {
            spinner.setMessage(activity.getResources().getString(R.string.conn_starting)+" "+stage.getName());
        }
    }

    @Override
    public void stageComplete(Stage stage) {
    }

    private void stopConnection() {
        if (connecting || connected) {
            connecting = connected = false;
            conn.stop();
        }

        // Close the Evdev reader to allow use of captured input devices
        if (evdevHandler != null) {
            evdevHandler.stop();
            evdevHandler = null;
        }

        // Enable cursor visibility again
        NvMouseHelper.setCursorVisibility(activity, true);
    }

    @Override
    public void stageFailed(Stage stage) {
        if (spinner != null) {
            spinner.dismiss();
            spinner = null;
        }

        if (!displayedFailureDialog) {
            displayedFailureDialog = true;
            stopConnection();
            MainActivity.nativeShowError(activity.getAppPtr(), activity.getResources().getString(R.string.conn_error_title)
            		+ " - " + activity.getResources().getString(R.string.conn_error_msg)+" "+stage.getName());
        }
    }

    @Override
    public void connectionTerminated(Exception e) {
        if (!displayedFailureDialog) {
            displayedFailureDialog = true;
            e.printStackTrace();

            stopConnection();
            MainActivity.nativeShowError(activity.getAppPtr(), activity.getResources().getString(R.string.conn_terminated_title)
            		+ " - " + activity.getResources().getString(R.string.conn_terminated_msg));
        }
    }

    @Override
    public void connectionStarted() {
        if (spinner != null) {
            spinner.dismiss();
            spinner = null;
        }

        connecting = false;
        connected = true;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Hide the mouse cursor now. Doing it before
                // dismissing the spinner seems to be undone
                // when the spinner gets displayed.
                NvMouseHelper.setCursorVisibility(activity, false);
            }
        });

        hideSystemUi(1000);
    }

    @Override
    public void displayMessage(final String message) {
    	activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.nativeShowError(activity.getAppPtr(), message);
            }
        });
    }

    @Override
    public void displayTransientMessage(final String message) {
 /*       if (!prefConfig.disableWarnings) {
        	activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MainActivity.nativeShowError(activity.appPtr, message);
                }
            });
        }
 */   }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!connected && !connecting) {
            connecting = true;

            // Resize the surface to match the aspect ratio of the video
            // This must be done after the surface is created.
/*            if (deferredSurfaceResize) {
                resizeSurfaceWithAspectRatio((SurfaceView) findViewById(R.id.surfaceView),
                        prefConfig.width, prefConfig.height);
            }
*/
            activity.onVideoSizeChanged(prefConfig.width, prefConfig.height);
            conn.start(PlatformBinding.getDeviceName(), holder, drFlags,
                    PlatformBinding.getAudioRenderer(), decoderRenderer);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (connected) {
            stopConnection();
        }
    }

    @Override
    public void mouseMove(int deltaX, int deltaY) {
        conn.sendMouseMove((short) deltaX, (short) deltaY);
    }

    @Override
    public void mouseButtonEvent(int buttonId, boolean down) {
        byte buttonIndex;

        switch (buttonId)
        {
        case EvdevListener.BUTTON_LEFT:
            buttonIndex = MouseButtonPacket.BUTTON_LEFT;
            break;
        case EvdevListener.BUTTON_MIDDLE:
            buttonIndex = MouseButtonPacket.BUTTON_MIDDLE;
            break;
        case EvdevListener.BUTTON_RIGHT:
            buttonIndex = MouseButtonPacket.BUTTON_RIGHT;
            break;
        default:
            LimeLog.warning("Unhandled button: "+buttonId);
            return;
        }

        if (down) {
            conn.sendMouseButtonDown(buttonIndex);
        }
        else {
            conn.sendMouseButtonUp(buttonIndex);
        }
    }

    @Override
    public void mouseScroll(byte amount) {
        conn.sendMouseScroll(amount);
    }

    @Override
    public void keyboardEvent(boolean buttonDown, short keyCode) {
        short keyMap = keybTranslator.translate(keyCode);
        if (keyMap != 0) {
            if (handleSpecialKeys(keyMap, buttonDown)) {
                return;
            }

            if (buttonDown) {
                keybTranslator.sendKeyDown(keyMap, getModifierState());
            }
            else {
                keybTranslator.sendKeyUp(keyMap, getModifierState());
            }
        }
    }

    public boolean isConnected()
    {
    	return connected;
    }
}
