from fastapi import FastAPI, File, UploadFile
from paddleocr import PaddleOCR
import io

app = FastAPI(title="Clinic OCR Service")
ocr = PaddleOCR(use_angle_cls=True, lang="ch", show_log=False)


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/ocr")
async def recognize(file: UploadFile = File(...)):
    content = await file.read()
    result = ocr.ocr(content, cls=True)
    lines = []
    if result and result[0]:
        for block in result[0]:
            if block and len(block) >= 2 and block[1]:
                lines.append(block[1][0])
    text = "\n".join(lines).strip()
    return {"text": text}
