from __future__ import division, print_function

import numpy as np
import soundfile as sf
import pyworld as pw
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('file')
parser.parse_args()


def doShiftToOneFreq(x, fs, targetFreq):
    _f0_h, t_h = pw.harvest(x, fs)
    f0_h = pw.stonemask(x, _f0_h, t_h, fs)
    for i in range(len(f0_h)):
        f0_h[i] = targetFreq
    sp_h = pw.cheaptrick(x, f0_h, t_h, fs)
    ap_h = pw.d4c(x, f0_h, t_h, fs)
    y_h = pw.synthesize(f0_h, sp_h, ap_h, fs, pw.default_frame_period)
    return y_h


def main(args):
    file, samplingRate = sf.read(args.file)
    if type(file[0]) == np.ndarray:
        file = np.array([i[0] for i in file])  # Stereo to Mono
    targetFreq = 220.0 * (2 ** (3 / 12))
    result = doShiftToOneFreq(file, samplingRate, targetFreq)
    sf.write(args.file + '.result.wav', result, samplingRate)


if __name__ == '__main__':
    main(parser.parse_args())
