#ifndef SOUNDGENERATION_H
#define SOUNDGENERATION_H

#include <string>
#include <climits>

const double PI = 3.141592653589793238460;

#include <boost/shared_ptr.hpp>
#include <boost/lexical_cast.hpp>

#include <alcommon/alproxy.h>
#include <alcommon/albroker.h>
#include <alcommon/almodule.h>
#include <alproxies/almemoryproxy.h>
#include <alvalue/alvalue.h>



namespace AL
{
	class ALBroker;
}

class ALSoundGeneration : public AL::ALModule
{

public:
	ALSoundGeneration(boost::shared_ptr<AL::ALBroker> pBroker, const std::string& pName);
	virtual ~ALSoundGeneration();	
	void playSineWaves();

private:
	int sample_rate;
	int num_samples;
	int num_bins;	
	boost::shared_ptr <AL::ALProxy> audioDeviceProxy;
	AL::ALMemoryProxy fProxyToALMemory;
	std::vector<std::string> fALMemoryKeys;
	
};
#endif

