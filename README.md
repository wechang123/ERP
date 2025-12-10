# ERP 마이크로서비스 플랫폼

## 프로젝트 개요

**ERP 시스템**은 기업의 결재 및 직원 관리를 위한 **마이크로서비스 기반 웹 애플리케이션**입니다.
분산된 서비스들이 REST API, gRPC, Kafka 메시징을 통해 협력하여 **확장 가능하고 유지보수하기 쉬운 시스템**을 구축합니다.

**과목:** 고급 컴퓨터 프로그래밍 실습
**기한:** 2025년 12월 11일

---

## 시스템 아키텍처

![시스템 아키텍처 다이어그램](image/architecture_diagram.drawio-2.png)

---

## 5개 주요 서비스

### 1. **Employee Service** (포트 8081)
**직원 정보 관리 시스템**
- MySQL 데이터베이스에 직원 정보 저장
- REST API를 통한 직원 CRUD 작업
- 직원 목록 조회, 생성, 수정, 삭제
- 점수: 20점

### 2. **Approval Request Service** (포트 8082)
**결재 요청 관리 및 조정**
- MongoDB에 결재 요청 저장
- 다단계 결재 프로세스 관리
- Kafka를 통해 Approval Processing Service로 비동기 메시징
- gRPC 클라이언트로 Processing Service 호출
- 점수: 30점

### 3. **Approval Processing Service** (포트 8083, gRPC 9091)
**결재 처리 및 큐 관리**
- 메모리 기반 결재 큐 관리
- gRPC 서버로 승인/반려 처리
- Kafka 컨슈머로 비동기 요청 수신
- Kafka 프로듀서로 결과 발행
- 점수: 30점

### 4. **Notification Service** (포트 8080)
**WebSocket 기반 실시간 알림**
- Kafka 컨슈머로 결재 결과 수신
- WebSocket을 통해 클라이언트에 실시간 푸시
- 연결된 사용자에게 즉시 알림 전송
- 점수: 20점

### 5. **Web Service** (포트 8000)
**통합 웹 UI 및 오케스트레이션**
- Thymeleaf로 동적 웹 페이지 렌더링
- 4개 백엔드 서비스와 통신
- 직원 관리, 결재 요청, 결재 처리, 알림 통합
- 대시보드 및 통계 화면

---

## 기술 스택

| 계층 | 기술 |
|------|------|
| **Frontend** | HTML/CSS/JavaScript, Thymeleaf |
| **Backend** | Spring Boot 3.2.0, Spring Cloud |
| **통신** | REST API, gRPC 1.60.0, WebSocket |
| **메시징** | Apache Kafka (비동기 메시징) |
| **데이터** | MySQL (관계형), MongoDB (문서형) |
| **기타** | Lombok, Gradle, Docker Compose |

---

## 실행 방법

### 사전 요구사항
- Docker & Docker Compose 설치
- Java 17+
- Gradle 8.5+

### 1단계: 인프라 시작
```bash
docker compose up -d
```
Zookeeper, Kafka, MySQL, MongoDB 자동 시작

### 2단계: 각 서비스 실행
각 터미널에서:
```bash
# Terminal 1: Employee Service
cd employee-service && ./gradlew bootRun

# Terminal 2: Approval Request Service
cd approval-request-service && ./gradlew bootRun

# Terminal 3: Approval Processing Service
cd approval-processing-service && ./gradlew bootRun

# Terminal 4: Notification Service
cd notification-service && ./gradlew bootRun

# Terminal 5: Web Service
cd web-service && ./gradlew bootRun
```

### 3단계: 시스템 접속
```
http://localhost:8000
```

---

## 주요 기능

### 직원 관리
- 직원 정보 조회/생성/수정/삭제
- 직원 목록 검색 및 필터링

### 결재 워크플로우
1. **결재 요청**: Web Service에서 요청 생성
2. **비동기 처리**: Kafka를 통해 Processing Service로 전달
3. **결재 큐 관리**: 메모리 기반 결재자별 큐
4. **승인/반려**: gRPC를 통한 처리
5. **실시간 알림**: WebSocket으로 즉시 전달

### 다단계 결재
- 여러 결재자 설정 가능
- 순차적 결재 프로세스
- 각 단계별 상태 추적

### 실시간 알림
- 결재 결과를 실시간으로 클라이언트에 푸시
- 여러 사용자 동시 접속 지원

---

## 프로젝트 구조

```
ERP/
├── employee-service/          # 직원 관리 서비스
├── approval-request-service/  # 결재 요청 서비스
├── approval-processing-service/ # 결재 처리 서비스
├── notification-service/      # 알림 서비스
├── web-service/               # 웹 UI 서비스
├── proto/                      # gRPC 프로토콜 정의
├── docker-compose.yml         # 인프라 정의
└── README.md                  # 이 파일
```

---

## 주요 구현 사항

### 필수 기능
- 4개 마이크로서비스 (Employee, Approval Request, Approval Processing, Notification)
- REST API 기반 통신
- MySQL 데이터베이스 (Employee)
- MongoDB 데이터베이스 (Approval Requests)
- gRPC 동기 통신 (Processing Service)
- WebSocket 실시간 알림

### 추가 구현 기능
- Kafka 비동기 메시징 (Approval Request → Processing)
- Docker Compose 인프라 (Zookeeper, Kafka, MySQL, MongoDB)
- 웹 UI (Thymeleaf + JavaScript)
- 한글 주석 및 로깅
- 완전한 오류 처리

---

## 주요 화면

### 직원 관리
직원 정보 조회, 등록, 수정 기능

![직원 목록 (일반 사용자)](image/01_직원목록_일반사용자.png)
![직원 목록 (관리자)](image/02_직원목록_관리자.png)
![직원 등록 폼](image/04_직원등록_폼.png)
![직원 수정 폼](image/03_직원수정_폼.png)

### 결재 요청
결재 요청 생성 및 다단계 결재 설정

![결재 요청 목록](image/05_결재요청_목록.png)
![결재 요청 새 폼](image/06_결재요청_새폼.png)
![결재 요청 4단계 작성](image/07_결재요청_4단계작성.png)

### 결재 처리
결재 승인/반려 및 대기함 관리

![대시보드 알림 (새 결재 요청)](image/08_대시보드_알림_새결재요청.png)
![결재 대기함 (이승인)](image/09_결재대기함_이승인.png)
![결재 상세 (진행중)](image/10_결재상세_진행중_대기.png)
![결재 상세 (1단계 승인)](image/11_결재상세_1단계승인.png)

### 실시간 알림
WebSocket 기반 실시간 푸시 알림

![WebSocket 알림 팝업](image/31_WebSocket알림팝업.png)
![새 결재 알림 (실시간)](image/32_새결재알림_실시간.png)
![승인 알림 (이중 팝업)](image/33_승인알림_이중팝업.png)

### 통계 및 대시보드
결재 통계 및 시스템 상태 모니터링

![결재 통계 대시보드](image/15_결재통계_대시보드.png)
![대시보드 서비스 상태](image/26_대시보드_서비스상태.png)

### 데이터베이스
MySQL 및 MongoDB 데이터 구조

![Docker 컨테이너 상태](image/17_docker_컨테이너상태.png)
![MySQL Employees 테이블](image/18_mysql_employees테이블.png)
![MongoDB 결재 요청 컬렉션](image/19_mongodb_결재요청컬렉션.png)

---

## 라이선스

이 프로젝트는 교육 목적의 고급 프로그래밍 실습 과제입니다.

---

## 개발자

**유위창** (32202870)
2025년 12월
