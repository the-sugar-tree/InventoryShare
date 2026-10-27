# InventoryShare Architecture

InventoryShare가 어떻게 동작하고, 코드가 어떻게 나뉘어 있는지 설명합니다.

- 빌드·테스트·기여 방법: [CONTRIBUTING.md](../CONTRIBUTING.md)
- 서버 내부 클래스(NMS), Spigot과 Paper의 차이: [NMS_GUIDE.md](NMS_GUIDE.md)

> [!important]
> 문서 정리 작업 중입니다. 현재 AI와 함께 편집한 문서로, 읽기 불편할 수 있습니다. 양해바랍니다.

---

## 1. 개요

InventoryShare는 서버의 모든 플레이어(또는 같은 팀의 플레이어)가 **인벤토리 하나를 함께 쓰게** 하는 Bukkit 플러그인입니다.

### 동작 원리

마인크래프트 서버 안에서 플레이어 인벤토리는 "칸 목록(리스트)"을 가진 객체입니다. InventoryShare는 각 플레이어의 인벤토리 객체 안에 있는 칸 목록을 **모두 같은 리스트 객체로 바꿔 끼웁니다.**
그러면 한 명이 아이템을 넣거나 빼는 순간 다른 플레이어의 인벤토리에도 그대로 보입니다. 아이템을 복사해서 맞추는 방식이 아니라서 따로 동기화할 필요가 없습니다.

Bukkit API는 아이템을 복사해서 넣고 빼는 방법만 제공하므로, 이 "참조 공유"에는 서버 내부 클래스(NMS)를 리플렉션으로 직접 다뤄야 합니다. NMS 클래스 이름은 서버 종류(Spigot / Paper)와 버전마다 다릅니다. 자세한 내용은 [NMS_GUIDE.md](NMS_GUIDE.md)를 참고하세요.

### 지원 범위

- 마인크래프트 1.12.2 – 26.x, Spigot과 Paper. **jar 하나**로 모든 버전을 지원합니다.
- 인벤토리 구조가 1.21.5에서 바뀌어서, 두 가지 구현을 둡니다.

| 구조       | 버전          | 공유하는 것                                                                |
|------------|---------------|----------------------------------------------------------------------------|
| **Legacy** | 1.12 – 1.21.4 | 인벤토리 객체의 보관함·핫바 리스트, 갑옷 리스트, 왼손 리스트               |
| **Modern** | 1.21.5 이상   | 인벤토리 객체의 보관함·핫바 리스트, 장비 객체(`EntityEquipment`)의 장비 맵 |

---

## 2. 모듈과 의존 방향

```
InventoryShare/
├─ InventoryShare-bukkit/      배포 플러그인 (core). spigot-api로만 컴파일, Java 8 대상
├─ InventoryShare-nms/         NMS 접근 계층: InventoryAccess, ItemCodec, 서버 감지, 리플렉션 구현. Java 8 대상
├─ InventoryShare-nms-test/    NMS 이름을 실제 Spigot·Paper jar로 검증하는 테스트
├─ InventoryShare-dev/         개발용 샌드박스(<ver>/)와 로컬 Paper 테스트 서버 Task. -Pdev일 때만 포함, 배포 안 함
└─ docs/
```

- `InventoryShare-sponge/`: Sponge 서버용으로 시작했다가 중단된 폴더입니다. 빌드에 포함되지 않습니다.

```
InventoryShare-bukkit ──▶ InventoryShare-nms ◀── InventoryShare-nms-test
        ▲
InventoryShare-dev/<ver>  (core는 그대로, InventoryAccess 구현만 주입)
```

설계 원칙:
- **core는 NMS를 직접 다루지 않습니다.** 인벤토리 교체는 `InventoryAccess`(§4.4), 아이템 저장 변환은 `ItemCodec`(§4.5)으로만 합니다.
- **배포 jar는 리플렉션만 씁니다.** 그래서 기본 빌드에 BuildTools(Spigot 서버 jar)가 필요 없습니다. Spigot 서버 jar는 테스트(`InventoryShare-nms-test`)와 개발 샌드박스(`InventoryShare-dev`)에서만 씁니다.
- `InventoryShare-nms`의 jar는 `InventoryShare-bukkit` jar 안에 함께 들어갑니다.
- `InventoryShare-nms`는 spigot-api와 DataFixerUpper(`ItemCodec`용, 서버가 제공)로 컴파일합니다.

---

