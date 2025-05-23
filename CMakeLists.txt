cmake_minimum_required(VERSION 3.28)
project(Foto_InjectLoader C)

set(CMAKE_C_STANDARD 11)
SET(EXECUTABLE_OUTPUT_PATH  ${PROJECT_BINARY_DIR}/../build/natives)
SET(LIBRARY_OUTPUT_PATH  ${PROJECT_BINARY_DIR}/../build/natives)

add_library(injector
        SHARED
        src/main/native/injector/Inject.c
        src/main/native/injector/GetProcAddressR.c
        src/main/native/injector/LoadLibraryR.c
        src/main/native/java/jvmti.h
)

add_library(agent
        SHARED
        src/main/native/dll/utils.c
        src/main/native/dll/dllmain.c
        src/main/native/dll/ReflectiveLoader.c
        src/main/native/java/jvmti.h
)

add_library(hwidSpoofer
        SHARED
        src/main/native/dll/utils.c
        src/main/native/dll/HwidSpoofer.c
        src/main/native/dll/ReflectiveLoader.c
        src/main/native/java/jvmti.h
)

