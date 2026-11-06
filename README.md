# Splearn 프로젝트

> Spring + Learn으로, 온라인 러닝 플랫폼을 도메인 모델 패턴과 헥사고날 아키텍처로 구현하는 학습 프로젝트입니다.

---

## 1. 프로젝트 소개

splearn은 강의를 등록, 수강할 수 있는 온라인 러닝 서비스의 백엔드 도메인 모델을 설계하고 구현하는 과정을 기록한 레포지토리입니다. 기능을 빠르게 쌓기보다는 **설계 의사결정, 애그리거트 경계, 테스트 전략**을 하나씩 짚어가며 쌓아 올리는 데 중점을 둡니다.

### Part 1에서 다룬 범위

Part 1은 **회원(Member) 애그리거트**를 중심으로 다음을 완성했습니다.

- 회원 등록 / 활성화 / 탈퇴 / 정보 수정
- 회원 상세(`MemberDetail`)와 프로필(`Profile`), 자기소개, 가입/활성화/탈퇴 시각 관리
- 프로필 주소 중복 검증
- 비밀번호 BCrypt 해싱
- 이메일 발송(현재는 로그로 대체하는 Dummy 구현)
- 회원 등록 REST API와 RFC 9457 기반 오류 응답
- 헥사고날 아키텍처 의존 방향을 ArchUnit으로 강제
- SpotBugs 정적 분석 도입

Part 2는 추후 학습 예정

---

## 2. 기술 스택

| 영역 | 기술                                                         | 버전 |
|---|------------------------------------------------------------|---|
| 언어 | Java                                                       | 21 |
| 애플리케이션 | Spring Boot                                                | 4.1.2 |
| 웹 | spring-boot-starter-webmvc                                 | (BOM) |
| 영속성 | spring-boot-starter-data-jpa (Hibernate)                   | (BOM) |
| 검증 | spring-boot-starter-validation (Jakarta Validation)        | (BOM) |
| 보안 | spring-security-core (BCrypt)                              | (BOM) |
| DB | H2 (개발, 테스트), MySQL (로컬 docker compose)                    | latest |
| 널 애너테이션 | JSpecify (`@NullMarked`)                                   | (BOM) |
| 보일러플레이트 | Lombok                                                     | (BOM) |
| 테스트 | JUnit 5, AssertJ, Mockito, Spring Boot Test, MockMvcTester | (BOM) |
| 아키텍처 테스트 | ArchUnit JUnit5                                            | 1.4.1 |
| 정적 분석 | SpotBugs Gradle Plugin                                     | 6.5.13 |
| 빌드 | Gradle (Kotlin DSL)                                        | Wrapper |

---

## 3. 실행 방법

### 애플리케이션 실행

```bash
./gradlew bootRun
```

Spring Boot Docker Compose 지원이 활성화되어 있어 `bootRun` 시 `compose.yaml`의 MySQL 컨테이너를 자동으로 기동합니다. 라이프사이클은 `start_only`로 설정되어 애플리케이션 종료 시 컨테이너는 그대로 유지됩니다.

### 테스트 실행

```bash
./gradlew test  # 전체 테스트
./gradlew spotbugsMain  # SpotBugs 정적 분석
./gradlew build  # 컴파일 + 테스트 + 정적 분석
```

### 데이터베이스

- 기본 로컬 DB는 docker compose의 MySQL(database `splearn`, user `spring`, password `secret`)
- 통합 테스트는 H2 임베디드 DB 사용
- JPA ddl-auto: `update` (개발 편의상)

---

## 4. 아키텍처: 헥사고날 3계층

### 4.1 계층 다이어그램

```
                ┌─────────────────────────────────┐
                │           adapter               │
                │   (webapi · persistence ·       │
                │    security · integration)      │
                └────────────────┬────────────────┘
                                 │ depends on
                                 ▼
                ┌─────────────────────────────────┐
                │          application            │
                │   (provided · required ports)   │
                └────────────────┬────────────────┘
                                 │ depends on
                                 ▼
                ┌─────────────────────────────────┐
                │            domain               │
                │   (Member · MemberDetail ·      │
                │    Profile · Email · ...)       │
                └─────────────────────────────────┘
```

