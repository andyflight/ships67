# Shipmaster — client

Unity-клієнт мобільної 3D-гри Shipmaster (Android, iOS).

- Unity **6000.3.27f2** (6.3 LTS), URP
- Перед клонуванням: `git lfs install` (моделі, текстури й звуки зберігаються в Git LFS)

## Запуск

1. Відкрити `client/` у Unity Hub (або `unity open .`).
2. Відкрити сцену `Assets/_Project/Scenes/Bootstrap.unity` → Play.

Світ створюється з коду (`GameBootstrap`); поки немає арту, замість моделей — кольорові примітиви.

## Арт із Blender

### Експорт

- Формат **FBX**, одиниці — метри (Scene Scale 1.0), застосувати трансформації (`Ctrl+A → All Transforms`).
- Точка origin — внизу по центру моделі (для корабля — на ватерлінії по центру).
- Ніс корабля дивиться в **-Y** у Blender (вид спереду, Numpad 1, показує ніс).
- Назви: `snake_case`, без пробілів, напр. `ship_frigate.fbx`.
- Текстури — PNG, розмір степенем двійки (512, 1024, 2048).

### Імпорт

1. Покласти FBX у `Assets/_Project/Art/Models/` (текстури — в `Art/Textures/`).
   Налаштування імпорту (масштаб, осі Blender → Unity) застосовуються автоматично.
2. Створити префаб `Assets/_Project/Resources/Views/<Ключ>.prefab` (напр. `Ship.prefab`):
   порожній об'єкт → FBX дочірнім; тут же підкрутити поворот/масштаб, якщо треба.
3. Play — замість заглушки буде модель. Код змінювати не потрібно.

Ключі вью: `Assets/_Project/Scripts/Runtime/Views/IViewFactory.cs` → `ViewKeys`.
