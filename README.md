# HiveAR
Contains the android application project where it is possible to visualize in AR the mapping done by a connected swarm.
It is also able to send command to this swarm and to observe informations in real time of the swarm.

## Requirements

* `Android Studio 4.1+` to open android project with gradle dependencies.
* Android smartphone or emulator. (emulator has some limitation linked to serial communication)

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
Then open HiveAR folder into Android Studio. (Not the repo folder, but the HiveAR folder with the project)

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

## Emulator set-up

### ArCore

In order to be able to run the app, ArCore needs to be installed on emulator. Refer to this [link](https://developers.google.com/ar/develop/c/emulator#run_your_app) for installation steps

### Port forwarding

In order to be able to connect to a server running in the emulator, you will need the redirect the port from your system to the one on the emulator. To do this you will need to telnet in the emulator. First, get the token in the file `/home/<USER>/.emulator_console_auth_token`. Then run the following commands after starting the emulator:
```bash
telnet localhost 5554
auth <TOKEN>
redir tcp:<SYSTEM_PORT>:<EMULATOR_PORT>
```
This will route any packet sent to the host's TCP port SYSTEM_PORT to TCP port EMULATOR_PORT of the emulated android device.
