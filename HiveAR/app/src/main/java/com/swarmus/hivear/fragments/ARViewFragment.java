package com.swarmus.hivear.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
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
import com.swarmus.hivear.ar.CameraFacingNode;
import com.swarmus.hivear.models.CurrentArRobotViewModel;
import com.swarmus.hivear.models.Robot;
import com.swarmus.hivear.models.RobotListViewModel;
import com.swarmus.hivear.models.SettingsViewModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;

public class ARViewFragment extends Fragment {

    private ArFragment arFragment;
    private HashMap<String, AugmentedImage.TrackingMethod> trackableInfos;
    private ModelRenderable arrowRenderable;
    private RobotListViewModel robotListViewModel;

    private final static String ARROW_RENDERABLE_NAME = "Arrow Renderable";
    private final static String AR_ROBOT_NAME = "Robot Name";
    private final static float QRCodeWidth = 0.1f; // m

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

         robotListViewModel = new ViewModelProvider(requireActivity()).get(RobotListViewModel.class);

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.ar_view_fragment, container, false);

        CurrentArRobotViewModel currentArRobotViewModel = new ViewModelProvider(requireActivity()).get(CurrentArRobotViewModel.class);
        currentArRobotViewModel.getSelectedRobot().observe(requireActivity(), robot -> setRobotUI(view, robot));

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
            config.setFocusMode(Config.FocusMode.AUTO);
            config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL);
            config.setLightEstimationMode(Config.LightEstimationMode.DISABLED);
            session.configure(config);
            arFragment.getArSceneView().setupSession(session);
            arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> updateFrame(frameTime));

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
                        // Clean previous renderables before adding new ones
                        for (Node child : anchorNode.getChildren()) {
                            if (child instanceof TransformableNode) { anchorNode.removeChild(child); }
                        }

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
                        setRobotARInfoRenderable(uiNode, selectedRobot.getName(), node.getLocalPosition(), node.getLocalRotation());
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

    private void setRobotARInfoRenderable(TransformableNode tNode, String name, Vector3 pos, Quaternion rot)
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
                    ((TextView)viewRenderable.getView().findViewById(R.id.robot_ar_name)).setText(name);
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
        File folder = new File(settingsViewModel.getActiveFolderAbsolutePath());
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
