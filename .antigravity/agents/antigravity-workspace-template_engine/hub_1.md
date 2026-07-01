# Antigravity Hub Refresh & Retrieval Module — Knowledge Document

## Overview

This module (`hub_1` group) comprises two complementary systems for the antigravity-workspace-template engine:

1. **`refresh_pipeline.py`** — orchestrates project scanning, LLM-driven knowledge extraction, and artifact generation (conventions, structure, knowledge graphs, module documentation)
2. **`retrieval_graph.py`** — persistence layer for recording and storing retrieval tool invocations as lossless knowledge graphs

Together, they form the refresh workflow (scan → analyze → document) and the retrieval instrumentation layer (tool calls → graph records).

---

## File: `refresh_pipeline.py`

### Purpose
Orchestrates the complete refresh pipeline: scans the project filesystem, invokes multi-agent LLM swarms to analyze codebase structure and conventions, generates module-level knowledge documents, and persists all artifacts to `.antigravity/`. Supports quick (incremental) and failed-only (recovery) refresh modes.

### Key Classes & Functions

#### Core Orchestration
- **`refresh_pipeline(workspace, quick=False, failed_only=False)`** → `RefreshStatus`
  - Main async entry point. Coordinates 8 stages: scan, conventions, structure, knowledge graph, indexes, module docs, git insights, map generation.
  - Returns structured status with stage health and module states.
  - **Stage 1 (Scan):** calls `full_scan()` or `quick_scan()` → writes `scan_report.json`
  - **Stage 2 (Conventions):** invokes `ScanAnalyst → ArchitectureReviewer → ConventionWriter` swarm (3-hop handoff) → `conventions.md`
  - **Stages 3–6 (Full refresh only):** structure.md, knowledge graph artifacts (JSON/Markdown/Mermaid), document/data/media indexes
  - **Stage 7 (Module Docs):** launches per-module group agents (async with semaphores) → `agents/{module}.md` or `agents/{module}/{group}.md`
  - **Stage 8 (Map/Registry):** invokes Map Agent on all agent.md files → `map.md`; builds legacy module_registry for backward compat

#### Workspace Initialization
- **`_ensure_refresh_workspace_initialized(workspace)`** → `Path`
  - Creates `.antigravity/` directory structure and `manifest.json` if missing.
  - Safely checks for path conflicts before creating directories.
  - Returns path to `.antigravity`.

#### Retry & Resilience
- **`_run_with_retry(coro_fn, *args, max_retries=None, base_delay=None, timeout=None, context="", **kwargs)`** → `object`
  - Async wrapper with exponential backoff. Retries on timeout, network, rate-limit, 5xx errors.
  - Non-retryable: bare `TimeoutError` (LLM stalling, not transient failure).
  - Logs attempts to stderr; re-raises last exception if exhausted.

- **`_is_retryable_error(exc)`** → `bool`
  - Classifies exceptions as transient (timeout, connection, 429/502/503/504).

- **`_get_retry_config(max_retries, base_delay)`** → `tuple[int, float]`
  - Reads from `AG_REFRESH_RETRY_COUNT` / `AG_REFRESH_RETRY_DELAY` env vars.

#### State & Health Tracking
- **`_combine_states(current, new)`** → `str`
  - Merges two refresh states; returns the more severe (priority: failed > partial > success > skipped).

- **`_aggregate_states(states, skipped_state="success")`** → `str`
  - Aggregates list of stage/module states into single health status.

- **`_mark_stage_failure(status, stage, reason, partial)`** → `None`
  - Records stage-level failure in `RefreshStatus.failures` list.

- **`_mark_module_failure(status, module, group_name, reason, state)`** → `None`
  - Records module- or group-level failure.

- **`_write_refresh_status(ag_dir, status)`** → `None`
  - Persists `RefreshStatus` to `.antigravity/status.json`.

