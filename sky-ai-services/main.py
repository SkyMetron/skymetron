from __future__ import annotations

from typing import List

import httpx
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from embeddings.service import (
    EMBEDDING_DIMENSIONS,
    EMBEDDING_MODEL,
    embed_batch,
    embed_text,
)

app = FastAPI(
    title="SkyMetron AI Services",
    description="Python AI microservice: embeddings (Ollama nomic-embed-text), OCR, NLP.",
    version="0.1.0",
)


class EmbeddingRequest(BaseModel):
    text: str = Field(..., min_length=1, description="Text to embed.")


class EmbeddingBatchRequest(BaseModel):
    texts: List[str] = Field(..., min_length=1, description="Texts to embed.")


class EmbeddingResponse(BaseModel):
    embedding: List[float]
    model: str
    dimensions: int


class EmbeddingBatchResponse(BaseModel):
    embeddings: List[List[float]]
    model: str
    dimensions: int


class HealthResponse(BaseModel):
    status: str
    embedding_model: str
    embedding_dimensions: int


@app.get("/health", response_model=HealthResponse, tags=["meta"])
async def health() -> HealthResponse:
    return HealthResponse(
        status="ok",
        embedding_model=EMBEDDING_MODEL,
        embedding_dimensions=EMBEDDING_DIMENSIONS,
    )


@app.post("/api/embeddings", response_model=EmbeddingResponse, tags=["embeddings"])
async def post_embedding(request: EmbeddingRequest) -> EmbeddingResponse:
    try:
        vector = await embed_text(request.text)
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail=f"Ollama unreachable: {exc}") from exc
    return EmbeddingResponse(
        embedding=vector, model=EMBEDDING_MODEL, dimensions=len(vector)
    )


@app.post("/api/embeddings/batch", response_model=EmbeddingBatchResponse, tags=["embeddings"])
async def post_embedding_batch(request: EmbeddingBatchRequest) -> EmbeddingBatchResponse:
    try:
        vectors = await embed_batch(request.texts)
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail=f"Ollama unreachable: {exc}") from exc
    return EmbeddingBatchResponse(
        embeddings=vectors, model=EMBEDDING_MODEL, dimensions=len(vectors[0]) if vectors else 0
    )
