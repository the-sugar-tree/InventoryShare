# Contributing to InventoryShare

InventoryShare 개발에 참여해 주셔서 감사합니다.

> [!tip]
> 플러그인의 동작과정, 코드 구조는 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)를 참고하세요.
> 
> Spigot, Paper의 차이점, 버킷 내부 클래스를 조작하는 이유 및 방법에 대한 자세한 설명은 [docs/NMS_GUIDE.md](docs/NMS_GUIDE.md)를 참고하세요.

> [!important]
> 문서 정리 작업 중입니다. 현재 AI와 함께 편집한 문서로, 읽기 불편할 수 있습니다. 양해바랍니다.

---

## 1. 빠른 시작 (BuildTools 불필요)

```bash
git clone https://github.com/the-sugar-tree/InventoryShare.git
cd InventoryShare
./gradlew build          # Windows: gradlew.bat build
```

- 결과물: `InventoryShare-bukkit/build/libs/InventoryShare-bukkit-<version>-[1.12.2-26.3].jar` (루트 `build/libs/`에도 복사됩니다).
- 배포 플러그인은 `spigot-api`와 리플렉션만 사용하므로 **BuildTools로 Spigot을 설치하지 않아도 빌드됩니다.**
- Spigot jar가 필요한 테스트는 jar가 없으면 실패하지 않고 **skip**됩니다(§4.2).

### 필요 JDK

| JDK        | 필수 | 용도                                                                                     |
|------------|------|------------------------------------------------------------------------------------------|
| 17         | 필수 | Gradle 실행, `InventoryShare-bukkit` / `InventoryShare-nms` 컴파일 (Java 8 대상)         |
| 25         | 필수 | `InventoryShare-nms-test` 실행 (26.x 클래스 파일을 읽기 위해). 기본 `build`에 포함됩니다 |
| 21, 25     | 선택 | `-Pdev` 샌드박스 컴파일 (`v1_21_R4`: 21, `v26_2`: 25)                                    |
| 17, 21, 25 | 선택 | 로컬 Paper 테스트 서버 실행 (1.20.4: 17, 1.20.6 – 1.21.11: 21, 26.x: 25)                 |

Gradle은 설치된 JDK를 toolchain으로 자동 탐색합니다. `Cannot find a Java installation matching ... languageVersion=<N>` 오류가 나면 해당 JDK를 설치하세요.

---

## 2. 기여 흐름

