//
// Created by boyo on 18-7-12.
//

#include "SoxProcessor.h"


SoxProcessor::SoxProcessor() {

}

SoxProcessor::~SoxProcessor() {
}

void SoxProcessor::init() {
    assert(sox_init() == SOX_SUCCESS);
}

void SoxProcessor::uninit() {
    sox_quit();
}

int SoxProcessor::processFile(char *inFile, char *outFile, int reverberance, int HFDamping,
                              int roomScale,
                              int stereoDepth,
                              int preDelay, int wetGain) {
    in = sox_open_read(inFile, NULL, NULL, NULL);
    out = sox_open_write(outFile, &in->signal, NULL, NULL, NULL, NULL);

    chain = sox_create_effects_chain(&in->encoding, &out->encoding);

    /* Create an effects chain; some effects need to know about the input
   * or output file encoding so we provide that information here */
    chain = sox_create_effects_chain(&in->encoding, &out->encoding);

    sox_effect_t *e;
    /* The first effect in the effect chain must be something that can source
     * samples; in this case, we use the built-in handler that inputs
     * data from an audio file */
    e = sox_create_effect(sox_find_effect("input"));
    char *args[10];
    args[0] = (char *) in;
    assert(sox_effect_options(e, 1, args) == SOX_SUCCESS);
    /* This becomes the first `effect' in the chain */
    assert(sox_add_effect(chain, e, &in->signal, &in->signal) == SOX_SUCCESS);
    free(e);

//    e = sox_create_effect(sox_find_effect("echo"));
//    args[0] = "0.8";
//    args[1] = "0.88";
//    args[2] = "60";
//    args[3] = "0.4";
//    assert(sox_effect_options(e, 4, args) == SOX_SUCCESS);
//    /* This becomes the first `effect' in the chain */
//    assert(sox_add_effect(chain, e, &in->signal, &in->signal) == SOX_SUCCESS);
//    free(e);

    reverbEffect = sox_create_effect(sox_find_effect("reverb"));
    char *wetOnly = "-w";
    char reverberanceChar[5];
    sprintf(reverberanceChar, "%d", reverberance);
    char hfDampingChar[5];
    sprintf(hfDampingChar, "%d", HFDamping);
    char roomScaleChar[5];
    sprintf(roomScaleChar, "%d", roomScale);
    char stereoDepthChar[5];
    sprintf(stereoDepthChar, "%d", stereoDepth);
    char preDelayChar[5];
    sprintf(preDelayChar, "%d", preDelay);
    char wetGainChar[5];
    sprintf(wetGainChar, "%d", wetGain);
    char *argss[] = {reverberanceChar, hfDampingChar, roomScaleChar,
                     stereoDepthChar, preDelayChar, wetGainChar};
    assert(sox_effect_options(reverbEffect, 0, argss) == SOX_SUCCESS);

    assert(sox_add_effect(chain, reverbEffect, &in->signal, &in->signal) == SOX_SUCCESS);
    free(reverbEffect);

    e = sox_create_effect(sox_find_effect("output"));
    args[0] = (char *) out;
    assert(sox_effect_options(e, 1, args) == SOX_SUCCESS);
    assert(sox_add_effect(chain, e, &in->signal, &in->signal) == SOX_SUCCESS);
    free(e);

    sox_flow_effects(chain, NULL, NULL);

    sox_delete_effects_chain(chain);
    sox_close(out);
    sox_close(in);
    return 0;
}

int SoxProcessor::processBuffer(char *inBuffer, int inSize, char *outBuffer, int sampleRate,
                                int channel,
                                int reverberance,
                                int HFDamping,
                                int roomScale,
                                int stereoDepth,
                                int preDelay, int wetGain) {
    sox_signalinfo_t *signalinfo = new sox_signalinfo_t();
    signalinfo->rate = sampleRate;
    signalinfo->channels = channel;
    signalinfo->precision = SOX_DEFAULT_PRECISION;
    sox_encodinginfo_t *encodinginfo = new sox_encodinginfo_t();
    encodinginfo->encoding = SOX_ENCODING_SIGN2;
    encodinginfo->bits_per_sample = signalinfo->precision;
    encodinginfo->reverse_bytes = sox_option_default;
    encodinginfo->reverse_nibbles = sox_option_default;
    encodinginfo->reverse_bits = sox_option_default;
    assert(in = sox_open_mem_read(inBuffer, inSize, signalinfo, encodinginfo, NULL));
    size_t tmpBufferSize;
    assert(out = sox_open_memstream_write(&outBuffer, &tmpBufferSize, &in->signal, NULL, "sox",
                                          NULL));

    chain = sox_create_effects_chain(&in->encoding, &out->encoding);

    /* Create an effects chain; some effects need to know about the input
   * or output file encoding so we provide that information here */
    chain = sox_create_effects_chain(&in->encoding, &out->encoding);

    sox_effect_t *e;
    /* The first effect in the effect chain must be something that can source
     * samples; in this case, we use the built-in handler that inputs
     * data from an audio file */
    e = sox_create_effect(sox_find_effect("input"));
    char *args[10];
    args[0] = (char *) in;
    assert(sox_effect_options(e, 1, args) == SOX_SUCCESS);
    /* This becomes the first `effect' in the chain */
    assert(sox_add_effect(chain, e, &in->signal, &in->signal) == SOX_SUCCESS);
    free(e);

    reverbEffect = sox_create_effect(sox_find_effect("reverb"));
    char *wetOnly = "-w";
    char reverberanceChar[5];
    sprintf(reverberanceChar, "%d", reverberance);
    char hfDampingChar[5];
    sprintf(hfDampingChar, "%d", HFDamping);
    char roomScaleChar[5];
    sprintf(roomScaleChar, "%d", roomScale);
    char stereoDepthChar[5];
    sprintf(stereoDepthChar, "%d", stereoDepth);
    char preDelayChar[5];
    sprintf(preDelayChar, "%d", preDelay);
    char wetGainChar[5];
    sprintf(wetGainChar, "%d", wetGain);
    char *argss[] = {reverberanceChar, hfDampingChar, roomScaleChar,
                     stereoDepthChar, preDelayChar, wetGainChar};
    assert(sox_effect_options(reverbEffect, 0, argss) == SOX_SUCCESS);

    assert(sox_add_effect(chain, reverbEffect, &in->signal, &in->signal) == SOX_SUCCESS);
    free(reverbEffect);

    e = sox_create_effect(sox_find_effect("output"));
    args[0] = (char *) out;
    assert(sox_effect_options(e, 1, args) == SOX_SUCCESS);
    assert(sox_add_effect(chain, e, &in->signal, &in->signal) == SOX_SUCCESS);
    free(e);

    sox_flow_effects(chain, NULL, NULL);

    sox_delete_effects_chain(chain);
    sox_close(out);
    sox_close(in);

    return tmpBufferSize;
}


