# Module Map

## antigravity-workspace-template_cli/main
**Path:** `cli/`
**Description:** Core CLI module for the Antigravity cognitive architecture system, providing commands to scaffold AI context into projects, delegate LLM queries to an engine subprocess, and maintain append-only memory/decision logs under `.antigravity/`. Built with Typer and Rich.
**Key topics:** CLI commands, workspace initialization, template injection, LLM delegation, memory logging, decision logging, ag init, ag ask, ag refresh, antigravity context

## antigravity-workspace-template_cli/tests
**Path:** `cli/tests/`
**Description:** Test suite for the CLI module covering workspace init with force/no-force behavior, report and decision logging, engine hub discovery fallback chain, and context file reading. Uses `typer.testing.CliRunner` and `unittest.mock` with isolated `tmp_path` fixtures.
**Key topics:** CLI testing, init command, force flag, hub discovery, subprocess mocking, reader tests, memory append, decision log, CliRunner, test coverage

## antigravity_engine (core)
**Path:** `antigravity_engine/`
**Description:** Public API entry point and CLI dispatcher for the multi-agent knowledge hub, exposing `refresh_pipeline` and `ask_pipeline` via lazy imports and `ag-ask`, `ag-refresh`, `ag-mcp` CLI commands.
**Key topics:** public API, CLI entry points, pipeline dispatch, lazy imports, ag-ask, ag-refresh, ag-mcp, entry point configuration

## antigravity_engine.config
**Path:** `antigravity_engine/config.py`
**Description:** Pydantic-based settings management that loads from environment variables and `.env` files, resolving paths for LLM, memory, MCP, and artifacts configuration with a lazy singleton proxy.
**Key topics:** configuration, settings, environment variables, .env, LLM backend, OpenAI, paths, MCP config, singleton, pydantic

## antigravity_engine.mcp_client
**Path:** `antigravity_engine/mcp_client.py`
**Description:** Multi-server MCP (Model Context Protocol) client manager that connects to external tool servers via stdio, HTTP, and SSE transports, discovers tools, and exposes them as async or sync callables.
**Key topics:** MCP, Model Context Protocol, tool discovery, stdio transport, HTTP transport, SSE, async, tool callables, external tools, server connections

## antigravity_engine.memory
**Path:** `antigravity_engine/memory.py`
**Description:** Markdown-first conversational memory manager with append-only history, keyword search, checkpoint-based summarization, and context window construction for LLM prompts.
**Key topics:** memory, conversation history, markdown storage, summarization, context window, keyword search, retrieval-augmented context, checkpointing, agent memory

## ask
**Path:** `antigravity-workspace-template_engine/ask/`
**Description:** Question-answering pipeline that routes user questions to relevant modules, retrieves structured knowledge from agent.md files or legacy JSON facts, and synthesizes answers using LLM agents with code inspection tools. Supports provider failover, retry logic, streaming, and parallel module analysis.
**Key topics:** question answering, knowledge routing, agent.md, module facts, LLM agents, code inspection, git analysis, provider failover, retry logic, streaming, parallel analysis, evidence verification, workspace tools

## antigravity-workspace-template_engine/base
**Path:** `antigravity_engine/hub/language_adapters/base.py`, `antigravity_engine/sandbox/`
**Description:** Provides language adapter protocols for extracting semantic structure (symbols, imports, dependencies) from multiple programming languages, and a pluggable sandbox execution layer supporting local subprocess, Microsandbox, and E2B runtimes with configurable resource limits.
**Key topics:** language adapters, semantic analysis, code execution, sandbox, subprocess, Microsandbox, E2B, protocol pattern, SymbolDef, FileSemantics, ExecutionResult, timeout, output truncation

## hub
**Path:** `antigravity-workspace-template_engine/hub/`
**Description:** Multi-agent Knowledge Hub system for maintaining project context. Implements Refresh Swarm (analyzes and documents modules) and Ask Swarm (answers codebase questions using pre-generated knowledge). Uses Merkle trees for change detection, LLM provider failover, semantic indexing, and smart file grouping.
**Key topics:** multi-agent systems, codebase analysis, knowledge graphs, semantic indexing, LLM failover, Merkle trees, MCP server, OpenAI Agent SDK, incremental refresh, module documentation

