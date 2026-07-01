# Ask Pipeline Module — Knowledge Document

## Overview

The **ask** group implements the question-answering pipeline for the Antigravity engine. It routes user questions to relevant project modules, retrieves structured knowledge (via `agent.md` files or legacy JSON facts), and synthesizes answers using LLM agents with optional code inspection tools. The pipeline supports two paths: a modern structured artifacts approach (map.md → agent.md routing) and a legacy swarm-based fallback.

---

## File: `ask_pipeline.py`

### Purpose
Core question-answering orchestration. Implements the main `ask_pipeline()` entry point, handles provider failover, retry logic, and routes between structured facts (agent.md) and legacy multi-agent swarm workflows.

### Key Functions

#### `ask_pipeline(workspace: Path, question: str) -> str`
**Lines: ~170–189**  
Main entry point. Accepts a workspace and question, applies provider failover chain (from `_providers` module), delegates to `_ask_pipeline_once()`.
- **Parameters:** `workspace` (project root), `question` (natural language query)
- **Returns:** Answer string
- **Notes:** MCP servers auto-connect when `MCP_ENABLED=true` AND `AG_ALLOW_MCP=true`

#### `_ask_pipeline_once(workspace: Path, question: str) -> str`
**Lines: ~192–207**  
Tries structured artifacts first (if `AG_ASK_FORCE_LEGACY` is not set), falls back to legacy swarm if insufficient.
- **Control flow:** Checks `_structured_artifacts_available()`, calls `_ask_with_structured_facts()`, then `_ask_with_legacy_swarm()`

#### `_ask_with_legacy_swarm(workspace: Path, question: str) -> str`
**Lines: ~210–334**  
Legacy multi-agent workflow. Gathers project context, optionally retrieves code evidence, builds prompt, instantiates reviewer agent, streams/runs agent with retry + timeout handling.
- **Agent pipeline:** Router (optional) → AnswerAgent → Synthesizer (if multi-module)
- **Tools:** MCP tools (conditional), code inspection tools (optional)
- **Timeout:** `AG_ASK_TIMEOUT_SECONDS` (default 240s)

#### `_run_with_optional_stream(agent, prompt, max_turns=50, timeout=None, stream_enabled=False, progress_label=None) -> str`
**Lines: ~65–125**  
Wraps agent execution with optional streaming and exponential-backoff retry. Retries transient errors up to `AG_ASK_RETRY_COUNT` times (default 3) with base delay `AG_ASK_RETRY_DELAY` (default 5s).
- **Streaming:** Consumes `RunResultStreaming` events, prints progress to stderr
- **Event types:** agent_updated, run_item (tool_call, tool_output, message_output), raw_response

#### `_consume_stream_events(stream_result, progress_label=None) -> str`
**Lines: ~128–167**  
Async event consumer for streaming runs. Filters raw_response noise, tracks agent switches, prints tool calls and message previews.

#### `_get_ask_retry_config() -> tuple[int, float]`
**Lines: ~36–50**  
Parses `AG_ASK_RETRY_COUNT` and `AG_ASK_RETRY_DELAY` from environment; returns tuple `(max_retries, base_delay)`.

#### `_is_retryable_ask_error(exc: Exception) -> bool`
**Lines: ~53–61**  
Delegates to `is_retryable_provider_error()` from `_providers` module.

---

### Structured Facts Path (agent.md)

