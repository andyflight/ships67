# art-pipeline Specification

## Purpose
Technical contract every 3D art asset must meet so that a model built in Blender works in the Unity client
without manual fixes: naming, units and axes, origin, LODs, colliders, sockets, materials, textures and the
validation gate. Numeric budgets live in `client/Assets/_Project/Scripts/Editor/ArtBudgets.json` (the single
source of truth for these numbers); this spec defines the rules that use them.

## Requirements

### Requirement: Asset identity and categories
Every asset SHALL have a snake_case asset id `<category>_<name>` that is used, unchanged, as the Blender
collection name, the exported FBX file name and the texture prefix. The category SHALL be one of the
prefixes in `ArtBudgets.json` (`ship`, `part`, `prop`, `env`) and the id SHALL match that category's `idPattern`.

#### Scenario: Valid id
- **WHEN** an asset is named `ship_sloop_t1`, `part_cannon_bronze`, `prop_barrel` or `env_island_01`
- **THEN** the Unity validator accepts the id

#### Scenario: Unknown category
- **WHEN** an asset id starts with a prefix not listed in `ArtBudgets.json` (e.g. `rock_big`)
- **THEN** the Unity validator reports an "Unknown category" error

#### Scenario: Ship without tier
- **WHEN** a ship id has no `_t<n>` tier suffix (e.g. `ship_sloop`)
- **THEN** the Unity validator reports that the id must match the ship pattern

### Requirement: File locations
Exported models SHALL be placed at `client/Assets/_Project/Art/Models/<CategoryFolder>/<asset_id>.fbx`, where
`<CategoryFolder>` is the category's `unityFolder` (`Ships`, `Parts`, `Props`, `Environment`). Textures SHALL be
placed at `client/Assets/_Project/Art/Textures/<asset_id>/<asset_id>_<Suffix>.png`. Blender source files
(`.blend`, source textures, references) SHALL live in `assets/` and SHALL NOT be placed in `client/`.

#### Scenario: Model in the wrong folder
- **WHEN** `prop_barrel.fbx` is imported directly into `Art/Models/`
- **THEN** the Unity validator reports that it must be at `Art/Models/Props/prop_barrel.fbx`

### Requirement: Units, scale and axes
Models SHALL be authored in Blender with Metric units and Unit Scale 1.0 (1 Blender unit = 1 metre), with
all mesh transforms applied. The FBX SHALL be exported with Forward `-Z`, Up `Y`, Apply Scalings "FBX Units
Scale", "Apply Transform" disabled and without leaf bones; Unity bakes the axis conversion into the mesh on import.
After import every render and collider mesh SHALL have an identity local transform.

#### Scenario: Unapplied transform
- **WHEN** a mesh object still carries a location, rotation or scale
- **THEN** the imported mesh has a non-identity transform and the Unity validator reports an error

#### Scenario: Wrong unit scale
- **WHEN** a model is exported at the wrong scale (e.g. centimetres)
- **THEN** its longest side falls outside the category's size range and the Unity validator reports an error

### Requirement: Orientation and origin
The model's front SHALL face `-Y` in Blender (Unity `+Z`) and up SHALL be `+Z` in Blender (Unity `+Y`). The
origin SHALL depend on the category's `origin` rule: `waterline` (ships) — origin on the waterline, centred on
the hull, the hull dipping below it; `bottom` (props, environment) — origin at the bottom centre, resting on
Z=0; `mount` (parts) — origin at the point where the part attaches to its socket.

#### Scenario: Ship faces the right way
- **WHEN** a ship is imported
- **THEN** its length runs along Unity `+Z` and `socket_bow` has a positive Z position

#### Scenario: Ship faces backwards
- **WHEN** a ship's bow points to `+Y` in Blender
- **THEN** `socket_bow` has a negative Unity Z and the validator reports an error

#### Scenario: Floating prop
- **WHEN** a prop's lowest point is not at Z=0
- **THEN** the Unity validator reports that it must rest on Y=0

### Requirement: Render meshes and LODs
Each asset SHALL contain exactly one render mesh per LOD level defined by its category's `lodTriangles`. With a
single level the mesh SHALL be named `<asset_id>`; with several levels the meshes SHALL be named
`<asset_id>_LOD0`, `<asset_id>_LOD1`, … Each LOD SHALL stay within its triangle budget and SHALL have a UV map.
Unity builds the LOD group from these names.

#### Scenario: Over budget
- **WHEN** `ship_sloop_t1_LOD0` has more triangles than the first `ship` budget entry
- **THEN** the Unity validator reports the triangle count and the budget

#### Scenario: Missing LOD
- **WHEN** a ship has `_LOD0` and `_LOD1` but its category requires three levels
- **THEN** the Unity validator reports the missing `_LOD2` mesh

### Requirement: Colliders
Collision geometry SHALL be separate, simplified, convex meshes named `COL_<asset_id>[_<n>]`, each with at most
255 triangles (Unity's convex MeshCollider limit). They SHALL NOT have materials. On import Unity SHALL turn every `COL_` mesh into a convex `MeshCollider` and remove its renderer.

#### Scenario: Collider import
- **WHEN** a model contains `COL_ship_sloop_t1_hull`
- **THEN** the imported model has a convex MeshCollider on that object and no renderer on it

### Requirement: Sockets
Attachment points SHALL be Blender empties named `socket_<name>` with scale 1, whose position and rotation define
where and how something attaches (Unity `+Z` forward, `+Y` up). Every ship SHALL have a `socket_bow` empty at the
front of the hull, which also serves as the orientation check.

#### Scenario: Missing required socket
- **WHEN** a ship has no `socket_bow`
- **THEN** the Unity validator reports the missing socket

### Requirement: Materials
Every material SHALL be named `M_<name>`, every render mesh slot SHALL have a material, and an asset SHALL NOT
use more distinct materials than its category's `maxMaterials`. Materials SHALL be Principled BSDF in Blender so
they map onto URP Lit in Unity.

#### Scenario: Unprefixed material
- **WHEN** a mesh uses a material named `Wood`
- **THEN** the Unity validator reports that it must start with `M_`

### Requirement: Textures
Textures SHALL be PNG files with power-of-two sides, no larger than the category's `maxTextureSize`, named
`<asset_id>_<Suffix>.png` with `Suffix` one of `BaseMap` (colour, sRGB), `Normal` (OpenGL/+Y normal map),
`MaskMap` (URP mask: R metallic, G occlusion, A smoothness; linear), `PaintMask` (see art-ship-modularity;
linear) or `Emission` (sRGB). Unity SHALL set texture type, colour space and max size from the suffix and category.

#### Scenario: Non power-of-two texture
- **WHEN** `prop_barrel_BaseMap.png` is 500×512
- **THEN** the Unity validator reports that both sides must be powers of two

#### Scenario: Unknown suffix
- **WHEN** a texture is named `prop_barrel_Diffuse.png`
- **THEN** the Unity validator reports the allowed naming

### Requirement: Validation gate
An asset SHALL be considered done only when the Unity art gate passes with zero errors:
`unity test . --mode EditMode --filter Shipmaster.Tests.ArtAssetsTests` run from `client/`. Unity SHALL also log
violations on import and offer `Shipmaster/Art/Validate All`. The Blender side SHOULD run the same checks before
export, reading the numbers from `ArtBudgets.json`.

#### Scenario: Broken asset blocks the gate
- **WHEN** any model or texture under `Art/` violates this spec
- **THEN** `ArtAssetsTests` fails for that asset with the list of errors

#### Scenario: Clean project
- **WHEN** no art has been imported yet
- **THEN** `ArtAssetsTests` passes