#### Module Agent Execution
- **`_run_module(entry: tuple[str, list])`** → async coroutine
  - Nested async function launched per module. Runs all group agents in parallel (bounded by `_mod_sem`).
  - For each group, calls `_run_sub()` with API concurrency limit (`_api_sem`).
  - Writes agent.md artifacts; tracks partial/failed groups.

- **`_run_sub(group_name, group, sagent)`** → async coroutine
  - Runs one group agent via `Runner.run(sagent, prompt, max_turns=3, timeout=module_timeout)`.
  - Falls back to `_build_agent_md_fallback()` on failure.
  - Returns tuple `(group_name, md_output, failure_reason)`.

#### Agent Output Processing
- **`_write_agent_md_artifacts(agents_dir, module, group_outputs)`** → `None`
  - Writes single-group modules to `agents/{module}.md`.
  - Writes multi-group modules to `agents/{module}/{sanitized_group}.md` (no merging).

- **`_build_agent_md_fallback(module, group_name, group)`** → `str`
  - Minimal fallback Markdown: file listing with language, line count.
  - Marked with `AGENT_MD_FALLBACK_MARKER` / `AGENT_MD_FALLBACK_SENTINEL` constants.

#### Scan Report Handling
- **`_format_scan_report(report)`** → `str`
  - Formats `ScanReport` into plain-text prompt for conventions agent.
  - Includes languages, frameworks, directories, file types, README, config/entry-point content, git summary.

- **`_build_scan_payload(report)`** → `dict`
  - Extracts JSON-serializable fields for `scan_report.json` traceability.

#### Knowledge Graph & Indexes
- **`_export_normalized_graph_store(ag_dir, graph)`** → `None`
  - Exports knowledge graph nodes/edges to normalized JSONL files (`graph/nodes.jsonl`, `graph/edges.jsonl`).
  - Each line: `{"schema": "antigravity-graph-*-v1", "retrieval_id": ..., "tool_name": ..., "node"/"edge": ...}`.

- **`_build_non_code_indexes(report)`** → `tuple[str, str, str]`
  - Returns Markdown indexes: document_index, data_overview, media_manifest.
  - Groups files by type from `report.file_metadata`.

#### Map & Registry Generation
- **`_generate_map_md(workspace, model)`** → async `str`
  - Reads all `agents/*.md` files; splits into context-sized batches; calls Map Agent per batch.
  - Concatenates outputs (strips duplicate headers).
  - Falls back to `_build_fallback_map_md()` on LLM failure.

- **`_build_fallback_map_md(workspace)`** → `str`
  - Basic map from `detect_modules()` and module paths.
  - Includes brief agent.md summary if available.

- **`_build_module_registry_entries(workspace, status)`** → `list[ModuleRegistryEntry]`
  - Builds lightweight routing entries from module facts artifacts.
  - Extracts keywords from claims, top file paths, module summary.

- **`_extract_registry_keywords(document)`** → `list[str]`
  - Tokenizes module display name, claim types, statements, source file paths.

- **`_build_registry_summary(document)`** → `str`
  - Top 2 high-importance claims joined into summary.

- **`_render_module_registry_markdown(entries)`** → `str`
  - Renders registry as compact Markdown: one line per module with status and tags.

#### Module Facts (Legacy Paths)
- **`_parse_group_facts_output(output, module, group_name)`** → `GroupFactsDocument`
  - Parses LLM JSON output; normalizes claim IDs and dedupes evidence/source files.

- **`_build_group_facts_fallback(module, group_name, group)`** → `GroupFactsDocument`
  - Deterministic fallback: extracts symbol claims from Python/JS files via AST/regex.

- **`_extract_symbol_claims(source_file)`** → `list[ModuleClaim]`
  - Routes to language-specific extractors.

- **`_extract_python_symbol_claims(source_file)`** → `list[ModuleClaim]`
  - AST-based extraction: function/class/import definitions and dependencies.
  - Deprecated (legacy backward compat); new pipeline uses agent.md.

