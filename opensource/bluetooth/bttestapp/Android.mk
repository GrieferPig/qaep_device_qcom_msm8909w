LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
        $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := BTTestApp
LOCAL_CERTIFICATE := platform

LOCAL_MODULE_OWNER := qcom

LOCAL_STATIC_JAVA_LIBRARIES := com.android.vcard android.bluetooth.client.pbap android.bluetooth.client.map

LOCAL_PROGUARD_ENABLED := disabled

#Fixmeif needed
#include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH))
