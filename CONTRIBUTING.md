# Contributing to InventoryShare

InventoryShare 개발에 참여해 주셔서 감사합니다.

> [!important]
> 문서 정리 작업 중입니다. 현재 AI가 편집한 문서로, 읽기 불편할 수 있습니다. 양해바랍니다.

---

## 1. 빠른 시작 (BuildTools 불필요)

```bash
git clone https://github.com/the-sugar-tree/InventoryShare.git
cd InventoryShare
./gradlew build          # Windows: gradlew.bat build
```

- 결과물: `InventoryShare-bukkit/build/libs/InventoryShare-bukkit-<version>-[1.12.2-26.2].jar` (루트 `build/libs/`에도 복사됩니다)
- 배포 플러그인은 `spigot-api`와 리플렉션만 사용하므로 **BuildTools로 Spigot을 설치하지 않아도 빌드됩니다.**
- 설치되지 않은 Spigot 버전의 이름 검증 테스트는 실패하지 않고 **skip**됩니다.

### 필요 JDK

| JDK        | 용도                                                                                           |
|------------|------------------------------------------------------------------------------------------------|
| 17\*       | Gradle 실행, `InventoryShare-bukkit` / `InventoryShare-nms` 컴파일 (toolchain 17, Java 8 대상) |
| 25\*       | `InventoryShare-nms-test` (26.x class 파일을 읽기 위해 toolchain 25).                          |
| 21, 25     | `-Pdev` 샌드박스 컴파일 (`v1_21_R4`: 21, `v26_2`: 25)                                          |
| 17, 21, 25 | 로컬 Paper 테스트 서버 실행 (1.20.4: 17, 1.20.6 – 1.21.11: 21, 26.x: 25)                       |

\*은 필수항목입니다.

Gradle은 설치된 JDK를 toolchain으로 자동 탐색합니다. `Cannot find a Java installation matching ... languageVersion=<N>` 오류가 나면 해당 JDK를 설치하세요.

---

## 2. 모듈

| 모듈                      | 설명                                                                                           | Spigot jar 필요          |
|---------------------------|------------------------------------------------------------------------------------------------|--------------------------|
| `InventoryShare-bukkit`   | 배포 플러그인 (명령어, 리스너, 저장 등 core). `InventoryShare-nms`를 jar 안에 포함             | X                        |
| `InventoryShare-nms`      | `InventoryAccess`와 Provider, 서버 감지(`server`), 리플렉션 구현과 버전별 이름표(`reflection`) | X                        |
| `InventoryShare-nms-test` | 이름표를 실제 Spigot jar로 검증하는 파라미터화 테스트                                          | 있으면 검증, 없으면 skip |
| `InventoryShare-dev`      | `-Pdev`일 때만 포함. 직접 NMS로 작성하는 개발 샌드박스(`<ver>/`)와 로컬 Paper 테스트 서버 Task | 샌드박스: O              |

`InventoryShare-api`는 개발 예정이며, `InventoryShare-sponge`는 개발 중단되었습니다. 현재 둘 다 디렉터리 상태로 남아있습니다.

추가적인 구조 설명은 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)를 참고하세요.

---

## 3. NMS 이름 검증 테스트 (BuildTools 필요)

`InventoryShare-nms-test`는 mavenLocal(`~/.m2`)에 설치된 Spigot **서버** jar로 이름표를 검증합니다.
이때, BuildTools를 통한 Spigot 서버 로컬 빌드가 필요합니다.

### 3.1 Spigot 설치

