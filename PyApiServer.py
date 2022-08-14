import asyncio
import jiagu
from fastapi import FastAPI, status, Form
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
import BpmDetector
import vits_process

# Do initialization here
api = FastAPI()


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
    path = await loop.run_in_executor(None, vits_process.doTTS, text)
    return {'status': True, 'data': path}


# Api end

if __name__ == '__main__':
    import uvicorn

    uvicorn.run(app=api, host="0.0.0.0", port=10090)
