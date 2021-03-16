package com.swarmus.hivear.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
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
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.swarmus.hivear.R;
import com.swarmus.hivear.models.CurrentArRobotViewModel;
import com.swarmus.hivear.models.Robot;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Consumer;

public class ARViewFragment extends Fragment {

    private ArFragment arFragment;
    private HashMap<String, AugmentedImage.TrackingMethod> trackableInfos;
    private ModelRenderable arrowRenderable;

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
        }

        return view;
    }

    private void setRobotUI(View v, Robot robot) {
        Boolean isRobotSelected = robot != null;
        v.findViewById(R.id.robot_ar_selected).setVisibility(isRobotSelected ? LinearLayout.VISIBLE : LinearLayout.GONE);
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
                    if (augmentedImage.getIndex() == 0) // Instead, link with robot ID?
                    {
                        AnchorNode anchorNode = new AnchorNode(augmentedImage.createAnchor(augmentedImage.getCenterPose()));
                        anchorNode.setParent(arFragment.getArSceneView().getScene());

                        TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
                        node.setRenderable(arrowRenderable);
                        Quaternion arrowRot = Quaternion.axisAngle(new Vector3(1.0f, 0.0f, 0.0f), -90.0f).normalized(); // Align arrow to up vector
                        node.setLocalRotation(arrowRot);
                        node.setLocalPosition(new Vector3(0f, 0f, -augmentedImage.getExtentZ()/2));
                        node.setParent(anchorNode);
                        node.select();

                        // Disable selected visualizer
                        arFragment.getTransformationSystem().getSelectionVisualizer().removeSelectionVisual(node);
                    }

                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                }
                Log.i("ARCore", msg);
            }
        }
    }

    private ViewRenderable getRobotARInfoRenderable()
    {
        ViewRenderable.builder()
                .setView(requireContext(), R.layout.ar_robot_base_info)
                .build()
                .thenAccept(viewRenderable -> {
                    viewRenderable.setShadowCaster(false);
                    viewRenderable.setShadowReceiver(false);
                    viewRenderable.getView().findViewById(R.id.ar_view_layout).set
                })
                .exceptionally(
                        throwable -> {
                            Log.e(ARViewFragment.class.getName(), "Unable to load Renderable.", throwable);
                            return null;
                        });
    }
}
