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
In the case where revursive argument is forgot, it is possible to load the submodule with command:
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
