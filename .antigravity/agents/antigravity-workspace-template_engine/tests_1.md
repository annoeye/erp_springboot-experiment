# antigravity-workspace-template Engine Tests (tests_1) — Knowledge Document

## Overview

This test group validates core engine functionality across error handling, memory management, sandbox execution, configuration resolution, plugin packaging, retry logic, and skill loading. The tests ensure secure credential redaction, proper LLM error messaging, context window memory management with summarization, microsandbox sandbox isolation, path resolution anchoring, version alignment across plugin manifests, retry classification for transient failures, and dynamic skill discovery and registration.

---

## File: test_mcp_server_errors.py

**Purpose:** Validates secure error formatting and credential redaction in MCP (Model Context Protocol) server responses.

### Key Functions

- `test_redact_secrets_covers_common_token_patterns()` — Verifies `_redact_secrets()` masks OpenAI, Google, Anthropic, custom API keys, Bearer tokens, and sk-* patterns; replaces values with `<redacted>` markers.
- `test_mcp_log_permissions_are_private()` — Confirms diagnostic directory (mode 0o700) and log files (mode 0o600) are not world-readable; secrets are redacted from logs.
- `test_format_tool_error_redacts_secrets_in_response_and_log()` — End-to-end test: `_format_tool_error()` redacts secrets in both user-facing response and `ag-mcp.log`.
- `test_no_llm_error_points_to_setup_and_restart()` — Missing LLM config error includes user-actionable guidance (`/antigravity:ag-setup`, restart instructions).
- `test_generic_tool_error_includes_log_path()` — Generic errors expose diagnostic log location to users.
- `test_ag_mcp_exposes_project_tools()` — Async integration test: MCP stdio server successfully registers `ask_project` and `refresh_project` tools via `StdioServerParameters` and `ClientSession`.

### Dependencies

- `antigravity_engine.hub.mcp_server` — imports `_redact_secrets`, `_log_mcp_event`, `_format_tool_error`
- `mcp` — ClientSession, StdioServerParameters, stdio_client (async MCP client)
- `pytest`, `pathlib.Path`, `sys`

### Data Flow

1. User triggers MCP tool → error occurs
2. `_format_tool_error()` receives tool name and exception
3. `_redact_secrets()` masks credentials in error message
4. `_log_mcp_event()` writes redacted event to `ag-mcp.log` (with restricted perms)
5. User receives redacted response with log path reference

### Design Patterns

- **Defensive secreting:** Multi-layer redaction (response + log) prevents credential leakage.
- **User-actionable errors:** Special case for `ValueError` with "No LLM configured" → guides to `/ag-setup`.

### Public API

- `_redact_secrets(text: str) -> str` — Masks API keys and tokens
- `_log_mcp_event(message: str) -> Path` — Writes to `$CLAUDE_PLUGIN_DATA_DIR/ag-mcp.log`
- `_format_tool_error(tool_name: str, error: Exception) -> str` — Returns user-safe error with log reference

---

## File: test_memory.py

**Purpose:** Tests `MemoryManager` context window construction with optional summarization for token-constrained LLM conversations.

### Key Classes/Functions

- `MemoryManager(memory_file: str, summary_file: str)` — Manages conversation history and summary state
  - `add_entry(role: str, content: str)` — Appends message
  - `get_context_window(system_prompt: str, max_messages: int, summarizer=None) -> list[dict]` — Returns system prompt + optional summary + recent messages
  - `summary: str` — Current summarized content
  - `get_history() -> list[dict]` — Returns all stored messages

### Key Tests

- `test_context_window_without_overflow()` — No summarization needed; window: [system, user msg, assistant msg]
- `test_context_window_with_summary_buffer()` — Excess messages (4 user msgs, max_messages=2): invokes summarizer, returns [system, "Previous Summary: ...", msg2, msg3]
- `test_context_window_skips_empty_summary_message()` — Summarizer returns empty string → skipped from window
- `test_loads_legacy_memory_format()` — JSON format with `metadata` field loads but returns empty history (deprecated format)

### Dependencies

- `pathlib.Path`, `json`
- Internal: `antigravity_engine.memory.MemoryManager`

### Data Flow

1. User adds messages via `add_entry()`
2. `get_context_window()` checks message count vs `max_messages`
3. If overflow, calls `summarizer(old_msgs, prev_summary) -> str`
4. Updates `manager.summary` and returns [system_prompt, optional_summary_msg, recent_messages]