## antigravity-workspace-template_engine/hub_1
**Path:** `antigravity_engine/hub/`
**Description:** Orchestrates project refresh pipeline (scan, LLM analysis, knowledge extraction, artifact generation) and instruments retrieval tool calls as lossless knowledge graphs. Manages conventions, structure, module documentation, and map generation with retry/fallback resilience.
**Key topics:** refresh pipeline, knowledge graphs, module agents, LLM orchestration, retrieval instrumentation, scan analysis, conventions extraction, artifact generation, secret redaction, graph normalization

## antigravity-workspace-template_engine/hub_2
**Path:** `antigravity_engine/hub/`
**Description:** Core project analysis pipeline providing filesystem scanning, semantic indexing, and structure mapping. Uses pure Python for metadata extraction (languages, frameworks, dependencies, git history) and language adapters for semantic analysis without LLM inference.
**Key topics:** project scanning, module detection, semantic analysis, file tree generation, git insights, language adapters, framework detection, directory traversal, LSP integration, workspace structure

## language_adapters
**Path:** `antigravity-workspace-template/engine/antigravity_engine/hub/language_adapters/`
**Description:** Pluggable semantic analysis system that normalizes code structure (imports, symbols, metadata) across Python, Go, TypeScript/JavaScript into unified `FileSemantics` for dependency tracking and knowledge graphs. Uses AST parsing for Python, regex-based parsing for Go/TS/JS, with graceful fallback for unsupported languages.
**Key topics:** language adapters, semantic analysis, code parsing, AST, FileSemantics, symbol extraction, import resolution, dependency tracking, Python analyzer, Go analyzer, TypeScript analyzer, JavaScript analyzer, module identity, test detection, entrypoint detection

## antigravity-workspace-template_engine/skills
**Path:** `antigravity_engine/skills/`
**Description:** Modular skill framework for agent capabilities including dynamic skill discovery, project scaffolding from templates, semantic graph retrieval, knowledge artifact generation, and sandbox execution abstractions.
**Key topics:** skill discovery, tool registration, agent project initialization, knowledge graph retrieval, BFS traversal, workspace validation, hub pipelines, sandbox execution, template scaffolding, dynamic module loading

## antigravity-workspace-template_engine/tests
**Path:** `antigravity-workspace-template_engine/tests/`
**Description:** Test suite for the antigravity-workspace-template engine, covering CLI dispatch, LLM agent construction, code analysis, graph-based retrieval, sandbox execution, MCP integration, and knowledge graph generation across Python and polyglot (Go, TypeScript, JavaScript) codebases.
**Key topics:** pytest, CLI entrypoint, LLM provider failover, Merkle tree change detection, sandbox factory, module grouping, ask/refresh pipeline, retrieval graph, MCP, knowledge graph

## antigravity-workspace-template_engine/tests_1
**Path:** `antigravity-workspace-template_engine/tests/`
**Description:** Test suite validating core engine functionality including MCP server error handling with credential redaction, LLM memory management with summarization, microsandbox execution isolation, and dynamic skill discovery. Covers configuration path resolution, plugin manifest version alignment, and retry classification for transient vs fatal errors.
**Key topics:** MCP server, credential redaction, memory manager, summarization, microsandbox, sandbox execution, skill loader, retry logic, plugin packaging, path resolution, context window, JSON-RPC

## engine/tools
**Path:** `antigravity-workspace-template/engine/tools/`
**Description:** Modular utility collection for Antigravity agents providing code execution (sandboxed Python), external API integrations (web search, weather, stocks), math evaluation, memory management (markdown search with ripgrep), MCP server integration, and LLM proxies (Ollama, OpenAI-compatible endpoints).
**Key topics:** code execution, sandbox, MCP tools, memory search, ripgrep, Ollama, OpenAI API, AST math evaluation, plugin discovery, web search, stock prices, weather API

