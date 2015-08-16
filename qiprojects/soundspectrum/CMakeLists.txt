cmake_minimum_required(VERSION 2.8)
project(soundspectrum)
find_package(qibuild)

option(SOUNDSPECTRUM_IS_REMOTE
  "module is compiled as a remote module (ON or OFF)"
  ON)

set(_srcs
    main.cpp
    alsoundspectrum.h
    alsoundspectrum.cpp
)

if(SOUNDSPECTRUM_IS_REMOTE)
  add_definitions(" -DSOUNDSPECTRUM_IS_REMOTE ")
  qi_create_bin(soundspectrum ${_srcs})
else()
  qi_create_lib(soundspectrum SHARED ${_srcs} SUBFOLDER naoqi)
endif()

qi_use_lib(soundspectrum ALCOMMON ALAUDIO)