- **domain 계층**: 비즈니스 규칙과 애그리거트 위치하는 계층으로, 프레임워크 의존성을 최소화합니다.
- **application 계층**: 유스케이스. provided(인바운드 포트)를 구현하고 required(아웃바운드 포트)를 호출합니다.
- **adapter 계층**: 외부 세계와의 접점으로, 웹 API, 영속성, 보안, 외부 연동이 위치하는 계층입니다.

### 4.2 패키지 구조

```
src/main/java/domaindriven/splearn/
├── domain/
│   ├── AbstractEntity.java ← @MappedSuperclass, Hibernate 프록시 대응 equals/hashCode
│   ├── package-info.java ← @NullMarked
│   ├── member/ ← Member 애그리거트
│   │   ├── Member.java ← 애그리거트 루트
│   │   ├── MemberDetail.java ← 프로필/자기소개/시각 정보
│   │   ├── Profile.java ← VO (주소)
│   │   ├── MemberStatus.java ← PENDING/ACTIVE/DEACTIVATED
│   │   ├── MemberRegisterRequest.java
│   │   ├── MemberInfoUpdateRequest.java
│   │   ├── PasswordEncoder.java  ← 도메인 서비스 인터페이스
│   │   ├── DuplicateEmailException.java
│   │   └── DuplicateProfileException.java
│   └── shared/
│       └── Email.java ← VO, 다른 애그리거트에서도 재사용 가능
├── application/
│   └── member/
│       ├── MemberModifyService.java ← provided 구현 (쓰기)
│       ├── MemberQueryService.java ← provided 구현 (읽기)
│       ├── provided/ ← 인바운드 포트
│       │   ├── MemberRegister.java
│       │   └── MemberFinder.java
│       └── required/ ← 아웃바운드 포트
│           ├── MemberRepository.java ← Spring Data Repository
│           └── EmailSender.java
└── adapter/
    ├── ApiControllerAdvice.java ← ProblemDetail 기반 전역 예외 처리
    ├── webapi/
    │   ├── MemberApi.java ← POST /api/members
    │   └── dto/MemberRegisterResponse.java
    ├── persistence/ ← (추후 커스텀 구현)
    ├── security/
    │   └── SecurePasswordEncoder.java ← BCryptPasswordEncoder 래핑
    └── integration/
        └── DummyEmailSender.java ← @Fallback, 로그로 대체
```

### 4.3 의존 방향과 ArchUnit 강제

의존 방향은 `HexagonalArchitectureTest`가 빌드 시마다 검증합니다.

```java
Architectures.layeredArchitecture()
    .consideringAllDependencies()
    .layer("domain").definedBy("domaindriven.splearn.domain..")
    .layer("application").definedBy("domaindriven.splearn.application..")
    .layer("adapter").definedBy("domaindriven.splearn.adapter..")
    .whereLayer("domain").mayOnlyBeAccessedByLayers("application", "adapter")
    .whereLayer("application").mayOnlyBeAccessedByLayers("adapter")
    .whereLayer("adapter").mayNotBeAccessedByAnyLayer();
```

규칙을 바이트코드 수준에서 검사하므로, `domain`에 `adapter`의 클래스가 섞여 들어오면 즉시 테스트가 실패합니다.

---

## 5. 도메인 모델

### 5.1 애그리거트 경계

현재 애그리거트는 **Member** 하나입니다.

```
[Member Aggregate]
  ┌─────────────────────────────────┐
  │  Member (Aggregate Root)        │
  │   - email: Email (natural id)   │
  │   - nickname                    │
  │   - passwordHash                │
  │   - status: MemberStatus        │
  │   └─1:1─ MemberDetail           │
  │           - profile: Profile    │
  │           - introduction        │
  │           - registeredAt        │
  │           - activatedAt         │
  │           - deactivatedAt       │
  └─────────────────────────────────┘
```

- `Member`는 유일한 애그리거트 루트이며 외부에서는 반드시 `Member`를 통해 상태를 바꿉니다.
- `MemberDetail`은 `Member`와 생명주기를 공유합니다. JPA 상 `cascade=ALL`, `fetch=LAZY`로 설정했습니다.
- `Profile`과 `Email`은 값 객체이며 `record`로 선언합니다.

### 5.2 Member 상태 전이

```
     register()
   ──────────────▶  PENDING
                       │
               activate() ─ (현재 시각 → activatedAt)
                       ▼
                     ACTIVE  ◀────── updateInfo() 허용
                       │
             deactivate() ─ (현재 시각 → deactivatedAt)
                       ▼
                  DEACTIVATED
```

