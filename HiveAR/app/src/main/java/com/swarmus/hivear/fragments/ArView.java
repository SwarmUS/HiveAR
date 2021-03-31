package com.swarmus.hivear.fragments;

import android.graphics.ImageFormat;
import android.media.Image;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.CameraConfigFilter;
import com.google.ar.core.CameraIntrinsics;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.swarmus.hivear.R;
import com.swarmus.hivear.apriltag.ApriltagDetection;
import com.swarmus.hivear.apriltag.ApriltagNative;
import com.swarmus.hivear.computervision.common.helpers.CameraPermissionHelper;
import com.swarmus.hivear.computervision.common.helpers.SnackbarHelper;
import com.swarmus.hivear.computervision.common.helpers.TrackingStateHelper;
import com.swarmus.hivear.computervision.computervision.CpuImageDisplayRotationHelper;
import com.swarmus.hivear.computervision.computervision.CpuImageRenderer;
import com.swarmus.hivear.computervision.computervision.EdgeDetector;
import com.swarmus.hivear.computervision.computervision.FrameTimeHelper;
import com.swarmus.hivear.computervision.computervision.TextureReader;
import com.swarmus.hivear.computervision.computervision.TextureReaderImage;
import com.swarmus.hivear.utils.ConvertUtil;
import com.swarmus.hivear.utils.MathUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ArView extends Fragment implements GLSurfaceView.Renderer {
    private static final String TAG = ArView.class.getSimpleName();
    private static final String CAMERA_INTRINSICS_TEXT_FORMAT =
            "\tUnrotated Camera %s %s Intrinsics:\n\tFocal Length: (%.2f, %.2f)"
                    + "\n\tPrincipal Point: (%.2f, %.2f)"
                    + "\n\t%s Image Dimensions: (%d, %d)"
                    + "\n\tUnrotated Field of View: (%.2f˚, %.2f˚)"
                    + "\n\tRender frame time: %.1f ms (%.0ffps)"
                    + "\n\tCPU image frame time: %.1f ms (%.0ffps)";
    private static final float RADIANS_TO_DEGREES = (float) (180 / Math.PI);

    // This app demonstrates two approaches to obtaining image data accessible on CPU:
    // 1. Access the CPU image directly from ARCore. This approach delivers a frame without latency
    //    (if available), but currently is lower resolution than the GPU image.
    // 2. Download the texture from GPU. This approach incurs a 1-frame latency, but allows a high
    //    resolution image.
    private enum ImageAcquisitionPath {
        CPU_DIRECT_ACCESS,
        GPU_DOWNLOAD
    }

    // Select the image acquisition path here.
    private final ImageAcquisitionPath imageAcquisitionPath = ImageAcquisitionPath.CPU_DIRECT_ACCESS;

    // Multiple CPU image Resolution.
    private enum ImageResolution {
        LOW_RESOLUTION,
        MEDIUM_RESOLUTION,
        HIGH_RESOLUTION,
    }

    // Default CPU image is low resolution.
    private ImageResolution cpuResolution = ImageResolution.LOW_RESOLUTION;

    // Session management and rendering.
    private GLSurfaceView surfaceView;
    private Session session;
    private Config config;
    private boolean installRequested;
    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
    private CpuImageDisplayRotationHelper cpuImageDisplayRotationHelper;
    private TrackingStateHelper trackingStateHelper;
    private CpuImageRenderer cpuImageRenderer;
    private final EdgeDetector edgeDetector = new EdgeDetector();

    // This lock prevents changing resolution as the frame is being rendered. ARCore requires all
    // CPU images to be released before changing resolution.
    private final Object frameImageInUseLock = new Object();

    // Camera intrinsics text view.
    private TextView cameraIntrinsicsTextView;

    // The fields below are used for the GPU_DOWNLOAD image acquisition path.
    private final TextureReader textureReader = new TextureReader();
    private int gpuDownloadFrameBufferIndex = -1;

    // ARCore full resolution GL texture typically has a size of 1920 x 1080.
    private static final int TEXTURE_WIDTH = 1920;
    private static final int TEXTURE_HEIGHT = 1080;

    // We choose a lower sampling resolution.
    private static final int IMAGE_WIDTH = 1280;
    private static final int IMAGE_HEIGHT = 720;

    // For Camera Configuration APIs usage.
    private CameraConfig cpuLowResolutionCameraConfig;
    private CameraConfig cpuMediumResolutionCameraConfig;
    private CameraConfig cpuHighResolutionCameraConfig;

    private Switch cvModeSwitch;
    private boolean isCVModeOn = true;
    private Switch focusModeSwitch;
    RadioGroup radioGroup;
    RadioButton radioLow;
    RadioButton radioMedium;
    RadioButton radioHigh;

    private final FrameTimeHelper renderFrameTimeHelper = new FrameTimeHelper();
    private final FrameTimeHelper cpuImageFrameTimeHelper = new FrameTimeHelper();

    // April tag
    private ArrayList<ApriltagDetection> mDetections;
    // Projection * View * Model matrix
    float[] M = new float[16];
    float[] V = new float[16];
    float[] P = new float[16];
    float[] PVM = new float[16];

    private ByteBuffer yuvBuffer;

    int previewWidth;
    int previewHeight;

    private static final double APRIL_TAG_SCALE_M = 0.2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApriltagNative.apriltag_init("tag36h11", 4, 2, 0.0, 8);

        cpuImageRenderer = new CpuImageRenderer();

        trackingStateHelper = new TrackingStateHelper(requireActivity());

        getLifecycle().addObserver(renderFrameTimeHelper);
        getLifecycle().addObserver(cpuImageFrameTimeHelper);

        installRequested = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_ar_view, container, false);

        surfaceView = view.findViewById(R.id.surfaceview);
        cameraIntrinsicsTextView = view.findViewById(R.id.camera_intrinsics_view);
        surfaceView = view.findViewById(R.id.surfaceview);
        cvModeSwitch = view.findViewById(R.id.switch_cv_mode);
        cvModeSwitch.setOnCheckedChangeListener(this::onCVModeChanged);
        focusModeSwitch = view.findViewById(R.id.switch_focus_mode);
        focusModeSwitch.setOnCheckedChangeListener(this::onFocusModeChanged);
        radioGroup = view.findViewById(R.id.radio_camera_configs);
        radioLow = view.findViewById(R.id.radio_low_res);
        radioLow.setOnClickListener(this::onLowResolutionRadioButtonClicked);
        radioMedium = view.findViewById(R.id.radio_medium_res);
        radioLow.setOnClickListener(this::onMediumResolutionRadioButtonClicked);
        radioHigh = view.findViewById(R.id.radio_high_res);
        radioLow.setOnClickListener(this::onHighResolutionRadioButtonClicked);

        cpuImageDisplayRotationHelper = new CpuImageDisplayRotationHelper(requireContext());

        // Set up renderer.
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        surfaceView.setWillNotDraw(false);
        return view;
    }

    @Override
    public void onDestroy() {
        if (session != null) {
            // Explicitly close ARCore Session to release native resources.
            // Review the API reference for important considerations before calling close() in apps with
            // more complicated lifecycle requirements:
            // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
            session.close();
            session = null;
        }

        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(requireActivity(), !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(requireActivity())) {
                    CameraPermissionHelper.requestCameraPermission(requireActivity());
                    return;
                }

                session = session == null ? session : new Session(requireContext());
                config = new Config(session);
            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (Exception e) {
                message = "This device does not support AR";
                exception = e;
            }

            if (message != null) {
                messageSnackbarHelper.showError(requireActivity(), message);
                Log.e(TAG, "Exception creating session", exception);
                return;
            }
        }

        obtainCameraConfigs();

        cvModeSwitch.setChecked(cpuImageRenderer.getSplitterPosition() < 0.5f);
        focusModeSwitch.setChecked(config.getFocusMode() != Config.FocusMode.FIXED);

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            messageSnackbarHelper.showError(requireActivity(), "Camera not available. Try restarting the app.");
            session = null;
            return;
        }
        surfaceView.onResume();
        cpuImageDisplayRotationHelper.onResume();
    }



    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            cpuImageDisplayRotationHelper.onPause();
            surfaceView.onPause();
            session.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (!CameraPermissionHelper.hasCameraPermission(requireActivity())) {
            Toast.makeText(requireContext(), "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(requireActivity())) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(requireActivity());
            }
            requireActivity().finish();
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Create the texture and pass it to ARCore session to be filled during update().
        try {
            cpuImageRenderer.createOnGlThread(requireContext());

            // The image format can be either IMAGE_FORMAT_RGBA or IMAGE_FORMAT_I8.
            // Set keepAspectRatio to false so that the output image covers the whole viewport.
            textureReader.create(
                    requireContext(),
                    TextureReaderImage.IMAGE_FORMAT_I8,
                    IMAGE_WIDTH,
                    IMAGE_HEIGHT,
                    false);

        } catch (IOException e) {
            Log.e(TAG, "Failed to read an asset file", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        cpuImageDisplayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
        Matrix.setIdentityM(V, 0);
        Matrix.translateM(V, 0, width/2.0f, height/2.0f, 0);
        Matrix.orthoM(P, 0, 0, width, 0, height, -1, 1);

        // Update surface dimensions and scale preview to fit the surface
        // Scaling is done to maintain aspect ratio but maximally fill the surface
        // Note: Camera preview size and surface size have width/height swapped
        int preview_width = previewHeight;
        int preview_height = previewWidth;

        float width_ratio = width / (float) (preview_width);
        float height_ratio = height / (float) (preview_height);
        float scale_ratio = Math.max(width_ratio, height_ratio);
        int draw_width = (int) (previewWidth * scale_ratio);
        int draw_height = (int) (previewWidth * scale_ratio);

        Matrix.setIdentityM(this.M, 0);
        Matrix.scaleM(this.M, 0, draw_height, draw_width, 1.0f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (session == null) {
            return;
        }

        // Synchronize here to avoid calling Session.update or Session.acquireCameraImage while paused.
        synchronized (frameImageInUseLock) {
            // Notify ARCore session that the view size changed so that the perspective matrix and
            // the video background can be properly adjusted.
            cpuImageDisplayRotationHelper.updateSessionIfNeeded(session);

            try {
                session.setCameraTextureName(cpuImageRenderer.getTextureId());
                final Frame frame = session.update();
                final Camera camera = frame.getCamera();

                Image image = frame.acquireCameraImage();
                CameraIntrinsics intrinsics = camera.getImageIntrinsics();

                double[] principalPoint = ConvertUtil.convertToDoubleArray(intrinsics.getPrincipalPoint());
                double[] focalLength = ConvertUtil.convertToDoubleArray(intrinsics.getFocalLength());
                ProcessingThread thread = new ProcessingThread(
                        YUV_420_888_to_nv1(image),
                        image.getWidth(),
                        image.getHeight(),
                        principalPoint,
                        focalLength,
                        this);
                thread.run();
                image.close();

                // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
                trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

                renderFrameTimeHelper.nextFrame();

                switch (imageAcquisitionPath) {
                    case CPU_DIRECT_ACCESS:
                        renderProcessedImageCpuDirectAccess(frame);
                        break;
                    case GPU_DOWNLOAD:
                        renderProcessedImageGpuDownload(frame);
                        break;
                }

                // Update the camera intrinsics' text.
                requireActivity().runOnUiThread(() -> cameraIntrinsicsTextView.setText(getCameraIntrinsicsText(frame)));
            } catch (Exception t) {
                // Avoid crashing the application due to unhandled exceptions.
                Log.e(TAG, "Exception on the OpenGL thread", t);
            }
        }
    }

    /* Demonstrates how to access a CPU image directly from ARCore. */
    private void renderProcessedImageCpuDirectAccess(Frame frame) {
        try (Image image = frame.acquireCameraImage()) {
            if (image.getFormat() != ImageFormat.YUV_420_888) {
                throw new IllegalArgumentException(
                        "Expected image in YUV_420_888 format, got format " + image.getFormat());
            }

            ByteBuffer processedImageBytesGrayscale = null;
            // Do not process the image with edge dectection algorithm if it is not being displayed.
            if (isCVModeOn) {
                processedImageBytesGrayscale =
                        edgeDetector.detect(
                                image.getWidth(),
                                image.getHeight(),
                                image.getPlanes()[0].getRowStride(),
                                image.getPlanes()[0].getBuffer());
            }

            cpuImageRenderer.drawWithCpuImage(
                    frame,
                    image.getWidth(),
                    image.getHeight(),
                    processedImageBytesGrayscale,
                    cpuImageDisplayRotationHelper.getViewportAspectRatio(),
                    cpuImageDisplayRotationHelper.getCameraToDisplayRotation());

            image.close();

            // Measure frame time since last successful execution of drawWithCpuImage().
            cpuImageFrameTimeHelper.nextFrame();
        } catch (NotYetAvailableException e) {
            // This exception will routinely happen during startup, and is expected. cpuImageRenderer
            // will handle null image properly, and will just render the background.
            cpuImageRenderer.drawWithoutCpuImage();
        }
    }

    /* Demonstrates how to access a CPU image using a download from GPU. */
    private void renderProcessedImageGpuDownload(Frame frame) {
        // If there is a frame being requested previously, acquire the pixels and process it.
        if (gpuDownloadFrameBufferIndex >= 0) {
            TextureReaderImage image = textureReader.acquireFrame(gpuDownloadFrameBufferIndex);

            if (image.format != TextureReaderImage.IMAGE_FORMAT_I8) {
                throw new IllegalArgumentException(
                        "Expected image in I8 format, got format " + image.format);
            }

            ByteBuffer processedImageBytesGrayscale =
                    edgeDetector.detect(image.width, image.height, /* stride= */ image.width, image.buffer);

            // You should always release frame buffer after using. Otherwise the next call to
            // submitFrame() may fail.
            textureReader.releaseFrame(gpuDownloadFrameBufferIndex);

            cpuImageRenderer.drawWithCpuImage(
                    frame,
                    IMAGE_WIDTH,
                    IMAGE_HEIGHT,
                    processedImageBytesGrayscale,
                    cpuImageDisplayRotationHelper.getViewportAspectRatio(),
                    cpuImageDisplayRotationHelper.getCameraToDisplayRotation());

            // Measure frame time since last successful execution of drawWithCpuImage().
            cpuImageFrameTimeHelper.nextFrame();
        } else {
            cpuImageRenderer.drawWithoutCpuImage();
        }

        // Submit request for the texture from the current frame.
        gpuDownloadFrameBufferIndex =
                textureReader.submitFrame(cpuImageRenderer.getTextureId(), TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    public void onLowResolutionRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        if (checked && cpuResolution != ImageResolution.LOW_RESOLUTION) {
            // Display low resolution.
            onCameraConfigChanged(cpuLowResolutionCameraConfig);
            cpuResolution = ImageResolution.LOW_RESOLUTION;
        }
    }

    public void onMediumResolutionRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        if (checked && cpuResolution != ImageResolution.MEDIUM_RESOLUTION) {
            // Display medium resolution.
            onCameraConfigChanged(cpuMediumResolutionCameraConfig);
            cpuResolution = ImageResolution.MEDIUM_RESOLUTION;
        }
    }

    public void onHighResolutionRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        if (checked && cpuResolution != ImageResolution.HIGH_RESOLUTION) {
            // Display high resolution.
            onCameraConfigChanged(cpuHighResolutionCameraConfig);
            cpuResolution = ImageResolution.HIGH_RESOLUTION;
        }
    }

    private void onCVModeChanged(CompoundButton unusedButton, boolean isChecked) {
        cpuImageRenderer.setSplitterPosition(isChecked ? 0.0f : 1.0f);
        isCVModeOn = isChecked;

        // Display the CPU resolution related UI only when CPU image is being displayed.
        boolean show = (cpuImageRenderer.getSplitterPosition() < 0.5f);
        radioGroup.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    private void onFocusModeChanged(CompoundButton unusedButton, boolean isChecked) {
        config.setFocusMode(isChecked ? Config.FocusMode.AUTO : Config.FocusMode.FIXED);
        session.configure(config);
    }

    private void onCameraConfigChanged(CameraConfig cameraConfig) {
        // To change the AR camera config - first we pause the AR session, set the desired camera
        // config and then resume the AR session.
        if (session != null) {
            // Block here if the image is still being used.
            synchronized (frameImageInUseLock) {
                session.pause();
                session.setCameraConfig(cameraConfig);
                previewWidth = cameraConfig.getImageSize().getWidth();
                previewHeight = cameraConfig.getImageSize().getHeight();
                int nbytes = cameraConfig.getImageSize().getWidth() * cameraConfig.getImageSize().getHeight() * 3 / 2; // XXX: What's the 3/2 scaling for?
                if (yuvBuffer == null || yuvBuffer.capacity() < nbytes) {
                    // Allocate direct byte buffer so native code access won't require a copy
                    Log.i(TAG, "Allocating buf of mPreviewSize " + nbytes);
                    yuvBuffer = ByteBuffer.allocateDirect(nbytes);
                }
                try {
                    session.resume();
                } catch (CameraNotAvailableException ex) {
                    messageSnackbarHelper.showError(requireActivity(), "Camera not available. Try restarting the app.");
                    session = null;
                    return;
                }
            }

            // Let the user know that the camera config is set.
            String toastMessage =
                    "Set the camera config with CPU image resolution of "
                            + cameraConfig.getImageSize()
                            + " and fps "
                            + cameraConfig.getFpsRange()
                            + ".";
            Toast toast = Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM, /* xOffset= */ 0, /* yOffset=*/ 250);
            toast.show();
        }
    }

    // Obtains the supported camera configs and build the list of radio button one for each camera
    // config.
    private void obtainCameraConfigs() {
        // First obtain the session handle before getting the list of various camera configs.
        if (session != null) {
            // Create filter here with desired fps filters.
            CameraConfigFilter cameraConfigFilter =
                    new CameraConfigFilter(session)
                            .setTargetFps(
                                    EnumSet.of(
                                            CameraConfig.TargetFps.TARGET_FPS_30, CameraConfig.TargetFps.TARGET_FPS_60));
            List<CameraConfig> cameraConfigs = session.getSupportedCameraConfigs(cameraConfigFilter);
            Log.i(TAG, "Size of supported CameraConfigs list is " + cameraConfigs.size());

            // Determine the highest and lowest CPU resolutions.
            cpuLowResolutionCameraConfig =
                    getCameraConfigWithSelectedResolution(
                            cameraConfigs, /*ImageResolution*/ ImageResolution.LOW_RESOLUTION);
            cpuMediumResolutionCameraConfig =
                    getCameraConfigWithSelectedResolution(
                            cameraConfigs, /*ImageResolution*/ ImageResolution.MEDIUM_RESOLUTION);
            cpuHighResolutionCameraConfig =
                    getCameraConfigWithSelectedResolution(
                            cameraConfigs, /*ImageResolution*/ ImageResolution.HIGH_RESOLUTION);
            // Update the radio buttons with the resolution info.
            updateRadioButtonText(
                    radioLow, cpuLowResolutionCameraConfig, getString(R.string.label_low_res));
            updateRadioButtonText(
                    radioMedium,
                    cpuMediumResolutionCameraConfig,
                    getString(R.string.label_medium_res));
            updateRadioButtonText(
                    radioHigh, cpuHighResolutionCameraConfig, getString(R.string.label_high_res));
            cpuResolution = ImageResolution.LOW_RESOLUTION;

            int nbytes = cameraConfigs.get(0).getImageSize().getWidth() * cameraConfigs.get(0).getImageSize().getHeight() * 3 / 2; // XXX: What's the 3/2 scaling for?
            if (yuvBuffer == null || yuvBuffer.capacity() < nbytes) {
                // Allocate direct byte buffer so native code access won't require a copy
                Log.i(TAG, "Allocating buf of mPreviewSize " + nbytes);
                yuvBuffer = ByteBuffer.allocateDirect(nbytes);
            }
            previewWidth = cameraConfigs.get(0).getImageSize().getWidth();
            previewHeight = cameraConfigs.get(0).getImageSize().getHeight();
        }
    }

    private void updateRadioButtonText(RadioButton radioButton, CameraConfig cameraConfig, String prefix) {
        Size resolution = cameraConfig.getImageSize();
        radioButton.setText(prefix + " (" + resolution.getWidth() + "x" + resolution.getHeight() + ")");
    }

    /* Get the CameraConfig with selected resolution. */
    private static CameraConfig getCameraConfigWithSelectedResolution(
            List<CameraConfig> cameraConfigs, ImageResolution resolution) {
        // Take the first three camera configs, if camera configs size are larger than 3.
        List<CameraConfig> cameraConfigsByResolution =
                new ArrayList<>(
                        cameraConfigs.subList(0, Math.min(cameraConfigs.size(), 3)));
        Collections.sort(
                cameraConfigsByResolution,
                (CameraConfig p1, CameraConfig p2) ->
                        Integer.compare(p1.getImageSize().getHeight(), p2.getImageSize().getHeight()));
        CameraConfig cameraConfig = cameraConfigsByResolution.get(0);
        switch (resolution) {
            case LOW_RESOLUTION:
                cameraConfig = cameraConfigsByResolution.get(0);
                break;
            case MEDIUM_RESOLUTION:
                // There are some devices that medium resolution is the same as high resolution.
                cameraConfig = cameraConfigsByResolution.get(1);
                break;
            case HIGH_RESOLUTION:
                cameraConfig = cameraConfigsByResolution.get(2);
                break;
        }
        return cameraConfig;
    }

    private String getCameraIntrinsicsText(Frame frame) {
        Camera camera = frame.getCamera();

        boolean forGpuTexture = (cpuImageRenderer.getSplitterPosition() > 0.5f);
        CameraIntrinsics intrinsics =
                forGpuTexture ? camera.getTextureIntrinsics() : camera.getImageIntrinsics();
        String intrinsicsLabel = forGpuTexture ? "Texture" : "Image";
        String imageType = forGpuTexture ? "GPU" : "CPU";

        float[] focalLength = intrinsics.getFocalLength();
        float[] principalPoint = intrinsics.getPrincipalPoint();
        int[] imageSize = intrinsics.getImageDimensions();

        float fovX = (float) (2 * Math.atan2((double) imageSize[0], (double) (2 * focalLength[0])));
        float fovY = (float) (2 * Math.atan2((double) imageSize[1], (double) (2 * focalLength[1])));
        fovX *= RADIANS_TO_DEGREES;
        fovY *= RADIANS_TO_DEGREES;

        return String.format(
                CAMERA_INTRINSICS_TEXT_FORMAT,
                imageType,
                intrinsicsLabel,
                focalLength[0],
                focalLength[1],
                principalPoint[0],
                principalPoint[1],
                imageType,
                imageSize[0],
                imageSize[1],
                fovX,
                fovY,
                renderFrameTimeHelper.getSmoothedFrameTime(),
                renderFrameTimeHelper.getSmoothedFrameRate(),
                cpuImageFrameTimeHelper.getSmoothedFrameTime(),
                cpuImageFrameTimeHelper.getSmoothedFrameRate());
    }

    static int loadShader(int type, String shaderCode){
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);
        checkGlError("glCreateShader");

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        checkGlError("glShaderSource");
        GLES20.glCompileShader(shader);
        checkGlError("glCompileShader");

        return shader;
    }

    static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            Log.e(TAG, msg);
            throw new RuntimeException(msg);
        }
    }

    static class ProcessingThread extends Thread {
        byte[] bytes;
        int width;
        int height;
        double[] centralPoint;
        double[] focalLength;
        ArView parent;

        ProcessingThread(byte[] bytes, int width, int height, double[] focalLength, double[] centralPoint, ArView parent) {
            this.bytes = bytes;
            this.width = width;
            this.height = height;
            this.centralPoint = centralPoint;
            this.focalLength = focalLength;
            this.parent = parent;
        }

        public void run() {
            parent.mDetections = ApriltagNative.apriltag_detect_yuv(bytes, width, height, APRIL_TAG_SCALE_M, centralPoint, focalLength);
            if (parent.mDetections.size() > 0) {
                Log.i(TAG, "Detections: " + parent.mDetections.size());
                for (ApriltagDetection detection : parent.mDetections) {
                    Log.i(TAG, "Distance" + MathUtil.getNorm(detection.pose_t));
                }
            }
        }
    }

    byte[] YUV_420_888_to_nv1(Image image) {
        if (image.getFormat() != com.google.ar.core.ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("Invalid image format");
        }

        byte[] nv21;
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv21 = new byte[ySize + uSize + vSize];

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv21;
    }
}