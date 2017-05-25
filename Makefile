# GNU Makefile for JaC64

###############################################################
# VARIABLES
###############################################################

JAVAC ?= javac -encoding UTF-8
JAVACARGS ?= -deprecation

JAC64PACKAGES := com/dreamfabric/jac64 com/dreamfabric/c64utils resid
PACKAGES := com/dreamfabric/jac64 com/dreamfabric/jsidplay

OBJECTS := $(patsubst %.java,%.class,$(wildcard $(addsuffix /*.java, $(JAC64PACKAGES))))

OBJECTS_JSID := $(patsubst %.java,%.class,$(wildcard *.java)) com/dreamfabric/gui/DKnob2.class com/dreamfabric/gui/DCheckBox.class $(patsubst %.java,%.class,$(wildcard $(addsuffix /*.java,. $(PACKAGES))))

SOUNDS ?= sounds/motor.wav sounds/track.wav
SMALLUTILS ?= com/dreamfabric/c64utils/AutoStore.class com/dreamfabric/c64utils/C64Script.class
UTILS ?= $(SMALLUTILS) com/dreamfabric/c64utils/Debugger.class
DEMOFILES ?= c64programs/games1.d64 c64programs/Bonzieed.prg c64programs/Jeroen_tel_music.prg games.txt

# Set this to include any other files you want in your jar
EXTRAJARFILES ?=

###############################################################
# TARGETS
###############################################################

.PHONY:	compile

all:	compile

sid:    $(OBJECTS_JSID)

compile: $(OBJECTS) $(OBJECTS_JSID)

jar:    jac64.jar

smalljar: c64small.jar

jac64.jar: compile $(OBJECTS)
	jar cvfm $@ JaC64Manifest.txt com/dreamfabric/jac64/*.class JaC64*.class $(UTILS) $(SOUNDS) roms/*.* resid/*.class $(EXTRAJARFILES)

# Small(er) JaC64 Jarfile
c64small.jar: compile $(OBJECTS)
	jar cvf $@ com/dreamfabric/jac64/*.class C64Applet*.class $(SMALLUTILS) $(SOUNDS) roms/*.* resid/*.class $(EXTRAJARFILES)


# JSIDPlay including GUI, etc.
jsidplay.jar: $(OBJECTS_JSID)
	jar cvf $@ com/dreamfabric/jac64/{MOS6510Core,MOS6510Ops,C64Chips,CIA,DirEntry,Hex,M6510Ops,SID,SID6581,Loader,IMonitor,RS6581Waves,PatchListener,SIDMixer,Observer,SIDMixerListener,SIDMixerSE,SELoader,VICConstants,DefaultIMon}.class com/dreamfabric/c64utils/Assembler.class com/dreamfabric/jsidplay/*.class com/dreamfabric/gui/DKnob*.class com/dreamfabric/gui/DCheck*.class com/dreamfabric/jsidplay/{JSIDPlay,JSIDPlayer,JSCPU,JSIDChipemu,PSID,JSIDListener}.class sidplay.a65 roms/*.c64

# SIDPlay library
jsidlib.jar: $(OBJECTS_JSID)
	jar cvf $@ com/dreamfabric/jac64/{MOS6510Core,MOS6510Ops,C64Chips,CIA,DirEntry,Hex,M6510Ops,SID,SID6581,Loader,IMonitor,RS6581Waves,PatchListener,SIDMixer,Observer,SIDMixerListener}.class com/dreamfabric/c64utils/Debugger.class com/dreamfabric/jsidplay/{JSIDPlayer,JSCPU,JSIDChipemu,PSID,JSIDListener}.class

zip:	jac64.zip
jac64.zip: c64small.jar index_jac64.html
	zip -r jac64.zip c64small.jar $(DEMOFILES) index_jac64.html 

src: $(OBJECTS)
	zip -r jac64src.zip C64Test.java C64Applet.java com/dreamfabric/jac64/*.java com/dreamfabric/c64utils/*.java \
	  sounds/*.wav roms/*.c64 roms/*.rom readme.txt Makefile index_jac64.html c64small.jar $(DEMOFILES) index_jac64.html

###############################################################
# CLASS COMPILATION
###############################################################

%.class : %.java
	$(JAVAC) $(JAVACARGS) $<

###############################################################
# CLEAN
###############################################################

clean:
	rm -rf *.class $(OBJECTS) $(OBJECTS_JSID) c64.jar c64small.jar
