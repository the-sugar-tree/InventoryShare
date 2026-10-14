# InventoryShare Architecture

구조 개편 중입니다.

**현재 구현 상태 (요약)**

| 목표 | 현재 코드 |
|---|---|
| `InventoryShare-nms` (`InventoryAccess`, `NmsMapping`, Legacy/Modern) | 없음. `InventoryShare-bukkit`의 `VersionUtil`(매핑표) + `NMSLoader`(리플렉션)가 Legacy 모델만 처리 |
| `InventoryShare-nms-test` (파라미터화 테스트) | 없음. `InventoryShare-bukkit-test/<리비전>/NMSTest` (1.12 – 1.21.4, 리비전별 서브프로젝트) |
| `InventoryShare-dev/<ver>` | 없음. `InventoryShare-single/v1_21_R4`(1.21.5), `v26_2`(26.2)가 직접 NMS로 작성된 별도 배포 jar |
| 1.12.2 – 26.x 단일 jar | `InventoryShare-bukkit` jar는 1.12.2 – 1.21.4 |

---

## 1. 배경

InventoryShare는 플레이어 인벤토리의 내부 필드를 교체해 여러 플레이어가 **같은 NMS 리스트 객체를 참조**하게 만듭니다.
Bukkit API로는 참조 공유가 불가능해 NMS 접근이 필수이고, 버전마다 내부 구조와 이름이 다릅니다.

| 구간 | 인벤토리 모델 | 이름 |
|---|---|---|
| 1.12 – 1.16.5 | `items`, `armor`, `extraSlots`, `contents` | `net.minecraft.server.vX_Y_RZ` 패키지, 필드명 일부 원문 |
| 1.17 – 1.21.4 | 같은 모델 | 패키지 평탄화, Spigot 난독화 이름 |
| 1.21.5 – 1.21.11 | `items` + `EntityEquipment` | Spigot 난독화 이름 |
| 26.1 이상 | `items` + `EntityEquipment` | **난독화 없음 (Mojang 원문 이름)** |

---

## 2. 모듈 구조

```
InventoryShare/
├─ InventoryShare-bukkit/      배포 플러그인 (core). spigot-api로만 컴파일, release 8
├─ InventoryShare-nms/         InventoryAccess + legacy/modern 구현 + 매핑표. spigot-api로만 컴파일, release 8
├─ InventoryShare-nms-test/    매핑 검증 테스트 (Legacy/Modern 파라미터화)
├─ InventoryShare-dev/<ver>/   개발용 샌드박스 (-Pdev, 배포 안 함)
├─ InventoryShare-sponge/      사용하지 않음 (보존용, 모듈에 포함 X)
└─ docs/
```

```
InventoryShare-bukkit ──▶ InventoryShare-nms ◀── InventoryShare-nms-test
        ▲
InventoryShare-dev/<ver>  (core를 그대로 쓰고 InventoryAccess만 교체)
```

### 목표
- 배포 jar가 `InventoryShare-bukkit` 하나(nms 모듈 포함)로 1.12.2 – 26.x를 지원합니다.
- core가 NMS 타입을 알지 못하는 상태에서 `InventoryAccess`만 사용합니다.
- 배포 모듈은 리플렉션만 쓰므로 **기본 빌드에 BuildTools가 필요 없습니다.** BuildTools로 mavenLocal에 설치한 Spigot jar는 `InventoryShare-nms-test`와 `InventoryShare-dev`에서만 씁니다.

---

## 3. `InventoryShare-nms`

### 3.1 패키지

```
com.sugar_tree.inventoryshare.nms
├─ InventoryAccess           core가 아는 유일한 NMS 경계
├─ NmsInventory              불투명 핸들 (Legacy: items/armor/extraSlots, Modern: items/equipment map)
├─ ServerVersion             MC 버전 파싱/비교 ("1.21.11")
├─ NmsMapping                MC 버전 → 매핑 행 (enum)
├─ InventoryAccessFactory    버전 감지 → 구현 선택 → 멤버 해석
├─ legacy/
│   ├─ LegacyNames
│   ├─ LegacyMembers         resolve(LegacyNames, ClassLoader)
│   └─ LegacyInventoryAccess
└─ modern/
    ├─ Namespace             SPIGOT | MOJANG (클래스 이름 세트)
    ├─ ModernNames
    ├─ ModernMembers         resolve(ModernNames, ClassLoader)
    └─ ModernInventoryAccess
```

