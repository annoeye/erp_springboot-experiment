# Skills Module Knowledge Document

## Overview

The **skills** group provides a modular, extensible framework for agent capabilities. It enables dynamic discovery and registration of skill packages (tools and documentation), project initialization from templates, graph-based knowledge retrieval, file system analysis, and research operations. Each skill is self-contained in a subdirectory with `tools.py` (function exports) and optional `SKILL.md` (documentation).

---

## File: `agent_repo_init_core.py`

**Purpose:** Reusable core logic for initializing new agent projects from a template repository. Handles project scaffolding, environment configuration, and git initialization with support for quick and full initialization modes.

### Key Classes

#### `InitMode` (Enum)
- **Purpose:** Specifies initialization verbosity level
- **Values:** `QUICK`, `FULL`

#### `SandboxRuntime` (Enum)
- **Purpose:** Specifies sandbox execution preference for generated projects
- **Values:** `LOCAL`, `MICROSANDBOX`

#### `RepoInitOptions` (BaseModel)
- **Purpose:** Validation and configuration container for initialization parameters
- **Fields:**
  - `project_name: str` — destination directory name (validated against `[A-Za-z0-9._-]+`)
  - `destination_root: str` — parent directory (default: `"."`)
  - `mode: InitMode` — initialization mode (default: `QUICK`)
  - `enable_mcp: bool` — enable MCP in `.env` for full mode (default: `False`)
  - `enable_swarm: bool` — recommend swarm workflow (default: `True`)
  - `sandbox_runtime: SandboxRuntime` — runtime preference (default: `LOCAL`)
  - `init_git: bool` — run `git init` (default: `False`)
- **Validation:** `project_name` via `@field_validator` rejects invalid characters

#### `RepoInitResult` (BaseModel)
- **Purpose:** Structured result of initialization
- **Fields:**
  - `project_name: str`
  - `project_path: str` — absolute path to created project
  - `mode: InitMode`
  - `copied_entries: int` — filesystem entry count
  - `next_steps: List[str]` — recommended post-init commands

### Key Functions

#### `initialize_agent_repo(options, template_root=None) → RepoInitResult`
- **Purpose:** Main entry point; scaffolds project from template
- **Parameters:**
  - `options: RepoInitOptions` — configuration
  - `template_root: Optional[Path]` — template directory override (default: 2 levels up from this file)
- **Process:**
  1. Resolves template and destination paths
  2. Validates destination doesn't exist and is outside template (prevents recursive copies)
  3. Copies tree with ignore patterns (`.git`, `__pycache__`, `venv`, `artifacts`, etc.)
  4. Creates `artifacts/logs` directory
  5. Calls configuration functions based on mode
  6. Optionally initializes git
  7. Returns result with next steps
- **Raises:** `ValueError` if destination invalid; `OSError` if filesystem fails

#### `_configure_env_file(target_path, options) → None`
- **Purpose:** Create/update `.env` file based on initialization options
- **Behavior:**
  - Quick mode: skips configuration
  - Full mode: upserts `MCP_ENABLED`, `SANDBOX_TYPE`, `AGENT_NAME` variables
  - Copies from `.env.example` if `.env` missing

#### `_write_mission_file(target_path, options) → None`
- **Purpose:** Generate `mission.md` describing project runtime profile (full mode only)
- **Content:** Project name, objective, runtime settings

#### `_write_runtime_profile(target_path, options) → None`
- **Purpose:** Create `.context/agent_runtime_profile.md` documenting LLM setup and swarm/sandbox preferences (full mode only)

#### `_write_init_report(target_path, options) → None`
- **Purpose:** Log initialization state to `artifacts/logs/agent_repo_init_report.md` (full mode only)

#### `_upsert_env_var(lines, key, value) → List[str]`
- **Purpose:** Set or append environment variable in `.env` lines
- **Logic:** Skips commented lines; replaces first match or appends if not found
- **Returns:** Updated line list