## 3. core 동작 (`InventoryShare-bukkit`)

### 3.1 패키지와 클래스

| 패키지 / 클래스                                | 역할                                                                                     |
|------------------------------------------------|------------------------------------------------------------------------------------------|
| `InventoryShare`                               | 플러그인 진입점. 시작·종료 순서(§3.4)                                                    |
| `Commands`                                     | `/inventoryshare` 명령어. 설정 켜기·끄기, `reload`, `check`                              |
| `Listeners`                                    | 접속·퇴장·월드 저장·발전과제·사망 이벤트, 팀 변경 감지(`onTick`)                         |
| `PluginSettings`                               | `config.yml`의 설정값(`inventory`, `teaminventory` 등) 읽기·저장·`reload`                |
| `SharedConstants`                              | 플러그인 인스턴스, 로거, 접두어, 데이터 버전 등 바뀌지 않는 전역 값                      |
| `inventory/InventoryRegistry`                  | 인벤토리 보관소: 전체 공유 1개, 팀별 1개, 공유하는 동안 맡아 둔 플레이어별 개인 인벤토리 |
| `inventory/InventoryManager`                   | 플레이어를 개인 / 전체 공유 / 팀 인벤토리 사이에서 전환 (§3.2)                           |
| `file/FileManager`, `Codec`, `LoadResult`      | 공유·팀 인벤토리와 발전과제 저장·로드(§5)                                                |
| `utils/I18NUtil`                               | 언어 파일(`languages/lang_<locale>.yml`) 저장·갱신과 메시지                              |
| `utils/AdvancementUtil`, `AdvancementRegistry` | 발전과제 공유와 공유된 발전과제 목록 (§3.5)                                              |
| `utils/UpdateUtil`                             | 새 릴리스 확인                                                                           |
| `protocollib/ProtocolLibManager`               | ProtocolLib이 있으면, 블록을 캐는 중 아이템 갱신으로 채굴이 끊기는 문제 보정 (§3.5)      |

### 3.2 인벤토리 상태

`InventoryManager`는 플레이어마다 지금 어떤 인벤토리를 쓰고 있는지 기록합니다.

| 상태       | 뜻                                                                                |
|------------|-----------------------------------------------------------------------------------|
| `PERSONAL` | 자기 인벤토리를 쓰는 중 (처음 상태)                                               |
| `SAVED`    | 자기 인벤토리를 `InventoryRegistry`에 맡겨 두었고, 곧 공유 인벤토리를 적용할 상태 |
| `ALL`      | 전체 공유 인벤토리를 쓰는 중                                                      |
| `TEAM`     | 팀 인벤토리를 쓰는 중. 어느 팀인지도 함께 기록합니다(`appliedTeams`)              |

```
                  공유 적용 전: 자기 인벤토리를 맡겨 둠 (capture)
PERSONAL ───────────────────────────────▶ SAVED ──apply──▶ ALL / TEAM
    ▲                                                          │
    └─────── 맡겨 둔 자기 인벤토리를 되돌림 (apply) ───────────┘
```

어떤 인벤토리를 적용할지는 `updateInventory(player)`가 설정에 따라 정합니다.

```
inventory = false                         → 개인 인벤토리
inventory = true, teaminventory = false   → 전체 공유 인벤토리
inventory = true, teaminventory = true    → 팀이 있으면 그 팀의 인벤토리, 없으면 전체 공유 인벤토리
```

팀 인벤토리는 팀이 처음 쓰일 때 빈 인벤토리로 만들어집니다. 다른 팀으로 옮기면 옮긴 팀의 인벤토리로 바뀝니다.

### 3.3 이벤트별 처리

| 시점                                         | 처리                                                                                                                      |
|----------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| 플레이어 접속                                | 설정에 맞는 인벤토리 적용, 발전과제 맞추기                                                                                |
| 플레이어 퇴장                                | 개인 인벤토리로 되돌림 → 그 플레이어의 상태 기록 삭제 → 저장                                                              |
| 월드 저장                                    | 공유·팀 인벤토리 저장. 공유 중이면 각 플레이어를 잠시 개인 인벤토리로 되돌려 서버의 플레이어 데이터를 저장한 뒤 다시 적용 |
| 매 틱 (`onTick`)                             | 팀 인벤토리가 켜져 있으면 플레이어의 팀이 바뀌었는지 확인하고, 바뀌었으면 인벤토리를 다시 적용                            |
| `/inventoryshare inventory`, `teaminventory` | 설정을 바꾸고 접속 중인 모든 플레이어에게 다시 적용                                                                       |
| `/inventoryshare reload`                     | `config.yml`과 언어 파일을 다시 읽고 접속 중인 플레이어에게 다시 적용                                                     |
| 발전과제 달성                                | 발전과제 목록에 추가, 설정이 켜져 있으면 모두에게 달성 처리                                                               |
| 사망                                         | `announcedeath`가 켜져 있으면 사망 위치 알림. 아이템이 떨어지는 처리는 게임이 하며, 공유 리스트라 모두에게 반영됩니다     |

