# InventoryShare Architecture

빌드·테스트 방법은 [CONTRIBUTING.md](../CONTRIBUTING.md)를 참고하세요.

> [!important]
> 문서 정리 작업 중입니다. 현재 AI가 편집한 문서로, 읽기 불편할 수 있습니다. 양해바랍니다.

**현재 구현 상태 (요약)**

| 목표                                                                      | 현재 코드                                                                                                                         |
|---------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| `InventoryShare-nms` (`InventoryAccess`, `NmsNameFactory`, Legacy/Modern) | 완료. Legacy(1.12 – 1.21.4), Modern(1.21.5 – 26.x) 구현과 이름표. Spigot·Paper(Mojang 이름) 모두 지원                             |
| `InventoryShare-nms-test` (파라미터화 테스트)                             | 완료. `LegacyMembersTest`, `ModernMembersTest` (Spigot jar + `remapped-mojang` jar)                                               |
| `InventoryShare-dev/<ver>`                                                | 완료. `v1_21_R4`(1.21.5), `v26_2`(26.2) 샌드박스 + 로컬 Paper 테스트 서버(run-paper). `InventoryShare-single` 삭제                |
| 1.12.2 – 26.x 단일 jar                                                    | `InventoryShare-bukkit` jar 하나. Paper 10개 버전에서 활성화·명령·종료 확인. **플레이어 실동작(특히 Paper, 저장·복원)은 확인 중** |
| 개발자 문서                                                               | 완료. `CONTRIBUTING.md` (빌드, BuildTools/JDK, 샌드박스, 테스트 서버, 실서버 체크리스트)                                          |

---

## 1. 배경

InventoryShare는 플레이어 인벤토리의 내부 필드를 교체해 여러 플레이어가 **같은 NMS 리스트 객체를 참조**하게 만듭니다.
Bukkit API로는 참조 공유가 불가능해 NMS 접근이 필수이고, 버전마다 내부 구조와 이름이 다릅니다.

- 아래는 Spigot 기준 표입니다.
- Paper는 1.20.5+부터 난독화 없는 Mojang 원문 이름을 사용했습니다.

| 구간             | 인벤토리 모델                              | 이름                                                    |
|------------------|--------------------------------------------|---------------------------------------------------------|
| 1.12 – 1.16.5    | `items`, `armor`, `extraSlots`, `contents` | `net.minecraft.server.vX_Y_RZ` 패키지, 필드명 일부 원문 |
| 1.17 – 1.21.4    | 같은 모델                                  | 패키지 평탄화, Spigot 난독화 이름                       |
| 1.21.5 – 1.21.11 | `items` + `EntityEquipment`                | Spigot 난독화 이름                                      |
| 26.1 이상        | `items` + `EntityEquipment`                | **난독화 없음 (Mojang 원문 이름)**                      |

---

## 2. 모듈 구조

