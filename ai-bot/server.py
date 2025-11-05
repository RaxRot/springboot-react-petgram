from flask import Flask, request, jsonify
from flask_cors import CORS
import io, requests, random
from PIL import Image

# === Torch / Vision ===
import torch
import torch.nn.functional as F
from torchvision.models import resnet50, ResNet50_Weights

app = Flask(__name__)
CORS(app, resources={r"/*": {"origins": r"http://localhost:*"}})

# ---------------- SIMPLE FUN FACTS (НЕ ТРОГАЛА) ----------------
OFFLINE_FACTS = [
    "An octopus has three hearts and blue blood.",
    "A group of flamingos is called a 'flamboyance'.",
    "Elephants can recognize themselves in a mirror.",
    "Cows have best friends and can become stressed when separated.",
    "Sea otters hold hands while sleeping to avoid drifting apart.",
    "A snail can sleep for up to three years.",
    "Dolphins call each other by unique names.",
    "Ravens can solve puzzles and remember human faces.",
]

@app.route("/chat", methods=["POST"])
def chat():
    return jsonify({"reply": random.choice(OFFLINE_FACTS)})

@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})

# -------------------- LOAD MODEL ONCE --------------------
# CPU ок; вес ~100МБ скачается при первом запуске
WEIGHTS = ResNet50_Weights.DEFAULT
MODEL = resnet50(weights=WEIGHTS)
MODEL.eval()
PREPROCESS = WEIGHTS.transforms()
LABELS = WEIGHTS.meta.get("categories", [])  # 1000 ImageNet labels

# ключевые слова для видов
DOG_KEYWORDS = {
    "retriever","shepherd","terrier","hound","pug","husky","malamute","mastiff",
    "pointer","collie","chihuahua","bulldog","doberman","bloodhound","dalmatian",
    "rottweiler","poodle","shihtzu","shih-tzu","pekinese","papillon","affenpinscher",
    "borzoi","schnauzer","spitz","whippet","saluki","basenji","weimaraner","samoyed",
    "labrador","golden","pinscher","briard","keeshond","springer","setters","corgi","beagle","boxer","dachshund","malinois"
}
CAT_KEYWORDS = {"cat","tabby","siamese","persian","egyptian","lynx","ocelot"}
RABBIT_KEYS = {"rabbit","hare","bunny"}
BIRD_KEYS = {"bird","parrot","macaw","cockatoo","king penguin","goose","duck","eagle","owl","sparrow","finch"}
HORSE_KEYS = {"horse","pony"}
FISH_KEYS = {"fish","goldfish","angelfish","clownfish","pike","tench","eel","ray","shark"}
HAMSTER_KEYS = {"hamster","rodent","guinea pig","marmot","beaver","mouse","rat","gerbil","chinchilla"}

def load_image_from_url(url: str, timeout=12) -> Image.Image:
    r = requests.get(url, timeout=timeout, stream=True)
    r.raise_for_status()
    return Image.open(io.BytesIO(r.content)).convert("RGB")

def softmax_topk(tensor, k=5):
    probs = F.softmax(tensor, dim=1)[0].detach().cpu().tolist()
    topk_idx = sorted(range(len(probs)), key=lambda i: probs[i], reverse=True)[:k]
    return [(LABELS[i], probs[i]) for i in topk_idx]

def normalize_label(lbl: str) -> str:
    # приведение лейбла к удобному виду
    return lbl.replace("_", " ").lower()

def choose_species_from_label(lbl: str):
    L = normalize_label(lbl)
    # простая эвристика по ключевым словам
    if any(k in L for k in ("dog","puppy","canine")) or any(k in L for k in DOG_KEYWORDS):
        return "dog"
    if any(k in L for k in CAT_KEYWORDS):
        return "cat"
    if any(k in L for k in RABBIT_KEYS):
        return "rabbit"
    if any(k in L for k in BIRD_KEYS):
        return "bird"
    if any(k in L for k in HORSE_KEYS):
        return "horse"
    if any(k in L for k in FISH_KEYS):
        return "fish"
    if any(k in L for k in HAMSTER_KEYS):
        return "hamster"
    return None

@app.route("/analyze", methods=["POST"])
def analyze():
    body = request.get_json(silent=True) or {}
    image_url = body.get("imageUrl")
    if not image_url:
        return jsonify({"error": "imageUrl is required"}), 400

    try:
        img = load_image_from_url(image_url)
    except Exception as e:
        return jsonify({"error": "image_download_failed", "detail": str(e)}), 422

    try:
        inp = PREPROCESS(img).unsqueeze(0)  # [1,3,224,224]
        with torch.no_grad():
            logits = MODEL(inp)
        top5 = softmax_topk(logits, k=5)  # [(label, prob), ...]
    except Exception as e:
        return jsonify({"error": "ai_unavailable", "detail": str(e)}), 503

    # ищем первое животное из топ-5, которое мы умеем маппить к виду
    species, breed, score = None, None, None
    for lbl, prob in top5:
        sp = choose_species_from_label(lbl)
        if sp is not None:
            species, breed, score = sp, lbl, float(prob)
            break

    if species is None:
        # модель ответила, но животное не распознано как поддерживаемое
        return jsonify({
            "error": "unknown_species",
            "ai_tags": [{"label": l, "score": round(p, 4)} for (l, p) in top5]
        }), 422

    # порог уверенности: если ниже — считаем неуверенным
    if score < 0.25:
        return jsonify({
            "error": "low_confidence",
            "ai_tags": [{"label": l, "score": round(p, 4)} for (l, p) in top5]
        }), 422

    # УСПЕХ — возвращаем в формате, который ждёт фронт
    return jsonify({
        "species": species,                        # dog / cat / ...
        "breed": breed,                            # как вернул AI (формат A)
        "score": round(score, 4),
        "ai_tags": [{"label": l, "score": round(p, 4)} for (l, p) in top5],
        "ai_service": "torch_resnet50_imagenet"
    }), 200

if __name__ == "__main__":
    print("🚀 AI Detector (ResNet50) running at http://localhost:5000")
    app.run(host="0.0.0.0", port=5000)
