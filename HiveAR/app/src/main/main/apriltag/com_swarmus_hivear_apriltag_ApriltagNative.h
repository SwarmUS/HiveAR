/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_swarmus_hivear_apriltag_ApriltagNative */

#ifndef _Included_com_swarmus_hivear_apriltag_ApriltagNative
#define _Included_com_swarmus_hivear_apriltag_ApriltagNative
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_swarmus_hivear_apriltag_ApriltagNative
 * Method:    native_init
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_swarmus_hivear_apriltag_ApriltagNative_native_1init
  (JNIEnv *, jclass);

/*
 * Class:     com_swarmus_hivear_apriltag_ApriltagNative
 * Method:    apriltag_init
 * Signature: (Ljava/lang/String;IDDI)V
 */
JNIEXPORT void JNICALL Java_com_swarmus_hivear_apriltag_ApriltagNative_apriltag_1init
  (JNIEnv *, jclass, jstring, jint, jdouble, jdouble, jint);

/*
 * Class:     com_swarmus_hivear_apriltag_ApriltagNative
 * Method:    apriltag_detect_yuv
 * Signature: ([BII)Ljava/util/ArrayList;
 */
JNIEXPORT jobject JNICALL Java_com_swarmus_hivear_apriltag_ApriltagNative_apriltag_1detect_1yuv
  (JNIEnv *, jclass, jbyteArray, jint, jint);

#ifdef __cplusplus
}
#endif
#endif