### 3.2 InventoryAccess

```java
public interface InventoryAccess {
    NmsInventory createEmpty();                       // 공유/팀용 빈 인벤토리
    NmsInventory capture(Player player);              // 플레이어의 현재 리스트/맵 참조
    void apply(Player player, NmsInventory inv);      // 참조 교체 (공유의 핵심)
    ItemStack getItem(NmsInventory inv, int slot);    // Bukkit 사본
    void setItem(NmsInventory inv, int slot, ItemStack item);
}
```

### 3.3 슬롯 모델

슬롯 번호는 Bukkit `PlayerInventory.getContents()` 순서와 같습니다. 저장 코드는 이 번호만 사용하므로 버전을 알 필요가 없습니다.

| 슬롯 | 의미 | Legacy | Modern |
|---|---|---|---|
| 0 – 35 | 보관함/핫바 | `items[i]` | `items[i]` |
| 36 – 39 | 신발, 바지, 갑옷, 투구 | `armor[i - 36]` | `slotMapping.get(i)` → EquipmentSlot |
| 40 | 왼손 | `extraSlots[0]` | `slotMapping.get(40)` → OFFHAND |

* Spigot 1.21.x의 `EnumItemSlot` 상수명은 난독화(`a`..`h`)되어 `Enum.valueOf`를 쓸 수 없습니다. 
* Modern은 인벤토리 클래스의 정적 `slotMapping`(`Int2ObjectMap<EquipmentSlot>`, 즉 `Map<Integer, ?>`)으로 슬롯을 해석합니다.

### 3.4 멤버 해석과 ClassLoader

`LegacyMembers.resolve(names, loader)` / `ModernMembers.resolve(names, loader)`는 **모든 클래스를 인자로 받은 ClassLoader에서** 찾습니다(Bukkit `ItemStack` 포함).

- 런타임: 서버의 ClassLoader
- 테스트: 버전별로 격리된 `URLClassLoader`

이렇게 하면 테스트가 플러그인과 **같은 해석 코드**를 실행합니다. 모든 멤버는 플러그인 활성화 시점에 미리 해석되며, 실패하면 플러그인이 "지원하지 않는 버전"으로 비활성화됩니다.

### 3.5 버전 감지

