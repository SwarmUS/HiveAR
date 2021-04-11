package com.swarmus.hivear.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;


import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.CameraIntrinsics;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.NodeParent;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseTransformableNode;
import com.google.ar.sceneform.ux.SelectionVisualizer;
import com.google.ar.sceneform.ux.TransformableNode;
import com.swarmus.hivear.R;
import com.swarmus.hivear.apriltag.ApriltagDetection;
import com.swarmus.hivear.apriltag.ApriltagNative;
import com.swarmus.hivear.ar.CameraFacingNode;
import com.swarmus.hivear.models.Robot;
import com.swarmus.hivear.viewmodels.CurrentArRobotViewModel;
import com.swarmus.hivear.viewmodels.RobotListViewModel;
import com.swarmus.hivear.viewmodels.SettingsViewModel;
import com.swarmus.hivear.utils.ConvertUtil;
import com.swarmus.hivear.utils.MathUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class ARViewFragment extends Fragment {

    private static final String TAG = ARViewFragment.class.getName();
    private static final double APRIL_TAG_SCALE_M = 0.211;

    private ArFragment arFragment;
    private HashMap<String, AugmentedImage.TrackingMethod> trackableInfos;
    private ModelRenderable arrowRenderable;
    private ModelRenderable xyzRenderable;
    private RobotListViewModel robotListViewModel;
    private TextView debugInfo;

    private final static String ARROW_RENDERABLE_NAME = "Arrow Renderable";
    private final static String AR_ROBOT_NAME = "Robot Name";
    private final static float QRCodeWidth = 0.1f; // m
    private final Object frameImageInUseLock = new Object();

    private Handler timerHandler;
    private HashMap<Robot,TextView> timerTextViews = new HashMap<>();
    private ProcessingThread aprilTagProcessingThread;
    private AnchorNode worldNode;
    static Pose defPose = Pose.makeRotation(0.7071068f, 0, 0, 0.7071068f).compose(Pose.makeRotation(0,1,0,0));
    float vValue = 0f;
    static final float sqrtHalf = (float) Math.sqrt(0.5f);
    final static Pose xRot = Pose.makeRotation(sqrtHalf, 0, 0, sqrtHalf);
    final static Pose yRot = Pose.makeRotation(0, sqrtHalf, 0, sqrtHalf);
    final static Pose zRot = Pose.makeRotation(0, 0, sqrtHalf, sqrtHalf);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        robotListViewModel = new ViewModelProvider(requireActivity()).get(RobotListViewModel.class);

        timerHandler = new Handler();
        Runnable timerRunnable =  new Runnable() {
            @Override
            public void run() {
                // Update all timer text
                timerTextViews.forEach((r,tv) -> {
                    long millis = System.currentTimeMillis() - r.getLastUpdateTimeMillis();
                    String text = getResources().getString(R.string.last_update);
                    text += String.format(" %02dm : %02ds",
                            TimeUnit.MILLISECONDS.toMinutes(millis),
                            TimeUnit.MILLISECONDS.toSeconds(millis) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
                    );
                    tv.setText(text);
                });
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000); // pick every second by evaluating at each 0.5s

        ApriltagNative.apriltag_init("tag36h11", 2, 4, 0, 16);

        ModelRenderable.builder()
                .setSource(getContext(), R.raw.arrow)
                .build()
                .thenAccept(renderable -> {
                    arrowRenderable = renderable;
                    arrowRenderable.setShadowCaster(false);
                    arrowRenderable.setShadowReceiver(false);
                })
                .exceptionally(
                        throwable -> {
                            Log.e(ARViewFragment.class.getName(), "Unable to load Renderable.", throwable);
                            return null;
                        });

        ModelRenderable.builder()
                .setSource(getContext(), R.raw.xyz_arrow)
                .build()
                .thenAccept(renderable -> {
                    xyzRenderable = renderable;
                    xyzRenderable.setShadowCaster(false);
                    xyzRenderable.setShadowReceiver(false);
                })
                .exceptionally(
                        throwable -> {
                            Log.e(ARViewFragment.class.getName(), "Unable to load Renderable.", throwable);
                            return null;
                        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.ar_view_fragment, container, false);

        CurrentArRobotViewModel currentArRobotViewModel = new ViewModelProvider(requireActivity()).get(CurrentArRobotViewModel.class);
        currentArRobotViewModel.getSelectedRobot().observe(requireActivity(), robot -> setRobotUI(view, robot));

        debugInfo = view.findViewById(R.id.debug_info);

        setRobotUI(view, currentArRobotViewModel.getSelectedRobot().getValue());
        trackableInfos = new HashMap<>();

        arFragment = (ArFragment)getChildFragmentManager().findFragmentById(R.id.ux_fragment);
        if (arFragment!=null) {
            Session session = arFragment.getArSceneView().getSession();
            if (session == null) {
                try {
                    session = new Session(requireContext());
                } catch (UnavailableArcoreNotInstalledException e) {
                    e.printStackTrace();
                } catch (UnavailableApkTooOldException e) {
                    e.printStackTrace();
                } catch (UnavailableSdkTooOldException e) {
                    e.printStackTrace();
                } catch (UnavailableDeviceNotCompatibleException e) {
                    e.printStackTrace();
                }
            }
            Config config = new Config(session);
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            AugmentedImageDatabase augmentedImageDatabase = new AugmentedImageDatabase(session);
            setupArDatabase(augmentedImageDatabase);
            config.setAugmentedImageDatabase(augmentedImageDatabase);
            config.setFocusMode(Config.FocusMode.FIXED);
            config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL);
            config.setLightEstimationMode(Config.LightEstimationMode.DISABLED);
            session.configure(config);
            arFragment.getArSceneView().setupSession(session);
            //arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> updateFrame(frameTime));
            arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> getAprilTags(frameTime));

            // Override to show nothing instead of default grey circle underneath selected anchor node
            arFragment.getTransformationSystem().setSelectionVisualizer(new SelectionVisualizer() {
                @Override
                public void applySelectionVisual(BaseTransformableNode node) {}

                @Override
                public void removeSelectionVisual(BaseTransformableNode node) { }
            });
        }

        return view;
    }

    private void setRobotUI(View v, Robot robot) {
        Boolean isRobotSelected = robot != null;
        LinearLayout robotInfoLayout = v.findViewById(R.id.robot_ar_selected);
        robotInfoLayout.setVisibility(isRobotSelected ? LinearLayout.VISIBLE : LinearLayout.GONE);
        TextView robotName = v.findViewById(R.id.robot_ar_selected_name);
        robotName.setText(isRobotSelected ? robot.getName() : "");
        TextView robotUid = v.findViewById(R.id.robot_ar_selected_uid);
        robotUid.setText(isRobotSelected ? Integer.toString(robot.getUid()) : "");
    }

    // More details here: https://medium.com/free-code-camp/how-to-build-an-augmented-images-application-with-arcore-93e417b8579d
    private void updateFrame(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();

        // Set trackable
        Collection<AugmentedImage> augmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);
        for (AugmentedImage augmentedImage : augmentedImages) {
            AugmentedImage.TrackingMethod currentMethod = augmentedImage.getTrackingMethod();
            if (!trackableInfos.containsKey(augmentedImage.getName())) {
                trackableInfos.put(augmentedImage.getName(), augmentedImage.getTrackingMethod());
            }
            if (currentMethod != trackableInfos.get(augmentedImage.getName())) {
                trackableInfos.replace(augmentedImage.getName(), currentMethod);
                String msg = augmentedImage.getName() + " : " +currentMethod.toString();
                // Show to user if currently tracking
                if (currentMethod.equals(AugmentedImage.TrackingMethod.FULL_TRACKING)) {

                    // Set anchor node
                    String robotName = augmentedImage.getName().split("-")[0];
                    int robotUid = Integer.parseInt(augmentedImage.getName().split("-")[1]);
                    // Don't add AR stuff if robot not registered in swarm
                    if (robotListViewModel.getRobotFromList(robotUid) == null) {
                        Toast.makeText(requireContext(),
                                "Robot " + robotName + "-" + robotUid + " not in current swarm",
                                Toast.LENGTH_LONG).show();
                        continue;
                    }

                    NodeParent nodeParent = arFragment.getArSceneView().getScene().findInHierarchy(sceneNode -> sceneNode.getName().equals(augmentedImage.getName()));

                    AnchorNode anchorNode = nodeParent == null ? new AnchorNode() : (AnchorNode)nodeParent;
                    anchorNode.setAnchor(augmentedImage.createAnchor(augmentedImage.getCenterPose()));
                    anchorNode.setName(augmentedImage.getName());
                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    if (anchorNode.getChildren().size() == 0) {

                        // Set renderables
                        TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
                        node.setRenderable(arrowRenderable);
                        node.setName(ARROW_RENDERABLE_NAME);
                        Quaternion arrowRot = Quaternion.axisAngle(new Vector3(1.0f, 0.0f, 0.0f), -90.0f).normalized(); // Align arrow to up vector
                        node.setLocalRotation(arrowRot);
                        node.setLocalPosition(new Vector3(0f, 0f, -augmentedImage.getExtentZ()/2));
                        node.setParent(anchorNode);
                        Robot selectedRobot = selectRobotFromAR(node, robotName, robotUid);

                        node.setOnTouchListener((hitTestResult, motionEvent) -> {
                            selectRobotFromAR(node, robotName, robotUid);
                            return false;
                        });

                        TransformableNode uiNode = new CameraFacingNode(arFragment.getTransformationSystem(), arFragment.getArSceneView().getScene().getCamera());
                        setRobotARInfoRenderable(uiNode, selectedRobot, node.getLocalPosition(), node.getLocalRotation());
                        uiNode.setParent(anchorNode);
                    }

                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                }
                else if (currentMethod.equals(AugmentedImage.TrackingMethod.NOT_TRACKING)) {
                    // Remove anchor node if tracking is lost
                    NodeParent nodeParent = arFragment.getArSceneView().getScene().findInHierarchy(sceneNode -> sceneNode.getName().equals(augmentedImage.getName()));
                    if (nodeParent != null) { arFragment.getArSceneView().getScene().removeChild((Node)nodeParent); }
                }
                Log.i("ARCore", msg);
            }
        }
    }

    private void getAprilTags(FrameTime frameTime) {
        synchronized (frameImageInUseLock) {
            try {
                Frame frame = arFragment.getArSceneView().getArFrame();
                if (frame != null) {

                    if (worldNode == null && frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
                        worldNode = new AnchorNode();
                        /*worldNode.setAnchor(arFragment.getArSceneView().getSession().createAnchor(defPose));*/
                        worldNode.setAnchor(arFragment.getArSceneView().getSession().createAnchor(Pose.IDENTITY));
                        worldNode.setName("World");
                        worldNode.setRenderable(xyzRenderable);
                        worldNode.setParent(arFragment.getArSceneView().getScene());

                        Log.i("ARScene", "World anchor added.");
                    }

                    Image frameImage = frame.acquireCameraImage();
                    byte[] img = YUV_420_888_to_nv1(frameImage);
                    int imgWidth = frameImage.getWidth();
                    int imageHeight = frameImage.getHeight();

                    frameImage.close();
                    CameraIntrinsics intrinsics = frame.getCamera().getImageIntrinsics();

                    // To april tag recognition here
                    if (aprilTagProcessingThread == null || !aprilTagProcessingThread.isRunning) {
                        aprilTagProcessingThread = new ProcessingThread(
                                img,
                                imgWidth,
                                imageHeight,
                                ConvertUtil.convertToDoubleArray(intrinsics.getFocalLength()),
                                ConvertUtil.convertToDoubleArray(intrinsics.getPrincipalPoint()),
                                frame.getCamera().getPose(),
                                this);
                        aprilTagProcessingThread.run();

                    }
                }
            } catch (NotYetAvailableException e) {
                e.printStackTrace();
            }
        }
    }

    private void addOrUpdateNode(TrackingState state, @NonNull Node parent, String nodeName, Quaternion localRotation, Vector3 localTranslation) {
        if (state == TrackingState.TRACKING) {
            Node n = parent.findByName(nodeName);
            TransformableNode node = n == null ? new TransformableNode(arFragment.getTransformationSystem()) : (TransformableNode)n;
            node.setRenderable(xyzRenderable);
            node.setName(nodeName);
            Quaternion rot = localRotation.normalized(); // Align arrow to up vector
            node.setLocalRotation(rot);
            node.setLocalPosition(localTranslation);
            node.setParent(parent);
            Log.i("ARScene", "Anchor added: " + nodeName);
        }
    }

    private void addOrUpdateNode(TrackingState state, @NonNull NodeParent parent, String nodeName, Pose pose) {
        if (state == TrackingState.TRACKING) {
            Node n = parent.findByName(nodeName);
            TransformableNode node = n == null ? new TransformableNode(arFragment.getTransformationSystem()) : (TransformableNode)n;
            float[] q = pose.getRotationQuaternion();
            node.setWorldRotation(new Quaternion(q[0], q[1], q[2], q[3]));
            float[] t = pose.getTranslation();
            node.setWorldPosition(new Vector3(t[0], t[1], t[2]));
            node.setRenderable(xyzRenderable);
            node.setName(nodeName);
            node.setParent(parent);
            Log.i("ARScene", "Anchor added: " + nodeName);
        }
    }

    private Robot selectRobotFromAR(TransformableNode node, String robotName, int robotUid) {
        selectVisualNode(node);
        return selectRobotFromUID(robotUid);
    }

    private void selectVisualNode(TransformableNode node) {
        for (Node child : arFragment.getArSceneView().getScene().getChildren()) {
            if (child instanceof AnchorNode) {
                TransformableNode n = (TransformableNode)child.findByName(ARROW_RENDERABLE_NAME);
                if (n != null) {
                    Boolean isSelected = n == node;
                    ModelRenderable renderableCopy = (ModelRenderable) n.getRenderable().makeCopy();
                    Color selectedColor = new Color(android.graphics.Color.rgb(isSelected ? 0 : 255, isSelected ? 255 : 0, 0));
                    renderableCopy.getMaterial().setFloat3("baseColorTint", selectedColor);
                    n.setRenderable(renderableCopy);
                    n.select();

                    // Disable selected visualizer
                    arFragment.getTransformationSystem().getSelectionVisualizer().removeSelectionVisual(n);
                }
            }
        }
    }

    private Robot selectRobotFromUID(int uid) {
        // For now, there are no link between uid and images
        Robot robot = robotListViewModel.getRobotFromList(uid); // For now, uid starts at 1

        CurrentArRobotViewModel currentArRobotViewModel = new ViewModelProvider(requireActivity()).get(CurrentArRobotViewModel.class);
        currentArRobotViewModel.getSelectedRobot().setValue(robot);
        return robot;
    }

    private void setRobotARInfoRenderable(TransformableNode tNode, Robot robot, Vector3 pos, Quaternion rot)
    {
        ViewRenderable.builder()
                .setView(requireContext(), R.layout.ar_robot_base_info)
                .build()
                .thenAccept(viewRenderable -> {
                    viewRenderable.setShadowCaster(false);
                    viewRenderable.setShadowReceiver(false);
                    viewRenderable.getView().findViewById(R.id.ar_view_layout).setOnLongClickListener(view -> {
                        // Open delete popup
                        String alertMsg = "Delete Current Selected AR Marker?";
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Unrelevant AR Marker")
                                .setMessage(alertMsg)
                                .setPositiveButton("Yes", (dialog, whichButton) -> {
                                    timerTextViews.computeIfPresent(robot, (k,v) -> null); // remove from list
                                    AnchorNode arRobotNode = (AnchorNode) tNode.getParent();
                                    arFragment.getArSceneView().getScene().removeChild(arRobotNode);
                                    arRobotNode.getAnchor().detach();
                                    arRobotNode.setParent(null);
                                    arRobotNode = null;
                                }).setNegativeButton("No", (dialog, whichButton) -> {
                                    // Do nothing.
                                }).show();
                        return true;
                    });
                    ((TextView)viewRenderable.getView().findViewById(R.id.robot_ar_name)).setText(robot.getName());
                    TextView robotTimer = viewRenderable.getView().findViewById(R.id.last_update_timer);
                    timerTextViews.computeIfPresent(robot, (k,v) -> robotTimer);
                    timerTextViews.computeIfAbsent(robot, v -> robotTimer);
                    // Set in AR
                    tNode.setName(AR_ROBOT_NAME);
                    Quaternion oriChange = Quaternion.axisAngle(new Vector3(0.0f, 0.0f, 1.0f), 180.0f).normalized(); // Align arrow to up vector
                    tNode.setLocalRotation(Quaternion.multiply(oriChange, rot));
                    Vector3 offset = new Vector3(0f, 0f, -0.2f);
                    tNode.setLocalPosition(Vector3.add(pos, offset));
                    tNode.setRenderable(viewRenderable);
                })
                .exceptionally(
                        throwable -> {
                            Log.e(ARViewFragment.class.getName(), "Unable to load Renderable.", throwable);
                            return null;
                        });
    }

    private void setupArDatabase(AugmentedImageDatabase augmentedImageDatabase) {
        SettingsViewModel settingsViewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);
        String activeDatabasePath = settingsViewModel.getActiveFolderAbsolutePath();
        if (activeDatabasePath != null && !activeDatabasePath.isEmpty()) {
            File folder = new File(activeDatabasePath);
            FilenameFilter filter = (f, name) -> name.endsWith(".jpg");
            String[] filesInFolder = folder.list(filter);
            for (String filename : filesInFolder) {
                Bitmap bitmap;
                File f = new File(folder, filename);
                try (InputStream inputStream = new FileInputStream(f)) {
                    bitmap = BitmapFactory.decodeStream(inputStream);
                    filename = filename.replace(".jpg", "");
                    augmentedImageDatabase.addImage(filename, bitmap, QRCodeWidth);
                } catch (IOException e) {
                    Log.e(this.getClass().toString(), "I/O exception loading augmented image bitmap.", e);
                }
            }
        }
    }

    static class ProcessingThread extends Thread {
        byte[] bytes;
        int width;
        int height;
        double[] centralPoint;
        double[] focalLength;
        Pose cameraDisplayOrientedPose;
        boolean isRunning;
        ARViewFragment parent;

        ProcessingThread(byte[] bytes, int width, int height, double[] focalLength, double[] centralPoint, Pose cameraPose, ARViewFragment parent) {
            this.bytes = bytes;
            this.width = width;
            this.height = height;
            this.centralPoint = centralPoint;
            this.focalLength = focalLength;
            this.cameraDisplayOrientedPose = cameraPose;
            this.parent = parent;
            isRunning = false;
        }

        public void run() {
            isRunning = true;
            ArrayList<ApriltagDetection> detections = ApriltagNative.apriltag_detect_yuv(bytes, width, height, APRIL_TAG_SCALE_M, centralPoint, focalLength);
            Frame frame = parent.arFragment.getArSceneView().getArFrame();
            if (detections.size() > 0) {
                Log.i(TAG, "Detections: " + detections.size());
                for (ApriltagDetection detection : detections) {
                    Log.i(TAG, "Distance" + MathUtil.getNorm(detection.pose_t));
                    Log.i(TAG, "Pose" + Arrays.toString(detection.pose_r) + "  " + Arrays.toString(detection.pose_t));



                    float[] homogeneous_m = MathUtil.getHomogeneous(ConvertUtil.convertToFloatArray(detection.pose_r), ConvertUtil.convertToFloatArray(detection.pose_t));
                    //Matrix.rotateM(homogeneous_m, 0, -90, 0.0f, 0.0f, 1.0f);
                    float[] inverse = new float[16];
                    Matrix.invertM(inverse, 0, homogeneous_m, 0);
                    //Log.i("Homogeneous 1", MathUtil.homogeneousToString(homogeneous_m));
                    //Log.i("Homogeneous 2", MathUtil.homogeneousToString(inverse));

                    /*parent.addOrUpdateNode(
                            frame.getCamera().getTrackingState(),
                            parent.arFragment.getArSceneView().getScene().getCamera(),
                            "Test",
                            new Vector3(1.0f, 0.0f, 0.0f),
                            new Vector3(0.0f, 0.0f, -(float)detection.pose_t[2]));*/

                    float[] tr = {-homogeneous_m[12], homogeneous_m[13], homogeneous_m[14]};
                    Quaternion q = MathUtil.convertToRightHanded(MathUtil.getQuaternion(homogeneous_m));
                    float[] ro = {q.x, q.y, q.z, q.w};
                    Pose p = new Pose(tr, ro);

                    p = frame.getCamera().getPose().compose(Pose.makeTranslation(tr)); // ONLY translation
                    Pose p1 = frame.getCamera().getPose().compose(Pose.makeRotation(ro).inverse()).compose(zRot);
                    parent.debugInfo.setText(Arrays.toString(tr));
                    Log.i("POSE", p.toString());


                    if (frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
                        Node n = parent.arFragment.getArSceneView().getScene().findByName(Integer.toString(detection.id));
                        TransformableNode node;
                        if (n == null) {
                            node = new TransformableNode(parent.arFragment.getTransformationSystem());
                            node.setRenderable(parent.xyzRenderable);
                            node.setName(Integer.toString(detection.id));
                        } else {
                            node = (TransformableNode)n;
                        }
                        node.setWorldPosition(new Vector3(p.getTranslation()[0], p.getTranslation()[1], p.getTranslation()[2]));
                        node.setWorldRotation(new Quaternion(p1.qx(), p1.qy(), p1.qz(), p1.qw()));
                        node.setParent(parent.arFragment.getArSceneView().getScene());
                    }

                    parent.vValue += 0.01;

                }
            }
            isRunning = false;
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
