LOCAL_PATH := $(call my-dir)/build_arm64-v8a
$(error ******** $(LOCAL_PATH) ********)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := assembly
LOCAL_MODULE := ffmpeg

include $(LOCAL_PATH)/config.mak

#LOCAL_CFLAGS := -DHAVE_AV_CONFIG_H -std=c99 -mfloat-abi=softfp -mfpu=neon -marm -march=armv7-a -mtune=cortex-a8
#TARGET_ARCH_ABI :=armeabi-v7a
ASSEMBLY_SRC_FILES := $(addprefix libswscale/, $(sort $(SWSCALE_C_FILES)))

ASSEMBLY_C_FILES := libavcode/aarch64/h264pred_neon.S, \
                          libavcode/aarch64/h264idct_neon.S, \
                          libavcode/aarch64/h264cmc_neon.S, \
                          libavcode/aarch64/hpeldsp_neon.S, \
                          libavcode/aarch64/mdct_neon.S, \
                          libavcode/aarch64/imdct15_neon.S, \
                          libavcode/aarch64/fft_neon.S, \
                          libavcode/aarch64/h264dsp_neon.S, \
                          libavcode/aarch64/fmtconvert_neon.S, \
                          libavcode/aarch64/h264qpel_neon.S

LOCAL_SRC_FILES := \
	$(ASSEMBLY_SRC_FILES)

LOCAL_ARM_MODE := arm
LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)
