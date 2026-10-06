"""
AuditLens risk classifier service (local stand-in for the Lambda function).

Run from the repo root:
    $env:AUDITLENS_MODEL_DIR = "results\\checkpoint-41"     # folder containing config.json + model weights
    uvicorn nlp.serve:app --port 8000

Endpoints
    GET  /health    -> service status and label order
    POST /classify  -> {"text": "<model input>"}  returns risk_label, confidence, probabilities

The input must be built exactly as in training (concatenated risk-clause snippets).
The portal sends the prepared "model_input" from each CUAD .snippets.json file.
"""
import os
from contextlib import asynccontextmanager

import torch
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from transformers import AutoModelForSequenceClassification, AutoTokenizer

MODEL_DIR = os.environ.get("AUDITLENS_MODEL_DIR", "results/checkpoint-41")
BASE_TOKENIZER = os.environ.get("AUDITLENS_TOKENIZER", "bert-base-uncased")
# fine_tune.py saves id2label {0: Low, 1: Medium, 2: High} in config.json; that is used when present.
LABELS = os.environ.get("AUDITLENS_LABELS", "Low,Medium,High").split(",")
MAX_LEN = 512

state = {}


@asynccontextmanager
async def lifespan(_app):
    try:
        tokenizer = AutoTokenizer.from_pretrained(MODEL_DIR)
    except Exception:
        # Trainer checkpoints often contain no tokenizer files; fall back to the base tokenizer.
        tokenizer = AutoTokenizer.from_pretrained(BASE_TOKENIZER)
    model = AutoModelForSequenceClassification.from_pretrained(MODEL_DIR)
    model.eval()
    global LABELS
    id2label = getattr(model.config, "id2label", None) or {}
    if id2label and not str(id2label.get(0, "")).startswith("LABEL_"):
        LABELS = [id2label[i] for i in range(len(id2label))]
    if model.config.num_labels != len(LABELS):
        raise RuntimeError(f"Model has {model.config.num_labels} labels but LABELS has {len(LABELS)}")
    state.update(tokenizer=tokenizer, model=model)
    yield
    state.clear()


app = FastAPI(title="AuditLens classifier", lifespan=lifespan)


class ClassifyRequest(BaseModel):
    text: str


@app.get("/health")
def health():
    return {"status": "ok", "model_dir": MODEL_DIR, "labels": LABELS}


@app.post("/classify")
def classify(req: ClassifyRequest):
    if not req.text.strip():
        raise HTTPException(status_code=400, detail="text is empty")
    enc = state["tokenizer"](req.text, truncation=True, max_length=MAX_LEN, return_tensors="pt")
    with torch.no_grad():
        logits = state["model"](**enc).logits[0]
    probs = torch.softmax(logits, dim=-1).tolist()
    best = max(range(len(probs)), key=probs.__getitem__)
    return {
        "risk_label": LABELS[best],
        "confidence": round(probs[best], 4),
        "probabilities": {LABELS[i]: round(p, 4) for i, p in enumerate(probs)},
        "truncated": int(enc["input_ids"].shape[1]) >= MAX_LEN,
    }
