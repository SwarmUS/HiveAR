package com.swarmus.hivear.ar;

import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;

public class AlwaysStraightNode extends Node {

    @Override
    public void onUpdate(FrameTime frameTime) {
        super.onUpdate(frameTime);
        setWorldRotation(Quaternion.identity());
    }
}
