Device configuration for imoo Watch Phone Z5
================================

The imoo Watch Phone Z5 (or _"XiaoTianCai Watch Phone Z5q"_ in Mainland China) (codenamed as _"msm8909w"_) is a high-end
smartwatch for teens from imoo (_"XiaoTianCai"_ or _"Little Genius"_ in Mainland China).

## Device specifications

|                   Basic | Spec Sheet                                                   |
|------------------------:|:-------------------------------------------------------------|
|                     SoC | Qualcomm MSM8909w Snapdragon Wear 2100                       |
|                     CPU | Quad-core 1.2 GHz Cortex-A7                                  |
|                     GPU | Adreno 304                                                   |
|                  Memory | 512 MB RAM (LPDDR3)                                          |
| Shipped Android Version | 7.1.1                                                        |
|                 Storage | 4 GB eMMC 4.5 flash storage                                  |
|                 Battery | Non-removable Li-Po 680 mAh battery                          |
|              Dimensions | 51.83 x 41.40 x 14.2 mm (without band)                       |
|                 Display | 320 x 360 pixels, 1.41" AMOLED, 8:9 ratio (~342 ppi density) |
|            Front Camera | 5 MP, f/2.2, (wide), 1.12μm                                  |

## Build instructions

```bash
export LC_ALL=C
export ALLOW_MISSING_DEPENDENCIES=true
source build/envsetup.sh
lunch msm8909w-userdebug
make -j $(nproc --all)
```

## Device picture

![imoo Watch Phone Z5](https://cdn.shopify.com/s/files/1/0513/5115/8983/files/1-Market-Place-banner_9aba8cc2-5409-4608-a060-8595bffcd47e_1512x.jpg?v=1616118774)

## License

````text
Copyright (C) 2022 XTC Droid Port Team

Warning: XDPT Confidential
Unauthorized use or disclosure in any manner may result in
sciplinary action up to and including termination of group
relationship (in the case of our group member), termination
of an assignment or contract (in the case of our partner),
and potential civil and criminal liability (by imoo/XTC).
````

````text
Copyright (C) 2022 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
````
