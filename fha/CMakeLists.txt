cmake_minimum_required(VERSION 3.13)
project(fha)

set(CMAKE_CXX_STANDARD 11)

include_directories(${PROJECT_SOURCE_DIR}/ElfFixSection)

set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -s")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -s")

add_executable(
        fha
        main.cpp
        Utils.cpp
        ${PROJECT_SOURCE_DIR}/ElfFixSection/fix.cpp
)