LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libUnityAdsBridge

LOCAL_MODULE_FILENAME := libUnityAdsBridge

LOCAL_SRC_FILES := gen/UnityAdsAndroidBridge.cpp

LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/src $(LOCAL_PATH)/gen
LOCAL_C_INCLUDES += $(LOCAL_PATH)/src $(LOCAL_PATH)/gen

include $(BUILD_STATIC_LIBRARY)