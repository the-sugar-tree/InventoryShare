# 서버 내부 클래스(NMS) 입문: Spigot과 Paper

InventoryShare가 다루는 서버 내부 클래스가 무엇이고, 서버 종류와 버전에 따라 이름이 어떻게 달라지는지 설명합니다.
플러그인 구조는 [ARCHITECTURE.md](ARCHITECTURE.md), 빌드와 기여 방법은 [CONTRIBUTING.md](../CONTRIBUTING.md)를 참고하세요.

> [!important]
> 문서 정리 작업 중입니다. 현재 AI와 함께 편집한 문서로, 읽기 불편할 수 있습니다. 양해바랍니다.

---

## 1. 플러그인은 왜 서버 내부 클래스를 직접 쓰나

보통 플러그인은 Bukkit API(`org.bukkit.*`)만 사용합니다. 하지만 InventoryShare는 **여러 플레이어가 인벤토리 객체 하나를 함께 쓰도록** 만들어야 하고, 이것은 Bukkit API로는 할 수 없습니다.
Bukkit API는 아이템을 "복사해서" 넣고 빼는 방법만 제공하기 때문입니다. 그래서 서버 내부의 플레이어 인벤토리 객체를 직접 찾아, 그 안의 칸 목록(리스트)을 다른 플레이어와 **같은 객체**로 바꿔 끼웁니다.

이때 쓰는 서버 내부 코드는 두 종류입니다.

| 이름                           | 무엇인가                                                                       | 패키지                      |
|--------------------------------|--------------------------------------------------------------------------------|-----------------------------|
| **CraftBukkit**                | Bukkit API를 실제로 구현한 코드. 예: Bukkit의 `Player`를 구현한 `CraftPlayer`  | `org.bukkit.craftbukkit...` |
| **NMS** (net.minecraft.server) | 마인크래프트 서버 본체 코드. 예: 실제 플레이어 객체, 인벤토리 객체             | `net.minecraft...`          |

`CraftPlayer.getHandle()`을 호출하면 Bukkit의 `Player` 뒤에 있는 NMS 플레이어 객체를 얻을 수 있고, 거기서 NMS 인벤토리 객체로 넘어갑니다.

이 클래스들은 공개 API가 아니라서 **서버 종류와 버전마다 이름이 다릅니다.** 그래서 InventoryShare는 이름을 문자열로 정해 두고, 실행 중에 리플렉션(`Class.forName("...")`, `getDeclaredField("...")` 등)으로 찾습니다. 정해 둔 이름이 서버에 없으면 플러그인이 켜지지 않습니다.

---

## 2. 이름이 달라지는 두 가지 이유

### 2.1 CraftBukkit 패키지의 버전 번호

Spigot은 CraftBukkit 클래스를 마인크래프트 버전별 패키지에 넣습니다. 이것을 패키지 <strong>재배치(relocation)</strong>라고 부릅니다.

```
org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer      ← Spigot 1.21.4 (버전 번호 있음)
org.bukkit.craftbukkit.entity.CraftPlayer               ← Paper 1.20.5 이상, 26.1 이상의 모든 서버 (버전 번호 없음)
```

`v1_21_R3` 같은 부분은 마인크래프트 버전과 1:1로 대응하지 않습니다. 예를 들어 1.19, 1.19.1, 1.19.2는 모두 `v1_19_R1`입니다. 그래서 이 값은 버전에서 계산하지 않고, 실행 중인 서버에서 직접 읽어야 합니다.

1.17 이전에는 NMS 클래스도 같은 버전 번호 패키지(`net.minecraft.server.v1_16_R3.EntityHuman` 등)에 있습니다. 1.17부터 NMS는 `net.minecraft.world.entity.player...`처럼 기능별 패키지로 나뉘었습니다(이 문서에서는 **평탄화**라고 부릅니다).

### 2.2 NMS 이름 체계 (매핑)

마인크래프트 서버 본체는 클래스와 필드 이름을 알아보기 어렵게 바꿔서(**난독화**) 배포됩니다. 서버 소프트웨어마다 이 이름을 사람이 읽을 수 있게 되돌리는 방식(**매핑**)이 다릅니다.

| 이름 체계       | 예시 (플레이어 인벤토리)                                                               | 사용하는 서버                                                      |
|-----------------|----------------------------------------------------------------------------------------|--------------------------------------------------------------------|
| **Spigot 이름** | 클래스 `net.minecraft.world.entity.player.PlayerInventory`, 필드 `g`, `h`, `i`         | Spigot 1.21.11까지, Paper 1.20.4까지(일반 빌드)                    |
| **Mojang 이름** | 클래스 `net.minecraft.world.entity.player.Inventory`, 필드 `items`, `armor`, `offhand` | Paper 1.20.5 이상, Paper 1.20.4 Mojang 빌드, 26.1 이상의 모든 서버 |