- **`_extract_js_symbol_claims(source_file)`** → `list[ModuleClaim]`
  - Regex-based extraction: export/import statements.

- **`_merge_group_facts(module, group_docs)`** → `ModuleFactsDocument`
  - Merges multiple group facts into single module doc; dedupes claims, aggregates importance.

- **`_write_module_artifacts(modules_dir, document)`** → `None`
  - Persists `module.facts.json` and `module.md`.

- **`_render_module_markdown(document)`** → `str`
  - Human-readable Markdown summary of module facts.

#### Utilities
- **`_sanitize_claim_id(raw_claim_id)`** → `str`
  - Lowercases, replaces non-alphanumeric with `_`, collapses runs, strips boundaries.

- **`_slice_excerpt(lines, start_line, end_line)`** → `str`
  - Extracts bounded source snippet (max 20 lines).

- **`_dedupe_strings(items)`** → `list[str]`
  - Removes duplicates while preserving order.

- **`_dedupe_evidence(evidence)`** → `list[EvidenceSpan]`
  - Deduplicates evidence spans by `(file, start_line, end_line, excerpt)` tuple.

- **`_importance_rank(importance)`** → `int`
  - Returns numeric rank: high=3, medium=2, low=1.

- **`_max_importance(left, right)`** → `str`
  - Returns the higher-ranked importance label.

- **`_module_display_name(module_name)`** → `str`
  - Converts internal ID to human-readable label (e.g., "workspace root" for root, underscores → spaces).

- **`_tokenize_text(text)`** → `list[str]`
  - Extracts 3+ char tokens, filters stop words, lowercases for routing keywords.

- **`_get_head_sha(workspace)`** → `str | None`
  - Runs `git rev-parse HEAD`; returns current commit SHA or None.

- **`_compute_affected_modules(report, module_ids)`** → `set[str] | None`
  - Heuristic: matches changed file paths to module IDs; used in quick-scan filtering.

### Data Flow

1. **Initialization:** `refresh_pipeline()` creates `.antigravity/` via `_ensure_refresh_workspace_initialized()`.
2. **Scan:** `full_scan()` or `quick_scan()` scans filesystem → `ScanReport`.
3. **Scan Payload:** `_build_scan_payload()` serializes report → `scan_report.json`.
4. **Conventions:** `_format_scan_report()` generates prompt → `ScanAnalyst` agent → `conventions.md`.
5. **Structure/Graph (full refresh only):** `extract_structure()`, `build_knowledge_graph()` → Markdown + JSON + Mermaid + normalized JSONL store.
6. **Module Agents:** `detect_modules()` → per-module group agents (async, semaphore-bounded) → agent.md files.
7. **Map Generation:** all agent.md files → `_generate_map_md()` → `map.md`.
8. **Registry:** `_build_module_registry_entries()` → JSON + Markdown registry (legacy compat).
9. **Status Persistence:** `_write_refresh_status()` → `status.json`.

### Dependencies

**External/Internal Modules:**
- `antigravity_engine.hub._constants` — `AGENT_MD_FALLBACK_MARKER`, `SOURCE_CODE_EXTS`, `WORKSPACE_ROOT_MODULE_ID`
- `antigravity_engine.hub.contracts` — `RefreshStatus`, `GroupFactsDocument`, `ModuleFactsDocument`, `ModuleRegistryEntry`, `FailureRecord`, `EvidenceSpan`, `ModuleClaim`
- `antigravity_engine.hub.scanner` — `full_scan()`, `quick_scan()`, `build_knowledge_graph()`, `extract_structure()`, `render_knowledge_graph_markdown()`, `render_knowledge_graph_mermaid()`, `detect_modules()`, `resolve_module_path()`, `list_root_module_files()`
- `antigravity_engine.hub.agents` — `build_refresh_agent()`, `build_refresh_module_swarm_v2()`, `build_refresh_git_agent()`, `build_map_agent()`, `create_model()`
- `antigravity_engine.config` — `get_settings()`
- `agents` — `Runner`, `set_tracing_disabled()`
- stdlib: `asyncio`, `json`, `logging`, `os`, `re`, `subprocess`, `sys`, `datetime`, `pathlib`, `typing`, `ast`, `collections`

