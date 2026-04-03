# Real Estate Management System (Backend API) 🏢

![Java](https://img.shields.io/badge/Java-21-orange.svg) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.2-brightgreen.svg) ![MySQL](https://img.shields.io/badge/MySQL-Database-blue.svg) ![Architecture](https://img.shields.io/badge/Architecture-3--Tier-yellow.svg)

> A modern, robust RESTful API built with **Spring Boot 3** and **Java 21**, demonstrating solid foundations in Backend Engineering, Clean Architecture, and professional coding standards.

Welcome to the **Real Estate Management System**! This repository serves as a backend application designed to manage building and real estate information efficiently. It showcases my continuous learning journey in Java Backend Development, focusing heavily on writing clean, maintainable, and scalable code.

---

## ✨ Key Features & Highlights

> 💡 **Learning Journey Context**: The current branch reflects the **Foundational Phase** of my learning. I intentionally used tools like Native JDBC and manual `ModelMapper` to deeply understand Java Core and Database interactions before moving to advanced abstractions. I am currently learning and actively studying **JPA, Spring Data JPA, and Spring Security**.

This project was built from the ground up without relying heavily on ORM 'magic' (like Hibernate/JPA) in this initial phase. Instead, it utilizes **Native JDBC** and **Dynamic SQL** to demonstrate a deep, fundamental understanding of how Java interacts with databases under the hood.

- **Advanced Search & Filtering**: Complex dynamic queries supporting multiple search parameters (`Map<String, String>` and collections) to filter buildings by name, rent area, district, and types.
- **RESTful API Principles**: Following standard HTTP methods (`GET`, `POST`, `DELETE`, etc.) for building management endpoints.
- **Robust Error Handling**: Implemented a global exception handling mechanism using `@ControllerAdvice` and Custom Exceptions (e.g., `FieldRequiredException`) to return consistent, user-friendly API error responses.
- **Data Transfer Object (DTO) Pattern**: Strict separation of concerns by using DTOs to transfer data between the client and server, preventing exposure of internal database entities using `ModelMapper`.

## 🏗️ Technical Stack

*   **Language**: Java 21
*   **Framework**: Spring Boot (v3.2.2) 
    *   `spring-boot-starter-web`
*   **Database**: MySQL (`mysql-connector-j`)
*   **Design Patterns**: 
    *   3-Tier Architecture (Controller, Service, Repository layers)
    *   Dependency Injection (IoC)
    *   DTO Pattern
*   **Utilities**: ModelMapper for Object Mapping

## 📁 Project Structure (Clean Code)

The codebase is organized based on functional layers to ensure separation of concerns and maintainability.

```text
src/main/java/com/javaweb/
├── api/                   # REST Controllers (Endpoints)
├── config/                # Application & Bean Configurations
├── controllerAdvice/      # Global Exception Handling
├── converter/             # Entity <-> DTO Mapping logic
├── customException/       # Custom defined Exception classes
├── model/                 # Data Transfer Objects (DTOs), Requests/Responses
├── repository/            # Data Access Layer (Interfaces)
│   ├── entity/            # Database Entities mapping tables
│   └── impl/              # Native JDBC / Dynamic SQL Implementations
├── service/               # Business Logic Layer
│   └── impl/              # Service implementations
└── utils/                 # Utility, Helper classes (StringUtils, NumberUtils)
```

## 💎 Built for Maintainability (Clean Code & Best Practices)

> 💡 **Core Mindset:** *"Code is read far more often than it is written."*  
> Therefore, I place the highest priority on **readability, code quality, and ease of maintenance** to ensure long-term scalability and easy collaboration.

- **Naming Conventions:** Consistent, self-documenting naming for variables, methods, and classes to minimize the need for redundant comments.


## 🧠 Why Native JDBC First?

To truly master backend concepts, I intentionally built the data layer using **Native JDBC / Dynamic SQL** in phase 1. This hands-on approach allowed me to deeply understand Connection Pools, manual `ResultSet` mapping, and native query optimization before abstracting them away with Hibernate.

## 📌 Learning Roadmap & Next Steps (Actively Studying)

Having built a solid database foundation with JDBC, my current learning focus and next implementations encompass:

- [⏳] **Modern Data Access**: Learning and migrating to **JPA** and **Spring Data JPA** to handle complex entity relationships and caching.
- [⏳] **API Security**: Studying **Spring Security** & **JWT (JSON Web Tokens)** to implement robust authentication and authorization.
- [ ] **Testing**: Adding Unit Tests with **JUnit 5** and **Mockito** (`spring-boot-starter-test`).
- [ ] **Performance**: Implementing Paging and Sorting functionality for large datasets.

---
*Thank you for reviewing my project! Feel free to reach out to me for any feedback or opportunities.*
