package com.swarmus.hivear.fragments;

import android.content.DialogInterface;
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
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseTransformableNode;
import com.google.ar.sceneform.ux.SelectionVisualizer;
import com.google.ar.sceneform.ux.TransformableNode;
import com.swarmus.hivear.R;
import com.swarmus.hivear.models.CurrentArRobotViewModel;
import com.swarmus.hivear.models.Robot;
import com.swarmus.hivear.models.RobotListViewModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;

public class ARViewFragment extends Fragment {

    private ArFragment arFragment;
    private HashMap<String, AugmentedImage.TrackingMethod> trackableInfos;
    private ModelRenderable arrowRenderable;

    private final static String ARROW_RENDERABLE_NAME = "Arrow Renderable";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
            try {
                InputStream inputStream = requireContext().getAssets().open(getString(R.string.tags_db));
                Config config = new Config(session);
                config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
                config.setAugmentedImageDatabase(AugmentedImageDatabase.deserialize(session, inputStream));
                config.setFocusMode(Config.FocusMode.AUTO);
                config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL);
                config.setLightEstimationMode(Config.LightEstimationMode.DISABLED);
                session.configure(config);
                arFragment.getArSceneView().setupSession(session);
            } catch (IOException e) {
                e.printStackTrace();
            }
            arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
                updateFrame(frameTime);
            });

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
        robotInfoLayout.setOnLongClickListener(view -> {
            // Open delete popup
            String alertMsg = "Delete Current Selected AR Marker?";
            new AlertDialog.Builder(requireContext())
                    .setTitle("Unrelevant AR Marker")
                    .setMessage(alertMsg)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Remove ar marker and disable current selected robot
                            TransformableNode selectedNode = (TransformableNode)arFragment.getTransformationSystem().getSelectedNode();
                            selectedNode.getParent().removeChild(selectedNode);
                            CurrentArRobotViewModel currentArRobotViewModel = new ViewModelProvider(requireActivity()).get(CurrentArRobotViewModel.class);
                            currentArRobotViewModel.getSelectedRobot().setValue(null);
                        }
                    }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Do nothing.
                }
            }).show();
            return true;
        });
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

                        // Set renderable
                        TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
                        node.setRenderable(arrowRenderable);
                        node.setName(ARROW_RENDERABLE_NAME);
                        Quaternion arrowRot = Quaternion.axisAngle(new Vector3(1.0f, 0.0f, 0.0f), -90.0f).normalized(); // Align arrow to up vector
                        node.setLocalRotation(arrowRot);
                        node.setLocalPosition(new Vector3(0f, 0f, -augmentedImage.getExtentZ()/2));
                        node.setParent(anchorNode);
                        selectRobotFromAR(node, augmentedImage.getIndex());

                        node.setOnTouchListener((hitTestResult, motionEvent) -> {
                            selectRobotFromAR(node, augmentedImage.getIndex());
                            return false;
                        });
                    }

                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                }
                else if (currentMethod.equals(AugmentedImage.TrackingMethod.NOT_TRACKING)) {
                    // Remove anchor node if tracking is lost
                    NodeParent nodeParent = arFragment.getArSceneView().getScene().findInHierarchy(sceneNode -> sceneNode.getName().equals(augmentedImage.getName()));
                    arFragment.getArSceneView().getScene().removeChild((Node)nodeParent);
                }
                Log.i("ARCore", msg);
            }
        }
    }

    private void selectRobotFromAR(TransformableNode node, int uid) {
        selectVisualNode(node);
        selectRobotFromUID(uid);
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

    private void selectRobotFromUID(int uid) {
        RobotListViewModel robotListViewModel = new ViewModelProvider(requireActivity()).get(RobotListViewModel.class);
        // For now, there are no link between uid and images
        Robot robot = robotListViewModel.getRobotFromList(uid + 1); // For now, uid starts at 1

        CurrentArRobotViewModel currentArRobotViewModel = new ViewModelProvider(requireActivity()).get(CurrentArRobotViewModel.class);
        currentArRobotViewModel.getSelectedRobot().setValue(robot);
    }
}
