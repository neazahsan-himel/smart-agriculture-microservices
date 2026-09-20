import subprocess
import tempfile

from flask import Flask, request, Response, jsonify

app = Flask(__name__)
MODEL_PATH = "/voices/bn_BD-google-medium.onnx"


@app.route("/tts", methods=["POST"])
def tts():
    data = request.get_json(force=True, silent=True) or {}
    text = data.get("text", "")
    if not text.strip():
        return jsonify({"error": "text is required"}), 400

    with tempfile.NamedTemporaryFile(suffix=".wav") as tmp:
        proc = subprocess.run(
            ["piper", "--model", MODEL_PATH, "--output_file", tmp.name],
            input=text.encode("utf-8"),
            capture_output=True,
        )
        if proc.returncode != 0:
            return jsonify({"error": proc.stderr.decode("utf-8", "ignore")}), 500
        tmp.seek(0)
        audio_bytes = tmp.read()

    return Response(audio_bytes, mimetype="audio/wav")


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5002)
