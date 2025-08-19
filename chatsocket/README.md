# Spring Boot WebSocket 실시간 채팅 애플리케이션

Spring Boot + WebSocket을 사용한 실시간 채팅 시스템입니다.

## 🚀 주요 기능

- **실시간 채팅**: WebSocket을 통한 양방향 실시간 통신
- **다중 채팅방**: 여러 채팅방 생성 및 관리
- **사용자 관리**: 입장/퇴장 알림, 접속자 수 표시
- **반응형 UI**: Bootstrap 5 기반 모바일 친화적 인터페이스
- **성능 최적화**: 동시 접속자 1000명 지원, 자동 재연결

## 🛠 기술 스택

- **Backend**: Spring Boot 3.5.4, Java 21
- **WebSocket**: Spring WebSocket + SockJS
- **Database**: H2 (메모리 DB)
- **Frontend**: Thymeleaf, Bootstrap 5, JavaScript
- **Build Tool**: Maven

## 🏗 아키텍처

```
src/
├── main/
│   ├── java/com/ezlevup/chatsocket/
│   │   ├── config/          # WebSocket 설정, 성능 최적화
│   │   ├── controller/      # REST API, 페이지 컨트롤러
│   │   ├── handler/         # WebSocket 메시지 핸들러
│   │   ├── model/           # 데이터 모델, DTO
│   │   └── service/         # 백그라운드 서비스
│   └── resources/
│       ├── templates/       # Thymeleaf 템플릿
│       └── static/js/       # JavaScript 클라이언트
└── test/                   # 단위 테스트, 통합 테스트, 성능 테스트
```

## 🚦 빠른 시작

### 1. 저장소 클론
```bash
git clone https://github.com/space-cap/spring-boot-websocket-chat.git
cd spring-boot-websocket-chat/chatsocket
```

### 2. 애플리케이션 실행
```bash
# Maven 래퍼 사용 (권장)
./mvnw spring-boot:run

# 또는 Maven 직접 사용
mvn spring-boot:run
```

### 3. 웹 브라우저에서 접속
```
http://localhost:8080
```

## 📋 주요 엔드포인트

### REST API
- `GET /` - 메인 페이지 (채팅방 목록)
- `GET /chat/rooms` - 채팅방 목록 API
- `POST /chat/room` - 채팅방 생성 API
- `GET /chat/room/{roomId}` - 특정 채팅방 조회
- `GET /chat/room/{roomId}` - 채팅방 페이지

### WebSocket
- `ws://localhost:8080/ws/chat` - WebSocket 연결 엔드포인트

## 🔧 설정 및 최적화

### 성능 설정
- **최대 동시 연결**: 1,000개 세션
- **메시지 크기 제한**: 1KB (JSON), 500자 (내용)
- **세션 타임아웃**: 10분
- **하트비트 간격**: 25초
- **자동 재연결**: 최대 5회 시도

### 메모리 관리
- 빈 채팅방 자동 정리 (5분마다)
- 세션 통계 모니터링 (1분마다)
- 가비지 컬렉션 최적화

## 🧪 테스트

### 단위 테스트 실행
```bash
./mvnw test -Dtest="ChatControllerTests,ChatModelTests"
```

### 전체 테스트 실행
```bash
./mvnw test
```

### 성능 테스트
- 동시 연결 테스트 (20명)
- 메시지 처리량 테스트 (초당 50+ 메시지)
- 메시지 크기 제한 테스트

## 📊 모니터링

### H2 데이터베이스 콘솔
```
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:testdb
User: sa
Password: (비어있음)
```

### 로깅
- **INFO**: 일반 애플리케이션 로그
- **DEBUG**: 채팅방 정리 서비스
- **WARN**: WebSocket 관련 로그

## 🔒 보안 고려사항

- XSS 방지: 메시지 내용 이스케이핑
- 입력 검증: 메시지 크기 및 형식 검증
- 세션 관리: 자동 타임아웃 및 정리
- CORS 설정: 개발용 전체 허용 (운영 시 수정 필요)

## 🚀 배포 및 운영

### 프로덕션 빌드
```bash
./mvnw clean package
java -jar target/chatsocket-0.0.1-SNAPSHOT.jar
```

### Docker 배포 (선택사항)
```dockerfile
FROM openjdk:21-jdk-slim
COPY target/chatsocket-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

### 환경별 설정
- **개발**: `application.properties`
- **운영**: `application-prod.properties` (별도 설정 권장)

## 🤝 기여 방법

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 라이선스

This project is licensed under the MIT License.

## 🐛 이슈 리포팅

버그 발견이나 기능 제안은 [GitHub Issues](https://github.com/space-cap/spring-boot-websocket-chat/issues)에 등록해 주세요.

---

**개발자**: [Your Name]  
**이메일**: your.email@example.com  
**프로젝트**: Spring Boot WebSocket 채팅 시스템