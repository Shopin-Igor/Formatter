import yaml
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
YAML_PATH = ROOT / "src" / "test" / "resources" / "tests.yaml"

def load_cases(path: str | Path = None):
    if path is None:
        root = Path(__file__).resolve().parents[2]  # корень проекта
        path = root / "src" / "test" / "resources" / "tests.yaml"

    path = Path(path)
    data = yaml.safe_load(path.read_text(encoding="utf-8"))
    return data["cases"]