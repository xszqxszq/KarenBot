from fastapi import FastAPI, HTTPException, status, Form
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from typing import Optional
import io
import jiagu
import base64
import langid
import functools
import asyncio
import math
from concurrent.futures import ThreadPoolExecutor
from pypinyin import pinyin, Style
from PIL import Image

api = FastAPI()

@api.exception_handler(RequestValidationError)
async def doHandleValidationError(request, exc):
	return JSONResponse(
		status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
		content={'status': False, 'message': 'field required'}
	)

async def getDecodedText(text: str, encode: str):
	encode = encode.lower()
	if encode in ['utf-8', 'utf8', 'unicode']:
		text = text.encode('utf-8').decode('unicode_escape')
	elif encode == 'base64':
		text = base64.b64decode(text).decode('utf-8')
	return text

@api.post('/sentiment')
async def doHandleSentiment(text: str = Form(...), encode: str = Form(...)):
	result = jiagu.sentiment(await getDecodedText(text, encode))
	positive = False
	if str(result[0]) == 'positive':
		positive = True
	return {'status': True, 'data': {'positive': positive, 'value': result[1]}}

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
	before = Image.open(io.BytesIO(base64.b64decode(image)))
	loop = asyncio.get_event_loop()
	result = await loop.run_in_executor(None, spherize, before)
	resultBytes = io.BytesIO()
	result.save(resultBytes, format='PNG')
	return {'status': True, 'data': base64.b64encode(resultBytes.getvalue())}

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

if __name__ == '__main__':
	import uvicorn
	uvicorn.run(app=api, host="0.0.0.0", port=10090)