**서버의 플레이어 데이터에는 항상 개인 인벤토리가 저장됩니다.** 퇴장·월드 저장·종료 때 개인 인벤토리로 되돌린 뒤 저장되기 때문입니다. 공유 인벤토리는 플러그인의 파일(§5)에만 저장됩니다.

인벤토리 적용(`InventoryAccess#apply`)이 실패하면 오류를 로그에 남기고 아무것도 바꾸지 않습니다. 플레이어는 쓰던 인벤토리를 계속 쓰고, 상태 기록도 그대로입니다. 개인 인벤토리로 되돌리기가 실패하면 개인 인벤토리는 저장된 채로 남아 다음 시도(다음 월드 저장, 설정 변경 등) 때 다시 적용됩니다. 다만 그 사이에 서버가 플레이어 데이터를 저장하면 공유 인벤토리 내용이 저장됩니다. 퇴장할 때 실패하면 다음 시도가 없어, 재접속할 때 저장된 개인 인벤토리를 잃습니다.

### 3.4 시작과 종료

시작 (`onEnable`):

```
언어 파일 → bStats → 업데이트 확인
→ MinecraftVersion.current()
→ loadNMS(): ServerRuntime.current() → InventoryAccessFactory.create() → InventoryRegistry / InventoryManager / FileManager
→ ProtocolLib 연동
→ config.yml 생성 → FileManager.load()(인벤토리·발전과제 파일 생성·로드) → 명령어·리스너 등록 → 접속 중인 플레이어에 공유 적용(/reload 시)
```

버전 판별, `loadNMS()`, `FileManager.load()` 중 하나가 실패하면 플러그인을 비활성화합니다.

| 실패                                      | 원인                                                        | 로그                                                          |
|-------------------------------------------|-------------------------------------------------------------|---------------------------------------------------------------|
| `UnsupportedServerException`              | CraftBukkit 기반이 아닌 서버, 또는 읽을 수 없는 버전 문자열 | "이 플러그인은 이 서버를 지원하지 않습니다" (신고 불필요)     |
| `UnsupportedVersionException`             | 지원하지 않는 마인크래프트 버전                             | "이 플러그인은 이 버전을 지원하지 않습니다" (신고 불필요)     |
| `IncompatibleServerException`             | 지원하는 버전인데 서버 내부 이름을 찾지 못함 (§4.2)         | "Could not hook into Minecraft ... This is a bug" + 신고 안내 |
| `FileManager.load()`의 `RuntimeException` | 공유·팀 인벤토리를 읽는 중 오류                             | "Could not load the saved inventories ..." + 신고 안내        |

종료 (`onDisable`): 각 플레이어를 개인 인벤토리로 되돌리고, 팀 감지 작업을 멈춘 뒤 저장합니다. 시작 중 `setEnabled(false)`로 비활성화하면 `onDisable()`이 바로 실행되는데, 시작이 끝나지 않았으면 파일을 저장하지 않습니다. 덜 읽힌 상태로 파일을 덮어쓰지 않기 위해서입니다.

### 3.5 부가 기능

- **발전과제 공유:** 누군가 달성한 발전과제를 목록(`advancements.yml`)에 모아 두고, 설정(`advancement`)이 켜져 있으면 접속 중인 모든 플레이어에게 남은 조건을 달성 처리합니다.
- **ProtocolLib 보정 (1.13 이상):** 인벤토리를 공유하면, 한 플레이어가 블록을 캐는 동안 다른 플레이어가 같은 도구를 써서 내구도가 바뀔 수 있습니다. 이때 서버가 보내는 아이템 갱신 패킷 때문에 채굴이 처음부터 다시 시작됩니다. 그래서 블록을 캐는 동안 손에 든 아이템의 갱신 패킷을 막고, 채굴이 끝나면 인벤토리를 한 번 갱신합니다. ProtocolLib이 없어도 플러그인은 동작합니다.

