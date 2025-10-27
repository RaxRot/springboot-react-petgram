from flask import Flask, request, jsonify
from flask_cors import CORS
from transformers import AutoModelForCausalLM, AutoTokenizer
import torch
import re
import random

app = Flask(__name__)
CORS(app)

#Load dialogue model
model_name = "microsoft/DialoGPT-small"
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForCausalLM.from_pretrained(model_name)

@app.route("/chat", methods=["POST"])
def chat():
    msg = request.json.get("message", "").strip()
    if not msg:
        return jsonify({"reply": "Meow? Say something first 🐾"})

    # detect language
    if re.search("[а-яА-ЯёЁ]", msg):
        prompt = f"Ты милый котик. Отвечай по-русски коротко, мило и дружелюбно. Сообщение: {msg}"
        ending = random.choice([" мяу!", " мур~", " мяяяу!"])
    else:
        prompt = f"You are a cute, funny talking cat. Reply briefly and kindly to: {msg}"
        ending = random.choice([" meow!", " purr~", " nya~"])

    #generate
    inputs = tokenizer.encode(prompt + tokenizer.eos_token, return_tensors="pt")
    reply_ids = model.generate(
        inputs,
        max_length=80,
        pad_token_id=tokenizer.eos_token_id,
        do_sample=True,
        top_p=0.92,
        temperature=0.7,
    )
    reply = tokenizer.decode(reply_ids[:, inputs.shape[-1]:][0], skip_special_tokens=True)

    #clean
    reply = re.sub(r"[\r\n]+", " ", reply).strip()
    if len(reply) > 120:
        reply = reply[:120] + "..."
    return jsonify({"reply": reply + ending})


if __name__ == "__main__":
    app.run(port=5000)
