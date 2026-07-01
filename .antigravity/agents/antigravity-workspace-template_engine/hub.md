# Knowledge Hub — Module Analysis

## Overview

The **hub** group implements a multi-agent Knowledge Hub system for maintaining project context across a codebase. It provides two primary swarms: a **Refresh Swarm** that analyzes and documents modules, and an **Ask Swarm** that answers questions about the codebase using pre-generated knowledge. The system uses content-hash detection (Merkle trees), LLM provider failover, semantic indexing, and smart file grouping to efficiently analyze projects.

---

## File-by-File Analysis

### `__init__.py`
**Purpose:** Package marker declaring the module as a Knowledge Hub for multi-agent system context maintenance.

---

### `__main__.py`
**Purpose:** CLI entry point that delegates to `hub_main()` from `_cli_entry`.
- **Key function:** Re-exports `hub_main()` for command-line invocation.

---

### `_constants.py`
**Purpose:** Centralized constants for directory skipping, language mapping, and file-type classification used across scanner, ask_tools, and pipeline modules.

**Key constants:**
- `SKIP_DIRS` (frozenset): Directories to skip (`.git`, `node_modules`, `__pycache__`, `venv`, etc.)
- `LANG_MAP` (dict): File extension → language name (`.py` → Python, `.ts` → TypeScript, etc.)
- `DOCUMENTATION_EXTS`, `DATA_EXTS`, `MEDIA_EXTS`, `TEXT_EXTS`, `SOURCE_CODE_EXTS` (frozensets): File type classification
- `FRAMEWORK_MARKERS` (dict): Framework/tool detection markers (pyproject.toml, package.json, Cargo.toml, etc.)
- `WORKSPACE_ROOT_MODULE_ID`: "__workspace_root__" identifier for repo-root source files
- `AGENT_MD_FALLBACK_MARKER`, `AGENT_MD_FALLBACK_SENTINEL`: Markers for degraded knowledge documents when LLM analysis fails

---

### `_merkle.py`
**Purpose:** Content-hash change detection using Merkle-style trees for incremental refresh without trusting git state.

**Key classes:**
- `ModuleNode(frozen dataclass)`: Hash + per-file hashes for one module
- `MerkleTree(frozen dataclass)`: Root hash + module hashes covering entire workspace
- `MerkleDiff(frozen dataclass)`: Added/modified/removed modules between two trees
  - Property: `changed_modules` — modules requiring knowledge regeneration
  - Property: `is_empty` — no changes detected

**Key functions:**
- `compute_content_hash(content: str | bytes) → str`: SHA-256 hex digest
- `build_tree(module_file_hashes) → MerkleTree`: Deterministically roll up per-file hashes to module and root hashes
- `build_workspace_tree(workspace) → MerkleTree`: Build tree using same module detection as refresh pipeline
- `diff_trees(previous, current) → MerkleDiff`: Compare trees, report changes
- `save_snapshot(tree, path)` / `load_snapshot(path) → MerkleTree | None`: Persist/load snapshots to `.antigravity/merkle.json`

**Constants:**
- `SNAPSHOT_VERSION = 1`: Schema version for backward compatibility
- `SNAPSHOT_FILENAME = "merkle.json"`
- `_SEP = "\0"`: NUL separator for hashed lines (prevents path forgery)

**Data flow:** Module files → per-file hashes → module hashes → root hash. Snapshots enable detecting exactly which modules changed for incremental refresh.

---

### `_providers.py`
**Purpose:** Multi-provider LLM failover for sustained outage handling.

**Key functions:**
- `is_retryable_provider_error(exc) → bool`: Classifies exceptions as transient provider failures (timeout, 503, 429, etc.)
- `get_provider_chain(settings) → list[ProviderConfig]`: Builds ordered provider list from primary + `AG_LLM_FALLBACKS` env var (JSON array)
- `activate_provider(provider)`: Set `OPENAI_*` env vars and reset cached settings for next agent call
- `run_with_provider_failover(operation, *, providers, is_retryable, label) → T`: Retry operation against each provider until success; non-retryable errors raise immediately

**Key class:**
- `ProviderConfig(frozen dataclass)`: model, base_url, api_key, label for one LLM endpoint

