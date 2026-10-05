// Ultracraft (alfr0762, MIT): the real ULTRAKILL played inside Minecraft 1.21.11. Passthrough: a Fabric mod in Minecraft
// and the UltraBridge BepInEx plugin in ULTRAKILL, linked over a loopback socket. Rehosted on SIGFAI/ultracraft
// (standard upstream fusion) with the official BepInEx 5 x64 build.
// Launch: Minecraft only. The Fabric mod starts ULTRAKILL itself once Minecraft is up (UkLauncher.java:
// `steam -applaunch 1229490 -ultracraft ...`, Steam found through HKCU\Software\Valve\Steam SteamExe) and closes it
// with Minecraft, so the app must not start ULTRAKILL too.
//   node library/ultracraft/build.mjs [--fixture]       (outputs: library/lib.mjs)
import { unzip, mrpack, resolveFabricApi } from '../../orchestrator/src/recipe.js';
import { instanceName } from '../../orchestrator/scripts/package-fusion.mjs';
import { BEPINEX, asset, card, dl, emit, pinned, rawAt, zipAsset } from '../lib.mjs';

const UP = {
  repo: 'https://github.com/alfr0762/ultracraft', tag: 'v0.1.4', commit: '979c7f4654dd2e670b936bdc120c84a73b4e8e6b',
  license: 'MIT', authors: ['alfr0762'],
  zip: { file: 'Ultracraft.zip', sha256: 'aac08338c2be88ffd68ebf8ffaff24adfc062597c94eb7c2871a055a158a5345' }, // = GitHub digest
  jar: 'ultracraft-0.1.4.jar', bridge: 'UltraBridge.dll',
};
const MC = { mc: '1.21.11', loader: '0.19.5', fabricApi: '0.141.6+1.21.11', java: '21' }; // fabric/gradle.properties at the tag
const ID = 'ultracraft', VERSION = '0.1.4', NAME = 'Ultracraft';
const TAGLINE = 'The real ULTRAKILL inside Minecraft 1.21.11: become V1 in your Minecraft world, with ULTRAKILL\'s weapons, enemies, shop, P-rank and the Cyber Grind.';

const upZip = new Map(unzip(await pinned(`${UP.repo}/releases/download/${UP.tag}/${UP.zip.file}`, UP.zip.sha256)).map(e => [e.name, e.data]));
for (const f of [UP.jar, UP.bridge]) if (!upZip.has(f)) throw new Error(`${UP.zip.file} has no ${f}`);
const license = await rawAt(UP.repo, UP.commit, 'LICENSE');

const bepinex = asset(BEPINEX.file, await pinned(BEPINEX.url, BEPINEX.sha256), { zipped: true });
// UltraBridge.dll unchanged into {game}/BepInEx/plugins/UltraBridge (the folder upstream's README asks for, issue #5).
const bridge = zipAsset(`${ID}-ultrakill.zip`, [{ name: UP.bridge, data: upZip.get(UP.bridge) }, { name: 'LICENSE.txt', data: license }]);
const pack = async (offline) => {
  const fabricApi = offline ? null : await resolveFabricApi(MC.fabricApi, MC.mc);
  if (!offline && !fabricApi?.download) throw new Error(`Fabric API ${MC.fabricApi} not resolved on Modrinth`);
  return asset(`${ID}.mrpack`, mrpack({ name: NAME, summary: TAGLINE, versions: MC, versionId: VERSION, fabricApi,
    jars: [{ name: UP.jar, data: upZip.get(UP.jar) }], extra: [{ name: `overrides/licenses/${NAME}-LICENSE.txt`, data: license }] }));
};
const assets = [bepinex, bridge, await pack(false)];
const fixtureAssets = [bepinex, bridge, await pack(true)];