**Why:** Contracts define structured data (status, claims, facts). Scanner provides filesystem/AST analysis. Agents encapsulate LLM swarm logic. Config loads model settings.

### Design Patterns

- **Retry Pattern:** `_run_with_retry()` wraps async calls with exponential backoff; distinguishes retryable vs. non-retryable errors (bare timeout is non-retryable).
- **Fallback Pattern:** LLM failures invoke deterministic fallbacks (e.g., `_build_agent_md_fallback()`, `_build_fallback_conventions()`).
- **Semaphore Bounding:** `_mod_sem` (module concurrency) and `_api_sem` (global API concurrency) prevent resource exhaustion.
- **State Aggregation:** `_aggregate_states()` and `_combine_states()` enforce priority-based health merging (failed > partial > success > skipped).
- **Markdown Rendering:** Separate render functions (`_render_module_markdown()`, `render_retrieval_graph_markdown()`) decouple data models from presentation.

### Public API

**Main Entry Point:**
- `refresh_pipeline(workspace, quick=False, failed_only=False)` → `RefreshStatus`

**Environment Variables:**
- `AG_REFRESH_SCAN_ONLY` — skip LLM stages if "1"/"true"/"yes"
- `AG_REFRESH_RETRY_COUNT` — max retries (default 3)
- `AG_REFRESH_RETRY_DELAY` — base delay in seconds (default 1.0)
- `AG_SCAN_TIMEOUT_SECONDS` — scan timeout
- `AG_SCAN_MAX_FILES`, `AG_SCAN_SAMPLE_FILES`, `AG_SCAN_VERBOSE` — scan config
- `AG_REFRESH_AGENT_TIMEOUT_SECONDS` — conventions swarm timeout (default 300s)
- `AG_MODULE_AGENT_TIMEOUT_SECONDS` — module agent timeout (default 300s)
- `AG_REFRESH_CONCURRENCY` — module parallelism (default 8)
- `AG_API_CONCURRENCY` — global API call limit (default 5)
- `AG_MAP_BATCH_CHARS` — max chars per map agent batch (default 30000)
- `AG_MAP_AGENT_TIMEOUT_SECONDS` — map agent timeout (default 300s)

**Artifacts Generated:**
- `.antigravity/conventions.md` — project conventions
- `.antigravity/structure.md` — AST-derived codebase structure
- `.antigravity/knowledge_graph.json` — normalized graph (nodes/edges)
- `.antigravity/knowledge_graph.md`, `.mmd` — graph visualizations
- `.antigravity/document_index.md`, `data_overview.md`, `media_manifest.md` — file indexes
- `.antigravity/agents/{module}.md` — single-group agent output
- `.antigravity/agents/{module}/{group}.md` — multi-group agent outputs
- `.antigravity/map.md` — routing index
- `.antigravity/module_registry.json`, `.md` — legacy module registry
- `.antigravity/status.json` — refresh health status
- `.antigravity/scan_report.json` — scan metadata
- `.antigravity/graph/nodes.jsonl`, `edges.jsonl` — persistent graph store

### Configuration

- **Manifest:** `.antigravity/manifest.json` stores schema version, workspace path, creation timestamp.
- **Status:** `status.json` records per-stage and per-module health, failure reasons.
- **Scan Report:** `scan_report.json` preserves input snapshot for reproducibility.

---

## File: `retrieval_graph.py`

### Purpose
Instrumentation layer that wraps tool invocations to emit lossless retrieval graphs. Records tool inputs, outputs, and intermediate state as knowledge graph nodes/edges. Persists to `.antigravity/retrieval_graphs/` (full/compact modes) and normalized JSONL graph store (`.antigravity/graph/`). Redacts secrets before writing.

