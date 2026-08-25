cmake_minimum_required(VERSION 3.22.1)

project("llama_jni" C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 编译参数优化
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -fexceptions -frtti -Wall -Wextra")

# 引入头文件目录
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)

# 添加 JNI 动态库目标
add_library(
    llama_jni
    SHARED
    llama_jni.cpp
)

# 查找 Android 系统基础库
find_library(log-lib log)
find_library(android-lib android)

# 链接系统库
target_link_libraries(
    llama_jni
    ${log-lib}
    ${android-lib}
)
