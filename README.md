# HiveAR
Contains the android application project where it is possible to visualize in AR the state of agents inside a connected swarm.
It is also able to send command to those swarm agents and to observe their information in real time.

## Requirements

* `Android Studio 4.1+` to open android project with gradle dependencies.
* Android smartphone or emulator. (emulator has some limitation linked to serial communication)
* (optionnal, but mandatory for augmented reality features) 'ARCore' installed from Google Play Store.

## Building 
First, clone the repo with command:
```
git clone --recursive https://github.com/SwarmUS/HiveAR
```
Recursive argument will also clone the project's submodules.
In the case where recursive argument is forgotten, it is possible to load the submodule with command:
```
git submodule update --init
```
Then open HiveAR folder into Android Studio. (Not the repository folder, but the HiveAR folder with the project)

## Update
To update a submodule to its latest commit in its master branch, do this:
```
cd [submodule directory]
git checkout master
git pull
```
Then, to commit the change in the main repo:
```
cd [to repo directory]
git add [submodule directory]
git commit -m "move submodule to latest commit in its master"
git push
```

## Usage

This project is meant to deploy the application to an android device and to debug it.

## Installation

To install HiveAR on an android device, it is possible to deploy from source by using android studio, or to directly use an apk from github.

### Installing HiveAR through android studio

### Installing HiveAR from Github

When creating pull requests, the CICD generate artifacts containing an apk. This can be download and transferred to the desired android device. Then, from the android device, simply go into file explorer to find the apk and click on it to start the installation. Some warnings can be triggered: this is normal. The application wasn't design to be deployed on the Google Play Store, which makes the device warn you that file wasn't verified. Accept to install or refuse to abort the installation.

## Emulator set-up

### ArCore

In order to be able to run the app, ArCore needs to be installed on emulator. Refer to this [link](https://developers.google.com/ar/develop/c/emulator#run_your_app) for installation steps.

### Port forwarding

In order to be able to connect to a server running in the emulator, you will need the redirect the port from your system to the one on the emulator. To do this you will need to telnet in the emulator. First, get the token in the file `/home/<USER>/.emulator_console_auth_token`. Then run the following commands after starting the emulator:
```bash
telnet localhost 5554
auth <TOKEN>
redir add tcp:<SYSTEM_PORT>:<EMULATOR_PORT>
```
This will route any packet sent to the host's TCP port SYSTEM_PORT to TCP port EMULATOR_PORT of the emulated android device.

## Related repositories:
- The original AprilTag project repository: https://github.com/AprilRobotics/apriltag
- Pre-generated tag images: https://github.com/AprilRobotics/apriltag-imgs
