# 개발 환경 설정 가이드

## 개요

ChatSocket 프로젝트의 개발 환경을 설정하는 방법을 단계별로 설명합니다.

## 시스템 요구사항

### 필수 소프트웨어

- **Java Development Kit (JDK) 21**
  - Oracle JDK, OpenJDK, Amazon Corretto 모두 지원
  - `java -version`으로 설치 확인

- **Apache Maven 3.8+**
  - 프로젝트에 Maven Wrapper 포함되어 있음
  - `./mvnw --version` 또는 `mvnw.cmd --version` (Windows)

- **Git 2.0+**
  - 버전 관리 및 협업용

### 권장 IDE

- **IntelliJ IDEA 2023.3+** (Community/Ultimate)
- **Eclipse IDE 2023-12+**
- **Visual Studio Code** (Spring Boot Extension Pack 필요)

## IntelliJ IDEA 설정

### 1. 프로젝트 Import

```bash
# 1. 프로젝트 클론
git clone <repository-url>
cd chatsocket

# 2. IntelliJ IDEA에서 Open
File > Open > chatsocket 폴더 선택
```

### 2. JDK 설정

```
File > Project Structure > Project Settings > Project
- Project SDK: 21 (Java version 21.x.x)
- Project language level: 21 - Record patterns, pattern matching for switch
```

### 3. 필수 플러그인

#### 기본 설치 플러그인
- **Spring Boot** - Spring Boot 애플리케이션 지원
- **Spring MVC** - Spring MVC 프레임워크 지원
- **Maven** - Maven 빌드 도구 지원
- **Git** - Git 버전 관리

#### 추천 플러그인
```
Settings > Plugins > Marketplace

1. SonarLint - 코드 품질 분석
2. CheckStyle-IDEA - 코딩 스타일 검사
3. String Manipulation - 문자열 조작 도구
4. Rainbow Brackets - 괄호 색깔 구분
5. Grep Console - 콘솔 로그 하이라이팅
6. RestfulTool - REST API 테스트
7. JSON Viewer - JSON 포맷팅
```

### 4. 코드 스타일 설정

```
Settings > Editor > Code Style > Java

General:
- Tab size: 4
- Indent: 4
- Continuation indent: 8
- Use tab character: 체크 해제

Imports:
- Class count to use import with '*': 10
- Names count to use static import with '*': 10
- Import layout:
  - import static all other imports
  - <blank line>
  - import java.*
  - import javax.*
  - <blank line>
  - import all other imports
```

### 5. 실행 설정

#### Application 실행 설정
```
Run > Edit Configurations > + > Application

Name: ChatSocket Application
Main class: com.ezlevup.chatsocket.ChatsocketApplication
VM options: -Dspring.profiles.active=dev
Program arguments: 
Working directory: $MODULE_WORKING_DIR$
Environment variables: 
  SPRING_PROFILES_ACTIVE=dev
```

#### Maven 실행 설정
```
Run > Edit Configurations > + > Maven

Name: Spring Boot Run
Command line: spring-boot:run
Working directory: $PROJECT_DIR$
```

### 6. 디버깅 설정

#### Remote Debug 설정
```
Run > Edit Configurations > + > Remote JVM Debug

Name: ChatSocket Remote Debug
Host: localhost
Port: 5005
Command line arguments: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
```

#### 애플리케이션 디버그 실행
```bash
# Maven으로 디버그 모드 실행
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

## 개발 환경 프로필

### application-dev.properties
```properties
# 개발 환경 설정
spring.application.name=chatsocket-dev
server.port=8080

# 로깅 설정
logging.level.root=INFO
logging.level.com.ezlevup.chatsocket=DEBUG
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n

# H2 데이터베이스 콘솔 활성화
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# WebSocket 디버그 설정
logging.level.org.springframework.web.socket=DEBUG
logging.level.org.springframework.messaging=DEBUG

# 개발용 CORS 설정 (필요시)
websocket.allowed-origins=http://localhost:3000,http://localhost:8080
```

### application-test.properties
```properties
# 테스트 환경 설정
spring.application.name=chatsocket-test
server.port=0

# 테스트용 로깅
logging.level.org.springframework.test=DEBUG
logging.level.org.springframework.web.socket=DEBUG

# 테스트 데이터베이스 설정
spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL
```

## 빌드 및 실행

### Maven 명령어

```bash
# Windows 환경
mvnw.cmd clean compile        # 컴파일
mvnw.cmd test                # 테스트 실행
mvnw.cmd spring-boot:run     # 애플리케이션 실행
mvnw.cmd package             # JAR 파일 생성

