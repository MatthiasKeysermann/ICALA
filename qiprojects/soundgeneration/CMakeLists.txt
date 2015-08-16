cmake_minimum_required(VERSION 2.8)
project(soundgeneration)
find_package(qibuild)

option(SOUNDGENERATION_IS_REMOTE
  "module is compiled as a remote module (ON or OFF)"
  ON)

set(_srcs
    main.cpp
    alsoundgeneration.h
    alsoundgeneration.cpp
)

if(SOUNDGENERATION_IS_REMOTE)
  add_definitions(" -DSOUNDGENERATION_IS_REMOTE ")
  qi_create_bin(soundgeneration ${_srcs})
else()
  qi_create_lib(soundgeneration SHARED ${_srcs} SUBFOLDER naoqi)
endif()

qi_use_lib(soundgeneration ALCOMMON ALAUDIO)
