package com.swarmus.hivear.ar;

import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.ux.TransformationSystem;

public class AlwaysStraightNode extends TransformableNode {

    public AlwaysStraightNode(TransformationSystem transformationSystem) {
        super(transformationSystem);
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        super.onUpdate(frameTime);
        setWorldRotation(Quaternion.identity());
    }
}