불변식을 **애그리거트 루트의 메서드 안**에서 지키도록 강제합니다.

- `activate()`: `PENDING` 상태에서만 호출 가능
- `deactivate()`: `ACTIVE` 상태에서만 호출 가능
- `updateInfo()`: `ACTIVE` 상태에서만 호출 가능

### 5.3 VO: Email, Profile

두 값 객체 모두 **record** + **생성자 검증**으로 불변성을 보장합니다.

**Email**
- 정규식 기반 형식 검증
- `Member.email`은 Hibernate `@NaturalId` → `email_address` 컬럼에 UNIQUE 제약
- `domain/shared/`에 위치하여 다른 애그리거트에서도 재사용 가능

**Profile**
- 알파벳 + 숫자만 허용, 최대 15자
- 빈 문자열은 허용(프로필 주소 제거 의도)
- `profile_address` 컬럼에 UNIQUE 제약

### 5.4 도메인 서비스: PasswordEncoder

`domain/member/PasswordEncoder`는 도메인 인터페이스로 두고, 실제 BCrypt 구현(`SecurePasswordEncoder`)은 adapter 계층에 둡니다. 이렇게 하면 도메인이 Spring Security에 직접 의존하지 않습니다.

```
domain.PasswordEncoder (interface) ← Member.register() 가 사용
         ▲
         │ implements
adapter.security.SecurePasswordEncoder (BCrypt 래핑)
```

---

## 6. 설계 의사결정 기록

이 섹션은 "**왜 이 선택을 했는가**"를 기록합니다. 코드만 봐서는 보이지 않는 맥락을 보존하기 위한 목적입니다.

### 6.1 왜 애그리거트 단위로 패키지를 재편했는가

초기에는 `domain/`, `application/` 아래에 모든 클래스가 평평하게 놓여 있었습니다. Member 애그리거트에 `MemberDetail`, `Profile` 같은 구성원이 늘어나자 **경계가 보이지 않는** 문제가 생겼습니다.

- `domain/` → `domain/member/`, `domain/shared/` 로 분리
- `application/` → `application/member/` + `provided`/`required` 서브패키지

이 구조는 **도메인 전문가가 말하는 "애그리거트"가 코드의 패키지와 1:1로 매핑되도록** 만들어, 향후 강사, 강의, 수강 애그리거트가 추가되더라도 각자의 경계가 유지됩니다.


### 6.2 왜 JPA 애노테이션을 orm.xml로 완전 이관했는가

도메인 클래스에 `@Entity`, `@Column`, `@OneToOne`, `@Embedded` 같은 JPA 애노테이션이 붙으면 도메인이 영속성 기술에 강하게 묶입니다. 다음과 같은 문제가 있습니다.

1. 도메인 코드를 읽을 때 비즈니스 규칙과 매핑 정보가 뒤섞여 가독성이 떨어짐
2. 영속성 기술을 바꾸거나 테스트할 때 도메인 코드가 영향을 받음
3. VO(record)에까지 `@Embeddable`이 침투

따라서 splearn은 JPA 매핑을 전부 `src/main/resources/META-INF/orm.xml`로 이관했습니다.

```xml
<entity class="domaindriven.splearn.domain.member.Member">
    <table name="MEMBER">
        <unique-constraint name="UK_MEMBER_EMAIL_ADDRESS">
            <column-name>email_address</column-name>
        </unique-constraint>
        <unique-constraint name="UK_MEMBER_DETAIL_ID">
            <column-name>detail_id</column-name>
        </unique-constraint>
    </table>
    <attributes>
        <natural-id>
            <embedded name="email">
                <attribute-override name="address">
                    <column name="email_address" nullable="false" length="150"/>
                </attribute-override>
            </embedded>
        </natural-id>
        ...
        <one-to-one name="detail" fetch="LAZY">
            <cascade><cascade-all/></cascade>
        </one-to-one>
    </attributes>
</entity>
```

결과적으로 도메인 클래스에는 Lombok 이외의 프레임워크 애노테이션이 거의 남지 않습니다.


### 6.3 왜 Hibernate 프록시를 고려한 equals/hashCode인가

JPA 환경에서 `equals`/`hashCode`를 잘못 짜면 지연 로딩 프록시와 실제 엔티티가 서로 같지 않다고 판단되어, 컬렉션 안의 중복이나 영속성 컨텍스트 밖에서의 비교가 깨집니다.

