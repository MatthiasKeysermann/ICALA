#ifndef SOUNDSPECTRUM_H
#define SOUNDSPECTRUM_H

#include <string>
#include <complex>
#include <iostream>
#include <valarray>
#include <climits>

const double PI = 3.141592653589793238460;

typedef std::complex<double> Complex;
typedef std::valarray<Complex> CArray;

#include <boost/shared_ptr.hpp>
#include <boost/lexical_cast.hpp>

#include <alcommon/alproxy.h>
#include <alaudio/alsoundextractor.h>
#include <alproxies/almemoryproxy.h>
#include <alvalue/alvalue.h>



using namespace AL;

class ALSoundSpectrum : public ALSoundExtractor
{

public:
	ALSoundSpectrum(boost::shared_ptr<ALBroker> pBroker, std::string pName);
	virtual ~ALSoundSpectrum();
	void init();
	void fft(CArray& x);

public:
	void process(const int & nbOfChannels,
		const int & nbrOfSamplesByChannel,
		const AL_SOUND_FORMAT * buffer,
		const ALValue & timeStamp);

private:
	int sample_rate;
	int num_bins;
	bool simulate_sine;
	int freq_sine;
	ALMemoryProxy fProxyToALMemory;
	std::vector<std::string> fALMemoryKeys;
	
};

#endif
