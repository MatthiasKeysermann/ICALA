#include "alsoundspectrum.h"



ALSoundSpectrum::ALSoundSpectrum(boost::shared_ptr<ALBroker> pBroker, std::string pName) : ALSoundExtractor(pBroker, pName)
{
	setModuleDescription("This module writes the frequency spectrum to AlMemory.");
}



void ALSoundSpectrum::init()
{	
	sample_rate = 16000; // Hz
	num_bins = 16;
	simulate_sine = false;
	freq_sine = 0;
	
	for(int i=0; i<num_bins; i++)
	{
		std::string strI = boost::lexical_cast<std::string>(i);
		std::string str = "ALSoundSpectrum/front" + strI;
		fALMemoryKeys.push_back(str);
	}
	for(int i=0; i<num_bins; i++)
	{
		fProxyToALMemory.insertData(fALMemoryKeys[i], 0.0f);
	}
	
	audioDevice->callVoid("setClientPreferences",
						getName(),                //Name of this module
						sample_rate,              //requested sample rate
						(int) FRONTCHANNEL,       //requested channels
						1                         //deinterleaving?
						);
	
	printf("Starting detection...\n");
	startDetection();
}



ALSoundSpectrum::~ALSoundSpectrum()
{
	printf("Stopping detection...\n");
	stopDetection();
}



/// Description: The name of this method should not be modified as this
/// method is automatically called by the AudioDevice Module.
void ALSoundSpectrum::process(const int & nbOfChannels,
                              const int & nbOfSamplesByChannel,
                              const AL_SOUND_FORMAT * buffer,
                              const ALValue & timeStamp)
{	
	int num_samples = nbOfSamplesByChannel;	
	
	// DEBUG
	clock_t clockBegin = clock();
	
	// normalise data
	double data_norm[num_samples];
	for (int i=0; i<num_samples; i++)
	{
		data_norm[i] = (double) buffer[i] / SHRT_MAX;
	}
	
	// check for simulate sine wave	
	try
	{
		ALValue value = fProxyToALMemory.getData("ALSoundSpectrum/simulateSine");
		if((float) value == 0)
			simulate_sine = false;
		else
			simulate_sine = true;
	}
	catch(AL::ALError &e)
	{
		//throw ALError("ALSoundSpectrum", "process()", e.getDescription());
		simulate_sine = false;
	}
	
	// simulate sine wave
	if(simulate_sine)
	{		
		printf("Simulating sine wave of %d Hz\n", freq_sine);		
		double amplitude = 0.5;
		double samples_per_sine = (double) sample_rate / freq_sine;
		for(int i=0; i<num_samples; i++)
		{
			data_norm[i] = sin((double) i / samples_per_sine * 2 * PI) * amplitude;
		}
		freq_sine = (freq_sine + 100) % (sample_rate / 2);
	}
	
	/*
	// low pass filter
	double freq_cutoff = 10000;
	double RC = 1.0 / (freq_cutoff * 2 * PI);
	double dt = 1.0 / sample_rate;
	double alpha = dt / (RC + dt);
	for(int i=1; i<num_samples; i++)
	{
		data_norm[i] = data_norm[i-1] + (alpha * (data_norm[i] - data_norm[i-1]));
	}
	*/
	
	// apply window function
	for(int i=0; i<num_samples; i++)
	{
		// Hanning
		data_norm[i] *= 0.5 * (1 - cos( 2*PI*i / (num_samples-1) ));
		// Hamming
		//data_norm[i] *= 0.54 - 0.46 * cos( 2*PI*i / (num_samples-1) );
	}
	
	// convert to complex array
	Complex	data_complex[num_samples];
	for(int i=0; i<num_samples; i++)
	{
		data_complex[i] = data_norm[i];	
	}
    CArray buf(data_complex, num_samples);
    
	// apply FFT
    fft(buf);

	// ignore upper half (Shannon-Nyquist)
	num_samples /= 2;

	// transform into magnitude
	double buf_magn[num_samples];
	for(int i=0; i<num_samples; i++)
	{
		buf_magn[i] = sqrt(real(buf[i]) * real(buf[i]) + imag(buf[i]) * imag(buf[i]));
	}
	
	// combine into bins (equal bin size)
	double buf_bins[num_bins];
	int samples_per_bin = num_samples / num_bins;
	for(int i=0; i<num_bins; i++)
	{
		// sum
		buf_bins[i] = 0;
		for(int j=0; j<samples_per_bin; j++)
			buf_bins[i] += buf_magn[i * samples_per_bin + j];
	}

	/*
	// combine into bins (bin size doubles)
	double buf_bins[num_bins];
	int index = 0;
	int index_next;
	for(int i=0; i<num_bins; i++)
	{
		// set range
		if(i > 0)
			index = index_next;
		index_next = round(num_samples / pow(2, num_bins - (i+1)));
		// sum
		buf_bins[i] = 0;
		for(int j=index; j<index_next; j++)
			buf_bins[i] += buf_magn[j];
		// DEBUG
		//printf("bin %d from %d to %d\n", i, index, index_next - 1);
	}
	*/
	
	// normalise & limit bins
    for(int i=0; i<num_bins; i++)
    {
		buf_bins[i] /= num_samples;
		if(buf_bins[i] > 1.0)
		{
			buf_bins[i] = 1.0;
		}
    }
	
	// write to memory
	for(int i=0; i<num_bins; i++)
	{
		fProxyToALMemory.insertData(fALMemoryKeys[i], (float) buf_bins[i]);
	}
	
	// DEBUG
	for(int i=0; i<num_bins; i++)
	{
		printf("%.1f   ", buf_bins[i]);
	}
	printf("\n");
	
	// DEBUG
	clock_t clockEnd = clock();
	printf("processing took %.1f ms\n", (double) (clockEnd - clockBegin) / CLOCKS_PER_SEC * 1000);
		
}



// Cooley–Tukey FFT (in-place)
void ALSoundSpectrum::fft(CArray& x)
{
    const size_t N = x.size();
    if (N <= 1) return;
 
    // divide
    CArray even = x[std::slice(0, N/2, 2)];
    CArray  odd = x[std::slice(1, N/2, 2)];
 
    // conquer
    fft(even);
    fft(odd);
 
    // combine
    for (size_t k = 0; k < N/2; ++k)
    {
        Complex t = std::polar(1.0, -2 * PI * k / N) * odd[k];
        x[k    ] = even[k] + t;
        x[k+N/2] = even[k] - t;
    }
}
