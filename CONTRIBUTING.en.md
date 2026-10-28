<p align="center"><b>ENGLISH</b> | <a href="CONTRIBUTING.md">한국어</a></p>

# Contributing to InventoryShare

Thank you for contributing to InventoryShare development.

> [!tip]
> For how the plugin works and how the code is structured, see [docs/ARCHITECTURE.en.md](docs/ARCHITECTURE.en.md).
> 
> For a detailed explanation of the differences between Spigot and Paper, and why and how we manipulate server-internal classes, see [docs/NMS_GUIDE.en.md](docs/NMS_GUIDE.en.md).

> [!important]
> The documentation is being reorganized. These documents were edited with the help of AI and may be hard to read. Thank you for your understanding.

---

## 1. Quick Start (BuildTools not required)

```bash
git clone https://github.com/the-sugar-tree/InventoryShare.git
cd InventoryShare
./gradlew build          # Windows: gradlew.bat build
```

- Output: `InventoryShare-bukkit/build/libs/InventoryShare-bukkit-<version>-[1.12.2-26.3].jar` (it is also copied to the root `build/libs/` under the release name `InventoryShare-<version>.jar`).
- The released plugin uses only `spigot-api` and reflection, so **it builds without installing Spigot via BuildTools.**
- Tests that need a Spigot jar are **skipped**, not failed, when the jar is missing (§4.2).

### Required JDKs

| JDK        | Required | Purpose                                                                                                  |
|------------|----------|----------------------------------------------------------------------------------------------------------|
| 17         | Yes      | Running Gradle, compiling `InventoryShare-bukkit` / `InventoryShare-nms` (Java 8 target)                  |
| 25         | Yes      | Running `InventoryShare-nms-test` (to read 26.x class files). Included in the default `build`            |
| 21, 25     | Optional | Compiling `-Pdev` sandboxes (`v1_21_R4`: 21, `v26_2`: 25)                                                |
| 17, 21, 25 | Optional | Local Paper test servers, `preparePaperJars` (1.20.4: 17, 1.20.6 – 1.21.11: 21, 26.x: 25)                |

Gradle automatically discovers installed JDKs as toolchains. If you get `Cannot find a Java installation matching ... languageVersion=<N>`, install that JDK.

---

## 2. Contribution Workflow

