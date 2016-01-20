# home-ai

Inspired by https://github.com/gigasquid/clj-drone (opencv and video)

## Installation

Download from https://github.com/bemcho/home-ai.

## Usage
   install opencv (If you are on Windows x64 skip this just add existing jars to the local repo {see: Locally install the jars})

    sudo port install opencv +java -no_static +cuda +ffmpeg +tbb

    Now, to be able to add the libopencv_java311.dylib shared native lib to the local maven repository, we first need to package it as a jar file.

    The native lib has to be copied into a directories layout which mimics the names of your operating system and architecture.     I’m using a Mac OS X with a X86 64 bit architecture. So my layout will be the following:

    mkdir -p native/macosx/x86_64

    Copy into the x86_64 directory the libopencv_java310.dylib lib.

    cp ~/opt/opencv/build/lib/libopencv_java310.dylib native/macosx/x86_64/

    If you’re running OpenCV from a different OS/Architecture pair, here is a summary of the mapping you can choose from.

    OS

    Mac OS X -> macosx
    Windows  -> windows
    Linux    -> linux
    SunOS    -> solaris

    Architectures

    amd64    -> x86_64
    x86_64   -> x86_64
    x86      -> x86
    i386     -> x86
    arm      -> arm
    sparc    -> sparc

    Package the native lib as a jar

    Next you need to package the native lib in a jar file by using the jar command to create a new jar file from a directory.
    For your convenience naative libs for mac and windows are provided under native folder
    
    jar -cMf opencv-native-310.jar native



Locally install the jars¶

We are now ready to add the two jars as artifacts to the local maven repository with the help of the lein-localrepo plugin.
Find your .lein folder nad add profiles.clj file
Copy/paste this line {:user {:plugins [[lein-localrepo "0.5.2"]]}}

lein localrepo install opencv-310.jar opencv/opencv 3.1.0

Here the localrepo install task creates the 3.1.0. release of the opencv/opencv maven artifact from the opencv-310.jar lib and then installs it into the local maven repository. The opencv/opencv artifact will then be available to any maven compliant project (Leiningen is internally based on maven).

Do the same thing with the native lib previously wrapped in a new jar file.

lein localrepo install opencv-native-310.jar opencv/opencv-native 3.1.0

   
  
   This quite did not work :
   $ java -jar ai-home-0.1.0-standalone.jar [args]
   
   however:
   lein run  it's ok only so far Windows dlls are provided this will not work on mac os x so far, unles you build with contrib and new java wrapper for your OS.
   
  
   Under Windows you need camera set as default
   
     Install leiningen
	 Find your .lein/profiles.clj file
    	Copy/paste next line: 
	        {:user {:plugins [[lein-localrepo "0.5.2"]]}}
	        

	cd  home-ai folder 
	
	lein localrepo install opencv-310.jar opencv/opencv 3.1.0
	lein localrepo install opencv-native-310.jar opencv/opencv-native 3.1.0
	lein -o run	
	
## Options

FIXME: listing of options this app accepts.

native libs are not found when java -jar "generated uberjar-standalone.jar"

## Examples

...

### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
