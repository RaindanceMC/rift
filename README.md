# RaindanceMC/rift

- **`rift-module`** — Rain's Flexible Modules — Rift: a pocket dimension every player can open — a personal vault of 36-stack pages that grows as they play (free pages for active playtime, more bought with money, bonus pages by permission). Search it, filter it by kind, sort it once or keep it sorted, and sell a page or everything shown to the shop.
- **`rift-standalone`** — Rain's Rift: the module and the standard wrapper shaded together.

Part of [RaindanceMC](https://github.com/RaindanceMC); built on [RainsCore](https://github.com/RaindanceMC/RainsCore) and the shared parent in [framework](https://github.com/RaindanceMC/framework). Selling needs RainsEconomy 0.49.0 or later (Core's `ItemSales`).

## Use

- `/rift` (`/pv`, `/playervault`) opens it; `/rift <words>` opens it searching; `/rift sort` sorts it.
- `rains.rift.use`, `rains.rift.sell` — everybody by default. `rains.rift.pages.<n>` — n bonus pages (the highest held counts).
- Settings: `/settings` → Rift (pages to start, most pages, a free page every N hours played, page price and growth, selling).

## Build

```
mvn install
```

Needs Java 25 and read access to `packages.tstieh.de` (server ids `packages-releases` / `packages-snapshots` in `~/.m2/settings.xml`).

## Plugins

- `rift-standalone/target/RainsRift-0.2.0.jar` — also published as `de.raindancer.modules:rift-standalone:0.2.0`

## Versions

The module's version is `rift-module.version` in the root `pom.xml`, the plugin's is `plugin.version` in its own pom. CI publishes every version that is not in the registry yet on the next push to master.