### Design Patterns

- **Message summarization:** Pluggable summarizer function; caches result to avoid re-summarization
- **Legacy format handling:** Gracefully ignores JSON payload with `metadata` field

### Public API

- `MemoryManager.add_entry(role, content)`
- `MemoryManager.get_context_window(system_prompt, max_messages, summarizer=None) -> list[dict]`
- `MemoryManager.get_history() -> list[dict]`
- `MemoryManager.summary: str`

---

## File: test_memory_markdown.py

**Purpose:** Validates Markdown-based memory persistence and retrieval with checkpoint caching and semantic search.

### Key Tests

- `test_markdown_memory_files_written()` — Entries written to `agent_memory.md` with `### Entry N | role=role` headers
- `test_summary_checkpoint_avoids_resummarizing_same_history()` — Summarizer called once per checkpoint (not per window request); `summary_checkpoint` tracks checkpoint index
- `test_markdown_memory_retrieval_context()` — `build_retrieval_context(query: str, limit: int) -> str` searches memory and returns matching entries

### Dependencies

- `antigravity_engine.memory.MemoryManager`

### Key Methods

- `build_retrieval_context(query: str, limit: int) -> str` — Semantic/keyword search in memory file; returns formatted context

### Data Flow

1. `add_entry()` appends to Markdown file with structured headers
2. `get_context_window()` with summarizer updates `summary_checkpoint`
3. Repeated `get_context_window()` calls skip re-summarization if checkpoint unchanged
4. `build_retrieval_context()` searches Markdown content for query terms

### Design Patterns

- **Checkpoint memoization:** Tracks summarization boundary to avoid redundant processing
- **Markdown-native storage:** Human-readable format for debugging; supports grep-based search

---

## File: test_memory_tools.py

**Purpose:** Tests CLI/tool-layer access to memory files via `read_memory_md` and `search_memory_md` functions.

### Key Functions

- `read_memory_md(max_chars: int, memory_file: str) -> str` — Reads up to `max_chars` from memory file
- `search_memory_md(query: str, max_results: int, memory_file: str) -> str` — Searches memory for query; returns empty string if query is empty with error message

### Tests

- `test_read_memory_md()` — Reads and returns memory content
- `test_search_memory_md()` — Query "microsandbox" returns matching lines
- `test_search_memory_md_empty_query()` — Empty query returns error message containing "cannot be empty"

### Dependencies

- `antigravity_engine.tools.memory_tools` — read_memory_md, search_memory_md

### Public API

- `read_memory_md(max_chars, memory_file) -> str`
- `search_memory_md(query, max_results, memory_file) -> str`

---

## File: test_microsandbox_sandbox.py

**Purpose:** Tests `MicrosandboxSandbox` execution isolation with connection handling, timeouts, and output capture.

### Key Class

- `MicrosandboxSandbox()` — Sandbox executor communicating via HTTP JSON-RPC to microsandbox server
  - `execute(code: str, timeout: int) -> SandboxResult` — Runs code; returns result with exit_code, stdout, stderr, meta

### SandboxResult Structure

- `exit_code: int` — 0 (success), 1 (error), -1 (timeout)
- `stdout: str`, `stderr: str` — Captured output
- `meta: dict` — `{"runtime": "microsandbox", "timed_out": bool}`

### Key Tests

- `test_microsandbox_server_unavailable()` — ConnectionError → exit_code=1, stderr includes "msb server start --dev"
- `test_microsandbox_success_execution()` — POST requests: start → execute → stop; captures stdout "Hello from Microsandbox"
- `test_microsandbox_timeout()` — `requests.Timeout` during RPC → exit_code=-1, meta["timed_out"]=True
- `test_microsandbox_stderr_marks_failure()` — Server returns status="error" with stderr output → exit_code=1

### Dependencies

- `requests` — HTTP client (mocked in tests)
- `antigravity_engine.sandbox.microsandbox_exec.MicrosandboxSandbox`
- Env: `SANDBOX_TYPE=microsandbox`, `SANDBOX_MAX_OUTPUT_KB` (e.g., "10")

### Data Flow

1. `MicrosandboxSandbox.execute()` POSTs to microsandbox server at `/api/v1/rpc`
2. RPC calls: "start_session" → "run_code" → "stop_session"
3. Server returns JSON-RPC response with execution result or error
4. Parsed into `SandboxResult` object

### Design Patterns