const make = (urls, set) => {
  const mp = set.find(a => a.name.endsWith('.mrpack'));
  const offline = set !== assets;
  return {
    id: `sigf/${ID}`,
    version: VERSION,
    name: NAME,
    tagline: TAGLINE,
    kind: 'passthrough',
    games: [
      { game: 'minecraft', role: 'host', label: 'Minecraft', engine: 'Minecraft Java 1.21.11 + Fabric mod (Java)', mc: MC.mc, loader: `fabric@${MC.loader}`, java: MC.java },
      { game: 'ultrakill', role: 'guest', label: 'ULTRAKILL', engine: 'ULTRAKILL (Unity, Mono, x64) + BepInEx 5 plugin UltraBridge (C#)', apps: { steam: '1229490' }, runtime: 'current Steam build (upstream pins none)' },
    ],
    requires: [
      { id: BEPINEX.id, version: BEPINEX.version, license: `${BEPINEX.license}, shipped unchanged`, page: `${BEPINEX.repo}/releases/tag/v${BEPINEX.version}`,
        note: 'installed into the ULTRAKILL folder by the app', source: { url: urls[bepinex.name], sha256: bepinex.sha256 } },
      { id: 'fabric-loader', version: MC.loader },
      { id: 'fabric-api', version: MC.fabricApi, note: 'in the Minecraft pack (downloaded from Modrinth)' },
      { id: 'steam', page: 'https://store.steampowered.com/about/', note: 'Steam running and signed in: Minecraft starts ULTRAKILL through it' },
    ],
    install: [
      { game: 'ultrakill', strategy: 'game-dir-snapshot', loader: 'bepinex', files: [
        { src: bepinex.name, dst: '{game}', unpack: true, contents: bepinex.contents, ...dl(bepinex, urls) },
        { src: bridge.name, dst: '{game}/BepInEx/plugins/UltraBridge', unpack: true, contents: bridge.contents, ...dl(bridge, urls) },
      ] },
      { game: 'minecraft', strategy: 'mrpack', pack: { src: mp.name, ...dl(mp, urls) } },
    ],
    // Minecraft only: its mod starts ULTRAKILL (hidden) through Steam and closes it on exit.
    launch: [{ game: 'minecraft' }],
    files: set.map(a => ({ name: a.name, ...dl(a, urls) })),
    source: {
      repo: UP.repo, license: 'MIT AND LGPL-2.1', upstream_license: UP.license, tag: UP.tag, commit: UP.commit,
      hosted: `https://github.com/SIGFAI/${ID}`,
      bundled: [{ name: 'BepInEx', version: BEPINEX.version, repo: BEPINEX.repo, commit: BEPINEX.commit, license: BEPINEX.license }],
    },
    media: {},
    built_by: { author: UP.authors[0], authors: UP.authors, packaged_by: 'SIGF' },
    idea_by: UP.authors[0],
    built_at: '2026-10-05T00:00:00.000Z',
    ...card(UP.repo),
    notes: [
      'You need both games: ULTRAKILL on Steam and Minecraft: Java Edition. Windows only.',
      'Have Steam running, then press Play: Minecraft starts, and ULTRAKILL starts by itself (hidden) through Steam and closes with Minecraft. Do not start ULTRAKILL yourself. Open a world and you are V1 (F8 switches back to Steve).',
      `The Minecraft side is the app's own Prism instance "${instanceName(`sigf/${ID}`)}" (Minecraft ${MC.mc}, Fabric Loader ${MC.loader}, Fabric API ${MC.fabricApi}, Java ${MC.java}). BepInEx 5.4.23.5 and the UltraBridge plugin are installed into the ULTRAKILL folder; Restore removes them.`,
      'Two games share your graphics card: if it is slow, set ULTRAKILL Resolution to 540p or 480p in the Ultracraft... settings (title screen or pause menu), and keep Minecraft\'s render distance at 8 to 12 chunks.',
      'Your ULTRAKILL save is never written: progress lives in the Minecraft world. Multiplayer is Minecraft\'s Open to LAN, everyone with the same mods.',
      'Beta, released days ago with open bug reports (V1 not appearing, ULTRAKILL not opening, rendering): report bugs to the author on the upstream issue tracker.',
      ...(offline ? [`Offline fixture: Fabric API ${MC.fabricApi} not in the pack.`] : []),
    ],
  };
};

emit({ slug: ID, version: VERSION, assets, fixtureAssets, make });