- 매핑표의 키는 **MC 버전 문자열**(`"1.21.11"`)입니다. Bukkit 버전 문자열(`1.21.11-R0.2-SNAPSHOT`)의 `-R0.x` 부분은 서버마다 다르므로 키로 쓰지 않습니다.
- Paper: `Server#getMinecraftVersion()`(리플렉션)을 사용합니다. Paper 26.x의 `getBukkitVersion()`은 `26.1.2.build.63-stable` 형식입니다 ([PaperMC/Paper#13885](https://github.com/PaperMC/Paper/issues/13885)).
- Spigot: `Bukkit.getBukkitVersion()`에서 `-` 앞부분을 사용합니다.
- 26.1 이상은 `atLeast("26.1")` 행으로 처리합니다. 난독화가 없어 이름이 안정적이므로 미래 버전도 일단 시도하고, 해석에 실패하면 비활성화합니다.

### 3.6 Legacy 매핑 (1.12 – 1.21.4)

| 리비전 | MC | inventory | items | armor | extraSlots | contents | emptyItem |
|---|---|---|---|---|---|---|---|
| v1_12_R1 | 1.12, 1.12.1, 1.12.2 | inventory (field) | items | armor | extraSlots | f | a |
| v1_13_R1 | 1.13 | inventory | items | armor | extraSlots | f | a |
| v1_13_R2 | 1.13.1, 1.13.2 | inventory | items | armor | extraSlots | f | a |
| v1_14_R1 | 1.14 – 1.14.4 | inventory | items | armor | extraSlots | f | a |
| v1_15_R1 | 1.15 – 1.15.2 | inventory | items | armor | extraSlots | f | a |
| v1_16_R1 | 1.16.1 | inventory | items | armor | extraSlots | f | b |
| v1_16_R2 | 1.16.2, 1.16.3 | inventory | items | armor | extraSlots | f | b |
| v1_16_R3 | 1.16.4, 1.16.5 | inventory | items | armor | extraSlots | f | b |
| v1_17_R1 | 1.17, 1.17.1 | getInventory() | h | i | j | n | b |
| v1_18_R1 | 1.18 | fq() | h | i | j | n | b |
| v1_18_R2 | 1.18.1, 1.18.2 | fr() | h | i | j | n | b |
| v1_19_R1 | 1.19 | fB() | h | i | j | n | b |
| v1_19_1_R1 (CB 패키지 v1_19_R1) | 1.19.1, 1.19.2 | fA() | h | i | j | n | b |
| v1_19_R2 | 1.19.3 | fE() | h | i | j | n | b |
| v1_19_R3 | 1.19.4 | fJ() | i | j | k | o | b |
| v1_20_R1 | 1.20, 1.20.1 | fN() | i | j | k | o | b |
| v1_20_R2 | 1.20.2 | fR() | i | j | k | o | b |
| v1_20_R3 | 1.20.3, 1.20.4 | fS() | i | j | k | o | f |
| v1_20_R4 | 1.20.5, 1.20.6 | gc() | h | i | j | n | l |
| v1_21_R1 | 1.21, 1.21.1 | fY() | h | i | j | n | l |
| v1_21_R2 | 1.21.2, 1.21.3 | gi() | g | h | i | l | j |
| v1_21_R3 | 1.21.4 | gi() | g | h | i | l | j |

### 3.7 Modern 매핑 (1.21.5 이상)

| MC | CraftBukkit 패키지 | Namespace | getInventory | items | equipment | EntityEquipment.items | slotMapping | EMPTY | createList |
|---|---|---|---|---|---|---|---|---|---|
| 1.21.5 | v1_21_R4 | SPIGOT | gj | i | k | b | g | l | a |
| 1.21.6 – 1.21.8 | v1_21_R5 | SPIGOT | gs | k | m | b | i | l | a |
| 1.21.9 – 1.21.10 | v1_21_R6 | SPIGOT | gB | l | n | b | i | l | a |
| 1.21.11 | v1_21_R7 | SPIGOT | gK | l | n | b | i | l | a |
| 26.1 이상 | (없음) | MOJANG | getInventory | items | equipment | items | EQUIPMENT_SLOT_MAPPING | EMPTY | withSize |

- AI가 `javap`를 사용해 추출해 낸 매핑입니다. 확실하지 않습니다.

| Namespace | 인벤토리 | 플레이어(인벤토리 접근자 보유) | 장비 슬롯 enum |
|---|---|---|---|
| SPIGOT | `net.minecraft.world.entity.player.PlayerInventory` | `net.minecraft.world.entity.player.EntityHuman` | `net.minecraft.world.entity.EnumItemSlot` |
| MOJANG | `net.minecraft.world.entity.player.Inventory` | `net.minecraft.world.entity.player.Player` | `net.minecraft.world.entity.EquipmentSlot` |

공통: `net.minecraft.world.entity.EntityEquipment`, `net.minecraft.world.item.ItemStack`, `net.minecraft.core.NonNullList`, `org.bukkit.craftbukkit[.<pkg>].inventory.CraftItemStack`.

---

## 4. 저장 형식

```yaml
format-version: 1
items:      [ ...36개 ]   # 슬롯 0 – 35
armor:      [ ...4개 ]    # 슬롯 36 – 39 (feet, legs, chest, head)
extraSlots: [ ...1개 ]    # 슬롯 40 (offhand)
```

- 공유 인벤토리는 `inventory.yml`, 팀 인벤토리는 `teams/<team>.yml`에 저장합니다.
- 각 아이템은 `ItemStack#serialize()` 결과이며, 로드 시 아이템의 data version(`v`)이 서버보다 높으면(다운그레이드) 로드를 중단합니다.

---

## 5. 테스트

### 5.1 `InventoryShare-nms` 단위 테스트 (항상 실행)
- Spigot jar 없이 `ServerVersion` 파싱/비교, `NmsMapping` 조회를 통해 테스트합니다.

### 5.2 `InventoryShare-nms-test` 매핑 테스트
- `LegacyMappingTest` (1.12.2 – 1.21.4), `ModernMappingTest` (1.21.5 – 26.x): `@ParameterizedTest`
- 각 버전의 Spigot 런타임 jar(classifier 없는 기본 jar)를 격리된 ClassLoader로 불러, 실제 `resolve()`를 실행하고 필드 타입까지 검증합니다.
- jar가 mavenLocal에 없으면 그 버전만 **skip**합니다. `-PnmsStrict`를 주면 누락 시 실패합니다.
- 26.x 클래스 파일을 읽어야 하므로 테스트 JVM은 Java 25입니다.

### 5.3 현재 테스트
- `InventoryShare-bukkit-test/<리비전>/` 서브프로젝트마다 해당 Spigot jar(`org.spigotmc:spigot:<ver>-R0.1-SNAPSHOT`)에 대해 컴파일되는 `NmsScratch`와 `NMSTest`가 있습니다.
- `NMSTest`는 `VersionUtil.SupportedVersions`의 해당 행을 `NmsScratch` 상수 및 실제 NMS 클래스(필드의 public 여부, 제네릭 타입, `getHandle` 반환 타입, `CraftItemStack` 변환 메서드 등)와 비교합니다.
- 실행 예: `./gradlew :InventoryShare-bukkit-test:v1_21_R1:test`. jar가 mavenLocal에 없으면 skip이 아니라 **컴파일 실패**합니다.
- `InventoryShare-single/v1_21_R4`에도 `NMSTest`가 있습니다.

---

## 6. 새 마인크래프트 버전 추가 절차

1. BuildTools로 해당 버전 Spigot jar를 mavenLocal에 설치합니다.
2. `InventoryShare-dev/<ver>`에서 `DirectInventoryAccess`(직접 NMS)로 동작을 확인합니다.
3. `InventoryShare-nms-test`의 Spigot jar 목록에 버전을 추가하고 `-PnmsStrict`로 테스트를 실행합니다. 테스트 리포트에서 해당 버전 케이스가 skip이 아니라 **실행되어 통과했는지** 확인합니다.
4. 실패하면 `NmsMapping`에 행을 추가합니다. 모델 자체가 바뀌었다면(1.21.5의 `EntityEquipment` 전환처럼) 새 `InventoryAccess` 구현을 추가합니다. 26.x는 `atLeast("26.1")` 행이 그대로 통과할 가능성이 높지만, 실제로 통과하기 전까지는 가정입니다.
5. 매핑 테스트는 멤버 존재와 타입만 확인합니다. 실서버에서 플러그인이 활성화되는지, 두 플레이어 사이에 슬롯 0 – 40(방어구, 왼손 포함)이 공유되는지, 재시작 후 `inventory.yml`/`teams/<team>.yml`이 다시 로드되는지 확인합니다.

---

## 7. 알려진 위험 요소

- **Paper 1.20.5 – 1.21.11**: Paper는 이 구간에서 런타임에 Mojang 이름을 사용합니다. Spigot 이름 리플렉션이 동작하지 않으면 `Namespace.MOJANG` 행을 재사용합니다(런타임에 `net.minecraft.world.entity.player.Inventory` 존재 여부로 선택).
- **JEP 500 (JDK 26, "Prepare to Make Final Mean Final")**: 리플렉션으로 final 필드를 바꾸는 것에 경고가 나오고 이후 기본 차단될 예정입니다. 이 플러그인의 핵심 동작이므로, 필요해지면 `InventoryAccess` 뒤에서 "내용 복사 동기화" 구현으로 교체합니다.
- **Modern 장비 맵 공유**: `EntityEquipment`의 EnumMap을 통째로 공유하면 플레이어가 쓰지 않는 슬롯(BODY, SADDLE)도 함께 공유됩니다. 무해할 것으로 보지만 실서버에서 확인합니다.
