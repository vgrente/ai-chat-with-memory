# ai-chat-with-memory

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-ai)
[![Spring Modulith](https://img.shields.io/badge/Spring%20Modulith-2.1-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-modulith)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org)
[![Angular](https://img.shields.io/badge/Angular-22-DD0031?logo=angular&logoColor=white)](https://angular.dev)
[![Gradle](https://img.shields.io/badge/Gradle-02303A?logo=gradle&logoColor=white)](https://gradle.org)

A Kotlin/Spring Boot chat backend on Google Gemini, with two independent verticals: a stateless one-shot chat endpoint and a chat-with-memory endpoint that persists conversation history to Postgres so replies stay in context across turns. Structured as a Spring Modulith app so each feature's controller → service → repository stack is enforced at build time, not just by convention.

## Module boundaries

<!-- module-boundary-map:start -->
```mermaid
flowchart TB
    subgraph chatmemory_group ["Chatmemory"]
        direction TB
        chatmemory["shared types"]
        chatmemory_repository["Repository<br/>ChatMemoryIDRepository"]
        chatmemory_service["Service<br/>ChatMemoryService"]
        chatmemory_controller["Controller<br/>MemoryChatController"]
        chatmemory_repository --> chatmemory
        chatmemory_service --> chatmemory_repository
        chatmemory_service --> chatmemory
        chatmemory_controller --> chatmemory_service
        chatmemory_controller --> chatmemory
    end
    common["Common<br/>GlobalExceptionHandler"]
    subgraph simplechat_group ["Simplechat"]
        direction TB
        simplechat["shared types"]
        simplechat_service["Service<br/>SimpleChatService"]
        simplechat_controller["Controller<br/>ChatController"]
        simplechat_service --> simplechat
        simplechat_controller --> simplechat_service
    end
    chatmemory_repository -.-> common
    simplechat_service -.-> common
    chatmemory_service -.-> common
    simplechat_controller -.-> common
    chatmemory_controller -.-> common
```
<!-- module-boundary-map:end -->
