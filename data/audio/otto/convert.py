import glob
import os
for i in glob.glob('ysddTokens/*.wav'):
	output = i.replace('.wav', '_output.wav')
	os.system('ffmpeg -i {} -ar 24k -ac 1 {}'.format(i, output))
	os.remove(i)
	os.rename(output, i)