#### `_structured_artifacts_available(workspace: Path) -> bool`
**Lines: ~338–361**  
Checks for new format (map.md + agents/) or legacy format (module_registry.json + modules/*.facts.json).

#### `_ask_with_structured_facts(workspace: Path, question: str) -> str | None`
**Lines: ~364–390**  
Routes to `_ask_with_agent_md()` if map.md exists, else falls back to `_ask_with_legacy_facts()`.

#### `_ask_with_agent_md(workspace: Path, question: str) -> str | None`
**Lines: ~489–701**  
Multi-step routing and synthesis:
1. **Router Agent:** Routes question via map.md → outputs "MODULES: mod1, mod2" format
2. **Module selection:** Parses output, matches to known agents/ directory entries (case-insensitive, substring fallback)
3. **Knowledge loading:** Reads agent.md files for selected modules (up to 3), detects fallback docs
4. **Project context:** Loads conventions.md, document_index.md, etc. via `_load_project_context()`
5. **Single-module path:** One AnswerAgent with code tools (30 max turns)
6. **Multi-module path:** Parallel Reader agents (semaphore: `AG_API_CONCURRENCY`, default 5) → Synthesizer agent
7. **Degradation banner:** Prepends warning if any module knowledge is fallback-only

**Key sub-functions:**
- `_parse_router_output(output: str) -> list[str]` — Extracts "MODULES:" line or falls back to line-by-line parsing
- `_match_to_known_modules(raw_names, known) -> list[str]` — Fuzzy-matches LLM module names to known identifiers (exact, case-insensitive, substring); deduplicates; caps at 3
- `_load_project_context(ag_dir, map_content="", max_chars=15000) -> str` — Loads project-wide docs (conventions, index, map, registry, structure) with budget/source caps
- `_is_fallback_doc(content: str) -> bool` — Checks for `AGENT_MD_FALLBACK_MARKER` or sentinel in content
- `_prepend_degradation_banner(answer, degraded_modules) -> str | None` — Adds warning banner before answer if modules are fallback-only

#### `_ask_with_legacy_facts(workspace: Path, question: str) -> str | None`
**Lines: ~704–780**  
Legacy JSON facts path (backward compatibility):
1. Loads registry entries, refresh status from JSON
2. Selects candidates via `_select_candidate_modules()`
3. Builds worker evidence from each module's facts.json
4. Verifies claims against source files
5. Synthesizes structured answer

**Key sub-functions:**
- `_load_registry_entries(workspace) -> list[ModuleRegistryEntry]`
- `_load_refresh_status(workspace) -> RefreshStatus`
- `_load_module_facts(workspace, module) -> ModuleFactsDocument | None`
- `_select_candidate_modules(question, registry_entries) -> list[ModuleRegistryEntry]` — Scores entries by keyword overlap; returns top 3
- `_question_tokens(question) -> list[str]` — Regex tokenizer (2+ chars, stops: the, and, for, how, what)
- `_score_registry_entry(question_tokens, question_lower, entry) -> int` — Scores by keyword match (3pts), module name match (5pts), path match (4pts), whole-question match (8pts)
- `_build_worker_evidence(question, entry, document, refresh_status) -> WorkerEvidence` — Selects top 3 claims, flags if module state != "success"
- `_select_claims_for_question(question, document) -> list[ModuleClaim]` — Scores claims by token overlap; fallback to high-importance claims if no match
- `_score_claim(question_tokens, claim) -> int` — Token overlap × 4 + importance bonus (high=3, medium=2, low=1)
- `_verify_worker_evidence(workspace, question, document, worker_output) -> VerificationResult` — Inspects first 2 evidence spans per claim; checks file existence and excerpt match; returns verified/partially_verified/unverified state
- `_synthesize_structured_answer(question, entries, documents, worker_outputs, verification_reports) -> str | None` — Composes markdown answer from verified claims; returns None if no claims verified

---

### Context & Evidence Builders

#### `_build_ask_context(workspace: Path, question: str = "") -> str`
**Lines: ~1010–1088**  
Collects project-level docs for legacy swarm context. Sources ordered: structure.md → conventions.md → knowledge_graph.md → rules.md → decisions/log.md → CONTEXT.md → AGENTS.md → document_index.md → data_overview.md → media_manifest.md → memory/*.md. Budget: `AG_ASK_CONTEXT_MAX_CHARS` (default 30000). Keyword-based source boosting: "media"/"image"/"video" → media_manifest, "data"/"csv"/"json" → data_overview, "document"/"doc"/"readme" → document_index.

#### `_is_structure_query(question: str) -> bool`
**Lines: ~1091–1103**  
Detects topology/dependency queries (keywords: 依赖, 关系, 调用, 结构, 拓扑, dependency, relations, calls, graph, topology, structure, impact).

#### `_build_graph_skill_context(workspace: Path, question: str) -> str | None`
**Lines: ~1106–1130**  
Calls `query_graph(question, max_hops=2)` from skills module; truncates to `AG_GRAPH_CONTEXT_MAX_CHARS` (default 8000, min 1000).

#### `_build_retrieval_semantic_answer(workspace: Path, question: str) -> str | None`
**Lines: ~1141–1243**  
Retrieval-first path (used when `AG_ASK_RETRIEVAL_FIRST` ∈ {1, 2}). Extracts identifiers via regex, finds function/shell-function defs, lists call sites. Returns None if no candidates.

**Key sub-functions:**
- `_extract_identifiers(question) -> list[str]` — Regex: `[A-Za-z_][A-Za-z0-9_]{2,}`, deduplicated
- `_find_function_defs(workspace, identifiers) -> list[dict]` — AST parse; scores by file-stem match; returns up to 6 with 20-line snippets
- `_find_call_sites(workspace, func_name, limit=12) -> list[str]` — Regex search in Python files
- `_find_shell_function_defs(workspace, identifiers) -> list[dict]` — Regex `function name() {` or `name() {`; brace-balanced; up to 6
- `_find_shell_call_sites(workspace, func_name, limit=12) -> list[str]` — Regex search in shell files
- `_extract_blueprints_from_app(workspace) -> list[str]` — Regex from `backend/app.py`: `"backend.blueprints.XXX"`
- `_iter_python_files(workspace) -> list[Path]` — Walks workspace, skips SKIP_DIRS + {data, logs}
- `_iter_shell_files(workspace) -> list[Path]` — Same skip rules; checks `.sh` or shebang `#!/usr/bin/env bash`

#### `_build_timeout_fallback_answer(workspace: Path, question: str) -> str`
**Lines: ~1246–1309**  
Returns relevant snippets from knowledge docs when agent times out. Extracts keyword-matching sections from conventions.md, structure.md, knowledge_graph.md via `_extract_relevant_sections()`.

#### `_extract_relevant_sections(text: str, keywords: list[str], max_chars: int = 6000) -> str`
**Lines: ~1312–1333**  
Splits markdown by `^#{1,3}\s` headers, scores sections by keyword overlap, returns top-scoring sections up to budget.

---

### Dependencies & Imports

| Module | Purpose |
|--------|---------|
| `asyncio` | Concurrency (semaphores, gather, wait_for) |
| `json` | Load/parse JSON facts, registry, status, scan_report |
| `os` | Environment variables, file system walks |
| `subprocess` | Git operations (git log, diff, blame) |
| `pathlib.Path` | File path operations |
| `re` | Tokenization, regex search, pattern matching |
| `antigravity_engine.hub._constants` | `AGENT_MD_FALLBACK_MARKER/SENTINEL`, `SKIP_DIRS` |
| `antigravity_engine.hub.contracts` | Pydantic models: `ModuleClaim`, `ModuleFactsDocument`, `ModuleRegistryEntry`, `RefreshStatus`, `VerificationResult`, `WorkerEvidence`, `ClaimVerification` |
| `antigravity_engine.config.get_settings` | Runtime config (MCP_ENABLED, STREAM_ENABLED, etc.) |
| `antigravity_engine.hub._providers` | Provider chain, failover, `is_retryable_provider_error()` |
| `antigravity_engine.hub.agents` | `build_reviewer_agent()`, `create_model()`, `_wrap_tools()` |
| `antigravity_engine.mcp_client.MCPClientManager` | MCP server lifecycle (conditional import) |
| `antigravity_engine.skills.loader.load_skills` | Graph skill loader (conditional) |
| `agents` (OpenAI SDK) | `Agent`, `Runner`, `Runner.run_streamed()` |

---

### Configuration & Environment Variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `AG_ASK_FORCE_LEGACY` | "" | If "1"/"true"/"yes", skip structured artifacts path |
| `AG_ASK_RETRY_COUNT` | "3" | Max retries for transient errors |
| `AG_ASK_RETRY_DELAY` | "5.0" | Base delay (seconds) for exponential backoff |
| `AG_ASK_TIMEOUT_SECONDS` | "240" | Agent execution timeout |
| `AG_ASK_CONTEXT_MAX_CHARS` | "30000" | Budget for project context (legacy swarm) |
| `AG_ASK_PROJECT_CTX_MAX_CHARS` | "15000" | Budget for project-level docs (agent.md) |
| `AG_GRAPH_CONTEXT_MAX_CHARS` | "8000" | Budget for graph skill output |
| `AG_ASK_RETRIEVAL_FIRST` | "1" | Retrieval mode: "1" (feed to LLM), "2" (return directly) |
| `AG_API_CONCURRENCY` | "5" | Semaphore limit for parallel reader agents |
| `AG_ALLOW_MCP` | "" | Enable MCP server auto-connect (requires `MCP_ENABLED=true`) |
| `MCP_ENABLED` | (from settings) | Enable/disable MCP support |

---

## File: `ask_tools.py`

### Purpose
Workspace-bound code inspection tools for ask-pipeline agents. Enables agents to search, read, list, inspect, and analyze source files at query time — converting the system from metadata-parroting to evidence-backed answering.

### Key Functions

#### `create_ask_tools(workspace: Path) -> dict[str, Callable]`
**Lines: ~49–250**  
Factory returns 8 workspace-scoped tools:

| Tool | Signature | Purpose |
|------|-----------|---------|
| `search_code` | `(query: str, file_pattern: str = "*") -> str` | Grep project files; returns up to 50 matching lines |
| `read_file` | `(file_path: str, start_line: int = 1, end_line: int = 100) -> str` | Read numbered source lines (max 200 lines per call) |
| `list_directory` | `(path: str = ".") -> str` | List directory contents with sizes |
| `read_file_metadata` | `(file_path: str) -> str` | Lightweight metadata (size, mime, binary flag, mtime) |
| `search_by_type` | `(file_type: str, limit: int = 50) -> str` | Find files by category: code/documentation/data/media/binary |
| `summarize_directory` | `(path: str = ".") -> str` | Aggregate file counts and sizes by extension |
| `read_binary_stub` | `(file_path: str, preview_bytes: int = 64) -> str` | Hex preview of binary files (max 256 bytes) |
| `git_file_history` | `(file_path: str, limit: int = 10) -> str` | Git log for a file (max 20 commits) |

**Implementation details:**
- All paths validated via `is_safe_path(ws, target)` — rejects path traversal
- Directory skipping via `should_skip_dir(d)` — respects SKIP_DIRS + {data, logs}
- Encoding: utf-8 with error replacement; binary detection: checks for `\x00` in first 2048 bytes
- All tools wrapped via `wrap_retrieval_tools(ws, tools)` — emits retrieval graph artifacts

#### `create_git_tools(workspace: Path) -> dict[str, Callable]`
**Lines: ~253–380**  
Specialized git tools for deeper analysis (GitAgent):

| Tool | Signature | Purpose |
|------|-----------|---------|
| `git_log` | `(limit: int = 20, path: str = "") -> str` | Show recent commits; optionally filtered by path (max 50) |
| `git_diff` | `(commit_hash: str) -> str` | Show commit diff (stat + full, truncated to 3000 chars) |
| `git_blame` | `(file_path: str, start_line: int = 1, end_line: int = 50) -> str` | Blame line range with author/date (max 100 lines) |

#### `create_write_tools(workspace: Path, module_name: str) -> dict[str, Callable]`
**Lines: ~383–410**  
Single tool for RefreshModuleAgent:

| Tool | Signature | Purpose |
|------|-----------|---------|
| `write_module_doc` | `(content: str) -> str` | Write `.antigravity/modules/{module_name}.md` |

#### `create_git_write_tools(workspace: Path) -> dict[str, Callable]`
**Lines: ~413–435**  
Single tool for RefreshGitAgent:

| Tool | Signature | Purpose |
|------|-----------|---------|
| `write_git_doc` | `(content: str) -> str` | Write `.antigravity/modules/_git_insights.md` |

---

### Tool Constants

| Constant | Value | Purpose |
|----------|-------|---------|
| `_MAX_SEARCH_RESULTS` | 50 | Cap for `search_code` matches |
| `_MAX_READ_LINES` | 200 | Cap for `read_file` line span per call |

---

### Dependencies & Imports

| Module | Purpose |
|--------|---------|
| `fnmatch` | Glob pattern matching for file filters |
| `mimetypes` | MIME type guessing |
| `os` | File system walks |
| `subprocess` | Git command execution |
| `pathlib.Path` | Path operations |
| `antigravity_engine.hub._constants` | `SKIP_DIRS` |
| `antigravity_engine.hub._utils` | `is_safe_path()`, `should_skip_dir()` |
| `antigravity_engine.hub.retrieval_graph` | `wrap_retrieval_tools()` |

---

### Public API

**Entry points:**
- `create_ask_tools(workspace)` — Returns dict of 8 code-inspection tools + retrieval graph tracking
- `create_git_tools(workspace)` — Returns dict of 3 git analysis tools + retrieval tracking
- `create_write_tools(workspace, module_name)` — Returns dict with 1 write tool
- `create_git_write_tools(workspace)` — Returns dict with 1 git write tool

All tools are coroutine-safe (subprocess calls are synchronous, but wrapped for agent integration).

---

## Data Flow Diagram

```
User Question
    ↓
ask_pipeline(workspace, question)
    ↓
[Provider Failover Loop]
    ├─ _ask_pipeline_once()
    │   ├─ Structured Path (if AG_ASK_FORCE_LEGACY ≠ true)
    │   │   ├─ _ask_with_structured_facts()
    │   │   │   ├─ _ask_with_agent_md()
    │   │   │   │   ├─ Router Agent (via map.md) → module selection
    │   │   │   │   ├─ Load agent.md for selected modules
    │   │   │   │   ├─ _load_project_context() → project-wide docs
    │   │   │   │   ├─ Single-module: AnswerAgent (with ask_tools)
    │   │   │   │   │   └─ (tool calls: search_code, read_file, etc.)
    │   │   │   │   └─ Multi-module: Parallel Readers → Synthesizer
    │   │   │   │       └─ (each reader uses ask_tools)
    │   │   │   └─ Fallback: _ask_with_legacy_facts()
    │   │   │       ├─ Load module_registry.json, status.json
    │   │   │       ├─ _select_candidate_modules() by keyword score
    │   │   │       ├─ Load *.facts.json documents
    │   │   │       ├─ _verify_worker_evidence() (inspect source files)
    │   │   │       └─ _synthesize_structured_answer()
    │   │
    │   └─ Legacy Path (if structured insufficient or forced)
    │       ├─ _build_ask_context() → project docs
    │       ├─ _build_retrieval_semantic_answer() → code evidence (optional)
    │       ├─ _is_structure_query() + _build_graph_skill_context() (optional)
    │       ├─ build_reviewer_agent() with MCP tools (optional)
    │       ├─ _run_with_optional_stream()
    │       │   ├─ Retry loop (exponential backoff)
    │       │   ├─ Runner.run() or Runner.run_streamed()
    │       │   └─ _consume_stream_events() (if streaming)
    │       └─ Timeout fallback: _build_timeout_fallback_answer()
    │
    └─ [Retry on transient error]
        └─ Next provider (if failover configured)

Final Answer → User
```

---

## Design Patterns

1. **Provider Failover Chain** — Wraps the entire ask pipeline in `run_with_provider_failover()`, enabling seamless provider switching on sustained outages.

2. **Retry with Exponential Backoff** — `_run_with_optional_stream()` implements retry loop with configurable count and base delay; exponential: delay × 2^attempt.

3. **Streaming Events** — Optional real-time progress reporting via `RunResultStreaming`; filters noise (raw_response), tracks agent switches, prints tool activity.

4. **Structured Facts Cascade** — Agent.md (new) → Legacy JSON (old) → Legacy Swarm (oldest), enabling gradual migration without breaking existing workflows.

5. **Parallel Module Analysis** — Multi-module agent.md paths use asyncio.Semaphore to bound concurrent API calls (`AG_API_CONCURRENCY`).

6. **Evidence Verification** — Legacy facts path inspects source files to verify claims (exact excerpt match, partial, or missing).

7. **Keyword-Based Routing** — Question tokens score candidate modules; soft matching handles typos and synonyms.

8. **Budget-Limited Context** — Project context, graph skill output, and retrieval results all respect configurable max-char limits to fit within model context windows.

9. **Fallback Docs Flagging** — Agent.md files marked as auto-generated refresh fallbacks trigger degradation banner so users never mistake a bare file listing for analyzed knowledge.

10. **Tool Binding at Answer Time** — Code inspection tools (search, read, list) are created per-workspace and injected into answer agents, enabling live source verification during reasoning.

---

## Summary

The **ask** group implements a multi-tier question-answering system: structured agent.md routing with project context + optional code tools, fallback to legacy JSON facts with verification, and ultimate fallback to a multi-agent swarm with retrieval and graph skills. Retry logic, provider failover, streaming, and timeouts ensure robustness. Tools are workspace-scoped and security-checked, enabling agents to ground answers in live source code.