# Shipmaster — client

Unity-клієнт мобільної 3D-гри Shipmaster (Android, iOS).

- Unity **6000.3.27f2** (6.3 LTS), URP
- Перед клонуванням: `git lfs install` (моделі, текстури й звуки зберігаються в Git LFS)

## Запуск

1. Відкрити `client/` у Unity Hub (або `unity open .`).
2. Відкрити сцену `Assets/_Project/Scenes/Bootstrap.unity` → Play.

Світ створюється з коду (`GameBootstrap`); поки немає арту, замість моделей — кольорові примітиви.

## Арт із Blender

Правила для арту (назви, осі, origin, LOD, колайдери, сокети, текстури, фарби) — у специфікаціях OpenSpec:

- `../openspec/specs/art-pipeline/spec.md` — загальні правила для всіх ассетів;
- `../openspec/specs/art-ship-modularity/spec.md` — кораблі за тирами, слоти для апгрейдів, деталі, фарби;
- `Assets/_Project/Scripts/Editor/ArtBudgets.json` — числа (трикутники, розміри, текстури) по категоріях.

Коротко: FBX → `Assets/_Project/Art/Models/<Ships|Parts|Props|Environment>/<asset_id>.fbx`,
текстури → `Assets/_Project/Art/Textures/<asset_id>/<asset_id>_<BaseMap|Normal|MaskMap|PaintMask|Emission>.png`.
Налаштування імпорту застосовуються автоматично, помилки пишуться в консоль при імпорті.

### Перевірка

```bash
unity test . --mode EditMode --filter Shipmaster.Tests.ArtAssetsTests   # гейт: кожен ассет має пройти
```

Або в редакторі: **Shipmaster → Art → Validate All**.

### Підключення до гри

Префаб `Assets/_Project/Resources/Views/<Ключ>.prefab` (порожній об'єкт → FBX дочірнім) перекриває заглушку.
Ключі вью: `Assets/_Project/Scripts/Runtime/Views/IViewFactory.cs` → `ViewKeys`.
