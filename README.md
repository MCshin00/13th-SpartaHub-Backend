# 13th-SpartaHub
내일배움캠프 Ch.2 대규모 AI 시스템 설계 프로젝트

----
### 📖 프로젝트 소개(SpartaHub) ###
![SPARTA](https://github.com/user-attachments/assets/f2b507b0-3918-4eea-b2eb-3a11f4c5fdca)



### MSA기반의 물류 관리 및 배송 시스템 ###
- #### 프로젝트 목표 ####
  - 흔히 접하던 모놀리식 구조가 아닌 MSA기반의 시스템을 설계하고 구현하면서, 다양한 기술과 방법론을 적용
  - 데이터 일관성을 유지하기 위한 트랜잭션 도입
  - GeminiAPI를 연동하여 AI 기능 구현
- #### 프로젝트 상세 ####
  - MASTER, HUB_MANAGER, SHIPMENT_MANAGER, COMPANY_MANAGER의 권한으로 관리하여 진행
  - 사용자, 업체, 허브, 상품, 주문, 배송 도메인으로 구성
  - 모든 테이블에 Audit 필드 추가하여 데이터 감사로그 기록(논리삭제(is_deleted) 컬럼 사용)
  - 개발기간 : 24.12.5 ~ 24.12.17
----
### 👩‍💻 팀원 역할 분담 ###
 - **신민철(팀장) :** `eureka server`, `gateway`, `user`, `slack`, `ai`, `company`, `product` 구현
 - **지현구 :** `hub(hub, hubRoute)`, `order` 구현
 - **임지은 :** `shipment(shipment, shipmentRoute, shipmentManager)` 구현
----
### 🔧 개발환경 ###
- **Framework  :** Spring Boot 3.4.0
- Java 17
- **Build Tool :** Gradle
- **IDE :** IntelliJ
- **ORM :** JPA
- **VCS :** GitHub(Forking Workflow 전략)
- **Database :** PostgreSQL, Redis
- **API documentation:** Swagger
----
### 📝 ERD ###
![image](https://github.com/user-attachments/assets/3461fa7e-c1d6-4536-bc84-0c16a28d8b8f)

----
### 📚 인프라 구성도 ###
![image](https://github.com/user-attachments/assets/c3d985fd-533c-4e41-8e8e-51cc52b90b8a)

----
### 🖥️ API Docs ###
- [API 명세서](https://teamsparta.notion.site/API-2feb032d3afd4efc9f2378574def97ba)