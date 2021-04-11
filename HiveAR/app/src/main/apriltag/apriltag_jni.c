#include <jni.h>

#include <android/bitmap.h>
#include <android/log.h>

#include "common/homography.h"include "apriltag.h"
#include "apriltag_pose.h"
#include "tag36h11.h"
#include "tag36h10.h"
#include "tag36artoolkit.h"
#include "tag25h9.h"
#include "tag25h7.h"
#include "tag16h5.h"

static struct {
    apriltag_detector_t *td;
    apriltag_family_t *tf;
    void(*tf_destroy)(apriltag_family_t*);
    apriltag_detection_info_t  info;

    jclass al_cls;
    jmethodID al_constructor, al_add;
    jclass ad_cls;
    jmethodID ad_constructor;
    jfieldID ad_id_field, ad_hamming_field, ad_c_field, ad_p_field, ad_pose_r_field, ad_pose_t_field;
} state;

JNIEXPORT void JNICALL Java_com_swarmus_hivear_apriltag_ApriltagNative_native_1init
    (JNIEnv *env, jclass cls)
{
    // Just do method lookups once and cache the results

    // Get ArrayList methods
    jclass al_cls = (*env)->FindClass(env, "java/util/ArrayList");
    if (!al_cls) {
        __android_log_write(ANDROID_LOG_ERROR, "apriltag_jni",
                            "couldn't find ArrayList class");
        return;
    }
    state.al_cls = (*env)->NewGlobalRef(env, al_cls);

    state.al_constructor = (*env)->GetMethodID(env, al_cls, "<init>", "()V");
    state.al_add = (*env)->GetMethodID(env, al_cls, "add", "(Ljava/lang/Object;)Z");
    if (!state.al_constructor || !state.al_add) {
        __android_log_write(ANDROID_LOG_ERROR, "apriltag_jni",
                            "couldn't find ArrayList methods");
        return;
    }

    // Get ApriltagDetection methods
    jclass ad_cls = (*env)->FindClass(env, "com/swarmus/hivear/apriltag/ApriltagDetection");
    if (!ad_cls) {
        __android_log_write(ANDROID_LOG_ERROR, "apriltag_jni",
                            "couldn't find ApriltagDetection class");
        return;
    }
    state.ad_cls = (*env)->NewGlobalRef(env, ad_cls);

    state.ad_constructor = (*env)->GetMethodID(env, ad_cls, "<init>", "()V");
    if (!state.ad_constructor) {
        __android_log_write(ANDROID_LOG_ERROR, "apriltag_jni",
                            "couldn't find ApriltagDetection constructor");
        return;
    }

    state.ad_id_field = (*env)->GetFieldID(env, ad_cls, "id", "I");
    state.ad_hamming_field = (*env)->GetFieldID(env, ad_cls, "hamming", "I");
    state.ad_c_field = (*env)->GetFieldID(env, ad_cls, "c", "[D");
    state.ad_p_field = (*env)->GetFieldID(env, ad_cls, "p", "[D");
    state.ad_pose_r_field = (*env)->GetFieldID(env, ad_cls, "pose_r", "[D");
    state.ad_pose_t_field = (*env)->GetFieldID(env, ad_cls, "pose_t", "[D");
    if (!state.ad_id_field ||
            !state.ad_hamming_field ||
            !state.ad_c_field ||
            !state.ad_p_field ||
            !state.ad_pose_r_field ||
            !state.ad_pose_t_field) {
        __android_log_write(ANDROID_LOG_ERROR, "apriltag_jni",
                            "couldn't find ApriltagDetection fields");
        return;
    }
}

/*
 * Class:     com_swarmus_hivear_apriltag_ApriltagNative
 * Method:    apriltag_init
 * Signature: (Ljava/lang/String;IDDI)V
 */
JNIEXPORT void JNICALL Java_com_swarmus_hivear_apriltag_ApriltagNative_apriltag_1init
        (JNIEnv *env, jclass cls, jstring _tfname, jint errorbits, jdouble decimate,
         jdouble sigma, jint nthreads) {
    // Do cleanup in case we're already initialized
    if (state.td) {
        apriltag_detector_destroy(state.td);
        state.td = NULL;
    }
    if (state.tf) {
        state.tf_destroy(state.tf);
        state.tf = NULL;
    }

    // Initialize state
    const char *tfname = (*env)->GetStringUTFChars(env, _tfname, NULL);

    if (!strcmp(tfname, "tag36h11")) {
        state.tf = tag36h11_create();
        state.tf_destroy = tag36h11_destroy;
    } else if (!strcmp(tfname, "tag36h10")) {
        state.tf = tag36h10_create();
        state.tf_destroy = tag36h10_destroy;
    } else if (!strcmp(tfname, "tag36artoolkit")) {
        state.tf = tag36artoolkit_create();
        state.tf_destroy = tag36artoolkit_destroy;
    } else if (!strcmp(tfname, "tag25h9")) {
        state.tf = tag25h9_create();
        state.tf_destroy = tag25h9_destroy;
    } else if (!strcmp(tfname, "tag25h7")) {
        state.tf = tag25h7_create();
        state.tf_destroy = tag25h7_destroy;
    } else if (!strcmp(tfname, "tag16h5")) {
        state.tf = tag16h5_create();
        state.tf_destroy = tag16h5_destroy;
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "apriltag_jni",
                            "invalid tag family: %s", tfname);
        (*env)->ReleaseStringUTFChars(env, _tfname, tfname);
        return;
    }
    (*env)->ReleaseStringUTFChars(env, _tfname, tfname);

    state.td = apriltag_detector_create();
    apriltag_detector_add_family_bits(state.td, state.tf, errorbits);
    state.td->quad_decimate = decimate;
    state.td->quad_sigma = sigma;
    state.td->nthreads = nthreads;
}

