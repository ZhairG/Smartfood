"""
Nombre: ca_coverage.py
Autor: SmartFood Team
Fecha: Octubre 2026
Descripción: Lista los códigos de trazabilidad (CA, RF, RNF y tareas Tnnn) que citan los nombres de las pruebas de la
app (`app/src/test` y `app/src/androidTest`), para comprobar que al reorganizar las pruebas ningún criterio quede sin
cubrir (#105). Reconoce las formas que usan los nombres: `CA-INV-01-02`, `caInv0102`, `CA_INV_01_02`, `ca_inv_01_02`,
`ca0201`, `caRec0301`, `t055`, `rnfInv04`, `rfInv01`… y las continuaciones de las pruebas por flujo (`caInv0101_0102`).

Uso:
    python tools/ca_coverage.py                  # imprime los códigos cubiertos
    python tools/ca_coverage.py --save BASE.txt  # guarda la lista
    python tools/ca_coverage.py --check BASE.txt # falla (código 1) si falta alguno de la lista
"""
import pathlib
import re
import sys

ROOTS = ["app/src/test", "app/src/androidTest"]

# Nombre de cada prueba: la función que sigue a @Test, entre comillas invertidas o en camelCase/snake_case.
TEST_NAME = re.compile(r"@Test\s*(?:@[\w.()\"= ]+\s*)*fun\s+(`[^`]+`|[\w]+)")

AREAS = {"inv": "INV", "ui": "UI", "rec": "REC", "leg": "LEG", "mail": "MAIL"}

# Un código puede seguir con más números de la misma área: `caInv0101_0102` son CA-INV-01-01 y CA-INV-01-02, y
# `ca0103_0105`, CA-01-03 y CA-01-05 (#105, pruebas por flujo).
MORE = r"((?:_\d{4}(?![0-9]))*)"
START = r"(?<![a-zA-Z0-9])"

PATTERNS = [
    # CA-INV-01-02, CA_INV_01_02, ca_inv_01_02, ca-ui-02-08
    (re.compile(START + r"(ca|rf|rnf)[-_](inv|ui|rec|leg|mail)[-_](\d{2})(?:[-_](\d{2}))?()", re.I), "area"),
    # caInv0102, rnfInv04, rfInv01, caRec0301, caLeg0101, caInv0101_0102
    (re.compile(r"(?<![a-z])(ca|rf|rnf)(Inv|Ui|Rec|Leg|Mail)(\d{2})(\d{2})?" + MORE), "area"),
    # CA-02-01, ca_05_03
    (re.compile(START + r"ca[-_](\d{2})[-_](\d{2})(?![0-9])()", re.I), "auth"),
    # ca0201, ca0103_0105
    (re.compile(START + r"ca(\d{2})(\d{2})(?![0-9])" + MORE), "auth"),
    # Tareas: T055, t055
    (re.compile(r"(?<![a-zA-Z])[tT](\d{3})(?![0-9])"), "task"),
]


def codes_in(name: str) -> set[str]:
    found = set()
    for regex, kind in PATTERNS:
        for m in regex.finditer(name):
            if kind == "area":
                prefix, area, a, b, more = m.groups()
                base = f"{prefix.upper()}-{AREAS[area.lower()]}"
                found.add(f"{base}-{a}" + (f"-{b}" if b else ""))
                found.update(f"{base}-{n[:2]}-{n[2:]}" for n in re.findall(r"\d{4}", more))
            elif kind == "auth":
                a, b, more = m.groups()
                found.add(f"CA-{a}-{b}")
                found.update(f"CA-{n[:2]}-{n[2:]}" for n in re.findall(r"\d{4}", more))
            else:
                found.add(f"T{m.group(1)}")
    return found


def coverage() -> dict[str, list[str]]:
    result: dict[str, list[str]] = {}
    for root in ROOTS:
        for path in sorted(pathlib.Path(root).rglob("*.kt")):
            text = path.read_text(encoding="utf-8")
            for m in TEST_NAME.finditer(text):
                for code in codes_in(m.group(1)):
                    result.setdefault(code, []).append(f"{path.stem}.{m.group(1).strip('`')}")
    return result


def main(argv: list[str]) -> int:
    cov = coverage()
    codes = sorted(cov)
    if len(argv) == 2 and argv[0] == "--save":
        pathlib.Path(argv[1]).write_text("\n".join(codes) + "\n", encoding="utf-8")
        print(f"{len(codes)} códigos guardados en {argv[1]}")
        return 0
    if len(argv) == 2 and argv[0] == "--check":
        base = [c for c in pathlib.Path(argv[1]).read_text(encoding="utf-8").split() if c]
        missing = [c for c in base if c not in cov]
        if missing:
            print("Códigos sin prueba:", ", ".join(missing))
            return 1
        print(f"OK: los {len(base)} códigos de la lista siguen cubiertos ({len(codes)} en total).")
        return 0
    for code in codes:
        print(f"{code}: {len(cov[code])}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
