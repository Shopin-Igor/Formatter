import subprocess
from pathlib import Path

import pytest
from .loader import load_cases


ROOT = next(p for p in Path(__file__).resolve().parents if (p / "pyproject.toml").exists())

YAML_PATH = ROOT / "src" / "test" / "resources" / "tests.yaml"
RULE_PATH = ROOT / "src" / "main" / "resources" / "rules" / "java-format.yml"  #пока пустой


cases = load_cases(YAML_PATH)


@pytest.mark.parametrize("case", cases, ids=[c.get("name", f"case_{i}") for i, c in enumerate(cases, 1)])
def test_formatter_case(case):
    input_code = case["input"]
    expected = case["expected"]


    proc = subprocess.run(
        [
            "sg",
            "run",
            "--rule",
            str(RULE_PATH),
        ],
        input=input_code.encode("utf-8"),
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )

    actual = proc.stdout.decode("utf-8")

    # правила не настроены => тут будет падать как раз
    assert actual == expected