#### `_init_git_repo(target_path) → None`
- **Purpose:** Run `git init` in destination
- **Raises:** `OSError` on git failure

#### `_is_within(child, parent) → bool`
- **Purpose:** Check if `child` path resolves under `parent`
- **Returns:** `True` if `child.relative_to(parent)` succeeds

#### `_count_entries(path) → int`
- **Purpose:** Count all files and directories recursively
- **Returns:** Count from `path.rglob("*")`

### Data Flow

```
User Request
    ↓
init_agent_repo (wrapper in tools.py)
    ↓
RepoInitOptions (validation)
    ↓
initialize_agent_repo
    ├→ _is_within (validate destination safety)
    ├→ shutil.copytree (copy template with ignore)
    ├→ _configure_env_file
    ├→ _write_mission_file
    ├→ _write_runtime_profile
    ├→ _write_init_report
    ├→ _init_git_repo
    └→ _count_entries (return stats)
    ↓
RepoInitResult (returned as dict)
```

### Dependencies

- `pathlib.Path` — filesystem operations
- `shutil` — tree copy with filters
- `subprocess` — git execution
- `re` — validation patterns
- `pydantic` — data validation

### Configuration

- **Environment:** Template root defaults to `Path(__file__).resolve().parents[2]` (engine root)
- **Constants:** Ignore patterns include `.git`, `venv`, `__pycache__`, `artifacts`, `*.pyc`
- **Generated files:** `.env`, `mission.md`, `.context/agent_runtime_profile.md`, `artifacts/logs/agent_repo_init_report.md`

### Public API

Exported via `agent-repo-init/tools.py` as:
- **`init_agent_repo(...) → dict`** — wrapper that converts options to `RepoInitOptions`, calls `initialize_agent_repo`, and returns `model_dump()` dict

---

## File: `loader.py`

**Purpose:** Dynamically discovers skill packages, loads callable tools from `tools.py`, and aggregates documentation from `SKILL.md` files into a unified context string.

### Key Functions

#### `load_skills(agent_tools) → str`
- **Purpose:** Scan `antigravity_engine/skills/` for skill subdirectories, load tools and docs
- **Parameters:**
  - `agent_tools: dict[str, Callable]` — dictionary to populate with discovered tools (mutated in-place)
- **Returns:** Combined markdown string of all `SKILL.md` contents
- **Process:**
  1. Uses `_SKILLS_CACHE` for caching (keyed by skills directory path)
  2. Iterates directories (skips `_*` and `__pycache__`)
  3. For each skill:
     - Loads `tools.py` via `importlib.util`: extracts public functions (no leading `_`), adds to `discovered_tools`
     - Reads `SKILL.md` if present, appends to `skill_docs`
  4. Updates `agent_tools` dict in-place
  5. Caches result
- **Error Handling:** Catches exceptions silently unless `AG_SKILLS_VERBOSE` enabled; logs with print

#### `_verbose() → bool`
- **Purpose:** Check if diagnostic output should print
- **Logic:** Returns `True` if `AG_SKILLS_VERBOSE` env var in `{"1", "true", "yes"}`

### Data Structures

#### `_SKILLS_CACHE`
- **Type:** `dict[str, tuple[dict[str, Callable], str]]`
- **Purpose:** Memoize discovered tools and documentation per skills directory path
- **Key:** Canonical skills directory path
- **Value:** Tuple of (tools dict, combined docs string)

### Data Flow

```
load_skills(agent_tools)
    ├→ Check _SKILLS_CACHE
    ├→ If cached: update agent_tools, return cached docs
    └→ If not cached:
        ├→ Iterate skills_dir
        └→ For each skill:
            ├→ Load tools.py
            │   └→ importlib.util.spec_from_file_location
            │   └→ Extract public functions
            │   └→ Add to discovered_tools
            ├→ Read SKILL.md
            │   └→ Append to skill_docs
        └→ Cache and return combined docs
```

