$(call inherit-product, $(BOARD_COMMON_DIR)/base.mk)

# For PRODUCT_COPY_FILES, the first instance takes precedence.
# Since we want use QC specific files, we should inherit
# device-vendor.mk first to make sure QC specific files gets installed.
$(call inherit-product-if-exists, $(QCPATH)/common/config/device-vendor.mk)

# For minimal build, inherit core_minimal instead of full_base_telephony
# to avoid pulling in unnecessary AOSP apps
ifeq ($(MINIMAL_BUILD),true)
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_minimal.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/telephony.mk)
# Add fonts and hyphenation (required for UI rendering)
$(call inherit-product-if-exists, frameworks/base/data/fonts/fonts.mk)
$(call inherit-product-if-exists, external/roboto-fonts/fonts.mk)
$(call inherit-product-if-exists, external/noto-fonts/fonts.mk)
$(call inherit-product-if-exists, external/hyphenation-patterns/patterns.mk)
# Add audio files for /system/media/audio
$(call inherit-product-if-exists, frameworks/base/data/sounds/AllAudio.mk)
# Add software audio/video codecs (required for OGG/Vorbis, MP3, etc.)
PRODUCT_PACKAGES += \
    libstagefright_soft_vorbisdec \
    libstagefright_soft_mp3dec \
    libstagefright_soft_aacdec \
    libstagefright_soft_aacenc \
    libstagefright_soft_amrdec \
    libstagefright_soft_amrnbenc \
    libstagefright_soft_amrwbenc \
    libstagefright_soft_flacenc \
    libstagefright_soft_g711dec \
    libstagefright_soft_gsmdec \
    libstagefright_soft_opusdec \
    libstagefright_soft_rawdec
else
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)
endif

PRODUCT_BRAND := qcom
PRODUCT_AAPT_CONFIG += hdpi mdpi

ifndef PRODUCT_MANUFACTURER
PRODUCT_MANUFACTURER := QUALCOMM
endif

PRODUCT_PROPERTY_OVERRIDES += \
    ro.vendor.extension_library=libqti-perfd-client.so \
    persist.radio.apm_sim_not_pwdn=1 \
    persist.radio.sib16_support=1 \
    persist.radio.custom_ecc=1 \
    ro.frp.pst=/dev/block/bootdevice/by-name/config \
    dalvik.vm.heapgrowthlimit=96m \
    dalvik.vm.heapsize=256m \
    dalvik.vm.heapstartsize=8m \
    dalvik.vm.heapmaxfree=8m

PRODUCT_PRIVATE_KEY := $(BOARD_COMMON_DIR)/qcom.key

$(call inherit-product, frameworks/native/build/phone-xhdpi-1024-dalvik-heap.mk)
#$(call inherit-product, frameworks/base/data/fonts/fonts.mk)
#$(call inherit-product, frameworks/base/data/keyboards/keyboards.mk)