**Configuration:**
- `OPENAI_BASE_URL`, `OPENAI_API_KEY`, `OPENAI_MODEL`: Primary provider (standard OpenAI or custom endpoint)
- `AG_LLM_FALLBACKS`: JSON array of backup provider configs

**Data flow:** On sustained provider failure, failover wrapper detects transient error, activates next provider, re-runs operation.

---

### `_utils.py`
**Purpose:** Shared utility helpers for environment variables, path safety, and directory skipping.

**Key functions:**
- `env_int(name, default, *, minimum) → int`: Parse integer env var with fallback and lower bound
- `env_float(name, default, *, minimum) → float`: Parse float env var with fallback and lower bound
- `is_safe_path(workspace, target) → bool`: Validate target resolves under workspace (no traversal attacks)
- `should_skip_dir(name) → bool`: Check if directory name is in `SKIP_DIRS` or ends with `.egg-info`

---

### `agents.py`
**Purpose:** OpenAI Agent SDK builders for Refresh Swarm (3-agent analysis chain) and Ask Swarm (dynamic module-based router-worker pattern).

**Key functions:**

#### Model Resolution
- `create_model(settings) → str`: Resolve LLM model identifier
  - Custom endpoint → `litellm/openai/{model}`
  - Standard OpenAI → `{model}`
  - Raises ValueError if unconfigured
- `_get_model_settings_kwargs() → dict`: Extract `AG_REASONING_EFFORT` for o1/o3 models as `ModelSettings` extra_body

#### Refresh Swarm
- `build_refresh_swarm(model) → Agent`: 3-agent chain: ScanAnalyst → ArchitectureReviewer → ConventionWriter
  - Flow: code analysis → structural review → conventions doc (< 300 words)

#### Refresh Module Swarm (v1 & v2)
- `build_refresh_module_swarm(model, workspace) → list`: Agents explore modules via tools (search_code, read_file, git_file_history) and write docs
- `build_refresh_module_swarm_v2(model, workspace, modules_filter) → list`: Smart functional grouping with pre-loaded file context
  - Uses `module_grouping.group_files()` to partition code into ~30K token groups
  - Pre-loads file contents into agent instructions (no tool calls needed)
  - Returns `[(module_name, [(group_name, FileGroup, Agent), ...]), ...]`

#### Refresh Git Agent
- `build_refresh_git_agent(model, workspace) → Agent`: Analyzes git history, produces `_git_insights.md`

#### Map Agent
- `build_map_agent(model) → Agent`: Generates `map.md` routing index from `agent.md` documents
  - Format: module name, path, 1-2 sentence description, 5-10 keywords

#### Ask Swarm
- `build_ask_swarm(model, workspace, mcp_tools) → Agent`: Dynamic router-worker topology
  - Router pre-loaded with map.md or module_registry.md
  - ModuleAgent per detected module (pre-loaded with knowledge document)
  - GitAgent for history questions
  - Module_full_project fallback
  - Star topology: workers hand off back to Router only
  - MCP tools injected into all workers

#### Helper Functions
- `_import_agent()`: Import Agent class with helpful error message
- `_wrap_tools(tool_dict) → list`: Wrap plain functions with `@function_tool` decorator
- `_detect_areas(workspace) → list[str]`: Detect module identifiers
- `_resolve_module_path(workspace, module_id) → Path`: Resolve module to filesystem
- `_read_module_knowledge(workspace, module_name) → str`: Read pre-generated agent.md (new format: `.antigravity/agents/`) or fallback to legacy `.antigravity/modules/`
- `_read_git_knowledge(workspace) → str`: Read `_git_insights.md`
- `_read_structure_map(workspace) → str`: Read `structure.md`
- `_read_map_md(workspace) → str | None`: Read `map.md` routing index
- `_read_module_registry(workspace) → str | None`: Read `module_registry.md`

