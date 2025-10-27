from flask import Flask, request, jsonify
from flask_cors import CORS
import requests, random, time

app = Flask(__name__)
CORS(app)

TIMEOUT = 6
RETRIES = 2

# 🦥 Offline fallback facts
OFFLINE_FACTS = [
    "An octopus has three hearts and blue blood.",
    "A group of flamingos is called a 'flamboyance'.",
    "Elephants can recognize themselves in a mirror.",
    "Cows have best friends and can become stressed when separated.",
    "Sea otters hold hands while sleeping to avoid drifting apart.",
    "A snail can sleep for up to three years.",
    "Dolphins call each other by unique names (signature whistles).",
    "Ravens can solve puzzles and remember human faces.",
]


def _try_get(url, *, params=None):
    for attempt in range(RETRIES):
        try:
            r = requests.get(url, params=params, timeout=TIMEOUT)
            if r.ok:
                return r.json()
        except requests.RequestException:
            if attempt + 1 == RETRIES:
                break
            time.sleep(0.3)
    return None


def get_animal_fact():
    # 1️⃣ Zoo Animal API — случайное животное
    data = _try_get("https://zoo-animal-api.vercel.app/api/random")
    if data and isinstance(data, dict) and data.get("name"):
        name = data.get("name")
        candidates = []
        if data.get("diet"):
            candidates.append(f"{name} typically eats {data['diet'].lower()}.")
        if data.get("habitat"):
            candidates.append(f"{name} lives in {data['habitat'].lower()}.")
        if data.get("geo_range"):
            candidates.append(f"{name} can be found in {data['geo_range'].lower()}.")
        if data.get("lifespan"):
            candidates.append(f"{name} can live about {data['lifespan']} years.")
        if data.get("active_time"):
            candidates.append(f"{name} is mainly active during the {data['active_time'].lower()}.")
        if candidates:
            return random.choice(candidates)

    # 2️⃣ Some Random API fallback
    animals = ["cat", "dog", "fox", "panda", "koala", "bird"]
    random.shuffle(animals)
    for a in animals:
        d = _try_get(f"https://some-random-api.com/animal/{a}")
        if d and isinstance(d, dict) and d.get("fact"):
            return d["fact"].strip()

    # 3️⃣ Cat facts fallback
    d = _try_get("https://catfact.ninja/fact")
    if d and isinstance(d, dict) and d.get("fact"):
        return d["fact"].strip()

    # 4️⃣ Offline fallback
    return random.choice(OFFLINE_FACTS)


@app.route("/chat", methods=["POST"])
def chat():
    fact = get_animal_fact()
    return jsonify({"reply": fact})


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