- **JSON-RPC protocol:** Three-phase execution (start/run/stop) ensures session isolation
- **Timeout handling:** Distinguishes connection timeout (transient) from execution timeout (limit exceeded)
- **Output size limit:** `SANDBOX_MAX_OUTPUT_KB` truncates large outputs

---

## File: test_path_resolution.py

**Purpose:** Validates that relative file paths are resolved relative to `PROJECT_ROOT` setting.

### Key Tests

- `test_memory_manager_default_path_is_anchored_to_project_root()` — `MemoryManager()` with `MEMORY_FILE="nested/agent_memory.md"` resolves to `PROJECT_ROOT/nested/agent_memory.md`
- `test_mcp_config_relative_path_is_anchored_to_project_root()` — `MCPClientManager(config_path="configs/mcp_servers.json")` loads from `PROJECT_ROOT/configs/mcp_servers.json`

### Dependencies

- `antigravity_engine.config` — settings, reset_settings
- `antigravity_engine.memory.MemoryManager`
- `antigravity_engine.mcp_client.MCPClientManager`

### Configuration

- `settings.PROJECT_ROOT` — Base directory for path resolution
- `settings.MEMORY_FILE` — Relative path to memory file

### Public API

- `reset_settings()` — Clears cached settings
- `MCPClientManager._load_server_configs() -> list` — Loads MCP server configs

---

## File: test_plugin_packaging.py

**Purpose:** Ensures version and manifest consistency across plugin targets and enforces plugin architectural constraints.

### Key Constants

- `REPO_ROOT = Path(__file__).resolve().parents[2]` — Repository root (two dirs above tests/)
- `PLUGIN_VERSION = "0.2.1"` — Expected version across all manifests

### Key Tests

- `test_plugin_versions_are_in_sync()` — `.claude-plugin/plugin.json`, `.codex-plugin/plugin.json`, `.claude-plugin/marketplace.json`, `engine/pyproject.toml`, and `engine/antigravity_engine/__init__.py` all declare version "0.2.1"
- `test_plugin_manifests_do_not_auto_register_mcp()` — `.claude-plugin` and `.codex-plugin` manifests lack `mcpServers` key; no `.mcp.json` at repo root (manual registration required)
- `test_optional_mcp_example_passes_workspace_to_ag_mcp()` — Example config `docs/examples/antigravity.mcp.json` specifies `ag-mcp --workspace /path/to/project` with `WORKSPACE_PATH` env var
- `test_slash_commands_run_cli_without_mcp_tools()` — `commands/ag-ask.md` and `commands/ag-refresh.md` declare `allowed-tools: ["Bash"]` (no MCP tool access)
- `test_legacy_unprefixed_mcp_tool_names_do_not_reappear()` — Codebase search excludes legacy tool names `mcp__antigravity__ask_project` and `mcp__antigravity__refresh_project`

### File Locations

- `.claude-plugin/plugin.json` — Claude plugin manifest
- `.codex-plugin/plugin.json` — Codex (VSCode) plugin manifest
- `.claude-plugin/marketplace.json` — Marketplace listing
- `engine/pyproject.toml` — Python package version
- `engine/antigravity_engine/__init__.py` — `__version__`
- `docs/examples/antigravity.mcp.json` — Example MCP config
- `commands/ag-ask.md`, `commands/ag-refresh.md` — Slash command specs

### Design Constraints

- **Version alignment:** Single source of truth prevents deployment mismatches
- **No auto-registration:** MCP servers are opt-in; CLI commands remain default
- **Tool isolation:** Slash commands use only Bash (no plugin MCP tools)
- **Legacy cleanup:** Namespace transition from `mcp__antigravity__*` to `ask_project`/`refresh_project` is complete

---

## File: test_refresh_retry.py

**Purpose:** Classifies exceptions as retryable or non-retryable for the refresh pipeline.

### Key Function

- `_is_retryable_error(error: Exception) -> bool` — Returns True if error should trigger retry; False if fatal

### Classification Rules

- **Non-retryable:** Bare `asyncio.TimeoutError()` or `TimeoutError()` with empty message (local deadline exceeded), `ValueError` with "invalid api key", `RuntimeError` with "bad request" or "malformed prompt"
- **Retryable:** `TimeoutError("504 Gateway Time-out")` (provider timeout with message), `RuntimeError` containing "connection reset", "rate limit", "503 Service Unavailable", "network is unreachable"

### Tests