## hooks
**Path:** `antigravity-workspace-template/hooks/`
**Description:** Cross-platform AWS Code SessionStart hook that ensures ag-mcp engine installation and PATH availability. Implements idempotent pipx-first/pip-fallback provisioning with version verification across macOS, Linux, and Windows.
**Key topics:** AWS Code hooks, ag-mcp installation, pipx, pip, PATH management, version checking, cross-platform provisioning, SessionStart, pyproject.toml, user-base scripts

## antigravity-workspace-template_scripts
**Path:** `antigravity-workspace-template/scripts/`
**Description:** Utility scripts for repository quality enforcement, developer onboarding, and GitHub project metadata management. Includes CI contract validation, tool demonstration, SVG-to-PNG asset rendering, and gh CLI-based repo configuration.
**Key topics:** repo contract validation, CI/CD checks, version alignment, GitHub metadata, social preview, tool registry demo, GitHub Actions, Python version enforcement, governance assets

## antigravity-workspace-template_skills
**Path:** `antigravity-workspace-template/skills/agent-repo-init/scripts/`
**Description:** CLI tool (`init_project.py`) that scaffolds new projects from the Antigravity template, handling directory copy, `.env` configuration, git initialization, and agent runtime profile documentation generation in quick or full mode.
**Key topics:** project scaffolding, template initialization, environment configuration, MCP setup, swarm workflows, sandbox runtime, git init, agent runtime profile, CLI argparse, full vs quick mode

## Merchandise
**Path:** `src/Merchandise/`
**Description:** Manages the product catalog including attributes/SKUs, categories, product images, products, and shopping carts. Integrates with MinIO for media storage, Redis for cache invalidation, and Elasticsearch for product search.
**Key topics:** product CRUD, attributes, SKU, categories, shopping cart, image upload, MinIO, Elasticsearch, Redis cache, soft delete, product search, pricing, promotions

## OrderManagement
**Path:** `src/OrderManagement/`
**Description:** Handles the full order lifecycle from creation through delivery and returns, enforcing a state machine for status transitions and publishing domain events via the outbox pattern. Integrates with inventory management and supports both COD and online payment flows.
**Key topics:** order creation, order status, state machine, COD, payment callback, shipping, delivery, returns, refund, inventory reservation, outbox events, order search, cart checkout

## ResponseConfig
**Path:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/dto/response/ResponseConfig/`
**Description:** Provides standardized HTTP response envelope classes for a Spring Boot ERP API, including generic wrappers for single and paginated responses with consistent status metadata. Uses Jackson for conditional JSON serialization and Lombok for boilerplate reduction.
**Key topics:** HTTP response wrapper, pagination, ApiStatus, PagingResponse, JSON serialization, REST API response format, Spring Data Page, builder pattern, generic types

## UserService
**Path:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/`
**Description:** Manages the full user account lifecycle including registration, email verification, JWT-based login/logout, password reset via OTP, token refresh with multi-device Redis session tracking, and avatar upload to MinIO storage.
**Key topics:** user registration, authentication, JWT, refresh token, email verification, password reset, OTP, MinIO avatar upload, Redis session, device tracking, Spring Security

## component
**Path:** `src/component/`
**Description:** Cross-cutting infrastructure components handling JWT authentication filtering, Redis Stream-based cache invalidation, Caffeine cache warm-up on startup, Oracle DB privilege initialization, DTO field filtering for public APIs, and default admin user seeding.
**Key topics:** JWT authentication, cache warm-up, cache invalidation, Redis Streams, Oracle privileges, user seeding, Spring Security filter, DTO projection, startup initialization

