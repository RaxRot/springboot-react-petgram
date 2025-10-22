from flask import Flask, request, jsonify
from flask_cors import CORS
from transformers import pipeline
import re
import random

app = Flask(_name_)
CORS(app, resources={r"/": {"origins": ""}})  # allow requests from React

# 💡 Russian-English lightweight model
chatbot = pipeline("text-generation", model="ai-forever/rugpt3small_based_on_gpt2")

@app.route("/chat", methods=["POST"])
def chat():
    msg = request.json["message"]

    # 🐾 detect language
    if re.search("[а-яА-ЯёЁ]", msg):
        prompt = f"Отвечай по-русски коротко и мило на: {msg}"
        ending = random.choice([" мяу!", " мур!", " мяяяу~"])
    else:
        prompt = f"Reply briefly and kindly in English to: {msg}"
        ending = random.choice([" meow!", " purr~", " meooow!"])

    # 🧠 generate text
    result = chatbot(prompt, max_new_tokens=60)
    text = result[0]["generated_text"]

    # clean
    if ":" in text:
        text = text.split(":", 1)[-1].strip()

    reply = text + ending
    return jsonify({"reply": reply})

if _name_ == "_main_":
    app.run(port=5000)