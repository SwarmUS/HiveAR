package com.swarmus.hivear.ar;

import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.ux.TransformationSystem;

public class CameraFacingNode extends TransformableNode {
    private Camera camera;

    public CameraFacingNode(TransformationSystem transformationSystem, Camera camera) {
        super(transformationSystem);
        this.camera = camera;
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        super.onUpdate(frameTime);
        Vector3 cameraPosition = camera.getWorldPosition();
        Vector3 anchorPosition = getWorldPosition();
        Vector3 direction = Vector3.subtract(cameraPosition, anchorPosition);
        Quaternion lookRotation = Quaternion.lookRotation(direction, Vector3.up());
        setWorldRotation(lookRotation);
    }
}
