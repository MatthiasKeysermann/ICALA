#include <signal.h>
#include <boost/shared_ptr.hpp>
#include <alcommon/albroker.h>
#include <alcommon/almodule.h>
#include <alcommon/albrokermanager.h>
#include <alcommon/altoolsmain.h>
#include "alsoundgeneration.h"

#ifdef SOUNDGENERATION_IS_REMOTE
# define ALCALL
#else
# ifdef _WIN32
#  define ALCALL __declspec(dllexport)
# else
#  define ALCALL
# endif
#endif

extern "C"
{

	ALCALL int _createModule(boost::shared_ptr<AL::ALBroker> pBroker)
	{
		// init broker with the main broker instance from the parent executable
		AL::ALBrokerManager::setInstance(pBroker->fBrokerManager.lock());
		AL::ALBrokerManager::getInstance()->addBroker(pBroker);

		// create module instances
		AL::ALModule::createModule<ALSoundGeneration>(pBroker, "ALSoundGeneration");
		return 0;
	}

	ALCALL int _closeModule()
	{
		return 0;
	}

} // extern "C"

#ifdef SOUNDGENERATION_IS_REMOTE

int main(int argc, char *argv[] )
{
	// pointer on createModule
	TMainType sig;
	sig = &_createModule;

	// call main
	ALTools::mainFunction("alsoundgeneration", argc, argv, sig);
}

#endif