---

## 4. NMS 접근 계층 (`InventoryShare-nms`)

### 4.1 역할과 흐름

이름(문자열) → 리플렉션 객체 → 실제 조작의 세 단계로 나뉘고, 각 단계는 앞 단계의 결과만 받습니다.

```
MinecraftVersion ─┐
                  ├─▶ *NameFactory ─▶ *Names ─▶ *Members.resolve(names, loader) ─▶ *InventoryAccess, VanillaItemCodec
ServerRuntime ────┘   (문자열 조립)    (문자열)   (Class / Field / Method)           (리플렉션 수행)
```

| 단계      | 클래스                                                     | 하는 일                                                                   |
|-----------|------------------------------------------------------------|---------------------------------------------------------------------------|
| 서버 정보 | `MinecraftVersion`                                         | 마인크래프트 버전 파싱·비교                                               |
|           | `ServerRuntime`                                            | 서버에서 CraftBukkit 패키지, 이름 체계(`MappingType`), 클래스 로더를 감지 |
| 이름 조립 | `InventoryNameFactory`, `ItemNameFactory`                  | 버전과 서버 정보로 **완성된 클래스·멤버 이름**을 만듦                     |
|           | `NmsNames`                                                 | 두 Factory가 함께 쓰는 이름 규칙                                          |
|           | `LegacyNameTable`, `ModernNameTable`, `ItemCodecNameTable` | 버전별 Spigot 멤버 이름표 (데이터만)                                      |
|           | `InventoryModel`                                           | 버전 → `LEGACY` / `MODERN`                                                |
| 데이터    | `LegacyNames`, `ModernNames`, `ItemCodecNames`             | 한 서버에서 쓸 이름 묶음. 로직 없음                                       |
| 변환      | `LegacyMembers`, `ModernMembers`, `ItemCodecMembers`       | 이름 + 클래스 로더 → `Class`/`Field`/`Method`                             |
| 수행      | `LegacyInventoryAccess`, `ModernInventoryAccess`           | 구조별 capture / apply / 칸 읽기·쓰기                                     |
|           | `VanillaItemCodec`                                         | 아이템 ↔ 저장 데이터 (§4.5)                                               |
|           | `Reflections`                                              | 리플렉션 호출과 예외 변환 유틸                                            |
| 조립      | `ReflectionAccessProvider`                                 | `InventoryModel` → `InventoryNameFactory` → `Members.resolve` → Access    |
|           | `ItemCodecFactory`                                         | `ItemNameFactory` → `ItemCodecMembers.resolve` → `VanillaItemCodec`       |
| 선택      | `InventoryAccessFactory`                                   | 이 버전을 맡을 Provider를 고름 (§4.2)                                     |

이름이 달라지는 이유는 두 가지이고, 서로 다른 곳에서 처리합니다.

> [!tip]
> 자세한 정보는 [NMS_GUIDE.md](NMS_GUIDE.md)를 확인해주세요.

| 축                     | 무엇이 달라지나                                                           | 어떻게 정하나     | 처리하는 곳                              |
|------------------------|---------------------------------------------------------------------------|-------------------|------------------------------------------|
| 구조 (Legacy / Modern) | 코드 구조 (Names, Members, Access)                                        | 마인크래프트 버전 | `reflection.inventory.legacy`, `.modern` |
| 이름                   | 이름 체계(Spigot/Mojang), NMS 패키지 형태(1.17 전/후), CraftBukkit 패키지 | 서버 감지 + 버전  | `*NameFactory`, `NmsNames`               |

- 이름 체계는 버전만으로 알 수 없습니다. 같은 1.20.4라도 Spigot은 Spigot 이름, Paper Mojang 빌드는 Mojang 이름을 씁니다. 그래서 `LegacySpigot*`, `LegacyMojang*` 같은 클래스는 없고, Members와 Access는 이름 체계와 상관없이 같은 코드로 동작합니다.
- Legacy와 Modern은 **도메인 코드를 공유하지 않습니다.** 비슷한 코드(플레이어 → 인벤토리, 아이템 변환)가 양쪽에 있지만, 더 이상 바뀌지 않는 Legacy가 계속 바뀌는 Modern의 변경에 영향받지 않게 하려는 의도입니다. 공유하는 것은 기술 유틸(`Reflections`)과 이름 조립(`InventoryNameFactory`)뿐입니다.

