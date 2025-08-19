# Spring Boot WebSocket Chat 배포 가이드

이 문서는 Spring Boot WebSocket 채팅 애플리케이션을 실제 서버 환경에 배포하기 위한 단계별 가이드입니다.

## 목차

1. [로컬 개발 환경 설정](#1-로컬-개발-환경-설정)
2. [Docker 컨테이너화](#2-docker-컨테이너화)
3. [클라우드 배포](#3-클라우드-배포)
4. [환경 변수 설정](#4-환경-변수-설정)
5. [데이터베이스 연동](#5-데이터베이스-연동)
6. [로드 밸런싱 및 스케일링](#6-로드-밸런싱-및-스케일링)
7. [모니터링 설정](#7-모니터링-설정)
8. [CI/CD 파이프라인](#8-cicd-파이프라인)
9. [트러블슈팅](#9-트러블슈팅)

---

## 1. 로컬 개발 환경 설정

### 필수 요구사항
- **Java 21** (OpenJDK 권장)
- **Maven 3.6+**
- **Git**

### 설정 단계

```bash
# 1. 프로젝트 클론
git clone <repository-url>
cd chatsocket

# 2. Maven Wrapper를 이용한 빌드 및 실행
./mvnw clean compile
./mvnw spring-boot:run

# Windows 사용자
mvnw.cmd clean compile
mvnw.cmd spring-boot:run
```

### 개발 환경 확인
- 애플리케이션이 포트 8080에서 실행되는지 확인
- WebSocket 연결이 정상적으로 작동하는지 테스트

---

## 2. Docker 컨테이너화

### Dockerfile 작성

```dockerfile
# Multi-stage build를 위한 베이스 이미지
FROM openjdk:21-jdk-slim as build

# 작업 디렉토리 설정
WORKDIR /app

# Maven wrapper와 설정 파일 복사
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# 의존성 다운로드 (캐시 최적화)
RUN ./mvnw dependency:go-offline

# 소스 코드 복사 및 빌드
COPY src src
RUN ./mvnw clean package -DskipTests

# Runtime 이미지
FROM openjdk:21-jre-slim

# 포트 설정
EXPOSE 8080

# 애플리케이션 실행을 위한 사용자 생성
RUN adduser --system --group springuser

# JAR 파일 복사
COPY --from=build /app/target/*.jar app.jar

# 사용자 변경
USER springuser

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### docker-compose.yml 설정

```yaml
version: '3.8'

services:
  chatsocket:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JAVA_OPTS=-Xms256m -Xmx512m
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    volumes:
      - app-logs:/app/logs

  # 선택사항: 외부 데이터베이스 사용시
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: chatsocket
      POSTGRES_USER: chatuser
      POSTGRES_PASSWORD: chatpass
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

volumes:
  app-logs:
  postgres_data:
```

### Docker 실행 명령어

```bash
# 이미지 빌드
docker build -t chatsocket:latest .

# 단일 컨테이너 실행
docker run -p 8080:8080 chatsocket:latest

# Docker Compose로 전체 스택 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f chatsocket
```

---

## 3. 클라우드 배포

### 3.1 AWS EC2 + Application Load Balancer

#### EC2 인스턴스 설정

```bash
# 1. EC2 인스턴스 접속 후 Docker 설치
sudo yum update -y
sudo yum install -y docker
sudo service docker start
sudo usermod -a -G docker ec2-user

# 2. Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 3. 애플리케이션 배포
git clone <repository-url>
cd chatsocket
docker-compose up -d
```

#### Application Load Balancer 설정

1. **대상 그룹 생성**
   - 프로토콜: HTTP
   - 포트: 8080
   - 상태 확인 경로: `/actuator/health`

2. **로드 밸런서 생성**
   - 유형: Application Load Balancer
   - 리스너: HTTP:80 → 대상 그룹

3. **WebSocket 지원을 위한 Sticky Session 설정**
   - 대상 그룹 → 속성 → 고정성: 애플리케이션 기반 쿠키

### 3.2 Heroku 배포

#### 설정 파일

```yaml
# app.json
{
  "name": "chatsocket",
  "description": "Spring Boot WebSocket Chat Application",
  "buildpacks": [
    {
      "url": "heroku/java"
    }
  ],
  "env": {
    "SPRING_PROFILES_ACTIVE": {
      "value": "heroku"
    }
  }
}
```

#### Procfile

```
web: java -Dserver.port=$PORT $JAVA_OPTS -jar target/*.jar
```

#### 배포 명령어

```bash
# Heroku CLI 로그인
heroku login

# 애플리케이션 생성
heroku create your-app-name

# 환경 변수 설정
heroku config:set SPRING_PROFILES_ACTIVE=heroku

# 배포
git push heroku main

# 로그 확인
heroku logs --tail
```

---

## 4. 환경 변수 설정

### 4.1 application-prod.properties

```properties
# 서버 설정
server.port=${PORT:8080}
server.servlet.context-path=/

# 로깅 설정
logging.level.com.ezlevup.chatsocket=INFO
logging.level.org.springframework.web.socket=INFO
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
logging.file.name=logs/chatsocket.log

# WebSocket 설정
spring.websocket.timeout=60000

# 액추에이터 설정
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized
management.health.websocket.enabled=true
```

### 4.2 환경별 설정

#### Development
```bash
export SPRING_PROFILES_ACTIVE=dev
export LOG_LEVEL=DEBUG
```

#### Staging
```bash
export SPRING_PROFILES_ACTIVE=staging
export LOG_LEVEL=INFO
export DATABASE_URL=jdbc:postgresql://staging-db:5432/chatsocket
```

#### Production
```bash
export SPRING_PROFILES_ACTIVE=prod
export LOG_LEVEL=WARN
export DATABASE_URL=jdbc:postgresql://prod-db:5432/chatsocket
export JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"
```

---

## 5. 데이터베이스 연동

### 5.1 PostgreSQL 연동

#### pom.xml 의존성 추가

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

#### application-prod.properties

```properties
# 데이터베이스 설정
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/chatsocket}
spring.datasource.username=${DB_USERNAME:chatuser}
spring.datasource.password=${DB_PASSWORD:chatpass}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA 설정
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

### 5.2 연결 풀 설정

```properties
# HikariCP 설정
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000
```

---

## 6. 로드 밸런싱 및 스케일링

### 6.1 수평 스케일링

#### Docker Compose 스케일링

```yaml
version: '3.8'

services:
  chatsocket:
    build: .
    deploy:
      replicas: 3
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.chatsocket.rule=Host(`chat.yourdomain.com`)"

  traefik:
    image: traefik:v2.9
    command:
      - --api.dashboard=true
      - --providers.docker=true
      - --entrypoints.web.address=:80
    ports:
      - "80:80"
      - "8080:8080"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
```

### 6.2 WebSocket Session 공유

#### Redis 기반 세션 저장소

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-redis</artifactId>
</dependency>
```

```properties
# Redis 설정
spring.redis.host=${REDIS_HOST:localhost}
spring.redis.port=${REDIS_PORT:6379}
spring.redis.password=${REDIS_PASSWORD:}
spring.session.store-type=redis
```

---

## 7. 모니터링 설정

### 7.1 Spring Boot Actuator

#### pom.xml

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

#### application.properties

```properties
# 액추에이터 엔드포인트
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
management.info.env.enabled=true

# 메트릭 설정
management.metrics.export.prometheus.enabled=true
management.metrics.distribution.percentiles-histogram.http.server.requests=true
```

### 7.2 Prometheus + Grafana

#### docker-compose-monitoring.yml

```yaml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    volumes:
      - grafana_data:/var/lib/grafana
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin

volumes:
  prometheus_data:
  grafana_data:
```

#### prometheus.yml

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'chatsocket'
    static_configs:
      - targets: ['chatsocket:8080']
    metrics_path: '/actuator/prometheus'
```

### 7.3 로그 집계

#### Logback 설정 (logback-spring.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    
    <springProfile name="prod">
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/chatsocket.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>logs/chatsocket.%d{yyyy-MM-dd}.%i.gz</fileNamePattern>
                <maxFileSize>100MB</maxFileSize>
                <maxHistory>30</maxHistory>
                <totalSizeCap>3GB</totalSizeCap>
            </rollingPolicy>
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        
        <root level="INFO">
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>
</configuration>
```

---

## 8. CI/CD 파이프라인

### 8.1 GitHub Actions

#### .github/workflows/deploy.yml

```yaml
name: Deploy to Production

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Cache Maven packages
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
    
    - name: Run tests
      run: ./mvnw clean test
    
    - name: Generate test report
      uses: dorny/test-reporter@v1
      if: success() || failure()
      with:
        name: Maven Tests
        path: target/surefire-reports/*.xml
        reporter: java-junit

  build-and-deploy:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Build with Maven
      run: ./mvnw clean package -DskipTests
    
    - name: Build Docker image
      run: |
        docker build -t chatsocket:${{ github.sha }} .
        docker tag chatsocket:${{ github.sha }} chatsocket:latest
    
    - name: Login to Docker Hub
      uses: docker/login-action@v2
      with:
        username: ${{ secrets.DOCKER_USERNAME }}
        password: ${{ secrets.DOCKER_PASSWORD }}
    
    - name: Push to Docker Hub
      run: |
        docker push chatsocket:${{ github.sha }}
        docker push chatsocket:latest
    
    - name: Deploy to server
      uses: appleboy/ssh-action@v0.1.5
      with:
        host: ${{ secrets.HOST }}
        username: ${{ secrets.USERNAME }}
        key: ${{ secrets.SSH_KEY }}
        script: |
          docker pull chatsocket:latest
          docker-compose down
          docker-compose up -d
```

### 8.2 자동 테스트 및 품질 검사

#### 코드 커버리지

```yaml
    - name: Generate code coverage report
      run: ./mvnw jacoco:report
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        file: target/site/jacoco/jacoco.xml
```

---

## 9. 트러블슈팅

### 9.1 일반적인 문제

#### WebSocket 연결 실패

**증상**: 클라이언트에서 WebSocket 연결이 되지 않음

**해결방법**:
```bash
# 1. 방화벽 확인
sudo ufw status
sudo ufw allow 8080

# 2. 네트워크 설정 확인
netstat -tulpn | grep 8080

# 3. 로그 확인
docker-compose logs chatsocket | grep -i websocket
```

#### 메모리 부족

**증상**: OutOfMemoryError 발생

**해결방법**:
```bash
# JVM 힙 메모리 조정
export JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# Docker 컨테이너 메모리 제한
docker run -m 1g chatsocket:latest
```

#### 데이터베이스 연결 실패

**증상**: 애플리케이션 시작 시 DB 연결 오류

**해결방법**:
```bash
# 1. 연결 정보 확인
echo $DATABASE_URL

# 2. 네트워크 연결 테스트
telnet db-host 5432

# 3. 연결 풀 설정 조정
spring.datasource.hikari.connection-timeout=30000
```

### 9.2 성능 최적화

#### JVM 튜닝

```bash
# G1GC 사용 (권장)
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=16m

# 힙 덤프 생성 (OOM 시)
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/app/dumps/

# GC 로깅
-Xlog:gc*:gc.log:time
```

#### WebSocket 최적화

```properties
# WebSocket 버퍼 크기 조정
spring.websocket.buffer-size=8192

# 연결 타임아웃 설정
spring.websocket.timeout=300000

# 메시지 크기 제한
spring.websocket.max-message-size=65536
```

### 9.3 보안 설정

#### HTTPS 설정

```properties
# SSL 인증서 설정
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${SSL_PASSWORD}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=chatsocket
```

#### 보안 헤더

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true)))
            .build();
    }
}
```

---

## 배포 체크리스트

### 배포 전 확인사항
- [ ] 모든 테스트 통과
- [ ] Docker 이미지 빌드 성공
- [ ] 환경 변수 설정 완료
- [ ] 데이터베이스 연결 확인
- [ ] SSL 인증서 설정 (Production)
- [ ] 모니터링 도구 설정
- [ ] 백업 계획 수립

### 배포 후 확인사항
- [ ] 애플리케이션 정상 실행
- [ ] WebSocket 연결 테스트
- [ ] 로드 밸런서 상태 확인
- [ ] 모니터링 대시보드 확인
- [ ] 로그 수집 정상 동작
- [ ] 알림 설정 테스트

---

## 연락처 및 지원

배포 과정에서 문제가 발생하면 다음을 확인하세요:

1. **로그 확인**: `docker-compose logs -f`
2. **상태 확인**: `curl http://localhost:8080/actuator/health`
3. **리소스 모니터링**: Grafana 대시보드
4. **문서 업데이트**: 이 가이드의 최신 버전 확인

배포 가이드는 정기적으로 업데이트됩니다.