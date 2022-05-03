ifneq (,$(filter $(QCOM_BOARD_PLATFORMS),$(TARGET_BOARD_PLATFORM)))
ifneq (,$(filter arm aarch64 arm64, $(TARGET_ARCH)))

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE      := page-nom.ko
LOCAL_MODULE_TAGS := debug
include $(DLKM_DIR)/AndroidKernelModule.mk

endif
endif