### 4.2 Provider와 예외

`InventoryAccess`를 만드는 구현을 Provider라고 부릅니다.

```
Provider 목록 = [ServiceLoader로 찾은 Provider들 (dev 샌드박스)..., ReflectionAccessProvider (항상 마지막)]

InventoryAccessFactory.create(version, runtime) = supports(version)가 true인 첫 Provider의 create()
                                                  맡을 Provider가 없으면 UnsupportedVersionException
```

| 클래스                                      | 책임                                                                                 |
|---------------------------------------------|--------------------------------------------------------------------------------------|
| `InventoryAccessProvider` (인터페이스)      | `supports(version)`: 이 버전을 맡는지 판단. 버전만 보고, 부작용 없음                 |
|                                             | `create(version, runtime)`: 완성된 Access를 돌려주거나 `IncompatibleServerException` |
|                                             |     `create()`로 Access를 돌려주기 전에, access가 동작하는지 테스트                  |
| `ReflectionAccessProvider`                  | 이름표에 있는 모든 버전. 배포 jar가 쓰는 기본 구현                                   |
| `DevInventoryAccessProvider` (dev 샌드박스) | 대상 버전 하나만 맡고, 직접 NMS로 작성한 `DirectInventoryAccess`를 돌려줌            |
| `InventoryAccessFactory`                    | Provider 목록과 순서. **지원 여부를 판단하는 유일한 곳**                             |

- `ReflectionAccessProvider`는 ServiceLoader로 찾아지는 것이 아니며, Factory에 의해 마지막 순서가 보장됩니다.
- 즉, dev 샌드박스의 Provider가 앞에 오므로, 이름표에 아직 없는 새 버전도 샌드박스로 실행할 수 있습니다.
- `supports`가 서버 정보를 받지 않는 이유: 지원 여부는 "이 버전을 지원하기로 했는가"라는 정책입니다. 서버 환경 때문에 생기는 실패는 `IncompatibleServerException`입니다.

| 예외                          | 뜻                                                                             | 던지는 곳                                                            |
|-------------------------------|--------------------------------------------------------------------------------|----------------------------------------------------------------------|
| `UnsupportedServerException`  | CraftBukkit 기반이 아닌 서버. 버전과 상관없이 **신고 불필요**                  | `ServerRuntime.current` (서버 클래스가 CraftBukkit에 없을 때)        |
| `UnsupportedVersionException` | 이 플러그인이 지원하지 않는 버전. **신고 불필요**                              | `InventoryAccessFactory.create` (맡을 Provider가 없을 때)            |
| `IncompatibleServerException` | 지원하는 버전인데 이 서버에서 Access나 코덱을 만들지 못함. **버그, 신고 필요** | Provider의 `create`, `ItemCodecFactory.create` (이름을 찾지 못함 등) |

### 4.3 패키지

```
com.sugar_tree.inventoryshare.nms                    API: core와 dev 샌드박스가 쓰는 것
├─ InventoryAccess, InventoryStorage                 인벤토리 칸 목록 교체 (§4.4)
├─ InventoryAccessFactory, InventoryAccessProvider   Provider 선택과 약속 (§4.2)
├─ ItemCodec, ItemCodecFactory                       아이템 ↔ 저장 데이터, 1.20.5 이상 (§4.5)
│
├─ server/                 MinecraftVersion, ServerRuntime, MappingType, 시작 실패 예외 3개
│
└─ reflection/             Reflections, VersionRange, NmsNames
    ├─ inventory/          ReflectionAccessProvider, InventoryModel, InventoryNameFactory
    │   ├─ legacy/         LegacyNameTable, LegacyNames, LegacyMembers, LegacyInventoryAccess
    │   └─ modern/         ModernNameTable, ModernNames, ModernMembers, ModernInventoryAccess
    └─ item/               ItemCodecNameTable, ItemNameFactory, ItemCodecNames, ItemCodecMembers, VanillaItemCodec
```

### 4.4 `InventoryAccess`

```java
public interface InventoryAccess {
    InventoryStorage createEmpty();                    // 공유·팀용 빈 인벤토리
    InventoryStorage capture(Player player);           // 플레이어가 지금 쓰는 칸 목록(참조)
    void apply(Player player, InventoryStorage inv);   // 플레이어의 칸 목록을 inv로 바꿔 끼움 (공유의 핵심)
    ItemStack getItem(InventoryStorage inv, int slot); // 사본 (CraftItemStack, NMS 데이터 그대로)
    void setItem(InventoryStorage inv, int slot, ItemStack item);
}
```

