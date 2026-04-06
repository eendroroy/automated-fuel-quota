1. **Tool Usage:** For related tasks, use a single tool whenever possible.
2. **File Access:** Read files directly from the filesystem; do not use intermediate tools to load files.
3. **File Creation:** Only create new documentation or migration files if explicitly instructed; otherwise, update existing files as required.
4. **Documentation Updates:** When updating documentation, modify all relevant existing docs to reflect the current project status. Do not create new documentation unless told to.
5. **README Standards:** Ensure the README.md remains professional and follows GitHub conventions; exclude specific technical implementation details.
6. **Database Indexes:** Add appropriate indexes to entities to improve query performance and prevent full table scans.
7. **Build Verification:** Do not check if the application starts; only confirm that the code compiles when compilation is necessary.
8. **Translations:** For all new frontend views, provide translations in both English and Bangla, and keep all existing translations current.
9. **Code Consistency:** Maintain consistent code style, naming conventions, and formatting as established in the existing codebase. Follow project-specific linting or formatting rules if present.
10. **Minimal Changes:** Only make changes necessary to fulfill the specific instructions. Avoid unrelated or speculative modifications.
11. **Performance Optimization:**
    - Write efficient, well-optimized code by minimizing computational complexity and memory/resource usage.
    - Use batch operations or bulk processing for high-throughput or high-volume data tasks; avoid per-item queries or updates wherever possible.
    - Use asynchronous processing, background jobs, or queuing systems for workload distribution when dealing with large data sets or operations likely to cause bottlenecks.
    - Cache frequently accessed data where appropriate to reduce redundant computations or database queries.
    - Profile, measure, and optimize critical code paths for latency and throughput, especially those handling large volumes of data.
    - Paginate, filter, and limit data sets in API responses or queries to avoid out-of-memory conditions or performance degradation.
