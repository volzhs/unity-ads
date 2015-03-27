#pragma once
#include <android/log.h>
#include "UnityAdsAndroidBridge.h"

void RegisterMethods();

static jobject adsWrapperObject;
static jclass adsWrapperClass;

#if DEBUGMODE
#define UNITYADS_DEBUG(...) printf_console("UnityAds: " __VA_ARGS__);
#else
#define UNITYADS_DEBUG(...)
#endif

#define PRINT_CLEAR_EXCEPTION \
    if (jni_env->ExceptionOccurred()) \
    {\
        __android_log_print(ANDROID_LOG_DEBUG, "UnityAds", "%s: Java exception detected, line: %i", __FUNCTION__, __LINE__); \
        jni_env->ExceptionClear(); \
    }

static UnityAdsCallback s_OnCampaignsAvailable;
static UnityAdsCallback s_OnCampaignsFetchFailed;
static UnityAdsCallback s_OnShow;
static UnityAdsCallback s_OnHide;
static UnityAdsCallback s_OnVideoStarted;
static UnityAdsCallbackStringBool s_OnVideoCompleted;

void UnityAdsOnCampaignsAvailable(JNIEnv* env, jobject* thiz) {
    UNITYADS_DEBUG("UNITYADSCLASS_CALLBACK:%s", __FUNCTION__);
    if (s_OnCampaignsAvailable != NULL)
        s_OnCampaignsAvailable();
}

void UnityAdsOnCampaignsFetchFailed(JNIEnv* env, jobject* thiz) {
    UNITYADS_DEBUG("UNITYADSCLASS_CALLBACK:%s", __FUNCTION__);
    if (s_OnCampaignsFetchFailed != NULL)
        s_OnCampaignsFetchFailed();
}

void UnityAdsOnShow(JNIEnv* env, jobject* thiz) {
    UNITYADS_DEBUG("UNITYADSCLASS_CALLBACK:%s", __FUNCTION__);
    if (s_OnShow != NULL)
        s_OnShow();
}

void UnityAdsOnHide(JNIEnv* env, jobject* thiz) {
    UNITYADS_DEBUG("UNITYADSCLASS_CALLBACK:%s", __FUNCTION__);
    if (s_OnHide != NULL)
        s_OnHide();
}

void UnityAdsOnVideoCompleted(JNIEnv* env, jobject* thiz, jstring rewardItemKey, int skipped) {
    UNITYADS_DEBUG("UNITYADSCLASS_CALLBACK:%s", __FUNCTION__);
    if (s_OnVideoCompleted != NULL)
    {
        const char* str = env->GetStringUTFChars(rewardItemKey, 0);
        s_OnVideoCompleted(str, skipped != 0);
        env->ReleaseStringUTFChars(rewardItemKey, str);
    }
}

void UnityAdsOnVideoStarted(JNIEnv* env, jobject* thiz) {
    UNITYADS_DEBUG("UNITYADSCLASS_CALLBACK:%s", __FUNCTION__);
    if (s_OnVideoStarted != NULL)
        s_OnVideoStarted();
}

void UnityAdsAndroidBridge::InitJNI(JavaVM* vm,
    UnityAdsCallback onCampaignsAvailable, 
    UnityAdsCallback onCampaignsFetchFailed,
    UnityAdsCallback onShow, 
    UnityAdsCallback onHide, 
    UnityAdsCallback onVideoStarted, 
    UnityAdsCallbackStringBool onVideoCompleted)
{
    s_OnCampaignsAvailable = onCampaignsAvailable;
    s_OnCampaignsFetchFailed = onCampaignsFetchFailed;
    s_OnShow = onShow;
    s_OnHide = onHide;
    s_OnVideoStarted = onVideoStarted;
    s_OnVideoCompleted = onVideoCompleted;

    /* HELP FOR SIGNATURES http://www.rgagnon.com/javadetails/java-0286.html */
    JNINativeMethod callbacks[] =
    {
        {"UnityAdsOnFetchCompleted", "()V", (void*)UnityAdsOnCampaignsAvailable},
        {"UnityAdsOnFetchFailed", "()V", (void*)UnityAdsOnCampaignsFetchFailed},
        {"UnityAdsOnShow", "()V", (void*)UnityAdsOnShow},
        {"UnityAdsOnHide", "()V", (void*)UnityAdsOnHide},
        {"UnityAdsOnVideoCompleted", "(Ljava/lang/String;I)V", (void*)UnityAdsOnVideoCompleted},
        {"UnityAdsOnVideoStarted", "()V", (void*)UnityAdsOnVideoStarted}
    };

    adsJavaVm = vm;
    JAVA_ATTACH_CURRENT_THREAD();

    jclass clsunityads = jni_env->FindClass("com/unity3d/ads/android/unity3d/UnityAdsUnityEngineWrapper");
    jmethodID midunityads = jni_env->GetMethodID(clsunityads, "<init>", "()V");

    if (jni_env->RegisterNatives(clsunityads, callbacks, sizeof(callbacks)/sizeof(callbacks[0])) < 0)
    {
        jni_env->FatalError("UnityAdsAndroidBridge: RegisterNatives failed");
    }

    jobject objunityadsLocal = jni_env->NewObject(clsunityads, midunityads);
    adsWrapperObject = jni_env->NewGlobalRef(objunityadsLocal);
    adsWrapperClass = (jclass)jni_env->NewGlobalRef(clsunityads);
    jni_env->DeleteLocalRef(objunityadsLocal);
    jni_env->DeleteLocalRef(clsunityads);
    RegisterMethods();      
}

void UnityAdsAndroidBridge::ReleaseJNI()
{
    JAVA_ATTACH_CURRENT_THREAD();
    jni_env->DeleteGlobalRef(adsWrapperObject);
    jni_env->DeleteGlobalRef(adsWrapperClass);
}