### Key Functions

#### Configuration
- **`_get_retrieval_mode()`** → `str`
  - Reads `AG_RETRIEVAL_MODE` env var; returns "off", "compact" (default), or "full".
  - Validates against `_RETRIEVAL_MODE_VALUES`.

#### Secret Redaction
- **`_redact_secrets(value)`** → `str`
  - Applies regex patterns to redact API keys, tokens, passwords, Bearer tokens, SK-* / AIza* patterns.

- **`_redact_jsonable(value)`** → `object`
  - Recursively redacts JSON-serializable structures; redacts dict values with keys matching `_SECRET_KEY_RE`.

- **`_SECRET_PATTERNS`** — tuple of `(regex, replacement)` pairs
  - Matches `API_KEY=...`, `Authorization: Bearer ...`, `sk-*`, `AIza*` patterns.

#### Artifact Management
- **`_trim_file_to_last_lines(path, max_lines)`** → `None`
  - Keeps only most recent N lines in a text file (for log rotation).

- **`_prune_retrieval_artifacts(out_dir, max_retrievals)`** → `None`
  - Removes stale .json/.md/.mmd artifact groups; keeps only latest N groups.

#### JSON Serialization
- **`jsonable(value)`** → `object`
  - Converts arbitrary Python values to JSON-safe equivalents (Path → posix string, custom objects → repr).

#### Rendering
- **`render_retrieval_graph_markdown(graph)`** → `str`
  - Renders retrieval graph as Markdown: schema, retrieval_id, tool_name, created_at, raw input/output (code fences), graph nodes/edges (JSON).

- **`render_retrieval_graph_mermaid(graph)`** → `str`
  - Renders graph as Mermaid flowchart: nodes as boxes, edges as labeled arrows.
  - Sanitizes node IDs (non-alphanumeric → underscore).

#### Graph Recording
- **`record_retrieval_graph(workspace, tool_name, raw_input, raw_output)`** → `None`
  - Main entry point. Constructs lossless retrieval graph record:
    - **Nodes:** project, tool, output, inputs.
    - **Edges:** project→tool (invokes), tool→output (produces), tool→inputs (uses_input).
  - Redacts secrets in input/output.
  - **Mode "full":** writes `.json`, `.md`, `.mmd` to `retrieval_graphs/`; prunes old artifacts.
  - **Mode "compact":** skips full artifacts; calls `_append_knowledge_graph_store()`.
  - **Mode "off":** no-op.

- **`_append_knowledge_graph_store(workspace, graph)`** → `None`
  - Appends nodes/edges to normalized JSONL store (`.antigravity/graph/nodes.jsonl`, `edges.jsonl`).
  - Each record wraps node/edge with schema, retrieval_id, tool_name.
  - Trims store to `AG_GRAPH_STORE_MAX_ROWS` (default 3000).
  - Writes `latest_graph_context.md` with most recent input/output.

#### Tool Wrapping
- **`wrap_retrieval_tools(workspace, tools)`** → `dict[str, Callable]`
  - Wraps each tool callable so every invocation records a retrieval graph.
  - Captures bound arguments as `raw_input_dict`; calls original function; captures return as `raw_output`.
  - On exception, records error state (no re-raise suppression).
  - Preserves original function signature.

### Data Flow

1. **Tool Call:** wrapped tool invoked with arguments.
2. **Binding:** `inspect.signature.bind_partial()` captures arguments.
3. **Execution:** original function runs; result captured or exception caught.
4. **Redaction:** inputs/outputs redacted via `_redact_jsonable()` / `_redact_secrets()`.
5. **Graph Construction:** nodes/edges built (project → tool → output, tool → inputs).
6. **Mode Selection:**
   - **"off":** skip entirely.
   - **"compact":** append JSONL store + write latest_graph_context.md.
   - **"full":** write all artifacts (.json/.md/.mmd), prune, and append store.

