# Thermic
![cat s60-2](https://cloud.githubusercontent.com/assets/3069650/25279881/a00767cc-26b0-11e7-826d-f4d060626223.jpg)

Fully functioning thermal imaging app running on the CAT S60 Phone

# Introduction

This android app project was created so as a playground for testing the CAT S60 thermal imaging phone's capabilities. Getting the FLIR SDK to work on the CAT S60 on the latest Android API was a challenge by itself. Official SDK docs not mentioning how to handle the internal thermal camera and only providing robust application example for detachable thermal devices. I thought this would benefit other developers who struggle trying to figure out how to get started.

## Capturing Image Data

This took me a while to figure out, when you configure the `FrameProcessor` to process a set of images it will give you those sets of images per second. To explain it better if you configure it to capture `VisibleAlignedRGBA8888Image` and `ThermalRadiometricKelvinImage` then the `FrameProcessor` will be processing those 2 image types at the same time. So your `onFrameProcessed()` method will receive a `RenderedImage` of type `VisibleAlignedRGBA8888Image` and a `RenderedImage` of type `ThermalRadiometricKelvinImage` for the same instance of time.

This app is set to capture 4 image types

* VisibleAlignedRGBA8888Image
* BlendedMSXRGBA8888Image
* ThermalLinearFlux14BitImage
* ThermalRadiometricKelvinImage

It will save them to a location on the phone so that they can be retrieved later on. The RGB images are saved in JPG format while the others are just raw bytes written to the phone's file-system.

# Contributing

It would be kick ass if other FLIR SDK developers would want to contribute anything they want to this code base. The idea is to learn from each other and encourage best practices.

# License

BSD 3 Clause license seems cool
