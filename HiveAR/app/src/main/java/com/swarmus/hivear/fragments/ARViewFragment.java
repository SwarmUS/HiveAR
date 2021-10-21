package com.swarmus.hivear.fragments;

import android.media.Image;
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

import com.google.ar.core.Anchor;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.CameraConfigFilter;
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
import com.google.ar.sceneform.Node;
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
import com.swarmus.hivear.ar.AlwaysStraightNode;
import com.swarmus.hivear.ar.CameraFacingNode;
import com.swarmus.hivear.models.Agent;
import com.swarmus.hivear.models.ProtoMsgStorer;
import com.swarmus.hivear.utils.ConvertUtil;
import com.swarmus.hivear.utils.MathUtil;
import com.swarmus.hivear.viewmodels.AgentListViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class ARViewFragment extends Fragment {

    private static final String TAG = ARViewFragment.class.getName();
    private static final double APRIL_TAG_SCALE_M = 0.12; // 12 cm width

    private ArFragment arFragment;
    private ModelRenderable arrowRenderable;
    private ModelRenderable xyzRenderable;
    private final static String AR_INDICATOR_NAME = "ARIndicator";
    private final static String AR_INDICATOR_UI = "AR Agent Info";
    private final static String AR_AGENT_ROOT = "Agent Root";

    private final static double UPDATE_DETECTION_DISTANCE_THRESHOLD = 1.0;

    private AgentListViewModel agentListViewModel;

    private Agent currentSelectedAgent;

    private final Object frameImageInUseLock = new Object();

    private Handler timerHandler;
    private Runnable timerRunnable;
    private HashMap<Agent,TextView> timerTextViews = new HashMap<>();
    private static final int AR_SHOW_LAST_COMMANDS_COUNT = 5;

    private static Toast currentToast;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        agentListViewModel = new ViewModelProvider(requireActivity()).get(AgentListViewModel.class);

        timerHandler = new Handler();
        timerRunnable =  new Runnable() {
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
        timerHandler.postDelayed(timerRunnable, 1000); // evaluate each second

        // Initialize the apriltag detector for family 36h11
        ApriltagNative.apriltag_init("tag36h11", 2, 3, 0, 16);

        initializeRenderables();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.ar_view_fragment, container, false);
        configureARSession();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setAgentUI(null);
    }

    @Override
    public void onDestroyView() {
        timerHandler.removeCallbacks(timerRunnable);
        setSelectedAgent(null);
        super.onDestroyView();
    }

    private void setSelectedAgent(Agent agent) {
        currentSelectedAgent = agent;
        setAgentUI(agent);
    }

    private void initializeRenderables() {
        // Agent identificator
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

        // Used mostly for debug, can help to visualize position and orientation in space
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

    private void configureARSession() {
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
            CameraConfigFilter filter = new CameraConfigFilter(session);
            filter.setDepthSensorUsage(EnumSet.of(CameraConfig.DepthSensorUsage.DO_NOT_USE));
            filter.setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30));
            filter.setFacingDirection(CameraConfig.FacingDirection.BACK);
            // Set configuration that matches the filter
            session.setCameraConfig(session.getSupportedCameraConfigs(filter).get(0));
            Config config = new Config(session);
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            config.setFocusMode(Config.FocusMode.AUTO);
            config.setDepthMode(Config.DepthMode.DISABLED);
            config.setInstantPlacementMode(Config.InstantPlacementMode.DISABLED);
            config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL);
            config.setLightEstimationMode(Config.LightEstimationMode.DISABLED);
            session.configure(config);
            arFragment.getArSceneView().setupSession(session);
            arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> getAprilTags());

            // Override to show nothing instead of default grey circle underneath selected anchor node
            arFragment.getTransformationSystem().setSelectionVisualizer(new SelectionVisualizer() {
                @Override
                public void applySelectionVisual(BaseTransformableNode node) {}

                @Override
                public void removeSelectionVisual(BaseTransformableNode node) { }
            });
        }
    }

    private void setAgentUI(Agent agent) {
        Boolean isAgentSelected = agent != null;
        LinearLayout agentInfoLayout = getView().findViewById(R.id.agent_ar_selected);
        agentInfoLayout.setVisibility(isAgentSelected ? LinearLayout.VISIBLE : LinearLayout.GONE);
        TextView agentName = getView().findViewById(R.id.agent_ar_selected_name);
        agentName.setText(isAgentSelected ? agent.getName() : "");
        TextView agentUid = getView().findViewById(R.id.agent_ar_selected_uid);
        agentUid.setText(isAgentSelected ? Integer.toString(agent.getUid()) : "");
    }

    private void getAprilTags() {
        synchronized (frameImageInUseLock) { // Process only one frame at a time (help a lot framedrop)
            try {
                Frame frame = arFragment.getArSceneView().getArFrame();
                if (frame != null) {
                    Image frameImage = frame.acquireCameraImage();
                    byte[] img = ConvertUtil.YUV_420_888_to_nv1(frameImage);
                    int imgWidth = frameImage.getWidth();
                    int imageHeight = frameImage.getHeight();
                    frameImage.close(); // Very important to release resource

                    CameraIntrinsics intrinsics = frame.getCamera().getImageIntrinsics();

                    aprilTagDetectAndProcess(
                            frame,
                            img,
                            imgWidth,
                            imageHeight,
                            ConvertUtil.convertToDoubleArray(intrinsics.getPrincipalPoint()),
                            ConvertUtil.convertToDoubleArray(intrinsics.getFocalLength()));
                }
            } catch (NotYetAvailableException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void aprilTagDetectAndProcess(Frame frame, byte[] img, int width, int height, double[] centralPoint, double[]focalLength) {
        if (frame != null) {
            // UNCOMMENT FOR DEBUG
            // Place an anchor at the center of the world
            //addWorldAnchor(frame);

            // Do april tag recognition here
            ArrayList<ApriltagDetection> detections =
                    ApriltagNative.apriltag_detect_yuv(img, width, height, APRIL_TAG_SCALE_M, centralPoint, focalLength);

            if (detections.size() > 0) {
                Log.i(TAG, "April tag detections count: " + detections.size());
                for (ApriltagDetection detection : detections) {

                    // Verify that agent is listed in known agents.
                    if(agentListViewModel.getAgentFromApriltag(detection.id ) == null) {
                        // If initialized and not showing, show new one
                        if (currentToast != null && !currentToast.getView().isShown()) {
                            currentToast.setText("Agent with id " + detection.id + " not registered in current swarm");
                            currentToast.show();
                        }
                        // Initialize if was never created
                        else {
                            currentToast = Toast.makeText(requireContext(),
                                    "Agent with id " + detection.id + " not registered in current swarm",
                                    Toast.LENGTH_LONG);
                            currentToast.show();
                        }
                        continue;
                    }

                    float[] homogeneous_m = MathUtil.getHomogeneous(ConvertUtil.convertToFloatArray(detection.pose_r), ConvertUtil.convertToFloatArray(detection.pose_t));

                    // Change detection left handed CS to right handed CS
                    float[] tr = {-homogeneous_m[12], homogeneous_m[13], homogeneous_m[14]};
                    Quaternion q = MathUtil.convertToRightHanded(MathUtil.getQuaternion(homogeneous_m));
                    float[] ro = {q.x, q.y, q.z, q.w};

                    // ONLY TESTED FOR SAMSUNG S10
                    // needs a 180 degrees z correction to get the correct desired orientation
                    Quaternion correction = Quaternion.eulerAngles(new Vector3(0, 0, 180));  // 180 degrees rotation around z

                    Pose tagPose = frame.getCamera().getPose() // Get camera in world
                            .compose(Pose.makeTranslation(tr)) // Apply tag detection position offset
                            .compose(Pose.makeRotation(ro).inverse()) // Remove rotation of tag detection orientation
                            .compose(Pose.makeRotation(correction.x, correction.y, correction.z, correction.w)); // apply rotation correction

                    Log.d("From camera Translation, id #" + detection.id, Arrays.toString(tr));
                    Log.d("From camera Orientation, id #" + detection.id, Arrays.toString(ro));

                    // Add AR visualization
                    // Uncomment to see 3 axis for debugging
                    //addOrUpdateIdARVisualDebug(frame, detection.id, tagPose);
                    addOrUpdateAgentARVisual(frame, detection.id, tagPose);
                }
            }
        }
    }

    private void addWorldAnchor(@NonNull Frame frame) {
        if (arFragment.getArSceneView().getScene().findByName("World") == null &&
                frame.getCamera().getTrackingState() == TrackingState.TRACKING) {

            AnchorNode worldNode = new AnchorNode();
            worldNode.setAnchor(arFragment.getArSceneView().getSession().createAnchor(Pose.IDENTITY));
            worldNode.setName("World");
            worldNode.setRenderable(xyzRenderable);
            worldNode.setParent(arFragment.getArSceneView().getScene());

            Log.i("ARScene", "World anchor added.");
        }
    }

    private void addOrUpdateIdARVisualDebug(@NonNull Frame frame, int id, Pose tagPose) {
        if (frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
            final String debugIdName = "Debug_" + id;
            Node n = arFragment.getArSceneView().getScene().findByName(debugIdName);
            AnchorNode node;

            // Create node or change if already existent
            if (n == null) {
                Anchor anchor = arFragment.getArSceneView().getSession().createAnchor(tagPose);
                node = new AnchorNode(anchor);
                node.setName(debugIdName);
                node.setRenderable(xyzRenderable);
                node.setParent(arFragment.getArSceneView().getScene());
            } else {
                node = (AnchorNode) n;
            }

            // Don't update if too close from current one
            if (isSamePose(tagPose, node.getAnchor().getPose())) { return; }

            // Need to detch anchor and than add a new one to udpate position
            node.getAnchor().detach();
            node.setAnchor(arFragment.getArSceneView().getSession().createAnchor(tagPose));
            node.setParent(arFragment.getArSceneView().getScene()); // Force sceneform node update
        }
    }

    private void addOrUpdateAgentARVisual(@NonNull Frame frame, int id, Pose tagPose) {
        Agent agent = agentListViewModel.getAgentFromApriltag(id);
        if (agent == null) {
            return;
        }

        if (frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
            final String agentNodeName = "Agent_" + agent.getUid();
            Node n = arFragment.getArSceneView().getScene().findByName(agentNodeName);
            AnchorNode node;

            // Create node or change if already existent
            if (n == null) {
                Anchor anchor = arFragment.getArSceneView().getSession().createAnchor(tagPose);
                node = new AnchorNode(anchor);
                node.setName(agentNodeName);
                node.setParent(arFragment.getArSceneView().getScene()); // Force sceneform node update
                initARIndicator(node, agent);
            } else {
                node = (AnchorNode) n;
            }

            // Don't update if too close from current one
            if (isSamePose(tagPose, node.getAnchor().getPose())) { return; }

            // Need to detach anchor and than add a new one to udpate position
            node.getAnchor().detach();
            node.setAnchor(arFragment.getArSceneView().getSession().createAnchor(tagPose));
            node.setParent(arFragment.getArSceneView().getScene()); // Force sceneform node update
        }
    }

    private void initARIndicator(AnchorNode parent, Agent agent) {
        TransformableNode arAgentRootNode = new TransformableNode(arFragment.getTransformationSystem());
        arAgentRootNode.setName(AR_AGENT_ROOT);
        arAgentRootNode.getRotationController().setEnabled(false);
        arAgentRootNode.getTranslationController().setEnabled(false);
        arAgentRootNode.setParent(parent);
        AlwaysStraightNode indicatorNode = new AlwaysStraightNode();
        indicatorNode.setRenderable(arrowRenderable);
        indicatorNode.setName(AR_INDICATOR_NAME);
        indicatorNode.setLocalPosition(new Vector3(0f, (float)(APRIL_TAG_SCALE_M / 2), 0f));
        indicatorNode.setParent(arAgentRootNode);
        indicatorNode.setOnTouchListener((hitTestResult, motionEvent) -> {
            selectAgentFromAR(arAgentRootNode, agent);
            return false;
        });

        CameraFacingNode uiNode = new CameraFacingNode(arFragment.getArSceneView().getScene().getCamera());
        setAgentARInfoRenderable(uiNode, arAgentRootNode, agent);
        uiNode.setName(AR_INDICATOR_UI);
        uiNode.setParent(arAgentRootNode);

        // At creation, make node selected if none are currently selected
        if (currentSelectedAgent == null) {
            selectAgentFromAR(arAgentRootNode, agent);
        }
    }

    private void selectAgentFromAR(TransformableNode node, Agent agent) {
        selectVisualNode(node);
        currentSelectedAgent = agent;
    }

    private void selectVisualNode(TransformableNode node) {
        for (Node child : arFragment.getArSceneView().getScene().getChildren()) {
            if (child instanceof AnchorNode) {
                TransformableNode robotRootNode = (TransformableNode)child.findByName(AR_AGENT_ROOT);
                if (robotRootNode != null) {
                    AlwaysStraightNode indicatorNode = (AlwaysStraightNode) robotRootNode.findByName(AR_INDICATOR_NAME);
                    CameraFacingNode uiNode = (CameraFacingNode)robotRootNode.findByName(AR_INDICATOR_UI);
                    if (indicatorNode != null && uiNode != null) {
                        Boolean isSelected = node == robotRootNode;
                        ModelRenderable renderableCopy = (ModelRenderable) indicatorNode.getRenderable().makeCopy();
                        Color selectedColor = new Color(android.graphics.Color.rgb(isSelected ? 0 : 255, isSelected ? 255 : 0, 0));
                        renderableCopy.getMaterial().setFloat3("baseColorTint", selectedColor);
                        indicatorNode.setRenderable(renderableCopy);
                        if (isSelected) { arFragment.getTransformationSystem().selectNode(robotRootNode); }

                        // Disable selected visualizer
                        arFragment.getTransformationSystem().getSelectionVisualizer().removeSelectionVisual(robotRootNode);
                    }
                }
            }
        }
    }

    private void setAgentARInfoRenderable(CameraFacingNode tNode, TransformableNode parent, Agent agent)
    {
        ViewRenderable.builder()
                .setView(getContext(), R.layout.ar_agent_base_info)
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
                                    timerTextViews.computeIfPresent(agent, (k, v) -> null); // remove from list
                                    AnchorNode arAgentNode = (AnchorNode) tNode.getParent();
                                    arFragment.getArSceneView().getScene().removeChild(arAgentNode);
                                    arAgentNode.getAnchor().detach();
                                    arAgentNode.setParent(null);
                                    // If deleting the current selected agent, notify the new value
                                    if (currentSelectedAgent == agent) {
                                        setSelectedAgent(null);
                                    }
                                }).setNegativeButton("No", (dialog, whichButton) -> {
                                    // Do nothing.
                                }).show();
                        return true;
                    });
                    viewRenderable.getView().findViewById(R.id.ar_view_layout).setOnClickListener(view -> {
                        selectAgentFromAR(parent, agent);
                    });
                    ((TextView)viewRenderable.getView().findViewById(R.id.agent_ar_name)).setText(agent.getName());
                    TextView robotTimer = viewRenderable.getView().findViewById(R.id.last_update_timer);
                    timerTextViews.computeIfPresent(agent, (k,v) -> robotTimer);
                    timerTextViews.computeIfAbsent(agent, v -> robotTimer);

                    TextView lastCommands = viewRenderable.getView().findViewById(R.id.lastCommands);
                    ProtoMsgStorer lastCommandsStorer = agent.getSentCommandsStorer();
                    lastCommands.setText(lastCommandsStorer.getSimplifiedLoggingString(AR_SHOW_LAST_COMMANDS_COUNT));
                    lastCommandsStorer.addObserver((observable, object) -> {
                        lastCommands.setText(lastCommandsStorer.getSimplifiedLoggingString(AR_SHOW_LAST_COMMANDS_COUNT));
                    });

                    // Set in AR
                    Vector3 offset = new Vector3(0f, (float)(4 * APRIL_TAG_SCALE_M), 0f);
                    tNode.setWorldPosition(Vector3.add(parent.getWorldPosition(), offset));
                    tNode.setRenderable(viewRenderable);
                })
                .exceptionally(
                        throwable -> {
                            Log.e(ARViewFragment.class.getName(), "Unable to load Renderable.", throwable);
                            return null;
                        });
    }

    private boolean isSamePose(Pose p1, Pose p2) {
        // verify if node should be updated based on its pose

        // For now, only update from position, not orientation difference
        return MathUtil.getDistance(p1.getTranslation(), p2.getTranslation()) <= UPDATE_DETECTION_DISTANCE_THRESHOLD;
    }
}
