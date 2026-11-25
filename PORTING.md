# Porting Game Tour to another ROM or device

Game Tour is a privileged system app. It will not work as a normal user app: most features need system permissions, a platform signature and a few hooks from the ROM. This guide lists everything it expects.

## 1. Where it was developed

- Device: POCO X6 Pro / Redmi K70E (`duchamp`, MediaTek MT6897).
- ROM: AlphaDroid A16 (Android 16, AOSP `bp4a`), built with `lunch <product>-bp4a-userdebug`.
- Package: `dev.sempaz.gametour` (rename it if you like, then update the manifest, privapp whitelist, sysconfig and `Android.bp`).

## 2. What the ROM must provide

1. **Platform signature and shared UID.** `Android.bp` uses `certificate: "platform"` and the manifest uses `android:sharedUserId="android.uid.system"` and `android:persistent="true"`. The app installs only inside the ROM image. A persistent system app cannot be updated with `adb install -r`; ship changes with a ROM build.
2. **Privileged app in `system_ext`** with the whitelist `privapp_whitelist_dev.sempaz.gametour.xml` (`/system_ext/etc/permissions`). Without it the platform will log or reject the privileged permissions.
3. **`game_space` framework service.** `GameTourService` binds `IGameSpaceService` (`ServiceManager.getService("game_space")`) and registers an `IGameSpaceCallback` to receive `onGameStart` / `onGameLeave`. This service comes from the AxionOS framework. If your ROM does not have it, you need to port it, or replace `GameTourService` with your own foreground-app detection.
4. **Game list in `Settings.System`.** The list of games is read from `gamespace_game_list` (`"pkg=mode;pkg=mode"`, mode 2 = performance) and `gamespace_denied_list`, the same keys GameSpace uses.
5. **`ax_platform_hooks` static library** (AxionOS) for screenshot and screen record. If you do not have it, remove the dependency in `Android.bp` and the two tiles that call `AxPlatformClient` in `overlay/OverlayUi.kt`.
6. **LMOFreeform (optional).** Quick apps open in a floating window through the broadcast `com.libremobileos.freeform.START_FREEFORM` (receiver `com.libremobileos.freeform/.receiver.StartFreeformReceiver`, extras `packageName`, `activityName`, `userId`). Without it the quick-apps row is hidden.
7. **Install for the right users.** `sysconfig/preinstalled-packages-gametour.xml` installs the app for the main user and work profiles, not for the clone profile.

## 3. Device-specific parts (change these for your hardware)

All of them are read-only unless noted. If a node does not exist on your device the metric simply shows no value.

| What | Path | File |
|---|---|---|
| Thermal profile (write) | `/sys/class/thermal/thermal_message/sconfig` (50 balanced, 19 game, 18 performance, 700 battery) | `sys/Control.kt` |
| Charge bypass (write) | `/proc/mtk_battery_cmd/current_cmd` (`"0 1"` / `"0 0"`) and `/sys/class/power_supply/battery/constant_charge_current` | `sys/Control.kt` |
| CPU usage | `/proc/stat` | `sys/Sys.kt` |
| CPU clocks | `/sys/devices/system/cpu/cpufreq/policy*/scaling_cur_freq` | `sys/Sys.kt` |
| GPU clock | `/sys/class/devfreq/13000000.mali/cur_freq` | `sys/Sys.kt` |
| GPU load | tries `/proc/mtk_mali/gpu_utilization`, `/sys/kernel/ged/hal/gpu_utilization`, `/sys/module/ged/parameters/gpu_loading` | `sys/Sys.kt` |
| Device temperature | `/sys/class/thermal/thermal_message/board_sensor_temp` | `sys/Sys.kt` |

Notes learned on MT6897: `/proc/mtk_mali/gpu_utilization` is `0440 root:root`, so a system app cannot read it; `/sys/module/ged/parameters/gpu_loading` works.

Frame time comes from `dumpsys SurfaceFlinger --timestats` (the older `--latency` is unusable on Android 16). FPS comes from `TaskFpsCallback` and needs `MANAGE_ACTIVITY_TASKS` and `ACCESS_FPS_COUNTER`.

## 4. SELinux

The app runs as `system_app`. See the `sepolicy/` folder for the exact rules used on duchamp:

- `proc_stat` read, `surfaceflinger_service find` and `binder_call(system_app, surfaceflinger)` for CPU usage and the TimeStats dump;
- custom types (`sysfs_gametour_gpu`, `proc_gametour_gpu`) labeled with `genfscon` for the GPU nodes, and `sysfs_ged` read for the GED fallback;
- a separate fix for the power HAL (`hal_power_default` → `hal_thermal_service`).

Adapt the type and path names to your vendor policy. If you build with strict neverallows you may have to move rules between `system_app.te` and your vendor policy.

## 5. Checklist

- [ ] `m GameTour` builds (needs the AOSP Compose prebuilts and `ax_platform_hooks`).
- [ ] The ROM image contains the whitelist XML and the sysconfig XML.
- [ ] `adb shell dumpsys package <your.package> | grep granted` shows the privileged permissions granted.
- [ ] A game start/leave shows in `logcat -s GameTourSession`.
- [ ] No `avc: denied` for `system_app` / your Game Tour domain in `logcat`.
- [ ] You kept `NOTICE` and credited the original author.