### Dependencies

- `importlib.util` — dynamic module loading
- `inspect` — introspect function members
- `pathlib.Path` — file discovery

### Configuration

- **Environment:** `AG_SKILLS_VERBOSE` (default: `"0"`) — enable print diagnostics
- **Convention:** Skills are subdirectories of `antigravity_engine/skills/`; `tools.py` is mandatory for tool export; `SKILL.md` is optional for docs

### Public API

- **`load_skills(agent_tools) → str`** — discovers all skills, populates agent_tools dict, returns combined documentation

---

## File: `agent-repo-init/tools.py`

**Purpose:** Thin wrapper exposing the repository initialization core as a callable skill tool.

### Key Function

#### `init_agent_repo(...) → dict`
- **Purpose:** User-facing entry point for project initialization
- **Parameters:**
  - `project_name: str` — destination directory name
  - `destination_root: str = "."` — parent directory
  - `mode: str = "quick"` — `"quick"` or `"full"`
  - `enable_mcp: bool = False`
  - `enable_swarm: bool = True`
  - `sandbox_runtime: str = "local"` — `"local"` or `"microsandbox"`
  - `init_git: bool = False`
- **Returns:** `dict` (result of `RepoInitResult.model_dump()`)
- **Process:**
  1. Constructs `RepoInitOptions` from parameters (converts string enums)
  2. Calls `initialize_agent_repo(options)`
  3. Returns result as dict
- **Raises:** `ValueError` on invalid arguments, `OSError` on filesystem failure

### Dependencies

- `antigravity_engine.skills.agent_repo_init_core` — core implementation

### Public API

Registered as a skill tool via `load_skills()` discovery.

---

## File: `graph-retrieval/tools.py`

**Purpose:** Semantic graph retrieval using knowledge graph nodes/edges, returning LLM-friendly triples and source evidence with multi-hop BFS exploration.

### Key Functions

#### `query_graph(query, max_hops=2, workspace=".") → dict`
- **Purpose:** Search knowledge graph and return relevant semantic subgraph
- **Parameters:**
  - `query: str` — user search query
  - `max_hops: int = 2` — BFS depth for expansion (clamped to 1–4)
  - `workspace: str = "."` — project root (validated via `_resolve_workspace`)
- **Returns:** Dict with keys:
  - `summary: str` — semantic explanation
  - `triples: list[list[str]]` — [subject, predicate, object] tuples (max 120)
  - `evidence: list[dict]` — retrieval metadata `{"retrieval_id", "tool_name"}` (max 80)
  - `nodes: list[dict]` — selected node records (max 200)
  - `edges: list[dict]` — selected edges `{"from", "type", "to"}` (max 200)
- **Process:**
  1. Reads `.antigravity/graph/nodes.jsonl` and `edges.jsonl` (max 2000 rows by env var `AG_GRAPH_QUERY_MAX_ROWS`)
  2. Falls back to `knowledge_graph.json` if graph files absent
  3. Tokenizes query (`_tokens`)
  4. Scores nodes/edges by token overlap
  5. Selects top 40 nodes + top 80 edges as seed
  6. BFS expansion: traverses adjacency up to `max_hops`
  7. Builds triples from edges and node labels
  8. Collects evidence from retrieval_id + tool_name
  9. Returns capped results

#### `_read_knowledge_graph_rows(workspace) → tuple[list, list]`
- **Purpose:** Fallback graph loader from `knowledge_graph.json`
- **Returns:** `(nodes_rows, edges_rows)` in normalized JSONL format
- **Handles:** Transforms flat graph structure to row format with metadata (schema, retrieval_id, tool_name)

#### `_read_jsonl(path, max_rows=None) → list[dict]`
- **Purpose:** Parse JSONL file with optional row limit (reads last N lines)
- **Returns:** List of parsed JSON dicts; skips empty/invalid lines

