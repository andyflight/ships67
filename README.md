# Shipmaster

Мобільна 3D-гра (Android + iOS): гравець має корабель, ставить на нього апгрейди (деталі),
потім еволюціонує його до наступного тиру або купує новий; корабель можна перефарбувати.

## Структура

```
shipmaster/
├── client/     Unity-проєкт (Unity 6000.3.27f2, URP) — див. client/README.md
├── assets/     вихідники з Blender (.blend, текстури-джерела, референси)
├── openspec/   спільні специфікації та зміни (OpenSpec)
└── .claude/    команди /opsx:* та скіли OpenSpec для Claude Code
```

| Хто | Працює в | Що робить |
|---|---|---|
| Unity-розробник | `client/`, `openspec/` | пропозиції змін, імпорт, префаби, код гри, перевірка арту |
| Blender-художник | `assets/` | моделі й текстури через Claude + Blender MCP; експорт FBX/PNG у `client/Assets/_Project/Art/` (більше нічого в `client/` не змінює) |

## Підготовка (один раз)

1. **Git LFS** — великі файли (.blend, .fbx, .png, звук) зберігаються в LFS, у git лише вказівники:
   ```bash
   brew install git-lfs    # або https://git-lfs.com
   git lfs install
   ```
2. **Клонувати** репозиторій (після `git lfs install`, щоб файли завантажились повністю).
3. **OpenSpec CLI** (потрібен Node.js ≥ 22.12):
   ```bash
   npm install -g @fission-ai/openspec@latest
   openspec --version
   ```
4. **Claude Code** — запускати **з кореня репозиторію** (`shipmaster/`), тоді підхоплюються команди `/opsx:*`.
5. Художнику — Blender + підключений до Claude Blender MCP. Unity-розробнику — Unity 6000.3.27f2 і `unity` CLI.

## Як створюється ассет

Правила для арту: `openspec/specs/art-pipeline/spec.md` і `openspec/specs/art-ship-modularity/spec.md`.
Числа (трикутники, розміри текстур, габарити): `client/Assets/_Project/Scripts/Editor/ArtBudgets.json`.

1. **Unity-розробник:** `/opsx:propose add-<asset>` → переглянути `openspec/changes/<change>/` → commit → push.
2. **Художник:** pull → `/opsx:apply <change>` → виконує лише задачі `[blender]`:
   модель, UV, LOD, колайдери, сокети, текстури → перевірка за специфікаціями й `ArtBudgets.json` → експорт у
   `client/Assets/_Project/Art/` → commit (FBX/PNG + .blend в `assets/`) → push.
3. **Unity-розробник:** pull → Unity імпортує й пише помилки в консоль → перевірка:
   ```bash
   cd client && unity test . --mode EditMode --filter Shipmaster.Tests.ArtAssetsTests
   ```
   Є помилки → повернути художнику зі списком. Немає → задачі `[unity]` (префаб, підключення в гру, перевірка в Play).
4. **Unity-розробник:** `/opsx:archive <change>` — контракт ассета переходить у `openspec/specs/`.

Імпортовані FBX/PNG вручну не правимо — виправляємо в Blender і експортуємо заново.

## Git

- Гілки: `master` — стабільна; фічі — окремі гілки, мерж через `develop` (коли з'явиться).
- `.blend` комітимо на етапах (не після кожного збереження): кожна версія займає місце в LFS.
- Один `.blend` редагує одна людина одночасно — бінарні файли не мерджаться.