**Key agent instruction templates:**
- `_SCAN_ANALYST_INSTRUCTIONS`: Code analysis (languages, frameworks, patterns)
- `_ARCHITECTURE_REVIEWER_INSTRUCTIONS`: Structural review (directory layout, testing, CI/CD, Docker)
- `_CONVENTION_WRITER_INSTRUCTIONS`: Final conventions doc (< 300 words)
- `_ROUTER_INSTRUCTIONS`: Route questions to appropriate agents
- `_MODULE_AGENT_INSTRUCTIONS_TEMPLATE`: Module-specific knowledge + code exploration tools
- `_GIT_AGENT_INSTRUCTIONS`: Git history analysis
- `_REFRESH_MODULE_INSTRUCTIONS_TEMPLATE`: Module self-learning with exploration tools
- `_REFRESH_PRELOADED_INSTRUCTIONS_TEMPLATE`: Pre-loaded file context analysis
- `_REFRESH_GIT_INSTRUCTIONS`: Git insights document generation
- `_MAP_AGENT_INSTRUCTIONS`: Generate routing index from agent.md files
- `_MCP_TOOLS_ADDENDUM`: Describe available MCP tools to agents

**Backward-compatible aliases:**
- `build_refresh_agent(model)` → `build_refresh_swarm(model)`
- `build_reviewer_agent(model, workspace, mcp_tools)` → `build_ask_swarm(model, workspace, mcp_tools)`

---

### `contracts.py`
**Purpose:** Structured Pydantic contracts for refresh-time module facts and ask-time verification results.

**Key enums:**
- `ClaimImportance`: high, medium, low
- `RefreshState`: success, partial, failed, skipped
- `VerificationState`: verified, partially_verified, unverified

**Key classes:**
- `EvidenceSpan`: file, start_line, end_line, excerpt for source-backed evidence
- `ModuleClaim`: claim_id, claim_type, statement, importance, source_files, evidence
- `GroupFactsDocument`: module, group_name, source_files, claims for one analysis group
- `ModuleFactsDocument`: module, groups, source_files, claims, generated_at for merged module facts
- `ModuleRegistryEntry`: module, keywords, top_paths, status, summary for lightweight routing
- `FailureRecord`: stage, module, group, reason for refresh failures
- `RefreshStatus`: refresh_run_id, generated_at, overall_status, stages, modules, failures
  - Property: `exit_code` → 0 (success), 2 (partial), 1 (hard failure)
- `WorkerEvidence`: module, draft_answer, claims_used, verification_required
- `ClaimVerification`: claim_id, state, notes, evidence
- `VerificationResult`: question, module, claims, verification_required

**Helper functions:**
- `utc_now_iso() → str`: Current UTC timestamp in ISO-8601 format

**Data flow:** Refresh pipeline generates ModuleFactsDocument and RefreshStatus. Ask pipeline consumes claims for answer synthesis and verification.

---

### `knowledge_graph.py`
**Purpose:** Build knowledge graph from scan metadata and semantic index; render as Markdown or Mermaid.

**Key functions:**
- `build_knowledge_graph(root, report) → dict`: Build workspace nodes/edges from:
  - Languages, frameworks, directories from scan report
  - Semantic edges (imports, package declarations, symbols, tests) from adapters
  - Returns JSON-serializable dict with schema, summary, nodes, edges
- `_extract_semantic_edges(root, report, existing_file_ids) → dict`: Adapter-driven semantic extraction (max 300 files)
- `_semantic_index_to_graph(semantic_index) → dict`: Convert SemanticIndex to nodes/edges
  - Node types: workspace, language, framework, directory, file, module, symbol
  - Edge types: uses_language, uses_framework, contains, imports, declares_package, defines, entrypoint, tests
- `render_knowledge_graph_markdown(graph) → str`: Markdown summary with summary JSON, sample nodes, sample edges
- `render_knowledge_graph_mermaid(graph) → str`: Mermaid graph definition (capped at 200 nodes/edges)

**Helper functions:**
- `_increment_edge_count(edge_counts, edge_type)`: Increment edge-type counter
- `_ensure_module_node(*, nodes, seen_modules, module_name, language) → str`: Create module node once
- `_ensure_symbol_node(*, nodes, seen_symbols, rel_path, symbol) → str`: Create symbol node once

**Data flow:** ScanReport + semantic index → knowledge graph (nodes/edges) → rendered Markdown/Mermaid for context/visualization.