#### `_tokens(text) → set[str]`
- **Purpose:** Tokenize text for query matching
- **Logic:** Split on non-alphanumeric, lowercase, filter empty; supports CJK characters

#### `_node_text(node) → str` / `_edge_text(edge) → str`
- **Purpose:** Extract searchable text from node/edge records
- **Returns:** Concatenated id, type, label, tool_name fields

#### `_resolve_workspace(workspace=None) → Path`
- **Purpose:** Validate workspace is within trusted root (uses `is_safe_path`)
- **Raises:** `ValueError` if workspace escapes root

#### `_workspace_root() → Path`
- **Purpose:** Resolve trusted workspace from `WORKSPACE_PATH` env or cwd

### Data Flow

```
query_graph(query)
    ├→ _resolve_workspace (validate)
    ├→ _read_jsonl (load nodes/edges JSONL)
    ├→ _read_knowledge_graph_rows (fallback)
    ├→ Tokenize query
    ├→ Score nodes/edges by token overlap
    ├→ Select top 40 nodes + 80 edges as seed
    ├→ BFS expansion via adjacency:
    │   ├→ Build id_to_node map
    │   ├→ Build adjacency (edges indexed by from/to)
    │   └→ BFS up to max_hops
    ├→ Extract triples from visited edges
    ├→ Collect evidence metadata
    └→ Return capped results
```

### Dependencies

- `pathlib.Path` — filesystem
- `json` — JSONL parsing
- `re` — tokenization
- `collections.defaultdict, deque` — graph structures
- `antigravity_engine.hub._utils.is_safe_path` — workspace validation

### Configuration

- **Environment:** `WORKSPACE_PATH` (trusted root), `AG_GRAPH_QUERY_MAX_ROWS` (default 2000, min 100)
- **Paths:** `.antigravity/graph/nodes.jsonl`, `edges.jsonl`, `.antigravity/knowledge_graph.json`

### Public API

- **`query_graph(query, max_hops=2, workspace=".") → dict`** — retrieve semantic subgraph

---

## File: `knowledge-layer/tools.py`

**Purpose:** High-level wrapper around hub pipelines for graph-first project analysis: refresh knowledge artifacts and answer questions using full file context.

### Key Functions

#### `refresh_filesystem(workspace=".", quick=False) → str`
- **Purpose:** Regenerate all knowledge artifacts (graph, index, overviews)
- **Parameters:**
  - `workspace: str = "."` — project root (validated)
  - `quick: bool = False` — incremental refresh from last git checkpoint
- **Returns:** Status summary listing generated artifacts
- **Process:**
  1. Validates workspace via `_resolve_workspace`
  2. Calls `antigravity_engine.hub.pipeline.refresh_pipeline` (async)
  3. Returns paths to:
     - `.antigravity/knowledge_graph.json`
     - `.antigravity/knowledge_graph.md`
     - `.antigravity/document_index.md`
     - `.antigravity/data_overview.md`
     - `.antigravity/media_manifest.md`

#### `ask_filesystem(question, workspace=".") → str`
- **Purpose:** Answer question using graph-first, all-file context
- **Parameters:**
  - `question: str` — natural language query
  - `workspace: str = "."` — project root (validated)
- **Returns:** Grounded answer with source references
- **Process:**
  1. Validates workspace
  2. Calls `antigravity_engine.hub.pipeline.ask_pipeline` (async)

#### `_resolve_workspace(workspace=None) → Path`
- Same as `graph-retrieval/tools.py`

#### `_workspace_root() → Path`
- Same as `graph-retrieval/tools.py`

### Dependencies

- `asyncio` — async execution
- `pathlib.Path` — filesystem
- `antigravity_engine.hub.pipeline` — refresh_pipeline, ask_pipeline
- `antigravity_engine.hub._utils.is_safe_path` — validation

### Configuration

- **Environment:** `WORKSPACE_PATH` (trusted root)

### Public API

