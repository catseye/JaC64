This is Cat's Eye Technologies' fork of the original JaC64 distribution.
The original README follows after the first line of hyphens in this file.

This fork was made from what was the tip revision of the JaC64 sources
on Sourceforge about 2 years ago, and what is still the tip revision as
of this writing, revision 140:

http://sourceforge.net/p/jac64/code/HEAD/tree/

Several bug fixes and minor enhancements have been applied.  The
full details can be found in the git log:

https://github.com/catseye/JaC64/commits/master

Some highlights are:
- Restartable!  There were problems before with applet start/stop
  (e.g. reloading a web page that has a JaC64 applet on it)
- More robust handling of joystick (does not initially "stick" in the
  top-left direction at the start of some games)
- More robust handling of broken audio support
- Easier building (cleaned up Makefile, build warnings)
- Refactored some code

Right now I'm thinking of reorganizing the sources and rewriting the
Makefile from scratch because it's still hugely annoying.

-Chris

-----------------------------------------------------------------------------
JaC64, 2007 - Originator and main developer, Joakim Eriksson, jac64.com,
	      dreamfabric.com
Readme for JaC64 - 100% Java C64 emulation
-----------------------------------------------------------------------------
JaC64 is a pure Java C64 emulator that can be run in any modern Java
enabled web-browser. It can also be run as stand-alone C64 emulator.

To run a test application type java C64Test (after building with make).

To make an archive, type "make smalljar" and you will get the file
c64small.jar - a jar file with only neccesary files for JaC64. It
will work with the example files (see below).

Example usage of JaC64 is in the index_jac64.html files. Showing
simple usage of JaC64 and describe how to use them.

For more information, games, documentation and latest release see:
  http://www.jac64.com

For the latest source-code look at the sourceforge project JaC64:
  http://sourceforge.net/projects/jac64/

Other contributors:
[2002] Jan Blok - reimplementation of memory model and fixing CPU bugs
[2006] Jörg Jahnke - help with refactoring of CPU class
[2006] ByteMaster of Cache64.com - extensive testing and bugreporting -
       huge thanks!
