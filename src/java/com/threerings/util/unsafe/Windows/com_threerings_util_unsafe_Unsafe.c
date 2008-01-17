/*
 * $Id: com_threerings_util_unsafe_Unsafe.c 3331 2005-02-03 01:25:21Z mdb $
 */

#include <stdio.h>
#include <jni.h>

#include "com_threerings_util_unsafe_Unsafe.h"

JNIEXPORT void JNICALL
Java_com_threerings_util_unsafe_Unsafe_nativeSleep (
    JNIEnv* env, jclass clazz, jint millis)
{
    /* not supported */
}

JNIEXPORT jboolean JNICALL Java_com_threerings_util_unsafe_Unsafe_nativeSetuid
    (JNIEnv *, jclass, jint)
{
    /* not supported */
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_threerings_util_unsafe_Unsafe_nativeSetgid
    (JNIEnv *, jclass, jint)
{
    /* not supported */
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_threerings_util_unsafe_Unsafe_nativeSeteuid (
    JNIEnv* env, jclass clzz, jint uid)
{
    /* not supported */
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_threerings_util_unsafe_Unsafe_nativeSetegid (
    JNIEnv* env, jclass clazz, jint gid)
{
    /* not supported */
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_threerings_util_unsafe_Unsafe_init (JNIEnv* env, jclass clazz)
{
    return JNI_TRUE;
}
