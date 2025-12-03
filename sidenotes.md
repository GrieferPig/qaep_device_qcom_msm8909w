# Sidenote discoveries

## Cover screen to sleep

This triggers:

```text
[10809.280795] cyttsp5_i2c_adapter 5-0024: cyttsp5_xy_worker: Large area detected forbitobject:0
[10809.280817] cyttsp5_i2c_adapter 5-0024: chenan Large area detected forbit_bigobject != 1
```

Cross-reference with public cyttsp5 [driver source](https://github.com/arter97/android_kernel_realme_sdm710/blob/67cd641f7c4039d0d37d57eccf835ffccf2447f2/drivers/input/touchscreen/cyttsp5/cyttsp5_mt_common.c):

```text
if (tch. hdr[CY_TCH_LO]) {
    parade_debug(dev, DEBUG_LEVEL_1, "%s: Large area detected\n",
        __func__);
    if (md->pdata->flags & CY_MT_FLAG_NO_TOUCH_ON_LO)
        num_cur_tch = 0;

    if (tch.hdr[CY_TCH_LO] && cd->large_power_state != 1) {
        dev_info(dev,
            "%s: Large area detected forbitobject:%d\n",
            __func__, cd->forbit_bigobject);
        if (cd->forbit_bigobject != 1) {
            input_report_key(md->input, KEY_POWER, 1);   // Power key press
            input_sync(md->input);
            input_report_key(md->input, KEY_POWER, 0);   // Power key release
            input_sync(md->input);
            cd->large_power_state = 1;
        }
    }
}
```

The event is reported through the same device as the touchscreen input (/dev/input/event1)

Turning it off (0=on, 1=off, default on):

```bash
echo 0 > /sys/bus/i2c/devices/5-0024/bigobject_off
```

`getevent -lt /dev/input/event1`

```text
[   12417.734101] EV_KEY       00fe                 DOWN
[   12417.734101] EV_SYN       SYN_REPORT           00000000
[   12417.734160] EV_KEY       00fe                 UP
[   12417.734160] EV_SYN       SYN_REPORT           00000000
```

It triggers keycode 0x00fe, which is not a standard key. Thanks, chenan.

## SELinux wizardry

Since XDPT team ceased to exist, the working SELinux config (in another GitHub repo) has been deleted. Hence, `androidboot.selinux=permissive` is used. I've also patched some files under [sepolicy](sepolicy/) to make them build properly, though they are not enforced.