- **`refresh_filesystem(workspace=".", quick=False) → str`** — regenerate artifacts
- **`ask_filesystem(question, workspace=".") → str`** — answer using full context

---

## File: `research/tools.py`

**Purpose:** Stub implementation of a deep research tool for demonstration/testing.

### Key Function

#### `deep_research(topic) → str`
- **Purpose:** Mock research analysis tool
- **Parameters:**
  - `topic: str` — subject to research
- **Returns:** Simulated research findings (hardcoded template)
- **Behavior:** Prints status, sleeps 1 second, returns mocked results

### Public API

- **`deep_research(topic) → str`** — mocked research tool

---

## File: `sandbox/__init__.py`

**Purpose:** Module glue layer; re-exports sandbox abstractions and factory.

### Public API

Exports:
- `ExecutionResult` — execution outcome type
- `CodeSandbox` — base abstraction
- `get_sandbox` — factory function
- `LocalSandbox` — local implementation
- `MicrosandboxSandbox` — microsandbox implementation

---

## File: `install.sh`

**Purpose:** Setup script for Linux/macOS environments; installs engine, CLI, and local configuration.

### Key Steps

1. **Prerequisites Check:** Python 3.10+, Git
2. **Virtual Environment:** Create `venv` if missing
3. **Pip Upgrade & Install:** Installs `-e ./cli` and `-e './engine[dev]'`
4. **Local Config:** Generates `.env` (if missing) with placeholders for `OPENAI_*`, `AG_RETRIEVAL_MODE`
5. **Directories:** Creates `artifacts/`, `.antigravity/`
6. **Gitignore:** Appends `.env` to `.gitignore` if needed

### Output

Prints next steps: activate venv, run `/ag-setup`, run `ag-refresh`, run `ag-ask`.

---

## Cross-File Data Flow & Design Patterns

### Loader → Tools Registration

`loader.py:load_skills()` discovers all skill subdirectories, dynamically imports their `tools.py`, and registers public functions. Each skill's tools become available to the agent runtime.

### Workspace Validation Pattern

`graph-retrieval` and `knowledge-layer` both use `_resolve_workspace()` + `is_safe_path()` to prevent path traversal attacks. This pattern enforces that all operations stay within a trusted workspace root.

### Async Hub Pipelines

`knowledge-layer` wraps async hub pipelines (`refresh_pipeline`, `ask_pipeline`) via `asyncio.run()`, abstracting complexity from skill callers.

### Graph Storage Layering

`graph-retrieval` reads `.antigravity/graph/nodes.jsonl` and `edges.jsonl` as primary store; falls back to `.antigravity/knowledge_graph.json` if graph files absent. This supports both new graph-native format and legacy single-file format.

### Skill Module Architecture

- **Skill Directory:** `antigravity_engine/skills/{skill_name}/`
- **Required:** `tools.py` (public callables)
- **Optional:** `SKILL.md` (documentation)
- **Discovery:** Via `load_skills()`; no configuration file needed
- **Tool Export:** Public functions (no leading `_`) in `tools.py` are automatically registered

---

## Summary Table

| File | Type | Purpose |
|------|------|---------|
| `agent_repo_init_core.py` | Implementation | Project scaffolding from template |
| `agent-repo-init/tools.py` | Implementation | Wrapper for repo init skill |
| `loader.py` | Implementation | Dynamic skill discovery & tool registration |
| `graph-retrieval/tools.py` | Implementation | Semantic graph search & retrieval |
| `knowledge-layer/tools.py` | Implementation | Hub pipeline wrappers (refresh/ask) |
| `research/tools.py` | Implementation | Mock research tool |
| `sandbox/__init__.py` | Glue | Sandbox re-exports |
| `graph-retrieval/__init__.py` | Glue | Skill package marker |
| `knowledge-layer/__init__.py` | Glue | Skill package marker |
| `install.sh` | Setup | Linux/macOS installation script |