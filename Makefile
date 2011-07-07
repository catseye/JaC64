#
# Makefile
#
# Required
#   javac - Java
#   rm

#CC = javac -target 1.3 -source 1.3
CC = javac
#CC = C:/jdk1.3.1_16/bin/javac
#CC = C:/j2sdk1.4.2_10/bin/javac -target 1.3 -source 1.3
CCARGS=-deprecation

JAC64PACKAGES := com/dreamfabric/jac64 com/dreamfabric/c64utils resid
PACKAGES := com/dreamfabric/jac64 com/dreamfabric/jsidplay

OBJECTS := $(patsubst %.java,%.class,$(wildcard $(addsuffix /*.java, $(JAC64PACKAGES))))

OBJECTS_JSID := $(patsubst %.java,%.class,$(wildcard *.java)) com/dreamfabric/gui/DKnob2.class com/dreamfabric/gui/DCheckBox.class $(patsubst %.java,%.class,$(wildcard $(addsuffix /*.java,. $(PACKAGES))))

SOUNDS ?= sounds/motor.wav sounds/track.wav
SMALLUTILS ?= com/dreamfabric/c64utils/AutoStore.class com/dreamfabric/c64utils/C64Script.class
UTILS ?= $(SMALLUTILS) com/dreamfabric/c64utils/Debugger.class

# Set this to include any other files you want in your jar
EXTRAJARFILES ?=

.PHONY:	compile

all:	compile

sid:    $(OBJECTS_JSID)

compile: $(OBJECTS) $(OBJECTS_JSID)

jar:    jac64.jar

smalljar: c64small.jar

jac64.jar: compile $(OBJECTS)
	jar cvfm $@ jac64manifest.txt com/dreamfabric/jac64/*.class JaC64*.class $(UTILS) $(SOUNDS) roms/*.* resid/*.class $(EXTRAJARFILES)

# Small(er) JaC64 Jarfile
c64small.jar: compile $(OBJECTS)
	jar cvf $@ com/dreamfabric/jac64/*.class C64Applet*.class $(SMALLUTILS) $(SOUNDS) roms/*.* resid/*.class $(EXTRAJARFILES)


jogltest: SimpleJoglApp.class
	javac -classpath ".;../../java/jogl-1_0_0-windows-i586/lib/jogl.jar" SimpleJoglApp.java

dknob:
	cp ../sicstools/courses/joakim/softsynth/com/dreamfabric/gui/DKnob2.java com/dreamfabric/gui/
	cp ../sicstools/courses/joakim/softsynth/com/dreamfabric/gui/DCheckBox.java com/dreamfabric/gui/

# JSIDPlay including GUI, etc.
jsidplay.jar: $(OBJECTS_JSID)
	jar cvf $@ com/dreamfabric/jac64/{MOS6510Core,MOS6510Ops,C64Chips,CIA,DirEntry,Hex,M6510Ops,SID,SID6581,Loader,IMonitor,RS6581Waves,PatchListener,SIDMixer,Observer,SIDMixerListener,SIDMixerSE,SELoader,VICConstants,DefaultIMon}.class com/dreamfabric/c64utils/Assembler.class com/dreamfabric/jsidplay/*.class com/dreamfabric/gui/DKnob*.class com/dreamfabric/gui/DCheck*.class com/dreamfabric/jsidplay/{JSIDPlay,JSIDPlayer,JSCPU,JSIDChipemu,PSID,JSIDListener}.class sidplay.a65 roms/*.c64

# SIDPlay library
jsidlib.jar: $(OBJECTS_JSID)
	jar cvf $@ com/dreamfabric/jac64/{MOS6510Core,MOS6510Ops,C64Chips,CIA,DirEntry,Hex,M6510Ops,SID,SID6581,Loader,IMonitor,RS6581Waves,PatchListener,SIDMixer,Observer,SIDMixerListener}.class com/dreamfabric/c64utils/Debugger.class com/dreamfabric/jsidplay/{JSIDPlayer,JSCPU,JSIDChipemu,PSID,JSIDListener}.class

# Java ME experiments...
jsidlib.src: $(OBJECTS_JSID)
	cp com/dreamfabric/jac64/{MOS6510Core,MOS6510Ops,C64Chips,CIA,DirEntry,Hex,M6510Ops,SID,SID6581,Loader,IMonitor,RS6581Waves,PatchListener,SIDMixer,Observer,SIDMixerListener}.java C:/SonyEricsson/JavaME_SDK_CLDC/PC_Emulation/WTK2/apps/JSIDPlay/src/com/dreamfabric/jac64/
	cp com/dreamfabric/c64utils/{Assembler,Debugger}.java C:/SonyEricsson/JavaME_SDK_CLDC/PC_Emulation/WTK2/apps/JSIDPlay/src/com/dreamfabric/c64utils
	cp com/dreamfabric/jsidplay/{JSIDPlayer,JSCPU,JSIDChipemu,PSID,JSIDListener}.java C:/SonyEricsson/JavaME_SDK_CLDC/PC_Emulation/WTK2/apps/JSIDPlay/src/com/dreamfabric/jsidplay
	cp sidplay.a65  C:/SonyEricsson/JavaME_SDK_CLDC/PC_Emulation/WTK2/apps/JSIDPlay/src/

zip:	jac64.zip
jac64.zip: c64small.jar index_jac64.html
	zip -r jac64.zip c64small.jar c64programs/games1.d64 c64programs/Bonzieed.prg c64programs/Jeroen_tel_music.prg index_jac64.html games.txt

src: $(OBJECTS)
	zip -r jac64src.zip C64Test.java C64Applet.java com/dreamfabric/jac64/*.java com/dreamfabric/c64utils/*.java sounds/motor.wav sounds/track.wav roms/*.c64 roms/*.rom readme.txt Makefile index_jac64.html c64small.jar c64programs/games1.d64 c64programs/Bonzieed.prg c64programs/Jeroen_tel_music.prg index_jac64.html games.txt

###############################################################
# CLASS COMPILATION
###############################################################

%.class : %.java
	$(CC) $(CCARGS) $<


###############################################################
# CLEAN
###############################################################

clean:
	rm -rf *.class $(OBJECTS) $(OBJECTS_JSID) c64.jar c64small.jar