- `test_bare_wait_for_timeout_is_not_retryable()` — Empty-message timeouts → False (model stalling, not transient)
- `test_messaged_gateway_timeout_is_retryable()` — "504 Gateway Time-out" → True
- `test_transient_provider_errors_are_retryable()` — Network and rate-limit errors → True
- `test_non_transient_errors_are_not_retryable()` — Config/request errors → False

### Dependencies

- `antigravity_engine.hub.refresh_pipeline._is_retryable_error`
- `asyncio`

### Design Pattern

- **Error message inspection:** Distinguishes local timeout (model slow) from provider timeout (transient) by presence of message
- **Transient detector:** Regex-like patterns detect recoverable failures (rate limits, 5xx, network)

### Public API

- `_is_retryable_error(error: Exception) -> bool`

---

## File: test_skills_loader.py

**Purpose:** Tests dynamic skill discovery and registration via `load_skills()` function.

### Key Function

- `load_skills(tools: dict) -> str` — Scans skill directories for `tools.py` and `SKILL.md`; populates `tools` dict with public functions; returns concatenated documentation

### Skill Directory Structure

```
skills/
  my_skill/
    tools.py          # Public functions registered as tools
    SKILL.md          # Optional documentation
  doc_skill/
    SKILL.md          # Only docs, no tools
```

### Key Tests

- `test_empty_directory_returns_empty_string()` — No skills → empty string, empty tools dict
- `test_skill_with_tools_registers_functions()` — `tools.py` with `hello(name: str) -> str` → `tools["hello"]` registered; `_private()` excluded (leading underscore)
- `test_skill_with_doc_returns_content()` — `SKILL.md` content included in returned docs string
- `test_bad_module_does_not_crash()` — Broken `tools.py` logs error but doesn't crash; returns empty tools
- `test_load_skills_caches_per_directory()` — Repeated calls reuse cached results; document changes after first load are not reflected

### Dependencies

- `antigravity_engine.skills.loader.load_skills`
- Module-level cache: `_SKILLS_CACHE` (dict)

### Data Flow

1. `load_skills()` iterates skills directories (siblings of `loader.py`)
2. For each skill:
   - If `tools.py` exists, imports and registers public functions (no leading `_`)
   - If `SKILL.md` exists, appends content to docs string
3. Caches result by directory path
4. Returns concatenated docs; mutates `tools` dict with registered functions

### Design Patterns

- **Dynamic module loading:** Imports `tools.py` at runtime; error handling prevents crashes
- **Caching:** `_SKILLS_CACHE` memoizes by directory to avoid reload overhead
- **Convention-based discovery:** `tools.py` and `SKILL.md` filenames trigger registration

### Public API

- `load_skills(tools: dict) -> str` — Populates tools dict; returns documentation
- `_SKILLS_CACHE` — Module-level cache (dict); users should call `_SKILLS_CACHE.clear()` to reset

---

## Cross-File Dependencies Summary

| Module | Used By |
|--------|---------|
| `antigravity_engine.hub.mcp_server` | test_mcp_server_errors.py |
| `antigravity_engine.memory` | test_memory.py, test_memory_markdown.py, test_path_resolution.py |
| `antigravity_engine.tools.memory_tools` | test_memory_tools.py |
| `antigravity_engine.sandbox.microsandbox_exec` | test_microsandbox_sandbox.py |
| `antigravity_engine.config` | test_path_resolution.py |
| `antigravity_engine.mcp_client` | test_path_resolution.py |
| `antigravity_engine.hub.refresh_pipeline` | test_refresh_retry.py |
| `antigravity_engine.skills.loader` | test_skills_loader.py |

---

## Configuration & Environment Variables

| Variable | Used In | Purpose |
|----------|---------|---------|
| `CLAUDE_PLUGIN_DATA_DIR` | test_mcp_server_errors.py | Diagnostic log directory for MCP events |
| `WORKSPACE_PATH` | test_mcp_server_errors.py, test_plugin_packaging.py | Project workspace root passed to ag-mcp |
| `SANDBOX_TYPE` | test_microsandbox_sandbox.py | Set to "microsandbox" for microsandbox executor |
| `SANDBOX_MAX_OUTPUT_KB` | test_microsandbox_sandbox.py | Output truncation limit (e.g., "10") |
| `PROJECT_ROOT` | test_path_resolution.py | Base directory for relative path resolution |
| `MEMORY_FILE` | test_path_resolution.py | Relative path to memory file (anchored to PROJECT_ROOT) |