- Spigot 이름은 클래스 이름은 대체로 읽을 수 있지만, 필드·메서드 이름은 `g`, `gK()`처럼 짧고 **마인크래프트 버전마다 바뀝니다.**
- Mojang 이름은 Mojang이 공개한 원래 이름입니다. **InventoryShare가 쓰는 Mojang 이름은 1.17 – 1.21.4 구간, 1.21.5 – 26.x 구간 안에서 바뀌지 않았습니다.**
- 26.1부터는 Mojang이 난독화를 하지 않아서 Spigot과 Paper 모두 Mojang 이름을 씁니다.

### 2.3 두 가지는 서로 따로 바뀐다

| 서버                              | CraftBukkit 패키지에 버전 번호                  | NMS 이름 체계   |
|-----------------------------------|-------------------------------------------------|-----------------|
| Spigot 1.12.2 – 1.21.11           | 있음 (`org.bukkit.craftbukkit.vX_Y_RZ`)         | Spigot 이름     |
| Paper 1.12.2 – 1.20.4 (일반 빌드) | 있음 (`org.bukkit.craftbukkit.vX_Y_RZ`)         | Spigot 이름     |
| **Paper 1.20.4 Mojang 빌드**      | **있음** (`org.bukkit.craftbukkit.v1_20_R3`)    | **Mojang 이름** |
| **Paper 1.20.5 – 1.21.11**        | **없음** (`org.bukkit.craftbukkit`)             | **Mojang 이름** |
| Spigot / Paper 26.1 이상          | 없음 (`org.bukkit.craftbukkit`)                 | Mojang 이름     |

Paper 1.20.4 Mojang 빌드처럼 **패키지에는 버전 번호가 있는데 NMS는 Mojang 이름인** 서버가 있습니다. 따라서 "Paper 1.20.5 이상이면 Mojang"처럼 마인크래프트 버전만 보고 판단하면 안 되고, 두 가지를 **각각 서버에서 확인**해야 합니다.

---

## 3. 인벤토리 구조는 버전에 따라 두 가지

이름과 별개로, 인벤토리의 **구조** 자체가 1.21.5에서 바뀌었습니다. InventoryShare는 이 둘을 Legacy, Modern으로 나눠 처리합니다.

| 구조                       | 인벤토리 객체가 가진 것                                                                                     | 갑옷·왼손이 있는 곳                               |
|----------------------------|-------------------------------------------------------------------------------------------------------------|---------------------------------------------------|
| **Legacy** (1.12 – 1.21.4) | 보관함·핫바 36칸 리스트, 갑옷 4칸 리스트, 왼손 1칸 리스트, 그리고 이 셋을 묶은 목록                         | 인벤토리 객체 안의 리스트                         |
| **Modern** (1.21.5 이상)   | 보관함·핫바 36칸 리스트, 플레이어의 **장비 객체**(`net.minecraft.world.entity.EntityEquipment`)에 대한 참조 | 장비 객체 안의 `장비 슬롯 → 아이템` 맵(`EnumMap`) |

InventoryShare의 슬롯 번호는 Bukkit `PlayerInventory.getContents()`와 같습니다. 0 – 35가 보관함·핫바, 36 – 39가 신발·바지·갑옷·투구, 40이 왼손입니다. Modern에서는 인벤토리 클래스에 있는 "칸 번호 → 장비 슬롯" 표로 36 – 40번을 장비 슬롯으로 바꿉니다.

---

## 4. 서버별로 실제로 쓰이는 이름

### 4.1 클래스

`{v}`는 CraftBukkit 패키지의 버전 번호(예: `v1_21_R3`)입니다.