### Dependencies

**External/Internal:**
- `antigravity_engine.hub._utils` — `env_int()` (read env var as int with bounds)
- stdlib: `functools`, `inspect`, `json`, `os`, `re`, `datetime`, `pathlib`, `typing`, `uuid`

**Why:** `_utils.env_int()` safely parses integer env vars. `functools.wraps` preserves metadata. `inspect` captures function signatures. `uuid4` generates unique retrieval IDs.

### Design Patterns

- **Wrapper Pattern:** `wrap_retrieval_tools()` decorates callables without modifying source.
- **Redaction Pattern:** Layered secret detection (patterns + key name heuristics) on serialization, not at rest.
- **Graph Normalization:** JSONL schema (schema, retrieval_id, tool_name, node/edge) enables downstream indexing and replay.
- **Append-Only Store:** JSONL files never rewritten; trimming keeps recent records; supports time-series analysis.

### Public API

**Main Entry Points:**
- `record_retrieval_graph(workspace, tool_name, raw_input, raw_output)` — record one tool call
- `wrap_retrieval_tools(workspace, tools)` — wrap dict of tools for automatic recording

**Rendering:**
- `render_retrieval_graph_markdown(graph)` → `str`
- `render_retrieval_graph_mermaid(graph)` → `str`

**Environment Variables:**
- `AG_RETRIEVAL_MODE` — "off", "compact" (default), "full"
- `AG_RETRIEVAL_ARTIFACT_MAX_FILES` — max artifact groups in `retrieval_graphs/` (default 300)
- `AG_GRAPH_STORE_MAX_ROWS` — max rows per JSONL file (default 3000)

**Artifacts Generated:**
- `.antigravity/retrieval_graphs/{tool}_{retrieval_id}.json` — full graph record (mode="full")
- `.antigravity/retrieval_graphs/{tool}_{retrieval_id}.md` — Markdown rendering (mode="full")
- `.antigravity/retrieval_graphs/{tool}_{retrieval_id}.mmd` — Mermaid diagram (mode="full")
- `.antigravity/graph/nodes.jsonl` — persistent normalized node records
- `.antigravity/graph/edges.jsonl` — persistent normalized edge records
- `.antigravity/graph/latest_graph_context.md` — latest retrieval context snapshot

### Configuration

- **Retrieval Mode:** `AG_RETRIEVAL_MODE` ∈ {"off", "compact", "full"} (default "compact")
- **Max Artifacts:** `AG_RETRIEVAL_ARTIFACT_MAX_FILES` (default 300) — old artifact groups pruned
- **Graph Store Size:** `AG_GRAPH_STORE_MAX_ROWS` (default 3000) — JSONL files trimmed to recent records

---

## Integration & Cross-Module Interaction

- **refresh_pipeline → retrieval_graph:** Not directly imported; `retrieval_graph` is used by downstream ask/tool modules.
- **Both → scanner:** `refresh_pipeline` heavily depends on scanner for structural analysis.
- **Both → contracts:** Share data models (RefreshStatus, ModuleClaim, etc.).
- **Both → agents:** `refresh_pipeline` imports agent builders; `retrieval_graph` is transparent to agents.

---

## Summary Table

| Component | Responsibility | Key Output |
|-----------|---|---|
| `refresh_pipeline()` | Orchestrate scan + LLM analysis + module docs | RefreshStatus; .antigravity/* artifacts |
| `_run_with_retry()` | Resilient async execution with backoff | Retried result or final exception |
| `_run_module()` | Launch per-module group agents | agent.md files |
| `_generate_map_md()` | Synthesize module map from agent docs | map.md |
| `record_retrieval_graph()` | Emit tool call as knowledge graph | nodes.jsonl, edges.jsonl, artifacts |
| `wrap_retrieval_tools()` | Auto-instrument tool dict | Wrapped callables with recording |
| `_redact_secrets()` | Scrub credentials before persistence | Safe text for storage |