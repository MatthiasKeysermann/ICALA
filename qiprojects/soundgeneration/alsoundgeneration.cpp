#include "alsoundgeneration.h"



using namespace AL;

ALSoundGeneration::ALSoundGeneration(boost::shared_ptr<ALBroker> pBroker, const std::string& pName ): ALModule(pBroker, pName )
{
	setModuleDescription("Generate sine waves of different frequencies and play on speakers.");
	
	// define callable methods with there description
	functionName("playSineWaves", getName(), "generates sine waves of different frequencies and plays on speakers");
	BIND_METHOD(ALSoundGeneration::playSineWaves);
	
	audioDeviceProxy = getParentBroker()->getProxy("ALAudioDevice");
		
	// set parameters
	sample_rate = 16000;
	num_samples = 3200; // 200 ms
	num_bins = 16;
	
	// create ALMemory keys
	for(int i=0; i<num_bins; i++)
	{
		std::string strI = boost::lexical_cast<std::string>(i);
		std::string str = "ALSoundGeneration/mono" + strI;
		fALMemoryKeys.push_back(str);		
	}
	
	playSineWaves();
}



ALSoundGeneration::~ALSoundGeneration()
{
}



void ALSoundGeneration::playSineWaves()
{
			
	// set output sample rate of audiodevice
	try
	{
		audioDeviceProxy->callVoid("setParameter", std::string("outputSampleRate"), sample_rate);
	}
	catch(ALError &e)
	{
		throw AL::ALError("ALSoundGeneration", "playSineWaves()", e.getDescription());
	}
	
	// initialise
	short* buffer_mono = new short[num_samples];
	short* buffer_stereo = new short[num_samples * 2];
	double* data_norm = new double[num_bins];
	double activation;
	int bin_size = sample_rate / 2 / num_bins;
	ALValue pDataBin;
    
	while (true)
	{
		
		// DEBUG
		clock_t clockBegin = clock();
		
		// read data from ALMemory
		for(int i=0; i<num_bins; i++)
		{
			try
			{
				ALValue value = fProxyToALMemory.getData(fALMemoryKeys[i]);
				data_norm[i] = (float) value;
			}
			catch(AL::ALError &e)
			{
				//throw ALError("ALSoundGeneration", "playSineWaves()", e.getDescription());
				data_norm[i] = 0.0;
			}
		}
		
		// read activation from ALMemory
		try
		{
			ALValue value = fProxyToALMemory.getData("ALSoundGeneration/activation");
			activation = (float) value;
		}
		catch(AL::ALError &e)
		{
			//throw ALError("ALSoundGeneration", "playSineWaves()", e.getDescription());
			activation = 0.0;
		}		
		
		// (de)amplify
		for(int i=0; i<num_bins; i++)
		{
			data_norm[i] *= activation; // use activation to set volume			
		}
		
		// DEBUG
		for(int i=0; i<num_bins; i++)
		{
			printf("%.1f   ", data_norm[i]);
		}
		printf("    activation: %.1f", activation);
		printf("\n");
				
		// clear audio buffer
		for(int j=0; j<num_samples; j++)
		{
			buffer_mono[j] = 0;
		}
		
		// fill audio buffer
		int freq_sine;
		double amplitude;
		double samples_per_sine;
		double silencing_factor = 0.1; // DEBUG: silence a bit	
		for(int i=0; i<num_bins; i++)
		{
			freq_sine = i * bin_size; // equal bin size assumed
			freq_sine += bin_size / 2; // shift by half bin size
			amplitude = data_norm[i];			
			// threshold
			if(amplitude > 0.2)
			{
				samples_per_sine = (double) sample_rate / freq_sine;
				for(int j=0; j<num_samples; j++)
				{
					buffer_mono[j] += (short) round(sin((double) j / samples_per_sine * 2 * PI) * silencing_factor * amplitude * SHRT_MAX);
				}
			}
		}
		
		// limit audio buffer
		for(int j=0; j<num_samples; j++)
		{
			if(buffer_mono[j] > SHRT_MAX)
				buffer_mono[j] = SHRT_MAX;
			if(buffer_mono[j] < SHRT_MIN)
				buffer_mono[j] = SHRT_MIN;
		}
		
		// convert to stereo
		int i = 0;
		for(int j=0; j<num_samples; j++)
		{
			buffer_stereo[i] = buffer_mono[j];
			buffer_stereo[i+1] = buffer_mono[j];
			i += 2;
		}
		
		// transform to binary samples
		pDataBin.SetBinary(buffer_stereo, num_samples * sizeof(short) * 2);
		
		// send to audiodevice module
		try
		{
			audioDeviceProxy->call<bool>("sendRemoteBufferToOutput", num_samples, pDataBin);
		}
		catch(AL::ALError &e)
		{
			throw ALError("ALSoundGeneration", "playSineWaves()", e.getDescription());
		}

		// DEBUG
		clock_t clockEnd = clock();
		printf("processing took %.1f ms\n", (double) (clockEnd - clockBegin) / CLOCKS_PER_SEC * 1000);		
		
	}
	
}
