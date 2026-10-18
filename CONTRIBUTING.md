# Contributing to InventoryShare

InventoryShare 개발에 참여해 주셔서 감사합니다.

---

## 1. 빠른 시작 (BuildTools 불필요)

```bash
git clone https://github.com/the-sugar-tree/InventoryShare.git
cd InventoryShare
./gradlew build          # Windows: gradlew.bat build
```

- 결과물: `InventoryShare-bukkit/build/libs/InventoryShare-bukkit-<version>-[1.12.2-26.2].jar` (루트 `build/libs/`에도 복사됩니다)
- 배포 플러그인은 `spigot-api`와 리플렉션만 사용하므로 **BuildTools로 Spigot을 설치하지 않아도 빌드됩니다.**
- 설치되지 않은 Spigot 버전의 매핑 테스트는 실패하지 않고 **skip**됩니다.

### 필요 JDK

| 용도                                                                                           | JDK |
|------------------------------------------------------------------------------------------------|-----|
| Gradle 실행, `InventoryShare-bukkit` / `InventoryShare-nms` 컴파일 (toolchain 17, Java 8 대상) | 17  |
| `InventoryShare-nms-test` (26.x class 파일을 읽기 위해 toolchain 25)                           | 25  |

Gradle은 설치된 JDK를 toolchain으로 자동 탐색합니다. `Cannot find a Java installation matching ... languageVersion=25` 오류가 나면 해당 JDK를 설치하세요.

---

## 2. 모듈

| 모듈                       | 설명                                                                                        | Spigot jar 필요          |
|----------------------------|---------------------------------------------------------------------------------------------|--------------------------|
| `InventoryShare-bukkit`    | 배포 플러그인 (명령어, 리스너, 저장 등 core)                                                | X                        |
| `InventoryShare-nms`       | `InventoryAccess`와 Legacy/Modern 구현, 버전별 이름표(`LegacyNameTable`, `ModernNameTable`) | X                        |
| `InventoryShare-nms-test`  | 이름표를 실제 Spigot jar로 검증하는 파라미터화 테스트                                       | 있으면 검증, 없으면 skip |
| `InventoryShare-dev/<ver>` | 직접 NMS로 작성하는 개발 샌드박스 (`-Pdev`)                                                 | O                        |

---

## 3. NMS 매핑 테스트 (BuildTools 필요)

`InventoryShare-nms-test`는 mavenLocal(`~/.m2`)에 설치된 Spigot **서버** jar로 이름표를 검증합니다.   
이때, BuildTools를 통한 spigot 서버 로컬 빌드가 필수적입니다.

### 3.1 Spigot 설치