---

### `mcp_server.py`
**Purpose:** MCP (Model Context Protocol) server exposing `ask_project` and `refresh_project` tools for AWS Code and MCP-compatible IDEs.

**Key functions:**
- `_redact_secrets(value) → str`: Redact API keys, Bearer tokens, sk-* keys, AIza* keys using regex patterns
- `_package_version() → str`: Return installed package version
- `_mcp_log_path() → Path`: User-visible log path (`.claude/plugins/data/antigravity-antigravity/ag-mcp.log`)
- `_log_mcp_event(message)`: Append diagnostic event (redacted) to log file
- `_format_tool_error(tool_name, exc) → str`: User-actionable error message with log path
- `_resolve_workspace(workspace) → Path`: Resolve from arg → WORKSPACE_PATH env → upward scan (.env/.git) → cwd
- `_root_uri_to_path(uri) → Path | None`: Convert MCP file:// root URI to Path
- `_maybe_upgrade_via_roots(ctx)`: Check MCP roots protocol for client-reported project root (idempotent)
- `serve(workspace)`: Start MCP server on stdio with `ask_project` and `refresh_project` tools
- `main()`: CLI entry point; parse --workspace/--version, resolve workspace, call `serve()`

**MCP tools:**
- `ask_project(question)`: Answer codebase question; calls `ask_pipeline()`
- `refresh_project(quick)`: Rebuild knowledge base; calls `refresh_pipeline()`

**Configuration:**
- `WORKSPACE_PATH`: Environment variable for workspace path
- `.claude/plugins/data/antigravity-antigravity/ag-mcp.log`: Diagnostic log

**Data flow:** MCP client (AWS Code, Cursor) → stdio transport → FastMCP server → ask/refresh pipelines → project knowledge.

---

### `module_grouping.py`
**Purpose:** Smart functional file grouping for RefreshModuleAgents using semantic imports, directory co-location, and filename prefixes.

**Key classes:**
- `SourceFile(dataclass)`: rel_path, abs_path, content, language, raw_tokens, category, effective_tokens, prefix, package_identity, imports_modules, signature_summary, semantics
- `FileGroup(dataclass)`: name, files, total_tokens, total_effective_tokens

**Key functions:**

#### Load & Classify
- `load_module_files(module_path, workspace) → list[SourceFile]`: Read all source files; classify by category (test/glue/config/interface/implementation)
- `_is_artifact(fpath) → bool`: Skip build artifacts (node_modules, dist, .egg-info, minified .js, etc.)
- `_classify_file(fpath, content, semantics) → str`: Categorize file (test/glue/config/interface/implementation)
- `_extract_prefix(stem) → str`: Extract filename prefix (community_providers → community)

#### Dependency Graph
- `build_file_dependency_graph(files, workspace) → dict[str, set[str]]`: Build undirected file adjacency from package identity and imports; exclude glue files from creating edges

#### Grouping
- `group_files(files, workspace, token_budget) → list[FileGroup]`: Multi-signal grouping:
  1. Import graph connected components
  2. Directory co-location + prefix matching
  3. 30K token soft budget + 20 file limit per group
  4. Min-cut split for oversized groups
  5. Merge tiny groups (< 3K tokens)
  6. Separate test files
  - Returns list of FileGroups ready for sub-agent assignment
- `_find_connected_components(graph, files) → list[list[SourceFile]]`: BFS to find connected components
- `_merge_by_directory_and_prefix(components, token_budget) → list[FileGroup]`: Merge small orphans by dir + prefix
- `_chunk_files(base_name, files, token_budget) → list[FileGroup]`: Split files into budget-sized chunks (respects hard char limit)
- `_split_large_group(group, dep_graph, token_budget) → list[FileGroup]`: Hub-removal min-cut split:
  1. Find most-connected file (hub)
  2. Remove hub, find components
  3. Hub full source → most-connected component
  4. Others get hub signature summary
- `_extract_signatures(source_file) → str`: Return adapter-provided signature summary
- `_merge_tiny_groups(groups, token_budget) → list[FileGroup]`: Merge groups < 3K tokens into neighbors

