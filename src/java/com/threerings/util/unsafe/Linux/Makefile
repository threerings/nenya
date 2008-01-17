#
# $Id: Makefile 3331 2005-02-03 01:25:21Z mdb $

#
# Executable definitions

CC=gcc
RM=rm
CP=cp
MKDIR=mkdir

#
# Directory definitions

ROOT=../../../../../../..
LIBRARIES_PATH=-L/usr/X11R6/lib
INSTALL_PATH=${ROOT}/dist/lib/i686-Linux

#
# Parameter and file definitions

INCLUDES=-I.. -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux
LIBRARIES=-lX11
TARGETS=libunsafe.so libunsafegc.so

#
# Target definitions

all: ${TARGETS}

install:
	@${MKDIR} -p ${INSTALL_PATH}
	cp ${TARGETS} ${INSTALL_PATH}

libunsafe.so: com_threerings_util_unsafe_Unsafe.c
	@echo "Compiling $<"
	@${CC} ${INCLUDES} -c com_threerings_util_unsafe_Unsafe.c \
	    -o com_threerings_util_unsafe_Unsafe.o
	@echo "Creating $@"
	@${CC} -o libunsafe.so com_threerings_util_unsafe_Unsafe.o \
	    ${LIBRARIES} ${LIBRARIES_PATH} -shared

libunsafegc.so: com_threerings_util_unsafe_UnsafeGC.c
	@echo "Compiling $<"
	@${CC} ${INCLUDES} -c com_threerings_util_unsafe_UnsafeGC.c \
	    -o com_threerings_util_unsafe_UnsafeGC.o
	@echo "Creating $@"
	@${CC} -o libunsafegc.so com_threerings_util_unsafe_UnsafeGC.o \
	    ${LIBRARIES} ${LIBRARIES_PATH} -shared

clean:
	-${RM} -f *.o ${TARGETS}
