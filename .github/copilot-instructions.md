# Project Instruction Set

## 1. Toolset & Supported Technologies

- **Framework:** Use Spring Boot 4.0.5.
- **Language:** Use Java 25.
- **Database:** Use PostgreSQL 15 or newer.
- **Cache:** Use Redis if caching or distributed key-value storage is needed. Employ as needed for performance/scalability, not by default.
- **Scheduler:**
    - Use Quartz (open-source) for advanced or distributed scheduling.
    - Use Spring’s built-in `@Scheduled` for basic, local job scheduling.
- **Logging:**
    - Use SLF4J with Logback for logging.
    - Write logs at appropriate levels (`INFO`, `DEBUG`, `ERROR`).
    - Redact or omit sensitive/confidential data from logs.

## 2. Tooling, CI/CD, & File Management

- Use a single tool for related tasks; avoid switching between multiple tools for similar operations.
- Read files directly from the filesystem; avoid extra tooling or manual copying for file access.
- Only create new documentation or migration files if explicitly instructed; otherwise, update all existing files as needed.
- Do not verify if the application starts unless instructed; only confirm code compiles when compilation is required.

## 3. Documentation Standards

- Update all relevant existing docs to reflect the current project state; do not create new documentation unless explicitly asked.
- Keep README.md professional and aligned with GitHub standards; exclude technical/system implementation details.
- Use Javadoc for documenting Java code.
- Maintain CHANGELOG.md and update it with every release or significant change.
- Ensure API documentation is up-to-date if exposing APIs.

## 4. Code Quality, Security, and Best Practices

- Maintain consistent code style and naming conventions as established in the codebase; adhere to any existing linting/formatting rules.
- Write clear, efficient, and optimized code using established patterns (including SOLID and DRY principles).
- Store all credentials, access keys, and secrets in environment variables (do not hardcode or commit sensitive data).
- Regularly update dependencies and check for security patches.
- Redact sensitive information in logs; ensure compliance with data protection policies.

## 5. Testing & Code Review

- All new code must include unit tests. Add integration tests for code interacting with external services or databases.
- Use JUnit (or current project standard) for automated testing.
- Maintain or exceed existing code coverage percentages.
- Document issues for technical debt, known limitations, or future improvements in FUTURE.md or a similar file.

## 6. Internationalization & Accessibility

- For new frontend views, provide translations in both English and Bangla. Keep all existing translations current and correct.
- Ensure new UI components comply with accessibility best practices (WCAG 2.1 or later).

## 7. Data Handling & Database Optimization

- Always add appropriate indexes to database entities to improve query performance and prevent full table scans.
- For high-throughput or high-volume data:
    - Favor batch operations, bulk inserts/updates, and avoid per-item queries.
    - Use asynchronous processing, background jobs, or message queues (e.g., RabbitMQ, Kafka) as needed.
    - Implement partitioning/sharding for large tables if performance requires.
    - Cache frequently accessed data sensibly (invalidate cache on updates).
    - Paginate and filter large datasets in API responses.

## 8. Logging, Monitoring & Error Handling

- Log at appropriate severity levels; avoid excessive logging of INFO or DEBUG in production.
- Do not expose sensitive data in logs or error messages.
- Ensure centralized log aggregation and search where possible.
- Implement error handling using global exception handlers (e.g., Spring’s `@ControllerAdvice` for REST).

## 9. Performance, Maintainability, and Extensibility

- Profile, measure, and optimize performance of critical or data-intensive code paths after major changes.
- Regularly assess and refactor code for maintainability.
- Follow extensible design (SOLID, modularization), avoiding hardcoding where configuration is preferable.

## 10. Upgrade & Review Policy

- Review and update dependencies, frameworks, and tools at least quarterly or as part of regular release cycles.
- Deprecate outdated practices, dependencies, or libraries responsibly.


**Remember:**  
Follow these guidelines strictly unless the task requirements explicitly specify otherwise. These policies ensure codebase quality, maintainability, scalability, security, and project success.
