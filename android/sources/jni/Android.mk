LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libUnityAdsBridge

LOCAL_MODULE_FILENAME := libUnityAdsBridge

LOCAL_SRC_FILES := src/UnityAdsAndroidBridgeGenerated.cpp

LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
LOCAL_C_INCLUDES += $(LOCAL_PATH)

include $(BUILD_STATIC_LIBRARY)