칸 번호는 Bukkit `PlayerInventory.getContents()` 순서와 같습니다.

| 칸      | 의미                   | Legacy                 | Modern                                  |
|---------|------------------------|------------------------|-----------------------------------------|
| 0 – 35  | 보관함·핫바            | 보관함 리스트 `[i]`    | 보관함 리스트 `[i]`                     |
| 36 – 39 | 신발, 바지, 갑옷, 투구 | 갑옷 리스트 `[i - 36]` | 장비 맵 (칸 번호 → 장비 슬롯 표로 변환) |
| 40      | 왼손                   | 왼손 리스트 `[0]`      | 장비 맵 (OFFHAND)                       |

### 4.5 `ItemCodec` (1.20.5 이상)

- 마인크래프트의 `ItemStack.CODEC`으로 아이템을 NBT로 바꾸고, 컴포넌트마다 SNBT 문자열로 저장합니다. 형식은 Paper 1.21.5 이상의 `ItemStack#serialize()`와 같습니다(§5).
- `ItemMeta`를 거치지 않아 데이터가 빠지지 않고, Spigot과 Paper가 서로의 데이터를 읽습니다.
- 더 낮은 데이터 버전의 아이템은 서버의 DataFixer로 변환한 뒤 읽습니다.
- `VanillaItemCodec`은 DataFixerUpper 타입을 직접 써서 1.20.5 이상에서만 로드됩니다. 만들 때 아이템 하나를 저장했다 읽어 보고, 실패하면 `IncompatibleServerException`입니다.

### 4.6 서버 감지와 이름 조립

- **서버 감지 (`ServerRuntime`)**
  - CraftBukkit 패키지: `Bukkit.getServer()`의 클래스(`CraftServer`) 패키지에서 읽습니다.
  - 이름 체계: 서버 클래스 로더에 `net.minecraft.world.entity.player.Inventory`가 있으면 Mojang 이름(`MOJANG`), 없으면 Spigot 이름(`SPIGOT`)입니다.
  - 클래스 로더: `CraftServer`의 클래스 로더를 씁니다. 플러그인 클래스 로더는 다른 플러그인 jar까지 검색하므로 쓰지 않습니다.
- **이름 조립 (`InventoryNameFactory`, `ItemNameFactory`)**
  - CraftBukkit 클래스: `org.bukkit.craftbukkit[.<패키지>].entity.CraftPlayer` 등
  - NMS 클래스: 1.17 이전은 `net.minecraft.server.<패키지>.<이름>`, 1.17 이후는 이름 체계에 따라 Spigot 또는 Mojang 클래스 이름
  - Spigot 멤버 이름: 버전별 이름표(`LegacyNameTable`, `ModernNameTable`, `ItemCodecNameTable`)
  - Mojang 멤버 이름: 구조마다 고정값. Legacy(1.17 – 1.21.4), Modern(1.21.5 – 26.x) 구간 안에서 바뀌지 않았습니다.
  - 있을 수 없는 조합(1.17 이전 + Mojang 이름, 26.1 이상 + Spigot 이름 등)은 서버 감지가 잘못된 것으로 보고 `IncompatibleServerException`을 던집니다.

