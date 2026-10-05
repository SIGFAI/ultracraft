# Ultracraft

The real ULTRAKILL inside Minecraft 1.21.11: become V1 in your Minecraft world, with ULTRAKILL's weapons, enemies, shop, P-rank and the Cyber Grind.

**Ultracraft is made by [alfr0762](https://github.com/alfr0762).** All credit for the mod goes to them.

- Original project: https://github.com/alfr0762/ultracraft
- Report bugs and ask questions there: https://github.com/alfr0762/ultracraft/issues
- Upstream release packaged here: [v0.1.4](https://github.com/alfr0762/ultracraft/releases/tag/v0.1.4) (commit [`979c7f4`](https://github.com/alfr0762/ultracraft/tree/979c7f4654dd2e670b936bdc120c84a73b4e8e6b))

> **Beta.** Nobody at SIGF has played this build yet, and the author describes it as early work. Back up your saves.
> Bugs in the mod itself go to the author's issue tracker above; problems with the one-click install go to this repository's issues.

## What you need

- **Minecraft**: Java Edition 1.21.11.
- **ULTRAKILL** ([Steam](https://store.steampowered.com/app/1229490/)): current Steam build (upstream pins none).
- steam: Steam running and signed in: Minecraft starts ULTRAKILL through it (https://store.steampowered.com/about/).
- Windows and the [SIGF app](https://sigf.ai). The app installs bepinex 5.4.23.5, fabric-loader 0.19.5, fabric-api 0.141.6+1.21.11 for you.

## Install

In the SIGF app, open **Ultracraft** in the catalog, press **Install**, then **Play**. **Restore** puts your game folders back exactly as they were.
The app follows `mashup.json` in this repository: every download is pinned by sha256, and the files come from the release [`v0.1.4`](../../releases/tag/v0.1.4).

### Good to know

- You need both games: ULTRAKILL on Steam and Minecraft: Java Edition. Windows only.
- Have Steam running, then press Play: Minecraft starts, and ULTRAKILL starts by itself (hidden) through Steam and closes with Minecraft. Do not start ULTRAKILL yourself. Open a world and you are V1 (F8 switches back to Steve).
- The Minecraft side is the app's own Prism instance "sigf-ultracraft" (Minecraft 1.21.11, Fabric Loader 0.19.5, Fabric API 0.141.6+1.21.11, Java 21). BepInEx 5.4.23.5 and the UltraBridge plugin are installed into the ULTRAKILL folder; Restore removes them.
- Two games share your graphics card: if it is slow, set ULTRAKILL Resolution to 540p or 480p in the Ultracraft... settings (title screen or pause menu), and keep Minecraft's render distance at 8 to 12 chunks.
- Your ULTRAKILL save is never written: progress lives in the Minecraft world. Multiplayer is Minecraft's Open to LAN, everyone with the same mods.
- Beta, released days ago with open bug reports (V1 not appearing, ULTRAKILL not opening, rendering): report bugs to the author on the upstream issue tracker.

## What this repository holds

1. The upstream source tree at tag `v0.1.4`, commit [`979c7f4654dd2e670b936bdc120c84a73b4e8e6b`](https://github.com/alfr0762/ultracraft/tree/979c7f4654dd2e670b936bdc120c84a73b4e8e6b), every file unchanged (same git blobs). Upstream's own `README.md` is there, unchanged; GitHub shows this file (`.github/README.md`) first.
2. Added by SIGF in the same commit: this file, `THIRD-PARTY.md` (licenses and sources of the third-party files in the release), and `sigf/` (the scripts that built the release assets, for reference: they run inside the SIGF repository).
3. `mashup.json`, the SIGF app recipe (the next commit).
4. The release `v0.1.4` (its tag is the first commit):

| Asset | Size | sha256 | What it is |
|---|---|---|---|
| `BepInEx_win_x64_5.4.23.5.zip` | 639118 B | `82f9878551030f54657792c0740d9d51a09500eeae1fba21106b0c441e6732c4` | BepInEx 5.4.23.5 x64, the official build, unchanged (see THIRD-PARTY.md); unpacked into the ULTRAKILL folder. |
| `ultracraft-ultrakill.zip` | 105877 B | `fcf423c37e91cb80f22576ade279361b42612cbee9e77068eebc3b674a7a1c06` | `UltraBridge.dll` from upstream's release file `Ultracraft.zip` (`v0.1.4`, sha256 `aac08338...5345`), unchanged, with upstream's `LICENSE`; unpacked into `BepInEx/plugins/UltraBridge`. |
| `ultracraft.mrpack` | 236517 B | `c632a53d66880ea9d89c428a65501d837286a3e7466d0a4186a8f5bfbc3f1874` | the Minecraft side: upstream's `ultracraft-0.1.4.jar` from the same release file, unchanged, and upstream's LICENSE, for Minecraft 1.21.11 with Fabric Loader 0.19.5; Fabric API 0.141.6+1.21.11 is a Modrinth download link, not stored here. |

The sha256 of every file inside the zips is in `mashup.json` (`contents`).

## Licenses

| Part | License | Where |
|---|---|---|
| Ultracraft (all of the upstream tree) | MIT, Copyright Ultracraft contributors | `LICENSE` |
| BepInEx 5.4.23.5 and what its zip bundles (release asset) | MIT; UnityDoorstop LGPL-2.1 | `THIRD-PARTY.md` |
| Fabric API (downloaded from Modrinth by the app, not stored here) | Apache-2.0 | https://github.com/FabricMC/fabric |

## Why this repository exists

The SIGF app (https://sigf.ai) installs mods from recipes (`mashup.json`) whose downloads are pinned release files. This repository makes Ultracraft installable in one click, credited to alfr0762. If you are the author and want anything changed or taken down, open an issue here.
