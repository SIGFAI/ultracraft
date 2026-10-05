# Ultracraft

The real ULTRAKILL, played inside the real Minecraft 1.21.11. ULTRAKILL runs alongside Minecraft and is drawn into
its window; Minecraft's blocks and mobs become ULTRAKILL's world and enemies. Bosses with modifiers, P, the shop,
upgrades, the Cyber Grind, and LAN co-op.

You need to own **ULTRAKILL** (Steam) and **Minecraft Java Edition**. Windows only.

## Install

1. **BepInEx 5** (x64) into ULTRAKILL: download `BepInEx_win_x64_5.4.x.zip` from
   https://github.com/BepInEx/BepInEx/releases, unzip it into the ULTRAKILL folder
   (Steam → ULTRAKILL → Manage → Browse local files), start ULTRAKILL once, then close it.
2. From this repository's **Releases**, download `Ultracraft.zip`:
   - put `UltraBridge.dll` in `ULTRAKILL/BepInEx/plugins/UltraBridge/`
   - put `ultracraft-x.y.z.jar` in your Minecraft `mods` folder
3. Install **Fabric Loader** for Minecraft **1.21.11** (https://fabricmc.net/use/installer/) and put
   **Fabric API** for 1.21.11 (https://modrinth.com/mod/fabric-api) in the same `mods` folder.
4. Start Steam, then start Minecraft with the Fabric profile. ULTRAKILL starts by itself (and closes with
   Minecraft); open a world and you become V1 (F8 toggles back to Steve).

Settings: the **Ultracraft...** button on the title screen, the pause menu and Options (or `/uc settings`).
Commands: `/uc help`.

## If it's laggy

Two games run at once and share your graphics card. In **Ultracraft...** settings:
- **ULTRAKILL Resolution**: 540p or 480p is the biggest frame rate win (720p is the default).
- **ULTRAKILL FPS Cap**: Match Minecraft (the default) follows Minecraft's frame limit, the smoothest (as V1, Minecraft
  shows every ULTRAKILL frame once, in step with it); a lower cap leaves Minecraft more room on a weak graphics card.
- **Sharp Shop Screen**: off.

Also: Minecraft render distance 8-12 chunks, and Sodium (Fabric) helps Minecraft's side. ULTRAKILL's own
graphics options (in ULTRAKILL itself) apply too.

## Multiplayer

Everyone plays with their own ULTRAKILL (it starts by itself when Minecraft does), and everyone needs the same mods.

- **Easiest: [Essential](https://essential.gg)** (Fabric, 1.21.11) in the mods folder: host your world from Essential's
  menu and invite your friends; they join from their invites. Works with Ultracraft out of the box.
- Or **Open to LAN** from the pause menu (same network, or with a tunnel such as playit.gg).

You share the world and the fight: each player's ULTRAKILL runs the enemies around them, and the others see and hit
them too. Other players show up as V1. Each player has their own P, gear and upgrades.

## Controls

**Ultracraft... → ULTRAKILL Controls...** rebinds ULTRAKILL's keys (movement, dash, slide, fire, punch, arm, whiplash,
weapons...). Click an action, press the key or mouse button; Esc cancels. Minecraft's own action on that key steps aside
while you're V1 with guns out. F8 switches V1/Steve and V switches guns/Minecraft hands.

Back as Steve (F8), ULTRAKILL's enemies stay: you still see them, they still come for you (their hits cost hearts),
and you can fight them as Steve. Turn **ULTRAKILL While Steve** off to have them wait, frozen, until you're V1 again.

Your own ULTRAKILL save is never written: progress lives in the Minecraft world.

## Recipes

**Shop**: 5 iron ingots, 2 gold ingots, a glass pane and a block of redstone.

![Shop recipe](docs/recipes/shop.png)

Place it and walk up to it as V1: ULTRAKILL's shop terminal, where you spend P on weapons, variants and upgrades. The
revolver, shotgun and nailgun pages have an **ALTERNATE** button: it buys the alternate version, and after that switches
the weapon between standard and alternate.

## Build from source

- Fabric mod: JDK 21, `cd fabric && ./gradlew build` → `fabric/build/libs/`.
- ULTRAKILL plugin: .NET SDK, `dotnet build -c Release ultrabridge/UltraBridge.csproj -p:GameDir="<your ULTRAKILL folder>"`
  (it compiles against your own install's DLLs and copies the result into its BepInEx plugins).

No game files are included in this repository.
