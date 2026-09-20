FROM python:3.11-slim

RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir piper-tts flask

# Bangla (bn_BD) voice, verified present in the rhasspy/piper-voices catalog on Hugging Face.
RUN mkdir -p /voices && \
    curl -fsSL -o /voices/bn_BD-google-medium.onnx \
        https://huggingface.co/rhasspy/piper-voices/resolve/main/bn/bn_BD/google/medium/bn_BD-google-medium.onnx && \
    curl -fsSL -o /voices/bn_BD-google-medium.onnx.json \
        https://huggingface.co/rhasspy/piper-voices/resolve/main/bn/bn_BD/google/medium/bn_BD-google-medium.onnx.json

WORKDIR /app
COPY server.py .

EXPOSE 5002
CMD ["python", "server.py"]
