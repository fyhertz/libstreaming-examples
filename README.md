# Some examples of how to use libstreaming

This repository contains three simple examples of how to use libstreaming.

### Libstreaming ?

You can find out more about libstreaming [here](https://github.com/fyhertz/libstreaming).

### Example 1

Shows how to use the RTSP server.

### Example 2

Shows how to start a stream and obtain a SDP that you will then want to hand to the receiver.

### Example 3

**This example shows how you can use libstreaming with a Wowza Media Server.**

[Read this tutorial to find out of to use it](https://github.com/fyhertz/libstreaming/wiki/Using-libstreaming-with-Wowza-Media-Server)

### Build instructions

1. Clone the repository

2. libstreaming is referenced as git submodule in this repo, so you will need to run the two following commands:
```sh
git submodule init
git submodule update
```

3. Run the following command in the libstreaming directory and in the directory of the example you wish to compile:
```sh
cd libstreaming
android update project --path . --target android-19
cd ../example3/app
android update project --path . --target android-19
```

4. Run ant
```sh
ant debug
```

**Note: you will need to run 'ant clean' before compiling another example!**
