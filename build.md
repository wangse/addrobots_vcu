# How to build

This is a pretty much a standard Android Studio project with two modules: opencv and ffmpeg. 
Because we use Android Studio's (today experimental) NDK native plugin, you will need to use AS 2.2+ since it's the only platform that supports this plugin (as of today).

## OpenCV

I followed the instuctions to install an OpenCV module into an Android project: [OpenCV4Android](http://docs.opencv.org/2.4/doc/tutorials/introduction/android_binary_package/O4A_SDK.html)

## FFmpeg

OpenCV doesn't work fully without FFmpeg, but to generate FFmpeg shared libraries on Android for all ABI types is (was) painful. 
I created a Gradle Plugin/RuleSource that works with Android Studio's (today experimental) NDK native plugin.
Now this project automagically builds the FFmpeg sharedlibs for release/debug on all ABI types directly from an unmodified git clone of FFmpeg.
All you need to do is `git clone https://git.ffmpeg.org/ffmpeg.git ffmpeg` into a directory that's at the same level as this project. Generally, this location is ~/AndroidStudioProjects.

There are two flavors of the gradle.build for FFmpeg:

### Original (now deprecated) 
This version that scans the model's task list, and modifies the task structures to check autotools config and include the right files during the build. 
Most of this code is in autotools.gradle. At some point I'll delete is all, but I wanted a record to hit Google search since it solves some interesting problems
that you might have in a task-based Gradle build. These kinds of builds will be around until everyone finally moves to RuleSource builds.

### Current 
This version that uses a Plugin/RuleSource to intercept model rules via @Mutate during the right part of the build cycle. This is definitely more modern, 
and cleaner.

Both of these strategies add dependent tasks that check for incremental build state, and if needed config and add files to the build cycle depending on ABI type and mode (debug, release).

##Goals 
* Get Gradle Plugin/RuleSource to accept absolute paths, so that we can put the FFmpeg (and later OpenCV) repos whever we want. (e.g. ~/git).
* Use NDK to also natively build OpenCV from the raw git repo so that we can debug the sources.
* Generalize the AutotoolsPlugin class so that it works with *any* autotools-based project.
* Clean-up various messes. It's hard to know best practices in this part of Gradle and with the Android plugin because (today) it's all in flux and new.