1. **이슈 확인·등록:** 작업 전에 [Issues](https://github.com/the-sugar-tree/InventoryShare/issues)에 같은 내용이 있는지 확인하고, 없으면 등록해 주세요.
2. **브랜치:** 저장소를 fork하고 `master`에서 작업 브랜치를 만듭니다.
3. **확인:** `./gradlew build`가 통과해야 합니다. 서버에서 동작이 바뀌는 수정이라면 §6.3의 체크리스트 중 관련 항목을 실서버에서 확인해 주세요.
4. **Pull Request:** `master`로 PR을 보냅니다. 무엇을 왜 바꿨는지, 어떤 서버·버전에서 확인했는지 적어 주세요.

### 버그 신고

플러그인이 서버 로그에 "This is a bug ... Please report it" 메시지를 남겼다면 버그입니다. 신고할 때 다음을 함께 적어 주세요.
- 서버 종류와 버전 (예: Paper 1.21.4 build 232, Spigot 1.21.5), Java 버전
- 플러그인 활성화 로그 한 줄: `Minecraft <버전> (CraftBukkit ..., ... mappings), using <구현>`
- 오류 메시지와 스택 트레이스 전체

"이 플러그인은 이 버전을 지원하지 않습니다"는 버그가 아니라 아직 지원하지 않는 버전이라는 뜻입니다.

---

## 3. 목표에 따른 기여 방법

| 목표                                           | 위치                                                                                                                                        |
|------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| 명령어, 이벤트 처리, 공유·팀 인벤토리 전환     | `InventoryShare-bukkit` (`Commands`, `Listeners`, `inventory/`) — [ARCHITECTURE §3](docs/ARCHITECTURE.md#3-core-동작-inventoryshare-bukkit) |
| 저장·로드, 백업                                | `InventoryShare-bukkit`의 `file/` — [ARCHITECTURE §5](docs/ARCHITECTURE.md#5-저장-형식)                                                     |
| 메시지·언어 파일                               | `InventoryShare-bukkit`의 `utils/I18NUtil`, `src/main/resources/languages/`                                                                 |
| 새 마인크래프트 버전 지원, 서버 내부 이름 수정 | `InventoryShare-nms`의 `nms/reflection/` — [§6.2](#62-새-마인크래프트-버전-지원)                                                            |
| Spigot / Paper 감지                            | `InventoryShare-nms`의 `nms/server/`                                                                                                        |

| 모듈                      | 설명                                                                                           | Spigot jar 필요          |
|---------------------------|------------------------------------------------------------------------------------------------|--------------------------|
| `InventoryShare-bukkit`   | 배포 플러그인 (명령어, 리스너, 저장 등 core). `InventoryShare-nms`를 jar 안에 포함             | X                        |
| `InventoryShare-nms`      | `InventoryAccess`와 Provider, 서버 감지(`server`), 리플렉션 구현과 버전별 이름표(`reflection`) | X                        |
| `InventoryShare-nms-test` | 이름표를 실제 Spigot jar로 검증하는 파라미터화 테스트                                          | 있으면 검증, 없으면 skip |
| `InventoryShare-dev`      | `-Pdev`일 때만 포함. 직접 NMS로 작성하는 개발 샌드박스(`<ver>/`)와 로컬 Paper 테스트 서버 Task | 샌드박스: O              |

`InventoryShare-bukkit/InventoryShare-api`(외부 플러그인용 API, 개발 예정)와 `InventoryShare-sponge`(중단)는 빌드에 포함되지 않습니다.

---

## 4. 테스트

### 4.1 단위 테스트 (항상 실행)

```bash
./gradlew test
```

`InventoryShare-nms`(버전 파싱, 서버 감지, 이름 조립, Provider 선택)와 `InventoryShare-bukkit`(저장 파일 처리, 언어 파일 경로)의 테스트입니다. Spigot jar가 필요 없습니다.

### 4.2 NMS 이름 검증 테스트 (BuildTools 필요)

`InventoryShare-nms-test`는 mavenLocal(`~/.m2`)에 설치된 Spigot **서버** jar를 불러, 플러그인과 같은 코드로 모든 서버 내부 이름을 찾고 필드 타입까지 확인합니다. 서버 jar는 공식 저장소에 배포되지 않으므로 BuildTools로 직접 만들어야 합니다.

[BuildTools](https://www.spigotmc.org/wiki/buildtools/)를 내려받아 실행합니다.

```bash
java -jar BuildTools.jar --rev 1.21.4             # Spigot 서버 jar (Spigot 이름)
java -jar BuildTools.jar --rev 1.21.4 --remapped  # 1.17 – 1.21.11: Mojang 이름 jar도 설치 (Paper 검증용)
```

- BuildTools가 `org.spigotmc:spigot:<version>`을 mavenLocal에 설치합니다. `--remapped`를 주면 같은 버전의 `remapped-mojang` jar도 설치됩니다. (1.17 - 1.21.11)
- Paper 1.20.5 이상은 실행 중에 Mojang 이름을 쓰므로, `remapped-mojang` jar로 Paper에서 쓰는 이름을 검증합니다. 26.1 이상은 기본 jar가 이미 Mojang 이름입니다.
- 검사할 버전 목록은 `InventoryShare-nms-test/build.gradle`의 `spigotJars`입니다. 필요한 버전만 설치해도 됩니다.

| Minecraft        | BuildTools 실행 JDK |
|------------------|---------------------|
| 1.12 – 1.16.5    | 8                   |
| 1.17 – 1.17.1    | 16                  |
| 1.18 – 1.20.4    | 17                  |
| 1.20.5 – 1.21.11 | 21                  |
| 26.1 이상        | 25                  |

BuildTools가 요구하는 JDK는 BuildTools 버전에 따라 달라질 수 있습니다. 오류가 나면 BuildTools의 안내를 따르세요.

```bash
./gradlew :InventoryShare-nms-test:test
./gradlew :InventoryShare-nms-test:test -PnmsStrict   # 설치되지 않은 jar가 있으면 skip 대신 실패
```

- 결과: `InventoryShare-nms-test/build/reports/tests/test/index.html`
- 버전별로 **skip**인지 **통과**인지 리포트에서 확인하세요. skip은 jar가 설치되지 않았다는 뜻입니다.

---

## 5. 개발 환경 (`InventoryShare-dev`)

`-Pdev`를 주거나, 루트 `gradle.properties`(git에서 무시됨)에 `dev=true`를 적어 두면 `InventoryShare-dev` 모듈이 포함됩니다. IDE에서도 보이게 하려면 `gradle.properties`를 쓰세요.

```bash
./gradlew build -Pdev
```

```properties
# gradle.properties
dev=true
```

### 5.1 샌드박스

새 버전을 개발할 때는 리플렉션 대신 NMS를 직접 사용해 먼저 동작을 확인합니다.

- 결과물: `InventoryShare-dev/<ver>/build/libs/InventoryShare-dev-<version>-[<ver>].jar`. 서버에 바로 넣을 수 있는 플러그인입니다.
- 샌드박스는 배포 플러그인과 같은 core를 쓰고, `InventoryAccess`만 직접 NMS로 작성한 `DirectInventoryAccess`로 바꿉니다.
- `DevInventoryAccessProvider`가 대상 버전 하나만 맡고(`supports(version)`), 나머지 버전은 기본 리플렉션 구현이 맡습니다. **이름표에 아직 없는 버전도 샌드박스로 실행할 수 있습니다.**
- 서버 로그의 `Minecraft <버전> (...), using DirectInventoryAccess`로 샌드박스 구현이 쓰이는지 확인할 수 있습니다.
- 샌드박스는 Spigot 서버 jar에 대해 컴파일하므로, 해당 버전을 BuildTools로 설치해야 합니다.

### 5.2 로컬 Paper 테스트 서버

`InventoryShare-dev/build.gradle`의 `runPaper_<버전>` Task가 배포 플러그인 jar를 넣은 Paper 서버를 실행합니다([run-paper](https://github.com/jpenilla/run-task)).

```bash
./gradlew :InventoryShare-dev:tasks --group "run paper"   # Task 목록
./gradlew :InventoryShare-dev:runPaper_1_21_4
```

- 서버 폴더: `InventoryShare-dev/run/paper-<버전>/` (git에서 무시됨). Paper jar는 run-paper가 내려받습니다.
- 로컬 테스트 서버이므로 `-Dcom.mojang.eula.agree=true`로 [Minecraft EULA](https://aka.ms/MinecraftEULA)에 동의한 상태로 실행합니다.
- 내려받을 수 없는 빌드(예: Paper 1.20.4 Mojang 빌드)는 `gradle.properties`에 `paperJar_<Task 접미사>=<jar 경로>`를 적어 두면 Task가 생깁니다. 예: `paperJar_1_20_4_mojang=...`
- 이 Task는 배포 플러그인만 넣습니다. 샌드박스 jar를 시험하려면 별도 폴더에 샌드박스 jar만 넣고 Paper jar를 직접 실행하세요. 같은 폴더에 두 jar를 넣으면 같은 이름의 플러그인이 두 개가 됩니다.

---

## 6. 작업 유형별 안내

### 6.1 core 기능 수정·버그 수정

1. [ARCHITECTURE §3](docs/ARCHITECTURE.md#3-core-동작-inventoryshare-bukkit)에서 인벤토리 상태와 이벤트별 처리를 먼저 확인합니다.
2. core는 NMS를 직접 다루지 않고 `InventoryAccess`와 `ItemCodec`만 사용합니다. 서버 내부 동작이 필요하면 `InventoryAccess`에 메서드를 추가하고 Legacy·Modern 구현과 샌드박스 구현을 함께 고쳐야 합니다.
3. 로직은 가능하면 서버 없이 테스트할 수 있게 작성하고 단위 테스트를 추가합니다(예: `file/FileManagerTest`, `file/CodecTest`).
4. §5.2의 테스트 서버나 직접 띄운 서버에서 §6.3 체크리스트의 관련 항목을 확인합니다. Legacy(1.21.4 이하)와 Modern(1.21.5 이상)에서 한 번씩 확인하는 것을 권장합니다.

### 6.2 새 마인크래프트 버전 지원

1. BuildTools로 해당 버전을 설치합니다(1.17 – 1.21.11이면 `--remapped`을 포함해서 설치하세요.).
2. `InventoryShare-dev`의 가장 가까운 샌드박스를 복사해 `DirectInventoryAccess`로 동작을 확인합니다.
   (`build.gradle`의 Spigot 버전과 toolchain, 패키지명, `DevInventoryAccessProvider.TARGET`, `META-INF/services` 파일을 바꿉니다.)
3. `InventoryShare-nms-test/build.gradle`의 `spigotJars`에 버전을 추가하고 `-PnmsStrict`로 테스트합니다. 이름표에 그 버전이 없으면 이 테스트가 실패합니다.
4. 이름표에 행을 추가해 3번 테스트를 통과시킵니다. 인벤토리는 `LegacyNameTable` 또는 `ModernNameTable`(`nms/reflection/inventory/`), 아이템 저장(1.20.5 이상)은 `ItemCodecNameTable`(`nms/reflection/item/`)입니다. 샌드박스에서 쓴 필드·메서드 이름을 Spigot 이름으로 옮기면 됩니다. 이름을 찾는 방법은 [NMS_GUIDE.md §6](docs/NMS_GUIDE.md#6-이름을-직접-확인하는-방법)을 참고하세요.
   - 클래스 이름이나 Mojang 이름이 바뀌었다면 `InventoryNameFactory` 또는 `ItemNameFactory`를 고칩니다.
   - 인벤토리 구조 자체가 바뀌었다면 새 `InventoryAccess` 구현이 필요합니다.
5. 배포 jar로 실서버 검증을 합니다(§6.3). Paper는 §5.2의 테스트 서버로 확인할 수 있습니다.

26.1 이후 버전은 난독화가 없어 기존 Mojang 이름이 그대로 동작할 가능성이 높지만, 3번의 테스트로 꼭 확인하세요.

### 6.3 실서버 확인 체크리스트

- [ ] 플러그인이 활성화되고, 로그의 `Minecraft <버전> (CraftBukkit <패키지>, <SPIGOT|MOJANG> mappings), using <구현>`이 예상과 같다
  - 특히 `<구현>`이 의도한 구현(`LegacyInventoryAccess`, `ModernInventoryAccess`, 샌드박스면 `DirectInventoryAccess`)인지 확인하세요.
- [ ] 두 플레이어가 보관함·핫바(0 – 35)를 공유한다
- [ ] 갑옷 4칸과 왼손도 공유된다
- [ ] `/inventoryshare inventory false` → 각자 원래 인벤토리로 돌아온다
- [ ] 팀 인벤토리: `/team add`, `/team join` 후 `/inventoryshare teaminventory true` → 팀끼리만 공유된다. 팀을 옮기면 옮긴 팀의 인벤토리로 바뀐다
- [ ] 서버 재시작 후 `inventory.yml`, `teams/<팀 이름>.yml`에서 인벤토리가 복원된다
- [ ] 이전 버전 서버에서 저장한 `inventory.yml`이 그대로 로드된다
- [ ] Spigot과 Paper 모두에서 같은 항목이 동작한다

---

## 7. 코드 규칙

- `InventoryShare-bukkit`, `InventoryShare-nms`는 **Java 8 대상**(`release 8`)입니다. 이후 버전의 API를 쓰지 마세요.
- core(`InventoryShare-bukkit`)는 NMS/CraftBukkit 클래스를 직접 참조하지 않습니다. NMS 접근은 `InventoryAccess`와 `ItemCodec` 뒤에만 두고, core는 `nms`와 `nms.server` 패키지만 사용합니다.
- 서버 내부 이름(클래스·필드·메서드)은 `*NameFactory`, `NmsNames`와 이름표에만 적습니다.
- 테스트는 대상 클래스와 같은 패키지에 둡니다. package-private 멤버를 테스트 때문에 공개하지 않습니다.
- 주변 코드의 스타일을 따릅니다(4칸 들여쓰기, Lombok의 `@Getter` 등 사용).
- null 표시는 `org.jetbrains.annotations`의 `@NotNull`, `@Nullable`만 씁니다. Lombok도 이 `@NotNull`이 붙은 필드에 null 검사를 생성합니다.
- 의존성 버전은 `gradle/libs.versions.toml`에만 적습니다(`InventoryShare-nms-test`가 검사하는 Spigot 서버 jar와 `InventoryShare-dev` 제외).
- 모든 Java 파일에는 GPL 라이선스 헤더가 필요합니다. `./gradlew licenseFormat`이 자동으로 추가하며, `jar` 작업 전에도 실행됩니다.
- 커밋 메시지는 [Conventional Commits](https://www.conventionalcommits.org/) 형식을 따릅니다 (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`).

---

## 8. 자주 묻는 빌드 문제

**`Could not find org.spigotmc:spigot:<version>`**
- `-Pdev` 빌드 또는 `-PnmsStrict` 테스트에서만 Spigot 서버 jar가 필요합니다. 기본 `./gradlew build`에서는 나오지 않아야 합니다.
- [§4.2](#42-nms-이름-검증-테스트-buildtools-필요)를 따라 BuildTools로 해당 버전을 설치하세요. `spigot-api`가 아니라 `spigot`(서버) jar가 필요합니다.

**`Cannot find a Java installation matching ... languageVersion=<N>`**
- §1의 표에 있는 JDK를 설치하세요.

**`runPaper_...` Task가 보이지 않음**
- `-Pdev` 또는 `gradle.properties`의 `dev=true`가 필요합니다. 로컬 jar가 필요한 Task는 `paperJar_<접미사>` 속성이 있어야 생깁니다.

---

## 9. Useful Links

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- [docs/NMS_GUIDE.md](docs/NMS_GUIDE.md)
- [BuildTools](https://www.spigotmc.org/wiki/buildtools/)
- [run-task (run-paper)](https://github.com/jpenilla/run-task)
