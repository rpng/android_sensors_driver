##############################################################################
# CMake
##############################################################################

cmake_minimum_required(VERSION 2.8.3)
project(android_sensors_driver)

##############################################################################
# Catkin
##############################################################################

find_package(catkin REQUIRED rosjava_build_tools)

catkin_rosjava_setup(publish installApp)

catkin_package()

##############################################################################
# Installation
##############################################################################

install(DIRECTORY ${CATKIN_DEVEL_PREFIX}/${CATKIN_GLOBAL_MAVEN_DESTINATION}/android_sensors_driver/
        DESTINATION ${CATKIN_GLOBAL_MAVEN_DESTINATION}/android_sensors_driver)