# Linux/macOS 환경
./mvnw clean compile         # 컴파일
./mvnw test                 # 테스트 실행
./mvnw spring-boot:run      # 애플리케이션 실행
./mvnw package              # JAR 파일 생성
```

### 개발 서버 실행

```bash
# 방법 1: Maven을 통한 실행
./mvnw spring-boot:run -Dspring.profiles.active=dev

# 방법 2: JAR 파일 실행
./mvnw package
java -Dspring.profiles.active=dev -jar target/chatsocket-0.0.1-SNAPSHOT.jar

# 방법 3: IDE에서 직접 실행
ChatsocketApplication.java 우클릭 > Run 'ChatsocketApplication'
```

### 애플리케이션 접속

- **메인 페이지**: http://localhost:8080
- **H2 콘솔**: http://localhost:8080/h2-console (dev 프로필에서만)
- **API 엔드포인트**: http://localhost:8080/api/*

## 디버깅 방법

### 1. 로그 기반 디버깅

```java
// 로깅 설정
private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

// 로그 레벨별 사용
logger.trace("상세한 실행 흐름");
logger.debug("디버그 정보: roomId={}", roomId);
logger.info("일반 정보: 새로운 룸 생성됨");
logger.warn("경고: 비어있는 룸 발견");
logger.error("오류: 룸 생성 실패", exception);
```

### 2. 브레이크포인트 디버깅

```java
public class ChatWebSocketHandler {
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 여기에 브레이크포인트 설정
        String sessionId = session.getId();
        logger.debug("WebSocket 연결 설정: {}", sessionId);
    }
}
```

### 3. WebSocket 디버깅

#### 브라우저 개발자 도구
```javascript
// 콘솔에서 WebSocket 연결 테스트
const ws = new WebSocket('ws://localhost:8080/websocket');
ws.onopen = () => console.log('연결됨');
ws.onmessage = (e) => console.log('메시지:', e.data);
ws.onerror = (e) => console.error('에러:', e);
```

#### WebSocket 메시지 로깅
```properties
# application-dev.properties
logging.level.org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator=DEBUG
```

### 4. API 테스트

#### cURL을 이용한 REST API 테스트
```bash
# 룸 목록 조회
curl -X GET http://localhost:8080/api/rooms

# 룸 생성
curl -X POST http://localhost:8080/api/rooms \
  -H "Content-Type: application/json" \
  -d '{"roomName": "테스트룸"}'

# 특정 룸 조회
curl -X GET http://localhost:8080/api/rooms/{roomId}
```

## 문제 해결

### 자주 발생하는 문제

#### 1. Java 버전 문제
```bash
# 현재 Java 버전 확인
java -version

# JAVA_HOME 설정 확인
echo $JAVA_HOME  # Linux/macOS
echo %JAVA_HOME% # Windows

# 올바른 Java 버전 설정
export JAVA_HOME=/path/to/jdk-21  # Linux/macOS
set JAVA_HOME=C:\path\to\jdk-21   # Windows
```

#### 2. 포트 충돌 문제
```bash
# 포트 사용 현황 확인
netstat -tulpn | grep 8080  # Linux
netstat -an | findstr 8080  # Windows

# 다른 포트로 실행
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

#### 3. Maven 의존성 문제
```bash
# 의존성 다시 다운로드
./mvnw dependency:purge-local-repository
./mvnw clean install
```

#### 4. WebSocket 연결 문제
```javascript
// 브라우저 콘솔에서 연결 테스트
const testConnection = () => {
  const ws = new WebSocket('ws://localhost:8080/websocket');
  ws.onopen = () => console.log('✅ WebSocket 연결 성공');
  ws.onerror = (e) => console.error('❌ WebSocket 연결 실패:', e);
  ws.onclose = (e) => console.log('연결 종료:', e.code, e.reason);
};
testConnection();
```

## 성능 모니터링

### JVM 모니터링
```bash
# JVM 힙 메모리 사용량 확인
jcmd <pid> VM.heap_info

# GC 로그 설정으로 실행
java -XX:+PrintGC -XX:+PrintGCDetails -jar target/chatsocket-0.0.1-SNAPSHOT.jar
```

### 애플리케이션 메트릭스 (Actuator 추가 시)
```properties
# application-dev.properties
management.endpoints.web.exposure.include=health,metrics,info
management.endpoint.health.show-details=always
```

이 가이드를 따라 개발 환경을 설정하면 ChatSocket 프로젝트를 효율적으로 개발할 수 있습니다.