[BuildTools](https://www.spigotmc.org/wiki/buildtools/)를 내려받아, 버전마다 맞는 JDK로 실행합니다.

```bash
java -jar BuildTools.jar --rev 1.21.4             # Spigot 런타임 jar (Spigot 이름)
java -jar BuildTools.jar --rev 1.21.4 --remapped  # 1.17 이상: Mojang 이름 jar도 설치 (Paper 검증용)
```

BuildTools가 `org.spigotmc:spigot:<version>`을 mavenLocal에 설치합니다. `--remapped`를 주면 같은 버전의 `remapped-mojang` jar도 설치됩니다. (1.17 - 1.21.11)
Paper 1.20.5 이상은 런타임에 Mojang 이름을 쓰므로, 이 jar로 Paper에서 쓰는 이름을 검증합니다.

| Minecraft        | BuildTools 실행 JDK |
|------------------|---------------------|
| 1.12 – 1.16.5    | 8                   |
| 1.17 – 1.17.1    | 16                  |
| 1.18 – 1.20.4    | 17                  |
| 1.20.5 – 1.21.11 | 21                  |
| 26.1 이상        | 25                  |

실행 JDK는 상이할 수 있습니다. 참고바랍니다.

검사 대상 버전 목록은 `InventoryShare-nms-test/build.gradle`의 `spigotJars`입니다. 필요한 버전만 설치해도 됩니다.

### 3.2 테스트 실행

```bash
./gradlew :InventoryShare-nms:test :InventoryShare-nms-test:test
# ./gradlew :InventoryShare-nms-test:test -PnmsStrict   # -PnmsStrict 인자가 설치되지 않은 버전이 있으면 skip이 아닌 fail 처리합니다.
```

- 결과: `InventoryShare-nms-test/build/reports/tests/test/index.html`
- 버전별 테스트가 **skip**인지 **통과**인지 리포트에서 확인하세요. skip은 jar가 설치되지 않았다는 뜻입니다.
- 각 jar에서 서버(CraftBukkit 패키지, Spigot/Mojang 이름)를 감지하고, 플러그인과 같은 코드(`NmsNameFactory` → `*Members.resolve`)로 이름을 해석합니다.

---

## 4. 개발 환경 (`InventoryShare-dev`)

```bash
./gradlew build -Pdev
```

또는 루트에 `gradle.properties`(git에서 무시됨) `dev=true`를 적어 두면 IDE에서도 `InventoryShare-dev` 모듈이 보입니다.
```properties
# gradle.properties
dev=true
```

### 4.1 샌드박스

새 버전을 개발할 때는 리플렉션 대신 NMS를 직접 사용해 먼저 동작을 확인합니다.

- 결과물: `InventoryShare-dev/<ver>/build/libs/InventoryShare-dev-<version>-[<ver>].jar`. 서버에 바로 넣을 수 있는 플러그인입니다.
- 샌드박스는 배포 플러그인과 같은 core를 쓰고, `InventoryAccess`만 직접 NMS 구현(`DirectInventoryAccess`)으로 바꿉니다.
- `DevInventoryAccessProvider`가 `supports(version)`로 대상 버전만 맡고, 나머지 버전은 기본 리플렉션 구현이 맡습니다. **이름표에 아직 없는 버전도 샌드박스로 실행할 수 있습니다.**
- 서버 로그의 `Minecraft <버전> (...), using DirectInventoryAccess`로 샌드박스 구현이 쓰이는지 확인할 수 있습니다.

### 4.2 로컬 Paper 테스트 서버

`InventoryShare-dev/build.gradle`의 `runPaper_<버전>` Task가 배포 플러그인 jar를 넣은 Paper 서버를 실행합니다([run-paper](https://github.com/jpenilla/run-task)).

```bash
./gradlew :InventoryShare-dev:runPaper_1_21_4
./gradlew :InventoryShare-dev:tasks --group "run paper"   # Task 목록
```

- 서버 폴더: `InventoryShare-dev/run/paper-<버전>/` (git에서 무시됨). Paper jar는 run-paper가 내려받습니다.
- 로컬 테스트 서버이므로 `-Dcom.mojang.eula.agree=true`로 [Minecraft EULA](https://aka.ms/MinecraftEULA)에 동의한 상태로 실행합니다.
- 내려받을 수 없는 빌드(예: Paper 1.20.4 Mojang 빌드)는 `gradle.properties`에 `paperJar_<Task 접미사>=<jar 경로>`를 적어 두면 Task가 생깁니다. 예: `paperJar_1_20_4_mojang=...`
- 이 Task는 배포 플러그인만 넣습니다. 샌드박스 jar를 시험하려면 별도 폴더에 샌드박스 jar만 넣고 Paper jar를 직접 실행하세요. 같은 폴더에 두 jar를 넣으면 같은 이름의 플러그인이 두 개가 됩니다.
- run-paper 3.1.0은 Gradle 9.7이 필요해 3.0.2를 씁니다(현재 Gradle 9.6.1).

---

## 5. 새 마인크래프트 버전 추가

1. BuildTools로 해당 버전을 설치합니다(1.17 - 1.21.11이면 `--remapped` 옵션을 활성화).
2. `InventoryShare-dev`의 가장 가까운 샌드박스를 복사해 `DirectInventoryAccess`로 동작을 확인합니다.
   (`build.gradle`의 Spigot 버전과 toolchain, 패키지명, `DevInventoryAccessProvider.TARGET`, `META-INF/services` 파일을 바꿉니다.)
3. `InventoryShare-nms-test/build.gradle`의 `spigotJars`에 버전을 추가하고 테스트합니다.
4. 샌드박스에서 확인 후 `LegacyNameTable` 또는 `ModernNameTable`(`InventoryShare-nms`의 `nms/reflection/`)에 행을 추가합니다. 샌드박스에서 쓴 필드·메서드 이름을 그대로 옮기면 됩니다.
   클래스 이름이나 Mojang 이름이 바뀌었다면 `NmsNameFactory`를, 인벤토리 모델 자체가 바뀌었다면 새 `InventoryAccess` 구현을 만듭니다.
5. 배포 jar로 실서버 검증을 합니다 (아래 체크리스트). Paper는 4.2의 테스트 서버로 확인할 수 있습니다.

26.1 이후 버전은 난독화가 없어 `ModernNameTable.v26` 행(Mojang 이름)이 그대로 동작할 가능성이 높지만, 3번의 테스트로 꼭 확인하세요.

---

## 6. 새 버전 추가 후 체크리스트

- [ ] 플러그인이 활성화되고, 로그의 `Minecraft <버전> (CraftBukkit <패키지>, <SPIGOT|MOJANG> mappings), using <구현>`이 예상과 같다
  - 이 체크리스트는 특히 중요합니다. <구현> 항목이 의도대로 되었는지 확인하세요.
- [ ] 두 플레이어가 보관함/핫바(0 – 35)를 공유한다
- [ ] 갑옷 4칸과 왼손도 공유된다 (Modern: `EntityEquipment`)
- [ ] `/inventoryshare inventory false` → 각자 원래 인벤토리로 돌아온다
- [ ] 팀 인벤토리: `/team add`, `/team join` 후 `/inventoryshare teaminventory true` → 팀끼리만 공유된다. 팀을 옮기면 옮긴 팀의 인벤토리로 바뀐다
- [ ] 서버 재시작 후 `inventory.yml`, `teams/<team>.yml`에서 인벤토리가 복원된다
- [ ] (1.21.5 이상) 1.21.4 서버에서 저장한 `inventory.yml`이 그대로 로드된다
- [ ] Paper에서도 같은 항목이 동작한다

---

## 7. 코드 규칙

- `InventoryShare-bukkit`, `InventoryShare-nms`는 **Java 8 대상**(`release 8`)입니다. 이후 버전의 API를 쓰지 마세요.
- core(`InventoryShare-bukkit`)는 NMS/CraftBukkit 클래스를 직접 참조하지 않습니다. NMS 접근은 `InventoryAccess` 뒤에만 두고, core는 `nms`와 `nms.server` 패키지만 사용합니다.
- 테스트는 대상 클래스와 같은 패키지에 둡니다. package-private 멤버를 테스트 때문에 공개하지 않습니다.
- 모든 Java 파일에는 GPL 라이선스 헤더가 필요합니다. `./gradlew licenseFormat`이 자동으로 추가하며, `jar` 작업 전에도 실행됩니다.
- 커밋 메시지는 [Conventional Commits](https://www.conventionalcommits.org/) 형식을 따릅니다 (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`).

---

## 8. 자주 묻는 빌드 문제

**`Could not find org.spigotmc:spigot:<version>`**
- `-Pdev` 빌드 또는 `-PnmsStrict` 테스트에서만 Spigot 서버 jar가 필요합니다. 기본 `./gradlew build`에서는 나오지 않아야 합니다.
- [3.1](#31-spigot-설치)을 따라 BuildTools로 해당 버전을 설치하세요. `spigot-api`가 아니라 `spigot`(서버) jar가 필요하며, 이는 공식 저장소에 배포되지 않아 BuildTools로만 만들 수 있습니다.

**`Cannot find a Java installation matching ... languageVersion=<N>`**
- 1절의 표에 있는 JDK를 설치하세요.

**`runPaper_...` Task가 보이지 않음**
- `-Pdev` 또는 `gradle.properties`의 `dev=true`가 필요합니다. 로컬 jar가 필요한 Task는 `paperJar_<접미사>` 속성이 있어야 생깁니다.

---

## 9. Useful Links

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- [BuildTools](https://www.spigotmc.org/wiki/buildtools/)
- [run-task (run-paper)](https://github.com/jpenilla/run-task)
