# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot WebSocket chat application (`chatsocket`) built with Java 21. The project uses Maven as the build system and includes dependencies for Spring Boot Web, WebSocket support, and H2 database.

**Key Dependencies:**
- Spring Boot 3.5.4
- Spring Boot Starter Web
- Spring Boot Starter WebSocket  
- H2 Database (runtime)
- Java 21

## Development Commands

### Build and Run
```bash
# Build the project
./mvnw clean compile

# Run tests
./mvnw test

# Package the application
./mvnw package

# Run the application
./mvnw spring-boot:run
```

### Windows Users
Use `mvnw.cmd` instead of `./mvnw` for all Maven commands.

## Project Structure

```
src/
├── main/
│   ├── java/com/ezlevup/chatsocket/
│   │   └── ChatsocketApplication.java      # Main Spring Boot application class
│   └── resources/
│       ├── application.properties          # Application configuration
│       ├── static/                        # Static web resources (empty)
│       └── templates/                     # View templates (empty)
└── test/
    └── java/com/ezlevup/chatsocket/
        └── ChatsocketApplicationTests.java # Basic context loading test
```

## Architecture Notes

- **Main Application**: `ChatsocketApplication.java` is the standard Spring Boot entry point
- **Configuration**: Minimal configuration in `application.properties` (only application name set)
- **WebSocket Setup**: The project includes WebSocket starter dependency but implementation classes are not yet created
- **Database**: H2 in-memory database is configured for runtime use
- **Static Resources**: Empty `static/` and `templates/` directories suggest frontend resources will be added

## Development Guidelines

- The project follows standard Spring Boot Maven project structure
- WebSocket chat functionality needs to be implemented (configuration classes, message handling, etc.)
- Frontend resources (HTML, CSS, JS) should be placed in `src/main/resources/static/`
- Thymeleaf templates go in `src/main/resources/templates/` if using server-side rendering
- Tests use JUnit 5 and Spring Boot Test framework