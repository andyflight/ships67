# art-ship-modularity Specification

## Purpose
Art contract behind the core loop: a player owns a ship, installs upgrade parts on it, then evolves it to the next
tier or buys a new ship, and can repaint it. Ships, parts and paints must therefore be built as interchangeable
pieces: ships expose typed slots, parts snap onto those slots, and paint is applied
in Unity from a mask instead of being baked into textures. Global rules (units, naming, LODs, budgets) are in
art-pipeline.

## Requirements

### Requirement: Ship classes and tiers
Each ship model SHALL be one class at one tier, with id `ship_<class>_t<tier>` (tier starts at 1). Evolving a ship
SHALL mean swapping to the model of the next tier of the same class (`ship_sloop_t1` → `ship_sloop_t2`); each tier
is a separate asset with its own FBX.

#### Scenario: Evolution target exists
- **WHEN** the game offers evolving `ship_sloop_t1`
- **THEN** an asset `ship_sloop_t2` exists in `Art/Models/Ships/`

### Requirement: Upgrade slots on ships
A ship SHALL expose every place a part can be installed as a socket named `socket_<slot>_<nn>`, where `<slot>` is
the slot type — one lowercase word without underscores (e.g. `cannon`, `sail`, `figurehead`) — and `<nn>` a two-digit index (`socket_cannon_01`). The socket's
Unity `+Z` SHALL point the way the installed part faces and `+Y` SHALL be the part's up. Each ship's spec in its
catalog (`art-ships`) SHALL list its slot sockets.

#### Scenario: Slots listed in the ship spec
- **WHEN** a change adds `ship_sloop_t1`
- **THEN** its requirement in `art-ships` lists every `socket_<slot>_<nn>` the model provides

### Requirement: Parts
An upgrade part SHALL have id `part_<slot>_<name>`, where `<slot>` names the slot type it installs into. Its
origin SHALL be its mount point and it SHALL face Unity `+Z` with `+Y` up, so it can be parented to a matching
socket with no offset or rotation. A part SHALL fit every socket of its slot type on every ship, unless its spec in
`art-parts` names the ships it is limited to.

#### Scenario: Snap onto a socket
- **WHEN** `part_cannon_bronze` is parented to `socket_cannon_01` with an identity local transform
- **THEN** it sits on the deck facing outward through the gun port, with no manual adjustment

### Requirement: Paintable surfaces
Paint SHALL be applied in Unity, not baked into colour textures. A paintable ship or part SHALL provide
`<asset_id>_PaintMask.png` in the same UV layout as its BaseMap: R = primary colour area, G = secondary colour
area, B = trim/accent area, black = not paintable. The BaseMap SHALL keep paintable areas in neutral light grey
so the paint tint reads correctly.

#### Scenario: Paint mask present
- **WHEN** a ship is declared paintable in its catalog spec
- **THEN** `Art/Textures/<asset_id>/<asset_id>_PaintMask.png` exists and passes the texture rules

### Requirement: Paints are data
A paint SHALL be defined in Unity as data (id `paint_<name>`, primary/secondary/trim colours and optional
pattern), not as a Blender asset.

#### Scenario: New paint without art work
- **WHEN** a designer adds `paint_royal_blue`
- **THEN** no Blender work is needed; only Unity data changes