/*
 * Class:     com_swarmus_hivear_apriltag_ApriltagNative
 * Method:    apriltag_detect_yuv
 * Signature: ([BII)Ljava/util/ArrayList;
 */
JNIEXPORT jobject JNICALL Java_com_swarmus_hivear_apriltag_ApriltagNative_apriltag_1detect_1yuv
        (JNIEnv *env, jclass cls, jbyteArray _buf, jint width, jint height, jdouble tagWidth, jdoubleArray principalPoint, jdoubleArray focalLength) {
    // If not initialized, init with default settings
    if (!state.td) {
        state.tf = tag36h11_create();
        state.td = apriltag_detector_create();
        apriltag_detector_add_family_bits(state.td, state.tf, 2);
        state.td->quad_decimate = 2.0;
        state.td->quad_sigma = 0.0;
        state.td->nthreads = 4;
        __android_log_write(ANDROID_LOG_INFO, "apriltag_jni",
                            "using default parameters");
    }

    // Use the luma channel (the first width*height elements)
    // as grayscale input image
    jbyte *buf = (*env)->GetByteArrayElements(env, _buf, NULL);

    double *cPoint = (*env)->GetDoubleArrayElements(env, principalPoint ,NULL);
    double *fLength = (*env)->GetDoubleArrayElements(env, focalLength ,NULL);
    image_u8_t im = {
            .buf = (uint8_t*)buf,
            .height = height,
            .width = width,
            .stride = width
    };
    zarray_t *detections = apriltag_detector_detect(state.td, &im);
    (*env)->ReleaseByteArrayElements(env, _buf, buf, 0);

    // al = new ArrayList();
    jobject al = (*env)->NewObject(env, state.al_cls, state.al_constructor);
    for (int i = 0; i < zarray_size(detections); i += 1) {
        apriltag_detection_t *det;
        zarray_get(detections, i, &det);

        state.info.det = det;
        state.info.tagsize = tagWidth;
        state.info.fx = ((double*)fLength)[0];
        state.info.fy = ((double*)fLength)[1];
        state.info.cx = ((double*)cPoint)[0];
        state.info.cy = ((double*)cPoint)[1];
        apriltag_pose_t pose;

        estimate_pose_for_tag_homography(&state.info, &pose);

        // ad = new ApriltagDetection();
        jobject ad = (*env)->NewObject(env, state.ad_cls, state.ad_constructor);
        (*env)->SetIntField(env, ad, state.ad_id_field, det->id);
        (*env)->SetIntField(env, ad, state.ad_hamming_field, det->hamming);
        jdoubleArray ad_c = (*env)->GetObjectField(env, ad, state.ad_c_field);
        (*env)->SetDoubleArrayRegion(env, ad_c, 0, 2, det->c);
        jdoubleArray ad_p = (*env)->GetObjectField(env, ad, state.ad_p_field);
        (*env)->SetDoubleArrayRegion(env, ad_p, 0, 8, (double*)det->p);
        jdoubleArray ad_pose_r = (*env)->GetObjectField(env, ad, state.ad_pose_r_field);
        (*env)->SetDoubleArrayRegion(env, ad_pose_r, 0, 9, pose.R->data);
        jdoubleArray ad_pose_t = (*env)->GetObjectField(env, ad, state.ad_pose_t_field);
        (*env)->SetDoubleArrayRegion(env, ad_pose_t, 0, 3, pose.t->data);

        // al.add(ad);
        (*env)->CallBooleanMethod(env, al, state.al_add, ad);

        // Need to respect the local reference limit
        (*env)->DeleteLocalRef(env, ad);
        (*env)->DeleteLocalRef(env, ad_c);
        (*env)->DeleteLocalRef(env, ad_p);
        (*env)->DeleteLocalRef(env, ad_pose_r);
        (*env)->DeleteLocalRef(env, ad_pose_t);
    }

    // Cleanup
    (*env)->ReleaseDoubleArrayElements(env,principalPoint,cPoint,0);
    (*env)->ReleaseDoubleArrayElements(env,focalLength,fLength,0);
    apriltag_detections_destroy(detections);

    return al;
}