## config
**Path:** `src/config/`
**Description:** Central Spring Boot configuration module wiring all infrastructure: Spring Security filter chains with JWT and CORS, Caffeine in-memory caching, Kafka topics, MinIO S3 object storage, Redisson distributed locking, Redis template, OpenAPI/Swagger docs, and MVC interceptors.
**Key topics:** Spring Security, JWT config, CORS, Caffeine cache, Kafka topics, MinIO S3, Redisson distributed lock, Redis template, OpenAPI Swagger, multipart upload, authentication manager, BCrypt password encoder

## converter
**Path:** `src/converter/`
**Description:** JPA attribute converters and a Spring type converter that serialize Java collections and embedded objects (AuditEntry, DeviceInfo, MediaItem, OrderStatus, ProductQuantity, Promotion, SpecificationGroup, VariantOption, Set<String>) to/from JSON strings for database persistence, plus HTTP request parameter binding for DeviceInfo.
**Key topics:** JPA AttributeConverter, JSON serialization, ObjectMapper, Jackson, type conversion, embedded objects, collection persistence, Spring ConversionService, JavaTimeModule, database column mapping

## dto
**Path:** `src/dto/`
**Description:** Twenty data transfer objects covering the core ERP business domains—products, orders, payments, addresses, shopping cart, analytics, and audit—used for structured data exchange between controllers, services, and clients in a Spring Boot e-commerce application.
**Key topics:** product catalog, order management, payment, shopping cart, address, audit trail, analytics, Lombok, Jackson serialization, Jakarta validation

## embedded
**Path:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/model/embedded/`
**Description:** Thirteen lightweight JPA `@Embeddable` value objects and DTOs for cross-cutting concerns: audit trails, payment records, device session tracking, SKU generation, product specifications, and promotional data. Composed into parent entities via `@Embedded`.
**Key topics:** embeddable, value object, audit history, soft delete, payment info, device tracking, SKU generation, product specifications, promotions, CLOB serialization

## entity
**Path:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/model/`
**Description:** Twelve core JPA domain entities for an ERP e-commerce system covering users, products, orders, inventory, shopping carts, payments, and transactional outbox events. Implements patterns like optimistic locking, event sourcing, outbox pattern, and Spring Security integration.
**Key topics:** JPA entity, order lifecycle, product catalog, inventory, shopping cart, payment, outbox pattern, Kafka, Spring Security, UserDetails, optimistic locking, aggregate root, RBAC

## enums
**Path:** `src/enums/`
**Description:** 13 enumeration classes defining domain constants for a Spring Boot ERP e-commerce system, covering user states, order/payment lifecycles, RBAC roles, inventory status, and REST query operators.
**Key topics:** order status, payment status, payment methods, role-based access control, user account states, stock inventory, loyalty tiers, search filtering, error codes, caching states

## impl
**Path:** `src/impl/`
**Description:** Three REST controller implementations forming the presentation layer: authentication/profile management, product catalog CRUD (products, categories, attributes, images, MinIO uploads), and order lifecycle management with role-based access.
**Key topics:** authentication, login, registration, token refresh, password reset, product management, category management, attributes, file upload, MinIO, order creation, order status transitions, shipping, admin endpoints, pagination, RBAC, Spring Security

## interfaces
**Path:** `src/interfaces/`
**Description:** Nine service-layer contracts for a Spring Boot ERP application covering authentication, products, inventory attributes, categories, orders, shopping carts, product images, and Redis caching. All use a `Response<T>` wrapper pattern with DTO-based request/response objects.
**Key topics:** service contracts, REST API boundaries, order lifecycle, user authentication, shopping cart, Redis caching, product management, pagination, file upload, Spring interfaces

## mapper
**Path:** `src/mapper/`
**Description:** MapStruct-based compile-time DTO mapping infrastructure with 10 entity-specific mappers, a generic `EntityMapper` base interface, and a shared `DefaultConfigMapper` configuration. Handles bidirectional entity-DTO conversion, partial updates with null-skipping, and nested mapper composition.
**Key topics:** MapStruct, DTO mapping, entity conversion, partial update, null handling, Spring bean generation, OrderMapper, ProductMapper, UserMapper, mapper composition

