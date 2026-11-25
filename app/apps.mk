# Game Tour - SPDX-License-Identifier: Apache-2.0
# Incluído pela dt do duchamp via inherit-product-if-exists (mesmo padrão do camera.mk).
GAMETOUR_DIR := device/xiaomi/duchamp-apps/gametour

PRODUCT_PACKAGES += \
    GameTour

PRODUCT_COPY_FILES += \
    $(GAMETOUR_DIR)/sysconfig/preinstalled-packages-gametour.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/sysconfig/preinstalled-packages-gametour.xml
