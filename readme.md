# addrobots_vcu

## An Android-based program that allows cloud control of the robotic vehicle. 

* OpenCV camera control for both optical flow and forward motion imagery.
* Our smart motor auto-enumerates as a CDC/USB peripheral and auto-loads your concrete PID controller class.
* Accepts high-level control messages from Google Firebase (Cloud) Messaging.
* Converts messages and commands into  Google protocol-buffer-encoded sensor and motor control commands over the USB/CDC interface.

## Build
The build intructions for this Android project are [here](https://github.com/AddRobots/addrobots_vcu/blob/master/build.md).

### Smart Motor (a separate, but related project)
A stepper motor with an integrated step controller that processes cooked motor control commands into physical movement or sensor data. The motor also has an integrated 16-bit rotary encoder so that the system knows if the motor properly executes commands. This also allows the motor to operate in a current-limit regime where we compute in realtime the minimum  windings current required to correctly execute commands. This also allows a maximum current lock-out to automatically freewheel on over-force (current). This system also gives a first-order approximation of realtime force/load against the motor.

### Sample high-level (cloud) commands:
* Drive
* Halt
* Orbit

### Sample low-level (motor) commands:
* Rotate
* Goto
* Hold
* Sensor
