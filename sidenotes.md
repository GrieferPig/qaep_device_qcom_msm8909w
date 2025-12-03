# Sidenote discoveries

## Cover screen to sleep

This triggers:

```text
[10809.280795] cyttsp5_i2c_adapter 5-0024: cyttsp5_xy_worker: Large area detected forbitobject:0
[10809.280817] cyttsp5_i2c_adapter 5-0024: chenan Large area detected forbit_bigobject != 1
```

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
