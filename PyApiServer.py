from fastapi import FastAPI, HTTPException, status, Form
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from typing import Optional
import io
import copy
import base64
import langid
import functools
import asyncio
import array
import math
import BpmDetector
import paddleocr
import cv2
import os.path
import numpy as np
from sklearn.cluster import KMeans
from senta import Senta
from concurrent.futures import ThreadPoolExecutor
from pypinyin import pinyin, Style
from PIL import Image

api = FastAPI()

ocr = paddleocr.PaddleOCR(use_angle_cls=True, use_gpu=False)
senta = Senta()
senta.init_model(model_class="ernie_1.0_skep_large_ch", task="sentiment_classify", use_cuda=False)

@api.exception_handler(RequestValidationError)
async def doHandleValidationError(request, exc):
	return JSONResponse(
		status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
		content={'status': False, 'message': 'field required'}
	)

@api.post('/language')
async def doDetectLanguage(text: str = Form(...), encode: str = Form(...)):
	text = await getDecodedText(text, encode)
	return {'status': True, 'data': langid.classify(text)[0]}

@api.post('/pinyin')
async def doGetPinyin(text: str = Form(...), encode: str = Form(...)):
	text = await getDecodedText(text, encode)
	return {'status': True, 'data': ''.join([i[0] for i in pinyin(text.strip(), style=Style.FIRST_LETTER)])}

@api.post('/spherize')
async def doSpherizeImage(image: str = Form(...)):
	before = Image.open(io.BytesIO(base64.b64decode(image))) # Base64 only
	loop = asyncio.get_event_loop()
	result = await loop.run_in_executor(None, spherize, before)
	resultBytes = io.BytesIO()
	result.save(resultBytes, format='PNG')
	return {'status': True, 'data': base64.b64encode(resultBytes.getvalue())}

@api.post('/bpm')
async def getBPM(audio: str = Form(...)):
	if ':\\' in audio:
		audio = await convertWindowsPath(audio)
	loop = asyncio.get_event_loop()
	bpm = await loop.run_in_executor(None, BpmDetector.detectWav, audio)
	return {'status': True, 'data': bpm}

@api.post('/sentiment')
async def doDetectSentiment(text: str = Form(...)):
	return {'status': True, 'data': senta.predict(text)[0][1] == 'positive'}

@api.post('/hair_color')
async def getHairColor(img: str = Form(...)):
	loop = asyncio.get_event_loop()
	result = await loop.run_in_executor(None, handleHairColor, img)
	return {'status': True, 'data': ';'.join([','.join(map(str, reversed(i))) for i in result])}

@api.post('/imsohappy')
async def doImSoHappy(image: str = Form(...)):
	before = cv2.imdecode(np.frombuffer(base64.b64decode(image), dtype=np.uint8), flags=1) # Base64 only
	loop = asyncio.get_event_loop()
	result1, result2 = await loop.run_in_executor(None, handleImSoHappy, before)
	return {'status': True, 'data': base64.b64encode(cv2.imencode('.jpg', result1)[1].tobytes()) + b';' + base64.b64encode(cv2.imencode('.jpg', result2)[1].tobytes())}

@api.post('/ocr')
async def getOCR(url: str = Form(...)):
	r = ""
	for i in ocr.ocr(url, cls=True):
		r += i[-1][0] + " "
	return {'status': True, 'data': r}

async def convertWindowsPath(path: str):
	parts = path.split(':\\')
	return '/mnt/' + parts[0].lower() + '/' + parts[1].replace('\\', '/')

async def getDecodedText(text: str, encode: str):
	encode = encode.lower()
	if encode in ['utf-8', 'utf8', 'unicode']:
		text = text.encode('utf-8').decode('unicode_escape')
	elif encode == 'base64':
		text = base64.b64decode(text).decode('utf-8')
	return text

# From https://github.com/QuinnSong/JPG-Tools/blob/5aaa7b6e068909e3b6b9d5b886be519d43163f31/src/spherize.py
def spherize(image):
	width, height = image.size
	midX, midY = width / 2, height / 2
	maxMid = max(midX, midY)
	if image.mode != "RGBA":
		image = image.convert("RGBA")
	pix = image.load()
	result = Image.new("RGBA", (width, height))
	resultPix = result.load()
	for w in range(width):
		for h in range(height):
			offsetX, offsetY = w - midX, h - midY
			radian = math.atan2(offsetY, offsetX)
			radius = (offsetX ** 2 + offsetY ** 2) / maxMid
			x, y = int(radius * math.cos(radian)) + midX, int(radius * math.sin(radian)) + midY
			x, y = min(max(x, 0), width - 1), min(max(y, 0), height - 1)
			resultPix[w, h] = pix[x, y]
	return result

# Reference: https://www.pyimagesearch.com/2014/05/26/opencv-python-k-means-color-clustering/
def centroidHistogram(clt):
    numLabels = np.arange(0, len(np.unique(clt.labels_)) + 1)
    (hist, _) = np.histogram(clt.labels_, bins = numLabels)
    hist = hist.astype("float")
    hist /= hist.sum()
    return hist
def getMainColor(hist, centroids):
    bar = np.zeros((50, 300, 3), dtype = "uint8")
    startX = 0
    return sorted(zip(hist, centroids), key=lambda x:-x[0])[0][1]

def handleHairColor(filename, cascade_file = "./lbpcascade_animeface.xml"):
    if not os.path.isfile(cascade_file):
        raise RuntimeError("%s: not found" % cascade_file)

    cascade = cv2.CascadeClassifier(cascade_file)
    image = cv2.imread(filename, cv2.IMREAD_COLOR)
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    gray = cv2.equalizeHist(gray)
    
    faces = cascade.detectMultiScale(gray,
                                     # detector options
                                     scaleFactor = 1.1,
                                     minNeighbors = 1,
                                     minSize = (24, 24))
    if len(faces) == 0:
    	faces = [(0, 0, image.shape[1], image.shape[0])]
    results = []
    for (x, y, w, h) in faces:
        now = image[y:y+h//4, x:x+w]
        now = now.reshape((now.shape[0] * now.shape[1], 3))
        clt = KMeans(n_clusters = 6)
        clt.fit(now)
        results.append(getMainColor(centroidHistogram(clt), clt.cluster_centers_))
    return results

def handleImSoHappy(image):
	w, h = image.shape[1], image.shape[0]
	image1, image2 = copy.deepcopy(image), copy.deepcopy(image)
	image1[0:h, w-w//2:w] = cv2.flip(image1[0:h, 0:w//2], 1)
	image2 = cv2.flip(image2, 1)
	image2[0:h, w-w//2:w] = cv2.flip(image2[0:h, 0:w//2], 1)
	return image1, image2

if __name__ == '__main__':
	import uvicorn
	uvicorn.run(app=api, host="0.0.0.0", port=10090)