| 서버                                                     | CraftBukkit 플레이어                                 | NMS 플레이어 (인벤토리를 가진 클래스)           | NMS 인벤토리                                        | 장비 슬롯 enum (Modern)                    |
|----------------------------------------------------------|------------------------------------------------------|-------------------------------------------------|-----------------------------------------------------|--------------------------------------------|
| Spigot / Paper 1.12.2 – 1.16.5                           | `org.bukkit.craftbukkit.{v}.entity.CraftPlayer`      | `net.minecraft.server.{v}.EntityHuman`          | `net.minecraft.server.{v}.PlayerInventory`          | –                                          |
| Spigot 1.17 – 1.21.4,<br>Paper 1.17 – 1.20.4 (일반 빌드) | `org.bukkit.craftbukkit.{v}.entity.CraftPlayer`      | `net.minecraft.server.level.EntityPlayer`       | `net.minecraft.world.entity.player.PlayerInventory` | –                                          |
| Paper 1.20.4 Mojang 빌드                                 | `org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer` | `net.minecraft.server.level.ServerPlayer`       | `net.minecraft.world.entity.player.Inventory`       | –                                          |
| Paper 1.20.5 – 1.21.4                                    | `org.bukkit.craftbukkit.entity.CraftPlayer`          | `net.minecraft.server.level.ServerPlayer`       | `net.minecraft.world.entity.player.Inventory`       | –                                          |
| Spigot 1.21.5 – 1.21.11                                  | `org.bukkit.craftbukkit.{v}.entity.CraftPlayer`      | `net.minecraft.world.entity.player.EntityHuman` | `net.minecraft.world.entity.player.PlayerInventory` | `net.minecraft.world.entity.EnumItemSlot`  |
| Paper 1.21.5 – 1.21.11,<br>Spigot / Paper 26.1 이상      | `org.bukkit.craftbukkit.entity.CraftPlayer`          | `net.minecraft.world.entity.player.Player`      | `net.minecraft.world.entity.player.Inventory`       | `net.minecraft.world.entity.EquipmentSlot` |

1.17 이상에서 모든 서버가 같은 이름을 쓰는 클래스:

| 클래스                                                  | 용도                            |
|---------------------------------------------------------|---------------------------------|
| `net.minecraft.world.item.ItemStack`                    | NMS 아이템                      |
| `net.minecraft.core.NonNullList`                        | 인벤토리 칸 목록                |
| `net.minecraft.world.entity.EntityEquipment`            | 갑옷·왼손 장비 (1.21.5 이상)    |
| `org.bukkit.craftbukkit[.{v}].inventory.CraftItemStack` | Bukkit 아이템 ↔ NMS 아이템 변환 |

`CraftItemStack`은 `asNMSCopy`와 `asCraftMirror`(Paper 26.3부터 `asBukkitMirror`)만 씁니다. 아이템을 읽을 때는 미러를 `clone()`해서 NMS 아이템을 복사합니다(`ItemMeta`를 거치지 않음).

### 4.2 필드·메서드

**Legacy (1.17 – 1.21.4)** — 예: 1.21.4

| 찾는 것                                                    | Spigot 1.21.4    | Mojang 이름             |
|------------------------------------------------------------|------------------|-------------------------|
| 플레이어 → 인벤토리를 얻는 메서드                          | `gi()`           | `getInventory()`        |
| 보관함·핫바 36칸                                           | `g`              | `items`                 |
| 갑옷 4칸                                                   | `h`              | `armor`                 |
| 왼손 1칸                                                   | `i`              | `offhand`               |
| 위 셋을 묶은 목록                                          | `l`              | `compartments`          |
| 빈 아이템 (`ItemStack`의 상수)                             | `j`              | `EMPTY`                 |
| 빈 칸 목록 만들기 (`NonNullList`의 정적 메서드)            | `a(int, Object)` | `withSize(int, Object)` |

1.12 – 1.16.5는 인벤토리를 메서드가 아니라 필드 `inventory`로 얻고, 칸 목록 필드는 `items`, `armor`, `extraSlots`라는 읽을 수 있는 이름을 씁니다.

**Modern (1.21.5 이상)** — 예: 1.21.11

| 찾는 것                                                 | Spigot 1.21.11   | Mojang 이름 (Paper, 26.1 이상) |
|---------------------------------------------------------|------------------|--------------------------------|
| 플레이어 → 인벤토리를 얻는 메서드                       | `gK()`           | `getInventory()`               |
| 보관함·핫바 36칸                                        | `l`              | `items`                        |
| 장비 객체                                               | `n`              | `equipment`                    |
| 장비 객체 안의 슬롯별 아이템 (`EntityEquipment`의 필드) | `b`              | `items`                        |
| 인벤토리 칸 번호 → 장비 슬롯 표                         | `i`              | `EQUIPMENT_SLOT_MAPPING`       |
| 빈 아이템                                               | `l`              | `EMPTY`                        |
| 빈 칸 목록 만들기                                       | `a(int, Object)` | `withSize(int, Object)`        |

