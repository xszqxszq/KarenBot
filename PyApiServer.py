import asyncio
import base64
import json
import os.path
import wave

import cv2
import imutils
import jiagu
import langid
import random
import numpy as np
from PIL import Image
from fastapi import FastAPI, status, Form
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from paddleocr import PaddleOCR
from pydub import AudioSegment, effects
from pydub.silence import split_on_silence
from sklearn.cluster import KMeans

import BpmDetector

# Do initialization here
api = FastAPI()
ocr = PaddleOCR(use_angle_cls=True, lang="ch")


# Api begin

@api.exception_handler(RequestValidationError)
async def doHandleValidationError(_request, _exc):
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content={'status': False, 'message': 'field required'}
    )


@api.post('/language')
async def doDetectLanguage(text: str = Form(...), encode: str = Form(...)):
    text = await getDecodedText(text, encode)
    return {'status': True, 'data': langid.classify(text)[0]}


@api.post('/bpm')
async def getBPM(audio: str = Form(...)):
    if ':\\' in audio:
        audio = await convertWindowsPath(audio)
    loop = asyncio.get_event_loop()
    bpm = await loop.run_in_executor(None, BpmDetector.detectWav, audio)
    return {'status': True, 'data': bpm}


@api.post('/sentiment')
async def doDetectSentiment(text: str = Form(...)):
    return {'status': True, 'data': jiagu.sentiment(text)[0] == 'positive'}


@api.post('/hair_color')
async def getHairColor(image: str = Form(...)):
    loop = asyncio.get_event_loop()
    result = await loop.run_in_executor(None, handleHairColor, image)
    return {'status': True, 'data': ';'.join([','.join(map(str, reversed(j))) for j in result])}


@api.post('/ocr')
async def getOCR(image: str = Form(...)):
    r, result = "", ocr.ocr(image, cls=True)
    if result is not None:
        for j in result:
            r += j[-1][0] + " "
    return {'status': True, 'data': r}


@api.post('/is_lt')
async def isLt(image: str = Form(...)):
    return {'status': True, 'data': str(detectLt(image)).lower()}


@api.post('/scan')
async def getScan(file: str = Form(...)):
    loop = asyncio.get_event_loop()
    result = await loop.run_in_executor(None, doScan, file)
    return {'status': True, 'data': result}


# Api end


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


# Based on https://www.pyimagesearch.com/2014/05/26/opencv-python-k-means-color-clustering/
def centroidHistogram(clt):
    numLabels = np.arange(0, len(np.unique(clt.labels_)) + 1)
    (hist, _) = np.histogram(clt.labels_, bins=numLabels)
    hist = hist.astype("float")
    hist /= hist.sum()
    return hist


def getMainColor(hist, centroids):
    return sorted(zip(hist, centroids), key=lambda x: -x[0])[0][1]


def handleHairColor(filename, cascade_file="./lbpcascade_animeface.xml"):
    if not os.path.isfile(cascade_file):
        raise RuntimeError("%s: not found" % cascade_file)

    cascade = cv2.CascadeClassifier(cascade_file)
    gif = Image.open(filename)
    if gif.format == 'GIF':
        cap = cv2.VideoCapture(filename)
        _, image = cap.read()
        cap.release()
    else:
        image = cv2.imread(filename, cv2.IMREAD_COLOR)
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    gray = cv2.equalizeHist(gray)

    faces = cascade.detectMultiScale(gray,
                                     # detector options
                                     scaleFactor=1.1,
                                     minNeighbors=1,
                                     minSize=(24, 24))
    if len(faces) == 0:
        faces = [(0, 0, image.shape[1], image.shape[0])]
    results = []
    for (x, y, w, h) in faces:
        now = image[y:y + h // 4, x:x + w]
        now = now.reshape((now.shape[0] * now.shape[1], 3))
        clt = KMeans(n_clusters=6)
        clt.fit(now)
        results.append(getMainColor(centroidHistogram(clt), clt.cluster_centers_))
    return results


def detectLt(path):
    template1 = cv2.Canny(cv2.cvtColor(cv2.imread("/share/bot/lt.jpg"), cv2.COLOR_BGR2GRAY), 50, 200)
    template2 = cv2.Canny(cv2.cvtColor(cv2.imread("/share/bot/lt2.jpg"), cv2.COLOR_BGR2GRAY), 50, 200)
    (tH1, tW1) = template1.shape[:2]
    (tH2, tW2) = template2.shape[:2]
    gif = Image.open(path)
    if gif.format == 'GIF':
        cap = cv2.VideoCapture(path)
        _, image = cap.read()
        cap.release()
    else:
        image = cv2.imread(path)
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    gsize = max(gray.shape[:2])
    tsize = 500
    if gsize >= tsize:
        k = tsize / gsize
        gray = cv2.resize(gray, tuple(map(int, (gray.shape[0] * k, gray.shape[1] * k))), cv2.INTER_AREA)
    percentage = 0.0
    for scale in np.linspace(0.12, 1.2, 60)[::-1]:
        for angle in np.linspace(-7.0, 7.0, 8)[::-1]:
            resized = imutils.rotate(imutils.resize(gray, width=int(gray.shape[1] * scale)), angle)
            edged = cv2.Canny(resized, 50, 200)
            if resized.shape[0] >= tH1 and resized.shape[1] >= tW1:
                result1 = cv2.minMaxLoc(cv2.matchTemplate(edged, template1, cv2.TM_CCOEFF_NORMED))
                if result1:
                    percentage = max(result1[1], percentage)
            if resized.shape[0] >= tH2 and resized.shape[1] >= tW2:
                result2 = cv2.minMaxLoc(cv2.matchTemplate(edged, template2, cv2.TM_CCOEFF_NORMED))
                if result2:
                    percentage = max(result2[1], percentage)
    return round(percentage, 2) >= 0.45


def doScan(path):
    target = '/share/bot/mod/' + random.choice(os.listdir('/share/bot/mod/'))
    os.system('ffmpeg -i ' + path + ' ' + path + '.wav')
    path = path + '.wav'
    effects.normalize(AudioSegment.from_wav(path)).export(path, format='wav')
    song = AudioSegment.from_wav(path)
    chunks = split_on_silence(
        song,
        min_silence_len = 100,
        silence_thresh = -16
    )
    effects.normalize(random.choice(chunks)).export(path + '_source.wav', format='wav')
    os.system('/share/bot/genmod ' + path + '_source.wav "' + target + '" > ' + path + '.pcm')
    os.system('ffmpeg -f s16le -ar 44.1k -ac 2 -i ' + path + '.pcm -y -t 35 ' + path + '_result.mp3')
    os.remove(path + '_source.wav')
    os.remove(path + '.pcm')
    os.remove(path)
    return path + '_result.mp3'


if __name__ == '__main__':
    import uvicorn

    uvicorn.run(app=api, host="0.0.0.0", port=10090)
