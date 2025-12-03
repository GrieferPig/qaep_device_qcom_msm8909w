# Trimmed packages configuration for minimal bootable build
# This file removes test apps, sample apps, and unnecessary holder apps
# while keeping core system services, connectivity, and essential watch apps

# ============================================================================
# APPS TO REMOVE - Test, sample, demo, and unnecessary holder apps
# ============================================================================

# Clear test apps from base.mk
APPS :=
TINY_ALSA_TEST_APPS :=
KERNEL_TESTS :=
CM :=

# Remove test/debug packages from camera
LIBCAMERA := camera.msm8909
LIBCAMERA += camera.msm8909w
LIBCAMERA += libcamera
LIBCAMERA += libmmcamera_interface
LIBCAMERA += libmmcamera_interface2
LIBCAMERA += libmmjpeg_interface
LIBCAMERA += libmmlib2d_interface
LIBCAMERA += libqomx_core
# Removed: mm-qcamera-app, camera_test, org.codeaurora.camera

# Remove ExoplayerDemo from MM_VIDEO (keep core components)
MM_VIDEO := liblasic
MM_VIDEO += libOmxVdec
MM_VIDEO += libOmxVdecHevc
MM_VIDEO += libOmxVdpp
MM_VIDEO += libOmxVenc
MM_VIDEO += libOmxVidEnc
MM_VIDEO += libstagefrighthw
# Removed: ast-mm-vdec-omx-test, mm-vdec-omx-test, mm-venc-omx-test, mm-venc-omx-test720p
# Removed: mm-video-driver-test, mm-video-encdrv-test, mm-vdec-omx-property-mgr, ExoplayerDemo

# ============================================================================
# WATCH PACKAGES - Keep only essential watch apps
# Only WatchSettings related packages are preserved
# WatchFace apps are commented out - uncomment if needed
# ============================================================================

# Override WATCH_PACKAGES to include only essential apps
WATCH_PACKAGES := watchsettings
# Uncomment the following if WatchFace is needed:
# WATCH_PACKAGES += watchface
# WATCH_PACKAGES += watchfaceanalogclock
# WATCH_PACKAGES += watchfaceanalogdigitalclock
# Settings plugins needed for watchsettings functionality
WATCH_PACKAGES += watchwifi
WATCH_PACKAGES += watchbluetooth
WATCH_PACKAGES += watchairplanemode
WATCH_PACKAGES += watchcellular
WATCH_PACKAGES += watchdatetime
WATCH_PACKAGES += watchdeveloperoptions
# Context mode support (may be needed for watch functionality)
WATCH_PACKAGES += contextualmodedozeservice
WATCH_PACKAGES += qwcontextualmodelib.xml
WATCH_PACKAGES += qwcontextualmodelib

# Removed watch apps:
# - launcher (custom launcher - not needed)
# - watchhome (home screen - not needed)
# - watchalarm (alarm app)
# - WatchContacts (contacts app)
# - watchdialer (phone dialer)
# - watchmessenger (messaging app)
# - watchmusicplayer (music player)
# - STApp (ST sensor test app)
# - ctsintenthandler (CTS test handler)

# Additional removed apps (per user request):
# - QSensorTest, ArSensorTest (sensor test apps)
# - WebViewBrowserTester, Browser, Browser2 (browser apps)
# - DownloadProviderUi, Downloads (downloads app)
# - ODLT (Qualcomm DLT diagnostic tool)
# - diag_callback_client, diag_dci_sample, PktRspTest, test_diag (diagnostic tools)

# ============================================================================
# PRODUCT_PACKAGES TO REMOVE - Override specific unnecessary packages
# ============================================================================

# Remove Bluetooth test apps (keep core Bluetooth functionality)
# These are removed by not adding them - handled in base.mk conditionals
# BTTestApp, HiddTestApp, BTLogKit, BTLogSave are in ifneq ($(TARGET_USES_AOSP),true) block

# Force AOSP mode to exclude BT test apps
TARGET_USES_AOSP := true

# ============================================================================
# BASE PRODUCT_PACKAGES TRIMMING - Remove unnecessary AOSP apps
# ============================================================================

# Override base PRODUCT_PACKAGES to remove unnecessary apps for wearable
# This list keeps only essential system apps while removing:
# - DeskClock (alarm functionality removed)
# - Calculator (not needed on watch)
# - Calendar, CalendarProvider (not needed)
# - Camera (can be removed if not needed)
# - Email (not needed on watch)
# - Gallery2, SnapdragonGallery (not needed)
# - LatinIME (may not be needed depending on input method)
# - Mms (messaging removed)
# - Music (music player removed)
# - QuickSearchBox (not needed)
# - Sync, SyncProvider (may keep for basic functionality)
# - IM, VoiceDialer, VoiceInteraction (not needed on minimal build)
# - Protips, Provision (not needed)
# - CellBroadcastReceiver (may keep for emergency)
# - Updater (not needed in minimal build)
# - LiveWallpapers, LiveWallpapersPicker, VisualizationWallpapers (not needed)

# Remove LiveWallpapers packages
PRODUCT_PACKAGES_REMOVE += LiveWallpapers
PRODUCT_PACKAGES_REMOVE += LiveWallpapersPicker
PRODUCT_PACKAGES_REMOVE += VisualizationWallpapers

# Remove Downloads app
PRODUCT_PACKAGES_REMOVE += Downloads
PRODUCT_PACKAGES_REMOVE += DownloadProvider
PRODUCT_PACKAGES_REMOVE += DownloadProviderUi

# ============================================================================
# DISPLAY TESTS AND FTM - Remove test packages
# ============================================================================

# Clear display tests
DISPLAY_TESTS :=

# ============================================================================
# MODEM API TEST - Remove test packages  
# ============================================================================

MODEM_API_TEST :=

# ============================================================================
# GPS TEST APPS - Remove but keep core GPS functionality
# ============================================================================

# GPS hardware will be kept from base.mk GPS_HARDWARE variable
# Test apps are in vendor proprietary GPS variable and will be filtered there

# ============================================================================
# PROPRIETARY TEST PACKAGES TO EXCLUDE
# These are typically pulled in from device-vendor.mk
# We mark them as empty to prevent inclusion
# ============================================================================

# FTM (Factory Test Mode) - keep daemon but remove test apps
# Comment out if factory testing is needed
# FTM :=

# ============================================================================
# ADDITIONAL TRIMMING NOTES
# ============================================================================

# The following packages are KEPT for connectivity:
# - Bluetooth (core bluetooth stack, BluetoothExt without test apps)
# - WiFi (wpa_supplicant, hostapd, wcnss_service)
# - Telephony (Phone, telephony-ext, tcmiface, qcril)
# - NFC (if TARGET_USES_NQ_NFC is true)

# The following packages are KEPT for system services:
# - SystemUI
# - Settings
# - DrmProvider, CertInstaller
# - All audio hardware and policy
# - All display/graphics libs
# - All init scripts
# - Core multimedia codecs (without test apps)
# - OEM services and subsystem control
# - Thermal engine
# - Time services
# - Sensors
