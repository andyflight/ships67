# Shipmaster — Unity client

Мобільна 3D-гра (Android + iOS) з IAP, рекламою та власним сервером. Unity 6000.3.27f2 (6.3 LTS), URP.
Підхід **code-first**: усе, що можна, робиться кодом; сцени та Inspector — мінімально.

## Структура

```
Assets/
├── _Project/                    # усе наше; решта в Assets/ — від Unity/пакетів
│   ├── Art/Models|Materials|Textures/   # арт із Blender (FBX через git-lfs)
│   ├── Resources/Views/         # префаби вью за ключем: Views/{ViewKey}.prefab
│   ├── Scenes/Bootstrap.unity   # єдина сцена: камера + світло, решта з коду
│   ├── Scripts/Runtime/         # Shipmaster.Runtime.asmdef
│   │   ├── Core/                # GameBootstrap (composition root), GameServices
│   │   ├── Services/            # Ads, Purchases, Save — інтерфейс + реалізації
│   │   ├── Views/               # IViewFactory, ViewKeys, PlaceholderViews
│   │   └── Gameplay/            # логіка гри (MonoBehaviour на кореневих об'єктах)
│   ├── Scripts/Editor/          # Shipmaster.Editor.asmdef — імпорт, генератори, меню
│   └── Tests/EditMode/          # Shipmaster.Tests.EditMode.asmdef
└── Settings/                    # URP-ассети (Mobile_RPAsset — для телефонів)
```

## Правила архітектури

1. **Логіка ≠ вигляд.** Ігровий об'єкт = кореневий GameObject з логікою + дочірня вью з `IViewFactory.Create(ViewKeys.X, root)`.
   Логіка ніколи не посилається на меші/матеріали напряму. Новий тип об'єкта → новий ключ у `ViewKeys` + заглушка в `PlaceholderViews`.
2. **Арт підміняється без коду.** Префаб `Resources/Views/{key}.prefab` перекриває заглушку. Префаб = порожній root + FBX дочірнім (тут правимо поворот/масштаб, не в Blender).
3. **Сервіси — через інтерфейси** в `GameServices`, створюються лише в `GameBootstrap.CreateServices()`.
   Зараз `Fake*` для реклами/покупок; реальні SDK — нові класи, що реалізують той самий інтерфейс.
4. **Без сторонніх DI/async-бібліотек**, поки не знадобляться: async — `Awaitable` (Unity 6).
5. **Сцени не редагуємо вручну.** Об'єкти створюються в коді; налаштування імпорту — через `AssetPostprocessor` у `Scripts/Editor`.
6. Нова runtime-логіка без залежності від сцени → EditMode-тест у `Tests/EditMode`.
7. Ніколи не комітити ключі підпису, `.p8`, `.env` (вже в `.gitignore`).

## Команди (Unity CLI, запускати з `client/`)

```bash
unity test . --mode EditMode        # компіляція + EditMode-тести (редактор має бути закритий або використати запущений)
unity recompile                      # перекомпіляція в запущеному редакторі + помилки компіляції
unity logs --console --follow        # консоль запущеного редактора
unity status                         # які редактори запущені
unity open .                         # відкрити проєкт
```

Після змін у C# — завжди `unity recompile` (редактор відкритий) або `unity test . --mode EditMode` (закритий) і виправити помилки.

## Код-стиль

- Namespace = `Shipmaster.<Тека>`; один публічний тип на файл (дрібні пов'язані enum/const — можна поруч).
- `sealed` за замовчуванням, приватні поля `_camelCase`, константи замість магічних чисел.
- Коментарі англійською, коротко і лише «чому».