## repository
**Path:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/repository/`
**Description:** Spring Data JPA repositories for all core ERP entities — users, products, orders, inventory, payments, categories, and events. Implements soft-delete, pessimistic locking, transactional outbox, N+1 prevention via EntityGraph, and denormalized analytics.
**Key topics:** JPA repositories, CRUD, soft delete, pessimistic locking, transactional outbox, pagination, product analytics, order filtering, inventory locking, dynamic queries, JpaSpecificationExecutor, event retry, shopping cart expiration

## src/request
**Path:** `src/request/`
**Description:** 20 Jakarta-validated DTOs standardizing HTTP request payloads for an ERP system, covering product management, order lifecycle, cart operations, authentication, and payment callbacks. Uses Lombok for boilerplate, custom `@NormalizedId` annotation for ID normalization, and a shared `PagingRequest` abstraction for Spring Data pagination.
**Key topics:** request validation, DTOs, order creation, product search, cart, checkout, payment callback, pagination, Jakarta validation, Lombok

## src/request_1
**Path:** `src/request_1/`
**Description:** Request DTOs for ERP operations including order workflows (pickup, process, refund, return, ship), authentication (login, register, token refresh), and product/category management with Jakarta validation.
**Key topics:** order requests, authentication DTOs, product updates, user registration, refresh token, Jakarta validation, variant management, order state transitions, shipping updates, profile updates

## response
**Path:** `src/response`
**Description:** Core response infrastructure for an ERP Spring Boot application, covering authentication/profile DTOs, domain events, Kafka producers and consumers, RFC 7807 error handling, Elasticsearch product search, and Spring Security user details.
**Key topics:** JWT auth response, Kafka event producers, Kafka consumers, Elasticsearch sync, RFC 7807 error handling, Spring Security UserDetails, domain events, email verification, order processing, product search

## rest
**Path:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/web/rest/`
**Description:** HTTP API layer for a Spring Boot ERP application, exposing RESTful endpoints for authentication, product/category/attribute catalog management, and order lifecycle operations. Controllers delegate to service layers and return standardized `Response<T>` wrappers.
**Key topics:** REST endpoints, authentication, JWT token refresh, product CRUD, category management, product attributes, order management, order state machine, delivery PIN, multipart image upload, pagination, Spring Security, role-based access

## service
**Path:** `src/service/`
**Description:** Business logic and infrastructure services for a Spring Boot ERP system, covering inventory management with distributed Redis locking, JWT authentication, order processing with transactional outbox pattern, MinIO file storage, Thymeleaf email notifications, and Redis-backed multi-device session management.
**Key topics:** JWT authentication, Redis caching, distributed locking, inventory reservation, transactional outbox, Kafka event publishing, MinIO file upload, email notifications, token rotation, multi-device sessions, cache synchronization, Redisson

## specification
**Path:** `src/specification/`
**Description:** Reusable fluent query-building framework over JPA Criteria API for dynamic filtering of User, Product, Category, Attributes, and Order entities without raw SQL, using Builder and Strategy patterns.
**Key topics:** JPA Criteria API, dynamic filtering, Specification pattern, query builder, case-insensitive search, LIKE queries, order search, pagination, nested path resolution, Spring Data JPA

## tests
**Path:** `src/tests/`
**Description:** Unit, functional, and controller integration tests for order management, caching, REST endpoints, and exception handling using JUnit 5, Mockito, and Spring WebMvcTest.
**Key topics:** order state machine, Caffeine cache, shopping cart, REST controller testing, WebMvcTest, GlobalExceptionHandler, JUnit 5, Mockito, authentication, merchandise endpoints

## util
**Path:** `src/util/`
**Description:** Cross-cutting utilities for transactional cache eviction, bulk cache loading with Caffeine, client IP extraction from proxied HTTP requests, and Spring Security context access.
**Key topics:** cache invalidation, transaction synchronization, CacheUtils batch loading, IP address extraction, proxy headers, SecurityContext, current user, role checking, CustomUserDetails