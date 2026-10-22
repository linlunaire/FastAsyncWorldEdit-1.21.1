# Minecraft 1.21.1 upstream backports

This fork intentionally remains on Minecraft 1.21.1 and Java 21. These are
selective fixes from IntellectualSites/FastAsyncWorldEdit, not a merge of its
newer platform adapters, dependency versions, or release configuration.

## 2026-10-22: clipboard, history, and queue correctness

Base: `e668257e04fdbf964e255cbd01468bae887a8e78`.
Upstream reviewed through [17cfb5588](https://github.com/IntellectualSites/FastAsyncWorldEdit/commit/17cfb5588f93eb2c431fe7511e9a59c115c25cea).
The source commits are listed below and their authors are credited in the
backport commit. Production patches are unchanged from their upstream versions.

| Upstream commit | Fix included |
| --- | --- |
| [d8940377d](https://github.com/IntellectualSites/FastAsyncWorldEdit/commit/d8940377d90260c6af584fa1be2688150b610d44) | Preserve the clipboard brush's random rotation when combining an existing transform. |
| [9a076e337](https://github.com/IntellectualSites/FastAsyncWorldEdit/commit/9a076e3378e648396690acc6b279b871c6be5ebc) | Pass immutable dimensions to Sponge v3 clipboard creation, preventing corruption and division by zero on narrow schematics. |
| [4c8b9d5b5](https://github.com/IntellectualSites/FastAsyncWorldEdit/commit/4c8b9d5b50f8c9db98a2eb34f2e955a7f6cb4dc9) | Flush LZ4 payloads before disk clipboard NBT snapshots are taken, preserving block-entity data. |
| [9d7bfb0e5](https://github.com/IntellectualSites/FastAsyncWorldEdit/commit/9d7bfb0e5201ccc8891ec13ad5a8e582835ada22) | Paste entities consistently with `#fullcopy`, even without a clipboard transform. |
| [b54942dfc](https://github.com/IntellectualSites/FastAsyncWorldEdit/commit/b54942dfca75fcee80d8177f269f364face580c5) | Accept empty schematic entity lists with a non-compound element type, including cached reads, while validating nonempty lists. |
| [9d878105d](https://github.com/IntellectualSites/FastAsyncWorldEdit/commit/9d878105dc552f21f585243cf9b464db81d7d0f0) | Reset chunk metadata safely when optional collections were never initialized. |
| [43bf1ab2d](https://github.com/IntellectualSites/FastAsyncWorldEdit/commit/43bf1ab2dacaeb1fa9d933cda0257239291ccb5e) | Delete biome history files and calculate each history file's actual size. |
| [3e4c3bce5](https://github.com/IntellectualSites/FastAsyncWorldEdit/commit/3e4c3bce579de980af5522bf6fee1cc97880847e) | Close both history-header input streams on all exit paths. |
| [e7c4693ce](https://github.com/IntellectualSites/FastAsyncWorldEdit/commit/e7c4693ce3a45c83adf8d86eff8cedf27460811a) | Save the correct entity Y and Z coordinates in disk clipboards. |
| [d695987fe](https://github.com/IntellectualSites/FastAsyncWorldEdit/commit/d695987fea8355abf4f1550e0130b01e01ab85fa) | Correct block-light dispatch, entity-change classification, history summary Z offsets, and empty block-history handling. |

## Compatibility boundaries

- The 1.21.1 adapter, Java target, production dependencies, module selection,
  fork-specific item registry behavior, and CLI changes remain unchanged.
- The separate `fix/youer-command-completion` branch is not merged or modified.
- Minecraft 1.21.9/1.21.11/26.x support and Java 25/build-system migrations are
  deliberately excluded. A newer upstream version number is not assigned.
- [62e622380](https://github.com/IntellectualSites/FastAsyncWorldEdit/commit/62e6223804be994d80abf82d098df0068acc8005) (lazycopy size/world behavior) is deferred because it also changes
  clipboard volume limits and needs large-volume and cross-world integration
  coverage.
- [373dfbc85](https://github.com/IntellectualSites/FastAsyncWorldEdit/commit/373dfbc858563701a5ec0f827387b930e2b248f1) (structure-void handling) is deferred because it changes paste
  command/API shape and default structure import behavior.
- Larger ore generation, palette, registry, and adapter synchronization changes
  need separate compatibility review. This batch is not a claim that every
  upstream bug fix has been imported.
- [baf46e02b](https://github.com/IntellectualSites/FastAsyncWorldEdit/commit/baf46e02b79eca30fff3e79441eeadcc83bdc97f) (LZ4 shading/minimization) belongs to the newer upstream build
  layout and is not blindly copied into this fork's older build scripts.

## Validation

Use a full JDK 21 and the checked-in Gradle wrapper:

```sh
./gradlew :worldedit-core:test :worldedit-bukkit:build --no-configure-on-demand
```

Configuration-on-demand is disabled for this invocation because the existing
aggregate `worldedit-libs:build` task looks up tasks in sibling projects before
they have otherwise been configured.

Focused regression tests cover schematic dimensions and entity-list parsing,
compressed-stream flushing, history file accounting and cleanup, chunk reset,
and block-versus-sky light dispatch. The LZ4 test dependency is test-only; the
plugin's production dependency versions are unchanged.

Verification on JDK 21: all 47 regression cases passed, and
`:worldedit-core:test :worldedit-bukkit:compileJava` completed successfully.
The restricted build environment required preloading Mockito as a Java agent
via a temporary Gradle init script rather than JVM self-attachment. This is an
environment-only option, not a persisted project configuration change.

Full plugin packaging is **not verified**: `:worldedit-bukkit:build` reaches the
unchanged 1.21.1 adapter and fails with seven compilation errors against its
declared Paper development bundle (the `ServerLevel` constructor, `K`/`r`
fields, `craftWorldData`, and `setBlockStateCB`). The same seven errors were
reproduced in an independent checkout of the unmodified base commit above.
The fork's adapter customizations were deliberately preserved instead of being
replaced to force a Paper-only build. This repository has no GitHub Actions
workflows, so there is no remote CI run for this batch.

Server integration still requires a disposable Minecraft 1.21.1/Youer server:
load and paste a one-block-wide schematic, save/reload a clipboard containing
block entities and entities with distinct X/Y/Z coordinates, use `#fullcopy`
with and without rotation, and check undo/history and lighting. Back up worlds
before testing world-editing behavior. Do not treat unit tests as server QA.