#### Formatting
- `format_group_context(group) → str`: Format FileGroup's files into agent context string with headers and content

**Constants:**
- `DEFAULT_TOKEN_BUDGET = 30_000`: Target tokens per sub-agent
- `MAX_FILES_PER_GROUP = 20`: Hard limit on files per group
- `SOURCE_EXTENSIONS`: Source code file extensions
- `CATEGORY_WEIGHTS`: {test: 0.3, glue: 0.5, config: 0.5, interface: 1.0, implementation: 1.0} — effective token multipliers
- `_MERGE_THRESHOLD = 3000`: Groups below this get merged
- `_MAX_FILE_TOKENS = 50_000`: Discard oversized files (likely bundled/generated)
- `_MAX_RAW_CHARS_PER_GROUP`: Hard char limit (env var `AG_MAX_GROUP_CHARS`, default 800K)

**Data flow:** Module files → classify by category + semantics → build dependency graph → multi-signal grouping → tokenization & chunking → FileGroups pre-loaded in agent instructions.

---

### `pipeline.py`
**Purpose:** Backward-compatible re-export shim; actual implementations in `refresh_pipeline` and `ask_pipeline` modules.

**Re-exported from `refresh_pipeline`:**
- `refresh_pipeline()`
- `_format_scan_report()`, `_get_head_sha()`, `_build_non_code_indexes()`, `_build_scan_payload()`, `_build_fallback_conventions()`

**Re-exported from `ask_pipeline`:**
- `ask_pipeline()`
- `_read_context_file()`, `_build_ask_context()`, `_is_structure_query()`, `_build_graph_skill_context()`, `_build_retrieval_semantic_answer()`, `_build_timeout_fallback_answer()`, `_iter_python_files()`, `_iter_shell_files()`, `_extract_identifiers()`, `_find_function_defs()`, `_find_call_sites()`, `_find_shell_function_defs()`, `_find_shell_call_sites()`, `_extract_blueprints_from_app()`

---

## Public API

The hub exposes:

1. **Refresh workflow:** `build_refresh_swarm()`, `build_refresh_module_swarm()`, `build_refresh_module_swarm_v2()`, `build_refresh_git_agent()`, `build_map_agent()`
2. **Ask workflow:** `build_ask_swarm()` with dynamic module routing
3. **Change detection:** `build_workspace_tree()`, `diff_trees()` for incremental refresh
4. **LLM failover:** `get_provider_chain()`, `run_with_provider_failover()`
5. **MCP server:** `serve()` for AWS Code / Claude integration
6. **Structured facts:** Pydantic contracts for refresh results and verification

---

## Design Patterns

- **Multi-agent swarms:** Handoff chains (Refresh) and star topology (Ask Router ↔ workers)
- **Merkle tree:** Immutable content-hash accumulation for change detection
- **Provider failover:** Transient error detection + ordered backup provider activation
- **Smart grouping:** Functional partitioning via import graph + heuristics + token budgeting
- **Pre-loading:** File contents embedded in agent instructions (no tool calls for group analysis)
- **Hub removal:** Min-cut graph partitioning for splitting large connected components
- **MCP integration:** Workspace root detection via client roots protocol with fallback

---

## Configuration

**Environment variables:**
- `OPENAI_BASE_URL`, `OPENAI_API_KEY`, `OPENAI_MODEL`: Primary LLM endpoint
- `AG_LLM_FALLBACKS`: JSON array of backup providers
- `AG_REASONING_EFFORT`: Reasoning effort level (low/medium/high) for o1/o3 models
- `WORKSPACE_PATH`: Workspace directory for MCP server
- `AG_MAX_GROUP_CHARS`: Hard character limit per group (default 800K)

**Artifacts:**
- `.antigravity/merkle.json`: Content-hash snapshot for incremental refresh
- `.antigravity/agents/`: Module knowledge documents (new format)
- `.antigravity/modules/`: Legacy module knowledge location
- `.antigravity/structure.md`: Project structure map
- `.antigravity/map.md`: Routing index for Router agent
- `.antigravity/module_registry.md`: Legacy routing registry
- `.claude/plugins/data/antigravity-antigravity/ag-mcp.log`: MCP diagnostics