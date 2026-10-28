<p align="center"><a href="README.en.md">ENGLISH</a> | <b>한국어</b></p>

# Inventory Sharing Plugin (v4.0)
인벤토리 공유 플러그인 (v4.0)

> [!note]   
> [1.12-1.20.4]버전의 ProtocolLib 플러그인은 [링크](https://github.com/dmulloy2/ProtocolLib/releases)를 참조해주세요!

> [!note]   
> [1.20.5+]버전의 ProtocolLib 플러그인은 [링크](https://ci.dmulloy2.net/job/ProtocolLib/lastBuild/)를 참조해주세요! ([다운로드 방법](https://github.com/the-sugar-tree/InventoryShare/wiki/How-to-download-ProtocolLib))


> [!important]   
> 먼저 README파일(아래)을 다 읽어주세요!!!


## Change Log   
- 1.12.2 – 26.3 지원 (Spigot, Paper)
- 1.20.5 이상에서 아이템을 마인크래프트 자체 형식으로 저장합니다. 셜커 상자 내용물, 머리 스킨 등이 사라지지 않고, Spigot과 Paper가 서로의 데이터를 읽습니다. ([3.x에서 업데이트](#3x에서-업데이트) 참고)
- 명령어 권한 `inventoryshare.command` 추가 (기본값: OP)
- 월드 저장 때 데이터를 월드 수만큼 반복 저장하던 문제 수정

---
<p align="center">
    <a href="https://github.com/the-sugar-tree/InventoryShare/releases/download/v4.0/InventoryShare-4.0.jar">
        <img src="https://img.shields.io/badge/Plugin%20Download-CLICK-bbbbbb?style=for-the-badge" height="40" width="260">
    </a>
</p>

---
[![Youtube](https://img.shields.io/badge/youtube-sugar_tree-red.svg?logo=youtube&style=for-the-badge)](https://www.youtube.com/channel/UCtqLK2FrJI9BNB0BI8-sWHA)
[![Release](https://img.shields.io/github/v/release/the-sugar-tree/InventoryShare?style=for-the-badge)](https://github.com/the-sugar-tree/InventoryShare/releases/latest)
[![Java](https://img.shields.io/badge/java-8%2B-ED8B00.svg?logo=java&style=for-the-badge)](https://www.azul.com/)
[![DownloadCount](https://img.shields.io/github/downloads/the-sugar-tree/InventoryShare/total?style=for-the-badge)](https://github.com/the-sugar-tree/InventoryShare/releases)   
[![SupportVersion](https://img.shields.io/badge/Supported%20Minecraft%20Version-1.12.2—26.3-green?style=for-the-badge)](https://github.com/the-sugar-tree/InventoryShare)
[![License](https://img.shields.io/github/license/the-sugar-tree/InventoryShare?style=for-the-badge)](https://www.gnu.org/licenses/gpl-3.0.html)
[![Gradle](https://img.shields.io/badge/gradle-9.8.2-02303A.svg?logo=gradle&style=for-the-badge)](https://gradle.org)

---
### Contacts
<sugar_tree@outlook.kr>
<tjfxkdskan@gmail.com>

### 추가 안내

유튜브에 사용하실분은 제 깃허브 링크 [ https://github.com/the-sugar-tree/InventoryShare ] 를 설명란에 추가해주세요.   

https://www.koreaminecraft.net/plugins/2902818   
https://blog.naver.com/sugar-tree/222556850608   

위 게시물 및 현 깃허브 주소 이외의 다른 사이트에서 배포를 하고 있다면 <sugar_tree@outlook.kr> 로 알려주시면 감사하겠습니다.

\[[ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997)] 이라는 외부 플러그인을 사용하시면 블럭을 동시에 캘 때 생기는 문제를 해결 할 수 있습니다. [다운로드 방법](https://github.com/the-sugar-tree/InventoryShare/wiki/How-to-download-ProtocolLib) 

---

## Minecraft Version
### Current Versions
- 1.12.2-26.3 (Release 4.0) \[[Download](https://github.com/the-sugar-tree/InventoryShare/releases/download/v4.0/InventoryShare-4.0.jar)]

## 주의사항
**이 플러그인은 스피갓, 페이퍼 기반입니다!**   
**반드시 지원하는 버킷에서 사용해주세요!**

### 3.x에서 업데이트
마인크래프트 1.20.5 이상에서는 아이템 저장 형식이 바뀝니다. **서버 버전을 올리기 전에**, 지금 버전에서 새 플러그인으로 서버를 한 번 켰다 끄세요. 자세한 내용은 [위키](https://github.com/the-sugar-tree/InventoryShare/wiki/Updating-to-4.0)를 참고하세요.

## 도움말
팀 공유기능을 사용하시려면 아래 커맨드를 활용해 팀을 정해주시고
- /team add <팀 이름> 
- /team join <팀 이름> \[닉네임]


- /inventoryshare teaminventory true

를 사용하여 팀과의 인벤토리를 공유 할 수 있습니다.  

원래있던 인벤토리는 따로 저장되며, 팀에 배정되지 않았을 시 전체 인벤토리를 사용합니다.  
팀 인벤토리 공유는 전체 인벤토리 공유가 활성화 되었을때만 활성화 되며 팀 인벤토리도 따로 저장됩니다.  
그러나 페이퍼 서버의 한계로, 아무도 없는팀은 서버를 재시작할때 삭제되오니 이 점 참고 바랍니다.

## 알려진 문제
- 팀 인벤토리를 켜도 발전과제는 팀별로 나뉘지 않고 모든 플레이어가 공유합니다([#6](https://github.com/the-sugar-tree/InventoryShare/issues/6)). 발전과제 공유는 진행 상황까지 공유하는 별도 플러그인으로 옮길 예정이며, 그때 이 플러그인에서는 제거됩니다.

## 명령어
![](https://raw.githubusercontent.com/the-sugar-tree/InventoryShare/master/help_message.png)   

권한: `inventoryshare.command` (기본값: OP). 권한 플러그인으로 OP가 아닌 플레이어에게도 줄 수 있습니다.

### 기본 값
- inventory: true
- advancement: true
- announcedeath: false
- teaminventory: false

## 개발 참여
빌드 방법과 기여 절차는 [CONTRIBUTING.md](CONTRIBUTING.md), 구조 설명은 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)를 참고하세요.

## License / 라이센스

이 프로젝트는 \[GNU General Public License v3.0] 라이센스가 부여되어 있습니다. 자세한 내용은 [LICENSE](https://github.com/the-sugar-tree/InventoryShare/blob/master/LICENSE.md) 파일을 참고하세요.

## Youtube

[![Video Label](http://img.youtube.com/vi/PI6eTCOs-x4/0.jpg)](https://www.youtube.com/watch?v=PI6eTCOs-x4)