`AbstractEntity`는 이를 피하기 위해 다음을 수행합니다.

- 피연산자가 `HibernateProxy`면 실제 클래스를 꺼내어 비교
- `id`가 `null`인 영속화 전 상태에서는 서로 다른 인스턴스로 취급
- `hashCode`는 `id` 값이 아니라 클래스로 계산 (id가 insert 후에야 생기므로)


### 6.4 왜 ProblemDetail (RFC 9457)인가

오류 응답을 각 API마다 자유 양식으로 돌려주면 클라이언트가 모든 엔드포인트마다 다른 스키마를 파싱해야 합니다. splearn은 **표준**인 RFC 9457 `application/problem+json`을 채택했고, Spring 6의 `ProblemDetail` 클래스를 직접 반환합니다.

- `@ControllerAdvice`에서 도메인 예외(`DuplicateEmailException`, `DuplicateProfileException`)를 잡아 `409 Conflict`로 변환
- `timestamp`, `exception` 같은 보조 필드는 `ProblemDetail.setProperty()`로 확장

### 6.5 왜 CQS로 Modify/Query 서비스를 분리했는가

`MemberService` 하나에 등록, 조회, 활성화, 수정, 탈퇴가 모두 모여 있으면 다음 문제가 생깁니다.

- 조회만 필요한 곳에서도 쓰기 메서드가 보임
- 트랜잭션 전략이 모호해짐(읽기 전용? 쓰기?)
- 테스트할 때도 얽혀 있어 피드백이 느려짐

Command-Query Separation 관점에서 `MemberModifyService`(쓰기)와 `MemberQueryService`(읽기)로 분리하고, 각 서비스는 자기 책임의 인바운드 포트만 구현하도록 했습니다. `MemberModifyService`가 조회가 필요할 때는 `MemberFinder`(= `MemberQueryService`)를 주입받아 사용합니다.


### 6.6 왜 요청 객체에 Bean Validation을 걸었는가

Member의 가입, 정보 수정 요청은 도메인 규칙과 별개로 입력 자체의 형식 제약이 있습니다. 예: 닉네임은 5~20자, 이메일은 RFC 형식, 프로필 주소는 15자 이내

- 요청 레코드(`MemberRegisterRequest`, `MemberInfoUpdateRequest`)에 `@Email`, `@Size`, `@NotNull` 적용
- 서비스 메서드 시그니처에 `@Valid`를 붙이고 서비스 클래스에 `@Validated`를 선언 → 애플리케이션 계층에서도 검증 자동 발동

입력 검증은 어댑터(컨트롤러)뿐 아니라 애플리케이션 경계에서도 다시 걸리므로, 테스트에서도 잘못된 입력이 즉시 `ConstraintViolationException`으로 떨어집니다.


### 6.7 왜 프로필 중복 검사를 애플리케이션 서비스에 두었는가

프로필 주소의 "다른 회원과 중복되지 않는다"는 규칙은 단일 Member 애그리거트 내부에서 알 수 없습니다. 다른 애그리거트와의 비교가 필요하므로 도메인 메서드로 두기 어렵습니다.

- `MemberRepository.findByProfile(Profile)` 아웃바운드 포트 추가 (`@Query`로 `member.detail.profile` 매핑)
- `MemberModifyService.checkDuplicateProfile(member, newAddress)`에서 검사
- "본인이 본인의 현재 주소로 수정"인 경우는 통과시키는 예외 처리


---

## 7. 테스트 전략

### 7.1 계층별 테스트 접근

| 계층 | 테스트 종류 | 사용 도구 | 목적                   |
|---|---|---|----------------------|
| domain | 단위 테스트 | JUnit, AssertJ | 불변식, 상태 전이, 검증 규칙    |
| application | 통합 테스트 | `@SpringBootTest` + `@Transactional` | 유스케이스가 DB를 포함해 돌아가는가 |
| application (repo) | JPA 슬라이스 | `@DataJpaTest` | Repository 쿼리와 제약 조건 |
| adapter (web) | MockMvc 단위 | `@WebMvcTest` | 컨트롤러 입출력 매핑          |
| adapter (web) | 통합 | `@SpringBootTest` + `MockMvcTester` | API + DB 상태 동시 검증    |
| 아키텍처 | ArchUnit | `@AnalyzeClasses` | 의존 방향 강제             |