1. **Check or open an issue:** Before starting work, check [Issues](https://github.com/the-sugar-tree/InventoryShare/issues) for an existing one on the same topic, and open one if there isn't.
2. **Branch:** Fork the repository and create a working branch from `master`.
3. **Verify:** Before committing and building, run `./gradlew licenseFormat` to add license headers, and `./gradlew build` must pass. If your change alters behavior on a server, check the relevant items of the §6.3 checklist on a real server.
4. **Pull Request:** Send the PR to `master`. Describe what you changed and why, and on which servers and versions you verified it.

### Reporting Bugs

If the plugin logged a "This is a bug ... Please report it" message to the server log, it is a bug. When reporting, please include:
- Server type and version (e.g., Paper 1.21.4 build 232, Spigot 1.21.5) and Java version
- The plugin's activation log line: `Minecraft <version> (CraftBukkit ..., ... mappings), using <implementation>`
- The error message and the full stack trace

"This plugin does not support this version" is not a bug; it means the version is not supported yet.

---

## 3. Where to Contribute, by Goal

| Goal                                                  | Location                                                                                                                                                         |
|-------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Commands, event handling, shared/team inventory switching | `InventoryShare-bukkit` (`Commands`, `Listeners`, `inventory/`) — [ARCHITECTURE §3](docs/ARCHITECTURE.en.md#3-core-behavior-inventoryshare-bukkit)           |
| Saving/loading, backups                               | `file/` in `InventoryShare-bukkit` — [ARCHITECTURE §5](docs/ARCHITECTURE.en.md#5-storage-format)                                                                 |
| Messages and language files                           | `utils/I18NUtil` and `src/main/resources/languages/` in `InventoryShare-bukkit`                                                                                  |
| Supporting a new Minecraft version, fixing server-internal names | `nms/reflection/` in `InventoryShare-nms` — [§6.2](#62-supporting-a-new-minecraft-version)                                                            |
| Spigot / Paper detection                              | `nms/server/` in `InventoryShare-nms`                                                                                                                            |

| Module                    | Description                                                                                                                         | Needs Spigot jar                |
|---------------------------|-------------------------------------------------------------------------------------------------------------------------------------|---------------------------------|
| `InventoryShare-bukkit`   | The released plugin (core: commands, listeners, storage, etc.). Bundles `InventoryShare-nms` inside its jar                         | No                              |
| `InventoryShare-nms`      | `InventoryAccess` and Providers, server detection (`server`), the reflection implementation and per-version name tables (`reflection`) | No                           |
| `InventoryShare-nms-test` | Parameterized tests that verify the name tables against real Spigot jars                                                            | Verifies if present, skips if not |
| `InventoryShare-dev`      | Included only with `-Pdev`. Development sandboxes written directly against NMS (`<ver>/`) and local Paper test server tasks         | Sandboxes: Yes                  |

`InventoryShare-sponge` (discontinued) is not part of the build.

---

## 4. Testing

### 4.1 Unit tests (always run)

```bash
./gradlew test
```

These are the tests for `InventoryShare-nms` (version parsing, server detection, name assembly, Provider selection) and `InventoryShare-bukkit` (save file handling, language file paths). No Spigot jar is needed.

### 4.2 NMS name verification tests (BuildTools required)

`InventoryShare-nms-test` loads the Spigot **server** jars and Paper server jars installed in mavenLocal (`~/.m2`), finds every server-internal name using the same code as the plugin, and even checks field types. Spigot server jars are not published to an official repository, so you must build them yourself with BuildTools.

Download and run [BuildTools](https://www.spigotmc.org/wiki/buildtools/).

```bash
java -jar BuildTools.jar --rev 1.21.4             # Spigot server jar (Spigot names)
java -jar BuildTools.jar --rev 1.21.4 --remapped  # 1.17 – 1.21.11: also installs the Mojang-name jar (for verifying Paper)
```

- BuildTools installs `org.spigotmc:spigot:<version>` into mavenLocal. With `--remapped`, it also installs the `remapped-mojang` jar of the same version. (1.17 – 1.21.11)
- The `remapped-mojang` jar is the same Spigot version translated to Mojang names. For 26.1 and above, the default jar already uses Mojang names.
- The list of versions to check is `spigotJars` in `InventoryShare-nms-test/build.gradle`. You may install only the versions you need.

| Minecraft        | JDK to run BuildTools |
|------------------|-----------------------|
| 1.12 – 1.16.5    | 8                     |
| 1.17 – 1.17.1    | 16                    |
| 1.18 – 1.20.4    | 17                    |
| 1.20.5 – 1.21.11 | 21                    |
| 26.1 and above   | 25                    |

The JDK that BuildTools requires may vary with the BuildTools version. If you get an error, follow BuildTools' guidance.

```bash
./gradlew :InventoryShare-nms-test:test
./gradlew :InventoryShare-nms-test:test -PnmsStrict   # fail instead of skip if a jar is not installed
```

- Result: `InventoryShare-nms-test/build/reports/tests/test/index.html`
- Check the report to see, per version, whether a test was **skipped** or **passed**. A skip means the jar is not installed.

**Paper jars**

Paper patches CraftBukkit itself (e.g., in 26.3, `CraftItemStack#asCraftMirror` became `asBukkitMirror`). So Paper cannot be verified with Spigot jars.

```bash
./gradlew :InventoryShare-nms-test:preparePaperJars
```

- The list of versions to check is `paperJars` in `InventoryShare-nms-test/build.gradle`. It downloads the latest build of each version, builds a server jar, and places it in the Gradle user home (`~/.gradle/caches/inventoryshare/paper/`). It downloads again only when a new build is available.
- `-PnmsStrict` runs this task first.

---

## 5. Development Environment (`InventoryShare-dev`)

Pass `-Pdev`, or put `dev=true` in the root `gradle.properties` (ignored by git), to include the `InventoryShare-dev` module. Use `gradle.properties` if you want it visible in your IDE too.

```bash
./gradlew build -Pdev
```

```properties
# gradle.properties
dev=true
```

### 5.1 Sandboxes

When developing for a new version, first verify behavior by using NMS directly instead of reflection.

- Output: `InventoryShare-dev/<ver>/build/libs/InventoryShare-dev-<version>-[<ver>].jar`. It is a plugin you can drop straight into a server.
- A sandbox uses the same core as the released plugin, and replaces only `InventoryAccess` with `DirectInventoryAccess`, written directly against NMS.
- `DevInventoryAccessProvider` handles exactly one target version (`supports(version)`); the default reflection implementation handles all other versions. **You can run a sandbox even for a version that is not yet in the name tables.**
- You can confirm the sandbox implementation is in use from `Minecraft <version> (...), using DirectInventoryAccess` in the server log.
- A sandbox compiles against the Spigot server jar, so you must install that version with BuildTools.

### 5.2 Local Paper test server

The `runPaper_<version>` tasks in `InventoryShare-dev/build.gradle` launch a Paper server with the released plugin jar installed ([run-paper](https://github.com/jpenilla/run-task)).

```bash
./gradlew :InventoryShare-dev:tasks --group "run paper"   # list tasks
./gradlew :InventoryShare-dev:runPaper_1_21_4
```

- Server folder: `InventoryShare-dev/run/paper-<version>/` (ignored by git). run-paper downloads the Paper jar.
- Since this is a local test server, it runs with the [Minecraft EULA](https://aka.ms/MinecraftEULA) accepted via `-Dcom.mojang.eula.agree=true`.
- For builds that cannot be downloaded (e.g., the Paper 1.20.4 Mojang build), a task is created if you put `paperJar_<task suffix>=<jar path>` in `gradle.properties`. Example: `paperJar_1_20_4_mojang=...`
- These tasks install only the released plugin. To try a sandbox jar, put only the sandbox jar in a separate folder and run the Paper jar yourself. Putting both jars in the same folder results in two plugins with the same name.

---

## 6. Guide by Type of Work

### 6.1 Changing core features / fixing bugs

1. First read [ARCHITECTURE §3](docs/ARCHITECTURE.en.md#3-core-behavior-inventoryshare-bukkit) for the inventory states and per-event handling.
2. The core does not touch NMS directly; it uses only `InventoryAccess` and `ItemCodec`. If you need server-internal behavior, add a method to `InventoryAccess` and update the Legacy and Modern implementations and the sandbox implementation together.
3. Write logic so that it can be tested without a server where possible, and add unit tests (e.g., `file/FileManagerTest`, `file/CodecTest`).
4. On the test server from §5.2 or a server you start yourself, check the relevant items of the §6.3 checklist. We recommend checking once on Legacy (1.21.4 and below) and once on Modern (1.21.5 and above).

### 6.2 Supporting a new Minecraft version

1. Install that version with BuildTools (for 1.17 – 1.21.11, include `--remapped`).
2. Copy the nearest sandbox in `InventoryShare-dev` and verify behavior with `DirectInventoryAccess`.
   (Change the Spigot version and toolchain in `build.gradle`, the package name, `DevInventoryAccessProvider.TARGET`, and the `META-INF/services` file.)
3. Add the version to `spigotJars` and `paperJars` in `InventoryShare-nms-test/build.gradle` and test with `-PnmsStrict`. If the name tables lack that version, this test fails.
4. Add a row to the name tables to make the test in step 3 pass. For the inventory, it is `LegacyNameTable` or `ModernNameTable` (`nms/reflection/inventory/`); for item storage (1.20.5 and above), it is `ItemCodecNameTable` (`nms/reflection/item/`). Just carry the field and method names used in the sandbox over as Spigot names. For how to look up names, see [NMS_GUIDE.en.md §6](docs/NMS_GUIDE.en.md#6-how-to-check-names-yourself).
   - If a class name or a Mojang name changed, fix `InventoryNameFactory` or `ItemNameFactory`.
   - If the inventory structure itself changed, a new `InventoryAccess` implementation is needed.
5. Verify on a real server with the released jar (§6.3). For Paper, you can use the test server from §5.2.

Versions after 26.1 have no obfuscation, so the existing Mojang names will most likely keep working, but be sure to confirm with the test in step 3.

### 6.3 Real-server checklist

- [ ] The plugin enables, and the log's `Minecraft <version> (CraftBukkit <package>, <SPIGOT|MOJANG> mappings), using <implementation>` is as expected
  - In particular, check that `<implementation>` is the intended one (`LegacyInventoryAccess`, `ModernInventoryAccess`, or `DirectInventoryAccess` for a sandbox).
- [ ] Two players share the main inventory and hotbar (0 – 35)
- [ ] The 4 armor slots and the offhand are shared too
- [ ] `/inventoryshare inventory false` → each player returns to their original inventory
- [ ] Team inventory: after `/team add` and `/team join`, `/inventoryshare teaminventory true` → sharing happens only within a team. Moving to another team switches to that team's inventory
- [ ] After a server restart, inventories are restored from `inventory.yml` and `teams/<team name>.yml`
- [ ] An `inventory.yml` saved on an earlier-version server loads as is
- [ ] The same items work on both Spigot and Paper

---

## 7. Code Conventions

- `InventoryShare-bukkit` and `InventoryShare-nms` **target Java 8** (`release 8`). Do not use APIs from later versions.
- The core (`InventoryShare-bukkit`) never references NMS/CraftBukkit classes directly. Keep NMS access behind `InventoryAccess` and `ItemCodec`; the core uses only the `nms` and `nms.server` packages.
- Server-internal names (classes, fields, methods) are written only in `*NameFactory`, `NmsNames`, and the name tables.
- Put tests in the same package as the class under test. Do not make package-private members public just for tests.
- Follow the style of the surrounding code (4-space indentation, Lombok's `@Getter`, etc.).
- For null annotations, use only `@NotNull` and `@Nullable` from `org.jetbrains.annotations`. Lombok also generates null checks for fields annotated with this `@NotNull`.
- Write dependency versions only in `gradle/libs.versions.toml` (except for the Spigot server jars checked by `InventoryShare-nms-test` and for `InventoryShare-dev`).
- Every Java file needs the GPL license header. Please run `./gradlew licenseFormat` before committing and building. If any file lacks the header, `build` (`checkLicenses`) fails.
- Commit messages follow the [Conventional Commits](https://www.conventionalcommits.org/) format (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`).

---

## 8. Common Build Problems

**`Could not find org.spigotmc:spigot:<version>`**
- A Spigot server jar is needed only for `-Pdev` builds or `-PnmsStrict` tests. It should not appear with the default `./gradlew build`.
- Follow [§4.2](#42-nms-name-verification-tests-buildtools-required) to install that version with BuildTools. You need the `spigot` (server) jar, not `spigot-api`.

**`Cannot find a Java installation matching ... languageVersion=<N>`**
- Install the JDKs listed in the table in §1.

**`runPaper_...` tasks not showing up**
- You need `-Pdev` or `dev=true` in `gradle.properties`. Tasks that need a local jar exist only if the `paperJar_<suffix>` property is set.

---

## 9. Useful Links

- [docs/ARCHITECTURE.en.md](docs/ARCHITECTURE.en.md)
- [docs/NMS_GUIDE.en.md](docs/NMS_GUIDE.en.md)
- [BuildTools](https://www.spigotmc.org/wiki/buildtools/)
- [run-task (run-paper)](https://github.com/jpenilla/run-task)
