import asyncio
import jiagu
from fastapi import FastAPI, status, Form
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from paddleocr import PaddleOCR
from PIL import Image
from sklearn.cluster import KMeans
from skimage import color
import torch
import numpy as np
import cv2

import BpmDetector
import vits_process

# Do initialization here
api = FastAPI()
ocr = PaddleOCR(use_angle_cls=True, lang='ch')
img_common = torch.hub.load('ultralytics/yolov5', 'custom', path='yolo/common.pt')
anime_face = torch.hub.load('ultralytics/yolov5', 'custom', path='yolo/yolov5s_anime.pt')


# Api begin

@api.exception_handler(RequestValidationError)
async def doHandleValidationError(_request, _exc):
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content={'status': False, 'message': 'field required'}
    )


@api.post('/bpm')
async def getBPM(audio: str = Form(...)):
    loop = asyncio.get_event_loop()
    bpm = await loop.run_in_executor(None, BpmDetector.detectWav, audio)
    return {'status': True, 'data': bpm}


@api.post('/sentiment')
async def doDetectSentiment(text: str = Form(...)):
    return {'status': True, 'data': jiagu.sentiment(text)[0] == 'positive'}


@api.post('/tts')
async def getTTS(text: str = Form(...)):
    loop = asyncio.get_event_loop()
    path = await loop.run_in_executor(None, vits_process.doTTS, text, 'ja')
    return {'status': True, 'data': path}


@api.post('/tts_cn')
async def getTTSCN(text: str = Form(...)):
    loop = asyncio.get_event_loop()
    path = await loop.run_in_executor(None, vits_process.doTTS, text, 'zh')
    return {'status': True, 'data': path}


@api.post('/ocr')
async def getOCR(path: str = Form(...)):
    loop = asyncio.get_event_loop()
    result = await loop.run_in_executor(None, doOCR, path)
    return {'status': True, 'data': result}


@api.post('/lt')
async def isLt(path: str = Form(...)):
    loop = asyncio.get_event_loop()
    result = await loop.run_in_executor(None, detectLt, path)
    return {'status': True, 'data': result}


@api.post('/blonde')
async def isBlonde(path: str = Form(...)):
    loop = asyncio.get_event_loop()
    result = await loop.run_in_executor(None, detectBlonde, path)
    return {'status': True, 'data': result}


# Api end


def doOCR(path):
    return '\n'.join([line[1][0] for line in ocr.ocr(path, cls=True)])


def detectLt(path):
    img = Image.open(path)
    if img.size[0] < 100 or img.size[1] < 100:
        return False
    for area in img_common(img).xyxy[0]:
        if area[5] == 0 and area[4] > 0.8:
            return True
    return False


blondeColors = [
    [90.73948779100502, -4.477169141775095, 42.932090784625785],
    [96.01817010098311, -7.205140542378363, 28.57951059226316],
    [81.3990919790363, 10.1113281133971, 23.247659861636638],
    [95.85821436038806, -19.323659509977777, 67.48143654894507]
]


def detectBlonde(path):
    img = cv2.imread(path, cv2.IMREAD_COLOR)
    for (x1, y1, x2, y2, p, _) in anime_face(img).xyxy[0]:
        if p < 0.7:
            continue
        if y1 <= 20:
            y1 = 1
        else:
            y1 -= 20
        now = img[y1:y1+(y2-y1)//4, x1:x2]
        now = now.reshape((now.shape[0] * now.shape[1], 3))
        clt = KMeans(n_clusters=6)
        clt.fit(now)
        fringe = color.rgb2lab(getMainColor(centroidHistogram(clt), clt.cluster_centers_))
        for c in blondeColors:
            if color.deltaE_cie76(fringe, c) < 100:
                return True
    return False


# Based on https://www.pyimagesearch.com/2014/05/26/opencv-python-k-means-color-clustering/
def centroidHistogram(clt):
    numLabels = np.arange(0, len(np.unique(clt.labels_)) + 1)
    (hist, _) = np.histogram(clt.labels_, bins=numLabels)
    hist = hist.astype("float")
    hist /= hist.sum()
    return hist


def getMainColor(hist, centroids):
    return sorted(zip(hist, centroids), key=lambda x: -x[0])[0][1]


if __name__ == '__main__':
    import uvicorn

    uvicorn.run(app=api, host="0.0.0.0", port=10090)