[BuildTools](https://www.spigotmc.org/wiki/buildtools/)를 내려받아, 버전마다 맞는 JDK로 실행합니다.

```bash
java -jar BuildTools.jar --rev 1.21.4
```

BuildTools가 `org.spigotmc:spigot:<version>`을 mavenLocal에 설치합니다.

| Minecraft        | BuildTools 실행 JDK |
|------------------|---------------------|
| 1.12 – 1.16.5    | 8                   |
| 1.17 – 1.17.1    | 16                  |
| 1.18 – 1.20.4    | 17                  |
| 1.20.5 – 1.21.11 | 21                  |
| 26.1 이상        | 25                  |

검사 대상 버전 목록은 `InventoryShare-nms-test/build.gradle`의 `spigotJars`입니다. 필요한 버전만 설치해도 됩니다.

### 3.2 실행

```bash
./gradlew :InventoryShare-nms:test :InventoryShare-nms-test:test
./gradlew :InventoryShare-nms-test:test -PnmsStrict   # 설치되지 않은 버전이 있으면 실패
```

- 결과: `InventoryShare-nms-test/build/reports/tests/test/index.html`
- 버전별 테스트가 **skip**인지 **통과**인지 리포트에서 확인하세요. skip은 jar가 설치되지 않았다는 뜻입니다.

---

## 4. 개발 샌드박스 (`InventoryShare-dev`)

새 버전을 개발할 때는 리플렉션 대신 NMS를 직접 사용해 먼저 동작을 확인합니다.

```bash
./gradlew build -Pdev
```

또는 루트 `gradle.properties`(git에서 무시됨)에 `dev=true`를 적어 두면 IDE에서도 샌드박스 모듈이 보입니다.

- 결과물: `InventoryShare-dev/<ver>/build/libs/InventoryShare-dev-<version>-[<ver>].jar` — 서버에 바로 넣을 수 있는 플러그인입니다.
- 샌드박스는 배포 플러그인과 같은 core를 쓰고 `InventoryAccess`만 직접 NMS 구현(`DirectInventoryAccess`)으로 바꿉니다.
- 서버 로그의 `Minecraft <버전>, using DirectInventoryAccess`로 샌드박스 구현이 쓰이는지 확인할 수 있습니다.

---

## 5. 새 마인크래프트 버전 추가

1. BuildTools로 해당 버전을 설치합니다.
2. `InventoryShare-dev`의 가장 가까운 샌드박스를 복사해 `DirectInventoryAccess`로 동작을 확인합니다.
   (`build.gradle`의 Spigot 버전과 toolchain, 패키지명, `DevInventoryAccessProvider.TARGET`, `META-INF/services` 파일을 바꿉니다.)
3. `InventoryShare-nms-test/build.gradle`의 `spigotJars`에 버전을 추가하고 `-PnmsStrict`로 테스트합니다.
4. 실패하면 `LegacyNameTable` 또는 `ModernNameTable`에 행을 추가합니다. 샌드박스에서 쓴 필드·메서드 이름을 그대로 옮기면 됩니다.
   클래스 이름이나 Mojang 이름이 바뀌었다면 `NmsNameFactory`를, 인벤토리 모델 자체가 바뀌었다면 새 `InventoryAccess` 구현을 만듭니다.
5. 배포 jar로 실서버 검증을 합니다 (아래 체크리스트).

26.1 이후 버전은 난독화가 없어 `ModernNameTable.v26` 행(Mojang 이름)이 그대로 동작할 가능성이 높지만, 3번의 테스트로 꼭 확인하세요.

---

## 6. 새 버전 추가 후 체크리스트

- [ ] 플러그인이 활성화되고, 로그의 `Minecraft <버전>, using <구현>`이 예상과 같다
- [ ] 두 플레이어가 보관함/핫바(0 – 35)를 공유한다
- [ ] 갑옷 4칸과 왼손도 공유된다 (Modern: `EntityEquipment`)
- [ ] `/inventoryshare inventory false` → 각자 원래 인벤토리로 돌아온다
- [ ] 팀 인벤토리: `/team add`, `/team join` 후 `/inventoryshare teaminventory true` → 팀끼리만 공유된다
- [ ] 서버 재시작 후 `inventory.yml`, `teams/<team>.yml`에서 인벤토리가 복원된다
- [ ] (1.21.5 이상) 1.21.4 서버에서 저장한 `inventory.yml`이 그대로 로드된다
- [ ] Paper에서도 같은 항목이 동작한다 (특히 Paper 1.20.5 – 1.21.11은 런타임 이름이 Spigot과 다릅니다)

---

## 7. 코드 규칙

- `InventoryShare-bukkit`, `InventoryShare-nms`는 **Java 8 대상**(`release 8`)입니다. 이후 버전의 API를 쓰지 마세요.
- core(`InventoryShare-bukkit`)는 NMS/CraftBukkit 클래스를 직접 참조하지 않습니다. NMS 접근은 `InventoryAccess` 뒤에만 둡니다.
- 모든 Java 파일에는 GPL 라이선스 헤더가 필요합니다. `./gradlew licenseFormat`이 자동으로 추가하며, `jar` 작업 전에도 실행됩니다.
- 커밋 메시지는 [Conventional Commits](https://www.conventionalcommits.org/) 형식을 따릅니다 (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`).

---

## 8. 자주 묻는 빌드 문제

**`Could not find org.spigotmc:spigot:<version>`**
- `-Pdev` 빌드 또는 `-PnmsStrict` 테스트에서만 Spigot 서버 jar가 필요합니다. 기본 `./gradlew build`에서는 나오지 않아야 합니다.
- 개발 참여를 위해서 [3.1](#31-spigot-설치)를 따라 BuildTools로 해당 버전을 설치하세요. `spigot-api`가 아니라 `spigot`(서버) jar가 필요하며, 이는 공식 저장소에 배포되지 않아 BuildTools로만 만들 수 있습니다.

**`Cannot find a Java installation matching ... languageVersion=<N>`**
- 1절의 표에 있는 JDK를 설치하세요.

---

## 9. Useful Links

[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)