Spigot 이름은 버전마다 다르므로 위 표는 예시입니다. 버전별 전체 이름은 코드의 `LegacyNameTable`, `ModernNameTable`이 기준입니다(`InventoryShare-nms`의 `nms/reflection/`).

---

## 5. InventoryShare는 어떻게 맞추나

플러그인이 켜질 때 서버에서 두 가지를 **각각** 확인합니다(`com.sugar_tree.inventoryshare.nms.server.ServerRuntime`).

| 확인할 것            | 방법                                                             | 결과 예시                                                                                                           |
|----------------------|------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| CraftBukkit 패키지   | 서버 객체(`Bukkit.getServer()`)의 클래스 이름을 봄               | `org.bukkit.craftbukkit.v1_20_R3.CraftServer` → `v1_20_R3`<br>`org.bukkit.craftbukkit.CraftServer` → 버전 번호 없음 |
| NMS 이름 체계        | `net.minecraft.world.entity.player.Inventory` 클래스가 있는지 봄 | 있으면 Mojang 이름, 없으면 Spigot 이름                                                                              |

그다음 마인크래프트 버전으로 인벤토리 구조(Legacy / Modern)를 고르고, 위에서 확인한 패키지와 이름 체계에 맞는 클래스·멤버 이름을 조립합니다(`com.sugar_tree.inventoryshare.nms.reflection.inventory.InventoryNameFactory`, 아이템 저장용은 `…reflection.item.ItemNameFactory`).
그래서 같은 마인크래프트 1.21.4라도 Spigot에서는 `net.minecraft.world.entity.player.PlayerInventory`의 `g`를, Paper에서는 `net.minecraft.world.entity.player.Inventory`의 `items`를 찾습니다.

- 서버를 실행하는 클래스 로더(`CraftServer`의 클래스 로더)에서 찾습니다. 다른 플러그인 jar에 들어 있는 같은 이름의 클래스를 서버 것으로 잘못 찾지 않기 위해서입니다.
- 이름은 모두 플러그인이 켜질 때 한 번에 찾습니다. 하나라도 없으면 "버그이니 신고해 주세요" 메시지와 함께 플러그인이 비활성화되고, 인벤토리는 건드리지 않습니다.
- Paper는 Spigot 이름으로 된 리플렉션 호출 일부를 Mojang 이름으로 자동 변환해 주지만, InventoryShare는 이 기능에 기대지 않고 서버에 맞는 이름을 직접 씁니다.

플러그인이 켜질 때 서버 로그에 확인 결과가 출력됩니다.

```
Minecraft 1.21.4 (CraftBukkit v1_21_R3, SPIGOT mappings), using LegacyInventoryAccess          ← Spigot 1.21.4
Minecraft 1.20.4 (CraftBukkit v1_20_R3, MOJANG mappings), using LegacyInventoryAccess          ← Paper 1.20.4 Mojang 빌드
Minecraft 1.21.4 (CraftBukkit (not relocated), MOJANG mappings), using LegacyInventoryAccess   ← Paper 1.21.4
Minecraft 1.21.11 (CraftBukkit (not relocated), MOJANG mappings), using ModernInventoryAccess  ← Paper 1.21.11
```

---

## 6. 이름을 직접 확인하는 방법

새 버전을 지원하거나 이름이 맞는지 의심될 때 쓸 수 있는 방법입니다.

| 방법            | 설명                                                                                                                                                                                                                                                    |
|-----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 자동 테스트     | `InventoryShare-nms-test`가 BuildTools로 설치한 Spigot jar와 `remapped-mojang` jar(Mojang 이름)를 불러 플러그인과 같은 코드로 이름을 찾고, 필드 타입까지 확인합니다. [CONTRIBUTING.md §4.2](../CONTRIBUTING.md#42-nms-이름-검증-테스트-buildtools-필요) |
| `javap`         | 서버 jar의 클래스를 직접 열어 봅니다. 예: `javap -p -cp <서버 jar> net.minecraft.world.entity.player.Inventory`                                                                                                                                         |
| 직접 NMS로 작성 | `InventoryShare-dev` 샌드박스에서 NMS를 직접 호출해 동작을 먼저 확인한 뒤, 같은 이름을 이름표로 옮깁니다. [CONTRIBUTING.md §5](../CONTRIBUTING.md#5-개발-환경-inventoryshare-dev)                                                                       |

Paper 서버 jar(`versions/<버전>/paper-<버전>.jar`)는 실행 중인 이름 그대로(Mojang 이름) 들어 있어서 `javap`로 바로 확인할 수 있습니다.