```
InventoryShare/
├─ InventoryShare-bukkit/      배포 플러그인 (core). spigot-api로만 컴파일, release 8
├─ InventoryShare-nms/         InventoryAccess + 서버 감지 + 리플렉션 구현(이름표). spigot-api로만 컴파일, release 8
├─ InventoryShare-nms-test/    이름 검증 테스트 (Legacy/Modern 파라미터화, toolchain 25)
├─ InventoryShare-dev*/        개발용 샌드박스 <ver>/ + 로컬 Paper 테스트 서버 Task
├─ src/test/                   GithubAPITest (루트 컴파일이 꺼져 있어 실행되지 않음)
└─ docs/
```
`*`InventoryShare-dev는 dev 옵션이 필요합니다. [CONTRIBUTING.md/4. 개발 환경 (`InventoryShare-dev`)](../CONTRIBUTING.md#4-개발-환경-inventoryshare-dev)를 참고하세요.

`InventoryShare-bukkit/InventoryShare-api/`는 외부 플러그인용 API입니다. 개발 예정입니다.   
`InventoryShare-sponge/`는 Sponge 서버용 개발 폴더 입니다. 중단되었습니다.

```
InventoryShare-bukkit ──▶ InventoryShare-nms ◀── InventoryShare-nms-test
        ▲
InventoryShare-dev/<ver>  (InventoryAccess 주입)
```

### 목표
- core가 Reflection을 직접 수행하지 않고, `InventoryAccess`인터페이스를 통해 접근합니다.
- 1.12.2 이상 버전을 하나의 jar로 지원합니다.

### `InventoryShare-bukkit` (core)

NMS를 직접 알지 못하고 `InventoryAccess`(§3.4)만 사용합니다.

| 패키지 / 클래스                           | 역할                                                                                           |
|-------------------------------------------|------------------------------------------------------------------------------------------------|
| `InventoryShare`                          | 플러그인 진입점. 시작 순서와 실패 메시지 (아래)                                                |
| `Commands`, `Listeners`                   | `/inventoryshare` 명령어. 접속·퇴장·월드 저장·발전과제·사망 이벤트, 팀 변경 감지(`onTick`)     |
| `SharedConstants`                         | 설정값(`inventory`, `teaminventory` 등)과 `InventoryManager`/`FileManager` 등 전역 상태        |
| `inventory/InventoryRegistry`             | 인벤토리 저장소 모음: 공유 1개, 팀별 1개, 공유 중에 맡아 둔 개인 인벤토리                      |
| `inventory/InventoryManager`              | 플레이어를 개인/공유/팀 인벤토리 사이에서 전환 (`InventoryState`, 적용 중인 팀 `appliedTeams`) |
| `file/FileManager`, `Codec`, `LoadResult` | `inventory.yml`, `teams/<team>.yml`, `advancements.yml`, `config.yml` 저장·로드(§4)            |
| `utils/I18NUtil`                          | 언어 파일(`languages/lang_<locale>.yml`) 저장·갱신과 메시지                                    |
| `utils/AdvancementUtil`, `UpdateUtil`     | 발전과제 공유, 업데이트 확인                                                                   |
| `protocollib/*`                           | ProtocolLib을 통한 블록 동시 채굴 문제 보정                                                    |

시작 순서 (`onEnable`):

```
언어 파일 → bStats → 업데이트 확인
→ MinecraftVersion.current()
→ loadNMS(): ServerRuntime.current() → InventoryAccessFactory.create() → InventoryRegistry / InventoryManager / FileManager
             UnsupportedVersionException → 지원 불가 버전 안내, IncompatibleServerException·RuntimeException → 버그 안내
→ ProtocolLib 연동
→ 설정 파일 로드 → FileManager.load() → 명령어·리스너 등록 → 접속 중인 플레이어에 공유 적용(/reload 시)
```

### `InventoryShare-dev` 샌드박스
- `-Pdev`(또는 `gradle.properties`의 `dev=true`)일 때만 settings에 포함됩니다. 하위 폴더 중 `build.gradle`이 있는 것이 자동으로 포함됩니다.
- 각 샌드박스는 `InventoryShare-bukkit`에 의존하고, 직접 NMS로 작성한 `DirectInventoryAccess`와 `DevInventoryAccessProvider`만 가집니다. 명령어·리스너·저장 등 나머지는 배포 플러그인과 같은 코드입니다.
- Provider는 `META-INF/services/com.sugar_tree.inventoryshare.nms.InventoryAccessProvider`로 등록되며, `supports(version)`가 대상 버전에만 true를 돌려줍니다. 다른 버전은 리플렉션 구현이 맡습니다(§3.2). 이름표에 아직 없는 버전도 샌드박스로 실행할 수 있습니다.
- 결과물: `InventoryShare-dev/<ver>/build/libs/InventoryShare-dev-<version>-[<ver>].jar` (실행 가능한 플러그인). 활성화 로그에 사용 중인 `InventoryAccess` 구현 이름이 출력됩니다.
- 새 버전 샌드박스 추가: 기존 폴더를 복사해 `build.gradle`의 Spigot 버전·toolchain, 패키지명, `DevInventoryAccessProvider.TARGET`, services 파일을 바꿉니다.
- 로컬 Paper 테스트 서버: `InventoryShare-dev/build.gradle`의 `runPaper_<버전>` Task가 배포 플러그인 jar를 넣은 Paper 서버를 `InventoryShare-dev/run/paper-<버전>/`에서 실행합니다(run-paper 3.0.2). 사용법은 CONTRIBUTING §4.2.

---

## 3. `InventoryShare-nms`

### 3.1 역할과 흐름

이름(문자열) → 리플렉션 객체 → 실제 조작의 세 단계로 나뉘고, 각 단계는 앞 단계의 결과만 주입받습니다.

```
MinecraftVersion ─┐
                  ├─▶ NmsNameFactory ─▶ *Names ─▶ *Members.resolve(names, loader) ─▶ *InventoryAccess
ServerRuntime ────┘   (문자열 조립)     (문자열)   (Class / Field / Method)           (리플렉션 수행)
```

| 단계      | 클래스                                              | 하는 일                                                                       |
|-----------|-----------------------------------------------------|-------------------------------------------------------------------------------|
| 서버 정보 | `MinecraftVersion`                                  | MC 버전 파싱·비교                                                             |
|           | `ServerRuntime`                                     | 서버에서 CraftBukkit 패키지, `MappingType`, ClassLoader를 감지 (§3.6)         |
| 이름 조립 | `NmsNameFactory`                                    | 버전과 서버 정보로 **완성된 이름**을 만듦. 이름 규칙은 모두 여기에만 있음     |
|           | `LegacyNameTable`, `ModernNameTable`                | 버전별 Spigot 멤버 이름표 (데이터만, package-private)                         |
|           | `InventoryModel`                                    | 버전 → `LEGACY` / `MODERN`                                                    |
| 데이터    | `LegacyNames`, `ModernNames`                        | 한 서버에서 쓸 클래스 전체 이름 + 멤버 이름. 로직 없음 (`@Builder`)           |
| 변환      | `LegacyMembers`, `ModernMembers`                    | Names + ClassLoader → `Class`/`Field`/`Method`. 선택 로직 없음                |
| 수행      | `LegacyInventoryAccess`, `ModernInventoryAccess`    | 저장소 모양별 capture/apply/슬롯 배치                                         |
|           | `Reflections`                                       | `invoke`/`get`/`set`/`load`/`accessible`와 예외 변환 (기술 유틸)              |
| 조립      | `ReflectionAccessProvider`                          | `InventoryModel` → `NmsNameFactory` → `Members.resolve` → Access              |
| 선택      | `InventoryAccessFactory`                            | 지원하는 첫 Provider를 고름 (§3.2)                                            |

**Legacy / Modern**

| 축                     | 기준                                                                                      | 달라지는 것                        | 결정 방법        | 처리하는 곳                    |
|------------------------|-------------------------------------------------------------------------------------------|------------------------------------|------------------|--------------------------------|
| 모델 (Legacy / Modern) | 갑옷·왼손 저장 위치: `Inventory`의 `armor`/`offhand` 리스트 ↔ `EntityEquipment` (1.21.5+) | 코드 구조 (Names, Members, Access) | MC 버전 (이름표) | `reflection.legacy`, `.modern` |
| 문자열                 | 매핑 종류(Spigot/Mojang), NMS 패키지 형태(1.17 전/후), CraftBukkit 패키지 재배치 여부     | 문자열만                           | 서버 감지 + 버전 | `NmsNameFactory`만             |

- 매핑 종류는 버전만으로 알 수 없습니다. 같은 1.20.4라도 Spigot은 Spigot 이름, Paper mojang 빌드는 Mojang 이름을 씁니다.
- 그래서 `LegacySpigot*`, `LegacyMojang*` 같은 클래스는 없습니다. Members와 Access는 어느 매핑 위에서 도는지 모릅니다.
- Legacy와 Modern은 **도메인 코드를 공유하지 않습니다.** 플레이어 → 인벤토리, 아이템 변환 같은 비슷한 코드가 양쪽에 있지만, Legacy(검증이 끝난 구간)가 Modern(계속 바뀌는 구간)의 변경에 영향받지 않게 하려는 의도입니다.

### 3.2 Provider와 예외

```
Provider 목록 = [등록 파일(ServiceLoader)로 찾은 Provider들 (dev 샌드박스)..., ReflectionAccessProvider (항상 마지막)]

InventoryAccessFactory.create(version, runtime) = supports(version)가 true인 첫 Provider의 create()
                                                  Provider가 없으면 UnsupportedVersionException 발생
```

| 클래스                                    | 책임                                                                              | 모르는 것                             |
|-------------------------------------------|-----------------------------------------------------------------------------------|---------------------------------------|
| `InventoryAccessProvider` (인터페이스)    | `supports(version)`: 버전을 맡을 수 있는지 판단                                   | 다른 Provider, 순서, 로그             |
|                                           | 완성된 Access 또는 `IncompatibleServerException`. `@NotNull` 보장                 |                                       |
| `ReflectionAccessProvider` (`reflection`) | 이름표에 있는 모든 버전. 모델 판별 → 이름 조립 → Members → Access. (배포 버전 용) | 자신이 마지막이라는 것, 다른 Provider |
| `DevInventoryAccessProvider` (dev)        | `supports` = 대상 버전 하나, `create` = `DirectInventoryAccess`                   |                                       |
| `InventoryAccessFactory` (`nms`)          | Provider 목록과 순서.                                                             | 이름표, 모델, 리플렉션                |

- `ReflectionAccessProvider`를 등록 파일로 찾지 않는 이유: ServiceLoader는 순서를 보장하지 않으므로, "항상 마지막"을 Factory가 직접 보장합니다.
- `supports`가 서버 정보를 받지 않는 이유: 지원 여부는 "이 버전을 지원하기로 했는가"라는 정책입니다. 서버 환경 때문에 생기는 실패는 아래의 `IncompatibleServerException`입니다.

| 예외                          | 뜻                                                                      | 던지는 곳                                                       | core의 처리                                |
|-------------------------------|-------------------------------------------------------------------------|-----------------------------------------------------------------|--------------------------------------------|
| `UnsupportedVersionException` | 이 플러그인이 지원하지 않는 버전. **신고 불필요**                       | `InventoryAccessFactory.create` (지원하는 Provider가 없을 때만) | `not_supported_version`, 비활성화          |
| `IncompatibleServerException` | 지원하는 버전인데 이 서버에서 Access를 만들지 못함. **버그, 신고 필요** | Provider의 `create` (이름 해석 실패, 있을 수 없는 서버 조합 등) | "This is a bug... 신고해 주세요", 비활성화 |

core는 `loadNMS()`에서 `create()`를 한 번만 호출하고, 그 결과(예외)로 지원 여부를 판단합니다. 지원 여부를 판단하는 곳이 `create()` 하나뿐이므로, Provider만 있는 버전(이름표에 없는 dev 버전)도 통과합니다.

### 3.3 패키지

```
com.sugar_tree.inventoryshare.nms            API: core와 dev 샌드박스가 쓰는 것
├─ InventoryAccess           core가 아는 유일한 NMS 경계
├─ InventoryStorage          불투명 핸들 (Legacy: items/armor/extraSlots, Modern: items/equipment map)
├─ InventoryAccessFactory    Provider 선택, create(version, runtime)
├─ InventoryAccessProvider   Access를 만드는 구현의 약속 (supports / create)
├─ UnsupportedVersionException   지원하지 않는 버전
├─ IncompatibleServerException   지원하는 버전인데 이 서버에서 생성 실패
│
├─ server/                   ① 서버 조사
│   ├─ MinecraftVersion      MC 버전 파싱/비교 ("1.21.11")
│   ├─ ServerRuntime         CraftBukkit 패키지 + MappingType + ClassLoader 감지
│   └─ MappingType           SPIGOT | MOJANG
│
└─ reflection/               ② 리플렉션 수행
    ├─ ReflectionAccessProvider   기본 Provider (항상 마지막)
    ├─ Reflections           리플렉션 유틸
    ├─ InventoryModel        LEGACY | MODERN, of(version)
    ├─ NmsNameFactory        legacy(version, runtime) / modern(version, runtime) → Names
    ├─ LegacyNameTable       버전별 Spigot 멤버 이름 (package-private)
    ├─ ModernNameTable       〃
    ├─ VersionRange          이름표 행의 버전 목록 / atLeast (package-private)
    ├─ legacy/
    │   ├─ LegacyNames
    │   ├─ LegacyMembers     resolve(LegacyNames, ClassLoader)
    │   └─ LegacyInventoryAccess
    └─ modern/
        ├─ ModernNames
        ├─ ModernMembers     resolve(ModernNames, ClassLoader)
        └─ ModernInventoryAccess
```

의존 방향:

```
server            → Bukkit만 (nms, reflection을 모름)
reflection        → server, nms (InventoryAccess, InventoryAccessProvider 구현)
nms (Factory)     → server, reflection.ReflectionAccessProvider 하나만
core              → nms, server
```

- 이름 조립(`NmsNameFactory`, 이름표)은 리플렉션에서만 쓰이므로 `reflection`에 둡니다. 이름표와 `VersionRange`는 같은 패키지의 `NmsNameFactory`, `InventoryModel`만 쓰므로 package-private입니다.
- 테스트는 대상 클래스와 같은 패키지에 둡니다. package-private 멤버를 테스트용으로 공개할 필요가 없습니다.

| 테스트                                                          | 패키지           |
|-----------------------------------------------------------------|------------------|
| `MinecraftVersionTest`, `ServerRuntimeTest`                     | `nms.server`     |
| `NmsNameFactoryTest`, `ReflectionAccessProviderTest`            | `nms.reflection` |
| `InventoryAccessFactoryTest`                                    | `nms`            |
| nms-test `LegacyMembersTest`, `ModernMembersTest`, `SpigotJars` | `nms.reflection` |

### 3.4 InventoryAccess

```java
public interface InventoryAccess {
    InventoryStorage createEmpty();                    // 공유/팀용 빈 인벤토리
    InventoryStorage capture(Player player);           // 플레이어의 현재 리스트/맵 참조
    void apply(Player player, InventoryStorage inv);   // 참조 교체 (공유의 핵심)
    ItemStack getItem(InventoryStorage inv, int slot); // Bukkit 사본
    void setItem(InventoryStorage inv, int slot, ItemStack item);
}
```

### 3.5 슬롯 모델

슬롯 번호는 Bukkit `PlayerInventory.getContents()` 순서와 같습니다.

| 슬롯    | 의미                   | Legacy          | Modern                               |
|---------|------------------------|-----------------|--------------------------------------|
| 0 – 35  | 보관함/핫바            | `items[i]`      | `items[i]`                           |
| 36 – 39 | 신발, 바지, 갑옷, 투구 | `armor[i - 36]` | `slotMapping.get(i)` → EquipmentSlot |
| 40      | 왼손                   | `extraSlots[0]` | `slotMapping.get(40)` → OFFHAND      |

### 3.6 서버 감지 (`ServerRuntime`)

같은 MC 버전이라도 서버에 따라 클래스 이름이 다릅니다.

| 서버                             | CraftBukkit 패키지               | 매핑 (`MappingType`)  |
|----------------------------------|----------------------------------|-----------------------|
| Spigot ≤ 1.21.11, Paper ≤ 1.20.4 | `org.bukkit.craftbukkit.vX_Y_RZ` | SPIGOT (1.17+ 난독화) |
| Paper 1.20.4 mojang 빌드         | `v1_20_R3`                       | **MOJANG**            |
| Paper 1.20.5 – 1.21.11           | **버전 없음**                    | **MOJANG**            |
| Spigot / Paper 26.1+             | 버전 없음                        | MOJANG                |

- **CraftBukkit 패키지**: `Bukkit.getServer().getClass()`(= `CraftServer`)의 패키지에서 읽습니다. 1.17 이전 NMS 패키지(`net.minecraft.server.vX_Y_RZ`)도 같은 값을 씁니다.
- **매핑**: 서버 ClassLoader의 `net.minecraft.world.entity.player.Inventory` 존재여부로 판별합니다. 존재 시 `MOJANG Mapping`입니다(Spigot 1.17 – 1.21.11에서는 `PlayerInventory`).
- **ClassLoader**: `CraftServer` 클래스의 ClassLoader입니다. 플러그인 ClassLoader는 다른 플러그인 jar까지 검색하므로 쓰지 않습니다.
- 플러그인 활성화 로그: `Minecraft 1.21.4 (CraftBukkit (not relocated), MOJANG mappings), using LegacyInventoryAccess`

### 3.7 이름 조립 (`NmsNameFactory`)

`NmsNameFactory.legacy(version, runtime)` / `modern(version, runtime)`이 클래스 전체 이름과 멤버 이름을 모두 채운 Names를 돌려줍니다. 아무것도 로드하지 않습니다.

| 문자열             | 규칙                                                                                            |
|--------------------|-------------------------------------------------------------------------------------------------|
| CraftBukkit 클래스 | `org.bukkit.craftbukkit[.<패키지>].entity.CraftPlayer`, `...inventory.CraftItemStack`           |
| 1.17 이전 NMS      | `net.minecraft.server.<패키지>.EntityHuman` 등. 인벤토리는 필드(`inventoryField = true`)        |
| 1.17 이후 NMS      | 평탄화된 패키지. 매핑에 따라 클래스 이름 선택 (아래 표)                                         |
| SPIGOT 멤버 이름   | 버전별 이름표(§3.9, §3.10)                                                                      |
| MOJANG 멤버 이름   | 모델마다 고정 상수. 각 모델 안에서 바뀌지 않았습니다(1.17.1 – 1.21.4, 1.21.5 – 26.2 javap 확인) |

| 클래스 (1.17+)          | SPIGOT                                              | MOJANG                                        |
|-------------------------|-----------------------------------------------------|-----------------------------------------------|
| Legacy 인벤토리 소유자  | `net.minecraft.server.level.EntityPlayer`           | `net.minecraft.server.level.ServerPlayer`     |
| Modern 인벤토리 소유자  | `net.minecraft.world.entity.player.EntityHuman`     | `net.minecraft.world.entity.player.Player`    |
| 플레이어 인벤토리       | `net.minecraft.world.entity.player.PlayerInventory` | `net.minecraft.world.entity.player.Inventory` |
| 장비 슬롯 enum (Modern) | `net.minecraft.world.entity.EnumItemSlot`           | `net.minecraft.world.entity.EquipmentSlot`    |

공통: `net.minecraft.world.entity.EntityEquipment`, `net.minecraft.world.item.ItemStack`, `net.minecraft.core.NonNullList`, `org.bukkit.inventory.ItemStack`.

| MOJANG 멤버 이름 | Legacy (1.17 – 1.21.4)                                                         | Modern (1.21.5+)                                                                                  |
|------------------|--------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| 인벤토리 접근    | `getInventory()`                                                               | `getInventory()`                                                                                  |
| 필드             | `items`, `armor`, `offhand`, `compartments`                                    | `items`, `equipment`, `EQUIPMENT_SLOT_MAPPING`, `EntityEquipment.items`                           |
| 아이템           | `ItemStack.EMPTY`, `NonNullList.withSize`                                      | `ItemStack.EMPTY`, `NonNullList.withSize`                                                         |

실제로 존재하지 않는 서버 조합은 `IncompatibleServerException`입니다(서버 감지가 잘못된 것이므로 신고 대상): 1.17 이전 + MOJANG, 1.17 이전 + 재배치 안 된 패키지, 26.1 이상 + SPIGOT(이름표에 Spigot 이름 없음).

### 3.8 멤버 해석, 버전 감지

`LegacyMembers.resolve(names, loader)` / `ModernMembers.resolve(names, loader)`는 **모든 클래스를 인자로 받은 ClassLoader에서** 찾습니다(Bukkit `ItemStack` 포함).

- 런타임: `ServerRuntime`의 서버 ClassLoader
- 테스트: 버전별로 격리된 `URLClassLoader` (`ServerRuntime.detect(craftServerClass, loader)`)

테스트가 플러그인과 **같은 조립·해석 코드**를 실행합니다. 모든 멤버는 플러그인 활성화 시점에 미리 해석되며, 실패하면 `IncompatibleServerException`으로 "버그이니 신고" 메시지와 함께 비활성화됩니다(§3.2).

```java
ServerRuntime runtime = ServerRuntime.current();                        // 패키지, 매핑, ClassLoader
InventoryAccess access = InventoryAccessFactory.create(version, runtime);
```

`InventoryAccessProvider`(dev 샌드박스)는 플러그인 jar의 `META-INF/services`에 있으므로, 서버 ClassLoader가 아니라 `InventoryAccessProvider` 클래스 자신의 ClassLoader(= 플러그인 jar)로 찾습니다.

버전:
- 이름표의 키는 **MC 버전 문자열**(`"1.21.11"`)입니다. Bukkit 버전 문자열(`1.21.11-R0.2-SNAPSHOT`)의 `-R0.x` 부분은 서버마다 다르므로 키로 쓰지 않습니다.
- Paper: `Server#getMinecraftVersion()`(리플렉션)을 사용합니다. Paper 26.x의 `getBukkitVersion()`은 `26.1.2.build.63-stable` 형식입니다 ([PaperMC/Paper#13885](https://github.com/PaperMC/Paper/issues/13885)).
- Spigot: `Bukkit.getBukkitVersion()`에서 `-` 앞부분을 사용합니다.
- 26.1 이상은 `ModernNameTable.v26`(`atLeast("26.1")`)으로 처리합니다. 난독화가 없어 이름이 안정적이므로 미래 버전도 일단 시도하고, 해석에 실패하면 비활성화합니다.

### 3.9 Legacy 이름표 (`LegacyNameTable`, 1.12 – 1.21.4, SPIGOT)

| 행                              | MC                   | inventory         | items | armor | extraSlots | contents | emptyItem |
|---------------------------------|----------------------|-------------------|-------|-------|------------|----------|-----------|
| v1_12_R1                        | 1.12, 1.12.1, 1.12.2 | inventory (field) | items | armor | extraSlots | f        | a         |
| v1_13_R1                        | 1.13                 | inventory         | items | armor | extraSlots | f        | a         |
| v1_13_R2                        | 1.13.1, 1.13.2       | inventory         | items | armor | extraSlots | f        | a         |
| v1_14_R1                        | 1.14 – 1.14.4        | inventory         | items | armor | extraSlots | f        | a         |
| v1_15_R1                        | 1.15 – 1.15.2        | inventory         | items | armor | extraSlots | f        | a         |
| v1_16_R1                        | 1.16.1               | inventory         | items | armor | extraSlots | f        | b         |
| v1_16_R2                        | 1.16.2, 1.16.3       | inventory         | items | armor | extraSlots | f        | b         |
| v1_16_R3                        | 1.16.4, 1.16.5       | inventory         | items | armor | extraSlots | f        | b         |
| v1_17_R1                        | 1.17, 1.17.1         | getInventory()    | h     | i     | j          | n        | b         |
| v1_18_R1                        | 1.18                 | fq()              | h     | i     | j          | n        | b         |
| v1_18_R2                        | 1.18.1, 1.18.2       | fr()              | h     | i     | j          | n        | b         |
| v1_19_R1                        | 1.19                 | fB()              | h     | i     | j          | n        | b         |
| v1_19_1_R1 (CB 패키지 v1_19_R1) | 1.19.1, 1.19.2       | fA()              | h     | i     | j          | n        | b         |
| v1_19_R2                        | 1.19.3               | fE()              | h     | i     | j          | n        | b         |
| v1_19_R3                        | 1.19.4               | fJ()              | i     | j     | k          | o        | b         |
| v1_20_R1                        | 1.20, 1.20.1         | fN()              | i     | j     | k          | o        | b         |
| v1_20_R2                        | 1.20.2               | fR()              | i     | j     | k          | o        | b         |
| v1_20_R3                        | 1.20.3, 1.20.4       | fS()              | i     | j     | k          | o        | f         |
| v1_20_R4                        | 1.20.5, 1.20.6       | gc()              | h     | i     | j          | n        | l         |
| v1_21_R1                        | 1.21, 1.21.1         | fY()              | h     | i     | j          | n        | l         |
| v1_21_R2                        | 1.21.2, 1.21.3       | gi()              | g     | h     | i          | l        | j         |
| v1_21_R3                        | 1.21.4               | gi()              | g     | h     | i          | l        | j         |

### 3.10 Modern 이름표 (`ModernNameTable`, 1.21.5 이상, SPIGOT)

| 행       | MC               | getInventory                                  | items | equipment | EntityEquipment.items | slotMapping | EMPTY | createList |
|----------|------------------|-----------------------------------------------|-------|-----------|-----------------------|-------------|-------|------------|
| v1_21_R4 | 1.21.5           | gj                                            | i     | k         | b                     | g           | l     | a          |
| v1_21_R5 | 1.21.6 – 1.21.8  | gs                                            | k     | m         | b                     | i           | l     | a          |
| v1_21_R6 | 1.21.9 – 1.21.10 | gB                                            | l     | n         | b                     | i           | l     | a          |
| v1_21_R7 | 1.21.11          | gK                                            | l     | n         | b                     | i           | l     | a          |
| v26      | 26.1 이상        | (Spigot 이름 없음 — 모든 서버가 MOJANG, §3.7) |

- AI가 `javap`를 사용해 추출해 낸 이름입니다. 확실하지 않습니다.
- `ModernMembersTest`가 1.21.5, 1.21.6, 1.21.8, 1.21.10, 1.21.11, 26.1.2, 26.2 jar로 멤버 존재·타입과 슬롯 36 – 40 매핑을 검증했습니다. 1.21.7, 1.21.9는 jar가 없어 검증되지 않은 추정입니다.

---

## 4. 저장 형식

```yaml
format-version: 1
items:      [ ...36개 ]   # 슬롯 0 – 35
armor:      [ ...4개 ]    # 슬롯 36 – 39 (feet, legs, chest, head)
extraSlots: [ ...1개 ]    # 슬롯 40 (offhand)
```

- 공유 인벤토리는 `inventory.yml`, 팀 인벤토리는 `teams/<team>.yml`에 저장합니다.
- 각 아이템은 `ItemStack#serialize()` 로 직렬화됩니다.

### 로드할 수 없는 데이터 (다운그레이드 등)

- 아이템 단위로 처리합니다. 문제가 있는 아이템만 건너뛰고 나머지는 로드합니다.
- 다음 경우 해당 슬롯을 **빈칸으로 둡니다**:
  - 아이템의 data version(`v`)이 서버보다 높음
  - `v`가 숫자가 아님, 또는 `ItemStack.deserialize` 실패
- 파일의 `format-version`이 플러그인이 아는 값보다 높으면 경고합니다.
- 위 중 하나라도 있으면 **원본 파일을 `backups/<inventory|team-이름>-<yyyyMMdd-HHmmss>[-dv<가장 높은 data version>].yml`로 복사**하고, 건너뛴 슬롯(이유별로 한 줄, 예: `38 item(s) saved by a newer Minecraft version (...): slots 0-35, 38, 40`)과 백업 경로를 `SEVERE` 로그로 남깁니다.
- 복구: 그 데이터를 읽을 수 있는 서버(예: 원래의 상위 버전)에서 백업 파일을 `inventory.yml` / `teams/<team>.yml` 자리에 되돌립니다.

---

## 5. 테스트

### 5.1 `InventoryShare-nms` 단위 테스트 (항상 실행)
Spigot jar 없이 실행합니다.
- `MinecraftVersionTest`, `ServerRuntimeTest`: 버전 파싱·비교, CraftBukkit 패키지 감지
- `NmsNameFactoryTest`: 이름 조립. `ServerRuntime`을 직접 만들어 SPIGOT/MOJANG, 패키지 재배치 여부, 있을 수 없는 조합별로 확인
- `ReflectionAccessProviderTest`: 이름표 기준 `supports`, 서버 클래스가 없을 때 `IncompatibleServerException`
- `InventoryAccessFactoryTest`: Provider 우선순위, 이름표에 없는 버전도 Provider가 맡으면 지원, 지원하지 않는 버전

### 5.1.1 `InventoryShare-bukkit` 단위 테스트 (항상 실행)
- `file` 패키지: 아이템 로드 가능 여부 판정(`CodecTest`), 이유별 로그 문구(`LoadResultTest`), 백업 파일 생성과 지울 팀 파일 판정(`FileManagerTest`)
- `utils` 패키지: 언어 파일 경로(`I18NFileManagerTest`)

### 5.2 `InventoryShare-nms-test` 이름 검증 테스트
- `LegacyMembersTest` (1.12.2 – 1.21.4), `ModernMembersTest` (1.21.5 – 26.x): `@ParameterizedTest`
- 각 버전의 Spigot 런타임 jar(classifier 없는 기본 jar)와, 1.17 이상은 Mojang 이름의 `remapped-mojang` jar(Paper 1.20.5+ 대용)를 격리된 ClassLoader로 불러, jar에서 `ServerRuntime`을 감지하고 플러그인과 같은 `NmsNameFactory` → `resolve()`를 실행한 뒤 필드 타입까지 검증합니다.
- jar가 mavenLocal에 없으면 그 버전만 **skip**합니다. `-PnmsStrict`를 주면 누락 시 실패합니다.
- 26.x 클래스 파일을 읽어야 하므로 테스트 JVM은 Java 25입니다.

### 5.3 실행 방법
- 전체: `./gradlew test` (또는 `./gradlew build`)
- NMS만: `./gradlew :InventoryShare-nms:test :InventoryShare-nms-test:test`
- 검사할 버전 목록은 `InventoryShare-nms-test/build.gradle`의 `spigotJars`입니다. `LegacyNameTable`/`ModernNameTable`의 각 행이 최소 한 버전으로 검사되는지도 테스트합니다(`everyLegacyRowIsCovered`, `everyModernRowIsCovered`).
- Spigot classpath는 `build/spigot-classpaths.properties`로 테스트에 전달됩니다(Windows 명령줄 길이 제한 회피).

---

## 6. 새 마인크래프트 버전 추가 절차

[CONTRIBUTING.md §5–6](../CONTRIBUTING.md#5-새-마인크래프트-버전-추가)

---

## 7. 알려진 위험 요소

- **JEP 500 (JDK 26, "Prepare to Make Final Mean Final")**: 리플렉션으로 final 필드를 바꾸는 것에 경고가 나오고 이후 기본 차단될 예정입니다. 이 플러그인의 핵심 동작이므로, 필요해지면 `InventoryAccess` 뒤에서 "내용 복사 동기화" 구현으로 교체합니다.
- **Modern 장비 맵 공유**: `EntityEquipment`의 EnumMap을 통째로 공유하면 플레이어가 쓰지 않는 슬롯(BODY, SADDLE)도 함께 공유됩니다. 무해할 것으로 보지만 실서버에서 확인합니다.
- **실행 중 쓰기 실패**: 시작할 때는 필드를 찾고 접근 권한을 얻는 것까지만 확인하므로, 필드 쓰기가 막히는 환경(JEP 500 등)에서는 첫 접속 때 처음 실패합니다.
- **시작 실패 메시지**: dev Provider의 Error, 버전 문자열 해석 실패, 로드 단계의 실패 등 일부는 실패 종류별 메시지 없이 서버의 일반 오류로 끝납니다. 플러그인은 비활성화됩니다.