서버별 실제 클래스·멤버 이름은 [NMS_GUIDE.md §4](NMS_GUIDE.md#4-서버별로-실제로-쓰이는-이름)에 정리되어 있고, 버전별 전체 이름은 코드의 이름표가 기준입니다.

플러그인이 켜질 때 감지 결과가 로그에 남습니다.
```
Minecraft 1.21.4 (CraftBukkit (not relocated), MOJANG mappings), using LegacyInventoryAccess
```

### 4.7 멤버 해석과 버전 감지

- `*Members.resolve(names, loader)`는 모든 클래스를 **인자로 받은 클래스 로더**에서 찾습니다. 서버에서는 서버 클래스 로더, 테스트에서는 버전별로 격리한 클래스 로더를 넘기므로, **테스트가 플러그인과 같은 코드**를 실행합니다.
- 모든 이름은 플러그인이 켜질 때 한 번에 해석합니다. 하나라도 없으면 `IncompatibleServerException`으로 비활성화되고 인벤토리는 건드리지 않습니다.
- Access는 생성자에서 모든 멤버를 한 번 써 봅니다. 먼저 빈 인벤토리를 만들고, 보관함·갑옷·왼손 칸(0, 36, 40)에서 아이템을 읽어 다시 씁니다. 이어서 플레이어 없는 인벤토리를 생성자로 만들어(`PlayerInventory(null)`, Modern은 `Inventory(null, new EntityEquipment())`) 그 빈 인벤토리를 `apply`와 같은 코드로 써 넣고 다시 읽어 봅니다. 여기서 실패하면 역시 `IncompatibleServerException`입니다. 그래서 `apply`가 바꾸는 final 필드에 쓰지 못하는 환경(JVM의 final 필드 변경 제한 등)도 첫 접속이 아니라 시작할 때 드러납니다. 생성자는 필드에 값을 넣기만 하므로 플레이어가 `null`이어도 안전합니다(Spigot, Paper 1.12.2 – 26.3에서 확인).
- `apply`는 필드 여러 개(Legacy 4개, Modern 2개)를 `Reflections.setAll`로 바꿉니다. 중간에 하나라도 실패하면 이미 바꾼 필드를 원래 값으로 되돌린 뒤 예외를 던지므로, 플레이어 인벤토리가 일부만 바뀐 채로 남지 않습니다.
- dev 샌드박스의 Provider는 플러그인 jar의 `META-INF/services`에 있으므로, 서버 클래스 로더가 아니라 플러그인 jar의 클래스 로더로 찾습니다.
- 마인크래프트 버전: Paper는 `Server#getMinecraftVersion()`, Spigot은 `Bukkit.getBukkitVersion()`의 앞부분을 씁니다. 이름표는 Bukkit 버전 문자열(`1.21.11-R0.2-SNAPSHOT`)이 아니라 마인크래프트 버전(`1.21.11`)으로 찾습니다.
- 26.1 이상은 이름표의 한 행(`ModernNameTable.v26`, "26.1 이상")이 맡습니다. 난독화가 없어 이름이 안정적이므로 새 버전도 일단 시도하고, 이름을 찾지 못하면 비활성화합니다.

---

## 5. 저장 형식

공유 인벤토리는 `plugins/InventoryShare/inventory.yml`, 팀 인벤토리는 `plugins/InventoryShare/teams/<팀 이름>.yml`에 저장합니다.

```yaml
format-version: 2
items:      [ ...36개 ]   # 칸 0 – 35
armor:      [ ...4개 ]    # 칸 36 – 39 (feet, legs, chest, head)
extraSlots: [ ...1개 ]    # 칸 40 (offhand)
```

| `format-version` | 쓰는 서버        | 아이템 형식                     |
|------------------|------------------|---------------------------------|
| 2                | 1.20.5 이상      | `ItemCodec` (§4.5). 빈칸은 `{}` |
| 1, 또는 키 없음  | 1.20.4 이하, 3.x | Bukkit `ItemStack#serialize()`  |

```yaml
- DataVersion: 4903
  id: minecraft:diamond_sword
  count: 1
  components:
    minecraft:damage: '100'
    minecraft:enchantments: '{"minecraft:sharpness":5}'
  schema_version: 1
```

- 읽을 때는 아이템마다 형식을 판별합니다(`DataVersion` 키가 있으면 `ItemCodec`).
- 그 밖의 파일: `advancements.yml`(공유된 발전과제), `config.yml`(설정), `languages/`(언어 파일)

### 마이그레이션

- 1.20.5 이상에서 Bukkit 형식 아이템을 읽으면, 다음 저장 때 `ItemCodec` 형식으로 바뀝니다. 원본은 먼저 `backups/`에 복사합니다.
- 그 아이템이 서버보다 낮은 데이터 버전에서 저장됐으면 경고합니다. Bukkit은 옛 데이터를 새 버전에서 읽을 때 일부(셜커 상자 내용물 등)를 잃을 수 있습니다. 저장한 버전의 서버에서 먼저 한 번 실행하면 손실이 없습니다([위키](https://github.com/the-sugar-tree/InventoryShare/wiki/Updating-to-4.0)).

### 읽을 수 없는 데이터

다른 버전의 서버에서 저장한 파일을 읽을 때를 대비해 **아이템 단위**로 처리합니다.

- 다음 아이템은 건너뛰고 그 칸을 빈칸으로 둡니다. 나머지 아이템은 정상적으로 읽습니다.
  - 서버보다 높은 데이터 버전(`v`, `DataVersion`)에서 저장된 아이템 (다운그레이드)
  - 데이터 버전이 숫자가 아니거나, 아이템으로 되돌릴 수 없는 경우
- 하나라도 건너뛰면 **원본 파일을 `backups/<inventory|team-이름>-<날짜-시각>[-dv<데이터 버전>].yml`로 복사**하고, 건너뛴 칸과 백업 위치를 로그에 남깁니다. 원본 파일은 다음 저장 때 덮어써지므로, 복구하려면 그 데이터를 읽을 수 있는 서버에서 백업 파일을 원래 자리에 되돌립니다.
- 파일의 `format-version`이 플러그인이 아는 값보다 높으면 경고합니다.
- 아이템으로 되돌린 뒤 인벤토리에 넣는 단계(`InventoryAccess#setItem`)가 실패하면 건너뛰지 않습니다. 데이터가 아니라 Access의 문제이므로 `FileManager.load()`가 실패하고 플러그인이 비활성화됩니다(§3.4).

---

## 6. 테스트 구조

| 테스트                                     | 확인하는 것                                                                                                                                                             | Spigot jar               |
|--------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------|
| `InventoryShare-nms` 단위 테스트           | 버전 파싱, CraftBukkit 패키지 감지, 이름 조립(Spigot/Mojang, 패키지 형태별), 이름표 행, Provider 선택과 예외                                                            | 불필요                   |
| `InventoryShare-bukkit` 단위 테스트        | 아이템 형식 판별·마이그레이션·로드 판정과 로그 문구, 백업 파일, 지울 팀 파일 판정, 언어 파일 경로                                                                       | 불필요                   |
| `InventoryShare-nms-test` (`*MembersTest`) | 버전별 Spigot jar, `remapped-mojang` jar(Mojang 이름), Paper jar에서 서버를 감지하고, 플러그인과 같은 코드로 모든 이름을 찾은 뒤 필드 타입과 칸 → 장비 슬롯 표까지 확인 | 있으면 검증, 없으면 skip |

- 테스트는 대상 클래스와 같은 패키지에 둡니다. 그래서 package-private인 이름표도 직접 확인할 수 있습니다.
- 이름표의 모든 행이 최소 한 버전의 jar로 검사되는지도 테스트합니다.
- `ItemStack.CODEC`은 같은 타입의 필드가 여러 개라, Spigot 이름이 가리키는 필드가 `remapped-mojang` jar의 `CODEC`과 같은 위치인지 확인합니다.
- 실행 방법은 [CONTRIBUTING.md §4](../CONTRIBUTING.md#4-테스트)을 참고하세요.

---

## 7. 설계 결정과 알려진 한계

- **참조 공유를 택한 이유와 위험:** 아이템을 복사해 맞추는 대신 리스트 객체를 공유하므로 동기화가 필요 없고 복제 버그가 생기지 않습니다. 대신 서버 내부의 `final` 필드를 리플렉션으로 바꿔야 합니다. JDK 26부터 이 동작에 경고가 나오고 이후 막힐 예정입니다(JEP 500). 막히면 `InventoryAccess` 뒤에서 "내용 복사 동기화" 구현으로 바꿀 수 있도록 인터페이스를 나눠 두었습니다.
- **Modern 장비 맵 공유:** 장비 맵을 통째로 공유하므로, 플레이어가 쓰지 않는 장비 칸(말 갑옷 등에 쓰이는 BODY, SADDLE)도 함께 공유됩니다.
- **Paper ↔ Spigot 데이터 이동:** 1.20.5 이상은 손실 없이 옮겨집니다. Bukkit 형식(1.20.4 이하)은 Paper가 저장한 일부 아이템(셜커 상자 내용물 등)을 Spigot이 읽지 못합니다(1.21.4에서 확인).
- **1.20.4 이하에서 1.20.5 이상으로 올릴 때:** `ItemCodec`이 없는 버전이라 같은 버전에서 먼저 마이그레이션할 수 없습니다. 올리면서 변환되므로 §5의 손실이 생길 수 있습니다.
- **시작 실패 메시지:** 일부 시작 실패(개발 샌드박스의 오류 등)는 실패 종류별 메시지 없이 서버의 일반 오류로 끝납니다. 이때도 플러그인은 비활성화됩니다.

새 마인크래프트 버전을 지원하는 절차는 [CONTRIBUTING.md §6.2](../CONTRIBUTING.md#62-새-마인크래프트-버전-지원)를 참고하세요.