### 7.2 도메인 테스트

`MemberTest`, `ProfileTest`, `EmailTest`는 외부 의존성이 전혀 없는 순수 단위 테스트입니다.

- `MemberTest`: 등록 → 활성화 → 탈퇴 → 정보 수정 전 상태 전이, 각 메서드의 전제조건이 깨졌을 때 `IllegalStateException` 발생 확인
- `ProfileTest`: 잘못된 형식과 길이 초과에 대한 생성자 검증
- `EmailTest`: 이메일 형식 검증

도메인 테스트는 가장 빠르고 가장 많아야 합니다. 복잡성과 중요성이 가장 높은 영역이기 때문입니다.

### 7.3 애플리케이션 통합 테스트

`MemberRegisterTest`, `MemberFinderTest`는 `@SpringBootTest` + `@Transactional` + `SplearnTestConfiguration`을 활용합니다.

- 실제 Spring 컨텍스트와 H2 DB를 띄워 등록, 활성화, 정보 수정, 비활성화 전체 흐름을 검증
- `@Transactional`로 각 테스트 후 롤백
- `EntityManager.flush()` / `clear()`로 1차 캐시가 숨기는 버그를 드러냄
- `SplearnTestConfiguration`은 `EmailSender`를 Mock으로, `PasswordEncoder`는 테스트 전용 fixture로 교체

### 7.4 어댑터 테스트: WebMvcTest vs 통합 테스트

웹 레이어는 두 종류의 테스트를 함께 운영합니다.

- `MemberApiWebMvcTest` (`@WebMvcTest`): 컨트롤러만 로드합니다. `MemberRegister`는 `@MockitoBean`으로 대체. 요청/응답 매핑, 상태 코드에 집중.
- `MemberApiTest` (`@SpringBootTest` + `@AutoConfigureMockMvc` + `@Transactional`): 실제 서비스와 DB가 결합된 상태에서 등록 API 호출 결과가 DB에 반영되는지, 중복 등록 시 `409 Conflict`가 나오는지까지 검증합니다.

슬라이스 테스트는 빠르고 통합 테스트는 끝까지 검증합니다. 둘 다 있을 때 가장 유용합니다.

### 7.5 아키텍처 테스트: HexagonalArchitectureTest의 경험적 함정

`HexagonalArchitectureTest`는 도입 당시 두 가지 실수를 겪었고, README에 기록합니다.

**1) `dependOnClassesThat` vs `onlyDependOnClassesThat`**

학습용 ArchUnit 테스트에서 `dependOnClassesThat()`로 룰을 썼더니, domain이 adapter에 의존해도 테스트가 통과했습니다. 이유: `dependOnClassesThat()`은 "**적어도 하나의 의존성**이 조건을 만족하면 OK"라는 의미입니다. 모든 의존성에 대한 조건을 걸려면 `onlyDependOnClassesThat()` 를 써야 합니다.

**2) `ImportOption.OnlyIncludeTests` vs `DoNotIncludeTests`**

`@AnalyzeClasses(importOptions = ImportOption.OnlyIncludeTests.class)` 로 두면 테스트 클래스만 분석 대상이 됩니다. 프로덕션 코드 아키텍처 검증이 목적이라면 `DoNotIncludeTests`를 써야 합니다. 이름이 혼동되기 쉽습니다.

최종 `HexagonalArchitectureTest`는 `DoNotIncludeTests`와 `layeredArchitecture()` 조합으로 프로덕션 코드에만 규칙을 강제합니다. `learningtest/archunit/` 하위는 ArchUnit 자체를 배우기 위한 학습용 테스트로 분리해 두었습니다.

### 7.6 정적 분석: SpotBugs

빌드 단계에 SpotBugs가 포함됩니다. 특별히 억제한 규칙 하나:

- `EQ_CHECK_FOR_OPERAND_NOT_COMPATIBLE_WITH_THIS`: Hibernate 프록시 대응 `equals`에 대한 과잉 경고로, `spotbugs-exclude-filter.xml`에서 제외했습니다.

테스트 코드의 dead local store(미사용 변수) 같은 실수는 SpotBugs가 잡아 CI가 깨지므로 즉시 눈에 띕니다. 과거 실제로 `MemberRegisterTest`의 미사용 `Member member = ...` 할당이 SpotBugs에 걸려 제거된 사례가 있습니다.
