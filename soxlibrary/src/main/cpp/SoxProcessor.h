//
// Created by boyo on 18-7-12.
//
#include "sox.h"
#include <stdlib.h>
#include <stdio.h>
#include <assert.h>

#ifndef RECORD_ANDROID_SOXPROCESSOR_H
#define RECORD_ANDROID_SOXPROCESSOR_H

#endif //RECORD_ANDROID_SOXPROCESSOR_H

class SoxProcessor {
public:
    SoxProcessor();

    ~SoxProcessor();

public:
    void init();

    int processFile(char *inFile, char *outFile, int reverberance, int HFDamping, int roomScale,
                    int stereoDepth,
                    int preDelay, int wetGain);

    int processBuffer(char *inBuffer, int inSize, char *outBuffer, int sampleRate, int channel,
                      int reverberance, int HFDamping,
                      int roomScale,
                      int stereoDepth,
                      int preDelay, int wetGain);

    void uninit();

private:
    sox_format_t *in, *out;
    sox_effects_chain_t *chain;
    sox_effect_t *reverbEffect;
};