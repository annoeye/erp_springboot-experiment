# Antigravity Engine Module Documentation

## Overview

The **antigravity_engine** module is a dynamic multi-agent knowledge hub that provides a refresh pipeline for scanning projects and generating knowledge documents, and an ask pipeline for answering questions through a router and module agent cluster. The module exposes a public API through lazy imports and CLI entry points, with support for MCP (Model Context Protocol) server integration, persistent markdown-based memory management, and configurable LLM backends.

---

## File: `__init__.py`

**Purpose:** Public API entry point and lazy loader for pipeline operations.

**Responsibilities:** Exports version, Settings class, and lazy-loading functions for refresh and ask pipelines to defer imports until needed.

**Key Functions:**
- `refresh_pipeline(*args, **kwargs)` — Lazily imports and delegates to `antigravity_engine.hub.pipeline.refresh_pipeline`
- `ask_pipeline(*args, **kwargs)` — Lazily imports and delegates to `antigravity_engine.hub.pipeline.ask_pipeline`

**Public API Exports:**
- `Settings` — Configuration class (from `config` module)
- `refresh_pipeline` — Refresh the knowledge base
- `ask_pipeline` — Query the multi-agent cluster
- `__version__` — "0.2.1"

**Design Pattern:** Lazy import pattern to minimize startup time and memory overhead.

---

## File: `__main__.py`

**Purpose:** Entry point for `python -m antigravity_engine` execution.

**Responsibilities:** Single-line dispatcher that imports and calls `engine_main()` from `_cli_entry`.

**Key Functions:**
- Imports `engine_main` from `antigravity_engine._cli_entry` and executes it when module is run directly.

---

## File: `_cli_entry.py`

**Purpose:** CLI command dispatcher and argument parsing for three entry points: `ag-ask`, `ag-refresh`, and `ag-mcp`.

**Responsibilities:** 
- Parse CLI arguments for each command
- Set `WORKSPACE_PATH` environment variable
- Route to appropriate pipeline (ask, refresh, or MCP server)
- Handle errors and exit codes

**Key Functions:**

- `_parse_args(parser, argv)` — Parse CLI arguments from explicit argv list or sys.argv
  - Parameters: `parser` (ArgumentParser), `argv` (optional Sequence[str])
  - Returns: Parsed `argparse.Namespace`

- `ask_main(argv=None)` — Entry point for `ag-ask "question"` command
  - Parameters: `argv` (optional Sequence[str])
  - Sets `WORKSPACE_PATH`, calls `ask_pipeline(workspace, question)`, prints result or exits with error

- `refresh_main(argv=None)` — Entry point for `ag-refresh` command
  - Parameters: `argv` (optional Sequence[str])
  - CLI options: `--workspace`, `--quick` (only scan changed files), `--failed-only` (re-run failed modules)
  - Calls `refresh_pipeline(workspace, quick, failed_only)`, checks exit code

- `mcp_main(argv=None)` — Entry point for `ag-mcp` command
  - Parameters: `argv` (optional Sequence[str])
  - Imports and runs MCP server from `antigravity_engine.hub.mcp_server`

- `_dispatch_main(argv, prog)` — Dispatch subcommand-oriented entry point
  - Routes "ask", "refresh", or "mcp" commands to appropriate handlers
  - Used by `engine_main()` and `hub_main()`

- `engine_main(argv=None)` — Entry point for `python -m antigravity_engine`
- `hub_main(argv=None)` — Entry point for `python -m antigravity_engine.hub`

**Data Flow:**
CLI arguments → `_parse_args()` → command dispatch → pipeline or MCP server → result or error exit

---

## File: `config.py`

**Purpose:** Pydantic-based configuration management with environment variable and .env file support.

**Responsibilities:**
- Define application settings (LLM, memory, MCP, artifacts directories)
- Load/override settings from environment variables and .env files
- Provide path resolution utilities for relative/absolute paths
- Manage lazy global settings singleton with reset capability

**Key Classes:**

- `MCPServerConfig(BaseSettings)` — Configuration for a single MCP server
  - Fields: `name` (str), `transport` (str, default "stdio"), `command` (Optional[str]), `args` (List[str]), `url` (Optional[str]), `env` (dict), `enabled` (bool, default True)

- `Settings(BaseSettings)` — Main application configuration
  - Agent: `AGENT_NAME` (default "AntigravityAgent"), `DEBUG_MODE` (default False)
  - Streaming: `STREAM_ENABLED` (default False for LLM responses)
  - Paths: `PROJECT_ROOT` (defaults to WORKSPACE_PATH env var or cwd), `ANTIGRAVITY_DIR` (default ".antigravity"), `ARTIFACTS_DIR` (default "artifacts"), `MEMORY_FILE` (default "memory/agent_memory.md"), `MEMORY_SUMMARY_FILE` (default "memory/agent_summary.md")
  - LLM: `OPENAI_BASE_URL`, `OPENAI_API_KEY`, `OPENAI_MODEL` (default "gpt-4o-mini")
  - MCP: `MCP_ENABLED` (default False), `MCP_SERVERS_CONFIG` (default "mcp_servers.json"), `MCP_CONNECTION_TIMEOUT` (default 30s), `MCP_TOOL_PREFIX` (default "mcp_")
  - Methods:
    - `project_root_path` (property) — Returns PROJECT_ROOT as absolute Path
    - `resolve_path(path_value)` — Resolves relative paths against project root
    - `memory_file_path`, `memory_summary_file_path`, `antigravity_dir_path`, `artifacts_path` (properties) — Return resolved paths

**Module-level Functions:**

- `get_settings()` — Returns global Settings singleton, creating on first call (resolves .env from current WORKSPACE_PATH)
- `reset_settings()` — Clears cached settings singleton (useful for tests)

**Module-level Objects:**

- `settings` (_SettingsProxy) — Transparent proxy allowing `from config import settings` to work transparently with the lazy singleton

**Configuration Loading:**
Environment variables → .env file (from PROJECT_ROOT/.env) → Settings instance → path resolution

**Design Pattern:** Lazy singleton with proxy pattern to support late-binding of PROJECT_ROOT and test environment resets.

---

## File: `mcp_client.py`

**Purpose:** Multi-server MCP (Model Context Protocol) client manager that connects to external MCP servers, discovers tools, and exposes them as callable functions.

**Responsibilities:**
- Load MCP server configurations from JSON
- Establish connections (stdio, HTTP, SSE transports)
- Discover and catalog available tools
- Wrap tools as async/sync callables with metadata
- Manage server lifecycle and error handling

**Key Classes:**

- `MCPTool` (dataclass) — Represents a tool from an MCP server
  - Fields: `name`, `description`, `server_name`, `input_schema` (Dict), `original_name`
  - Methods: `get_prefixed_name(prefix)` — Returns tool name with optional prefix

- `MCPServerConnection` (dataclass) — Active connection to an MCP server
  - Fields: `config` (MCPServerConfig), `session`, `read_stream`, `write_stream`, `tools` (List[MCPTool]), `connected` (bool), `error` (Optional[str]), `_client_cm`

- `MCPClientManager` — Main async manager for all MCP connections
  - Constructor: `__init__(config_path=None)` — Initialize with optional config override
  - Key Methods:
    - `initialize()` — Load configs, connect all servers, discover tools (async)
    - `_load_server_configs()` — Parse JSON config file, return List[MCPServerConfig]
    - `_connect_server(config)` — Connect single server (async, supports stdio/http/sse)
    - `_connect_stdio(connection)` — Establish stdio transport connection (async)
    - `_connect_http(connection)` — Establish HTTP/streamable-http transport (async)
    - `_connect_sse(connection)` — Establish SSE transport (async, delegates to HTTP)
    - `_discover_tools(connection)` — Query connected server for available tools (async)
    - `get_all_tools()` — Return List[MCPTool] from all connected servers
    - `get_all_tools_as_callables()` — Return Dict[str, Callable] with prefixed names
    - `_create_tool_wrapper(connection, tool)` — Generate async callable wrapper for tool (async)
    - `get_tool_descriptions()` — Return formatted string of all tool descriptions
    - `call_tool(tool_name, arguments)` — Call MCP tool by prefixed name, return (success, result) tuple (async)
    - `shutdown()` — Close all connections gracefully (async)
    - `get_status()` — Return Dict with connection and tool counts

- `MCPClientManagerSync` — Synchronous wrapper for async MCPClientManager
  - Constructor: `__init__(config_path=None)` — Initialize async manager
  - Key Methods:
    - `initialize()` — Sync wrapper; detects running event loop and delegates to thread if needed
    - `_get_loop()` — Get or create event loop, detect if loop already running
    - `_run_in_new_thread(coro)` — Execute coroutine in dedicated thread with new event loop
    - `get_all_tools_as_callables()` — Return sync-wrapped tool callables
    - `get_tool_descriptions()` — Get tool descriptions
    - `shutdown()` — Close connections
    - `get_status()` — Get status info

**Data Flow:**
Config file (JSON) → `_load_server_configs()` → `_connect_server()` for each → transport-specific connection → `_discover_tools()` → MCPTool objects stored in connection → `get_all_tools_as_callables()` returns callable dict → `call_tool()` invokes via session

**Design Patterns:**
- Factory pattern: Tool wrapper creation
- Adapter pattern: Sync wrapper adapting async API to blocking contexts
- Connection pooling: Multiple server connections managed centrally

**Dependencies:**
- External: `mcp` library (ClientSession, StdioServerParameters, stdio_client, streamablehttp_client)
- Internal: `config.settings`, `config.MCPServerConfig`

**Public API:**
- `MCPClientManager` — async manager for agent runtime
- `MCPClientManagerSync` — sync alternative for non-async environments
- Tool callables exposed via `get_all_tools_as_callables()`

---

## File: `memory.py`

**Purpose:** Markdown-first conversational memory manager with append-only history, checkpoint-based summarization, and retrieval-augmented context building.

**Responsibilities:**
- Parse and persist agent conversation history as markdown
- Maintain summary state with checkpoint tracking
- Search memory by keyword for context retrieval
- Build context windows with summary compression for LLM prompts
- Support custom summarization logic

**Key Classes:**

- `MemoryManager` — Markdown-based conversational memory store
  - Constructor: `__init__(memory_file=None, summary_file=None)` — Initialize with optional file path overrides
  - Key Methods:
    - `_parse_markdown_entries(content)` — Parse markdown entry blocks (regex ENTRY_PATTERN) into structured history, return List[Dict]
    - `_load_markdown_memory()` — Load raw history from memory_file
    - `_load_markdown_summary()` — Load summary and checkpoint from summary_file
    - `_load_memory()` — Load both memory and summary from storage
    - `_render_markdown_memory()` — Render history as markdown content string
    - `_save_markdown_memory()` — Persist history to memory_file
    - `_render_markdown_summary()` — Render summary/checkpoint as markdown string
    - `_save_markdown_summary()` — Persist summary to summary_file
    - `_save_summary_state()` — Persist summary without rewriting history
    - `save_memory()` — Save both memory and summary
    - `add_entry(role, content, metadata=None)` — Add message entry with timestamp and persist
    - `get_history()` — Return full List[Dict] conversation history
    - `search_history(query, limit=6)` — Keyword search with relevance scoring, return top matches
    - `build_retrieval_context(query, limit=6, max_chars=1600)` — Format search results as compact string for prompt injection
    - `_default_summarizer(old_messages, previous_summary)` — Fallback compact summarization
    - `get_context_window(system_prompt, max_messages, summarizer=None)` — Build LLM-ready message list with summary compression
    - `clear_memory()` — Wipe history and summary, save empty state

**Data Structures:**

- Entry format (Dict): `{"role": str, "content": str, "metadata": Dict, "timestamp": str}`
- Markdown entry regex (ENTRY_PATTERN): Parses `### Entry N | role=X | ts=Y\nmetadata: {...}\n````text\n...\n````\n`
- Summary checkpoint regex (SUMMARY_CHECKPOINT_PATTERN): Parses `summary_checkpoint: N`
- Summary block regex (SUMMARY_BLOCK_PATTERN): Parses `## Summary\n````text\n...\n````\n`

**Key Algorithms:**

- `search_history()`: Tokenizes query (2+ char tokens), counts token occurrences in content, ranks by score and entry index
- `build_retrieval_context()`: Search entries, format each as `[role | timestamp] content`, truncate to max_chars
- `get_context_window()`: If history > max_messages, summarize old entries via summarizer function, keep recent messages verbatim, checkpoint summarization state

**Dependencies:**
- External: `json`, `re`, `datetime`, `typing`
- Internal: `config.settings`

**Public API:**
- `MemoryManager` — Full-featured memory manager for agent runtime
- Methods for adding entries, retrieving history, searching, building context windows

**Design Patterns:**
- Append-only log for conversation history
- Checkpoint-based summarization to manage context window size
- Extensible summarizer callback for custom compression logic

---

## Cross-Module Data Flow

1. **Initialization:**
   - CLI entry point (`_cli_entry.py`) sets `WORKSPACE_PATH` → `config.py` resolves paths
   - `Settings` singleton loads .env file from PROJECT_ROOT
   - `MCPClientManager` (optionally via sync wrapper) loads MCP servers config JSON

2. **Ask Pipeline:**
   - `ask_main()` parses question and workspace → calls `ask_pipeline(workspace, question)`
   - Router agent in hub.pipeline likely uses `MCPClientManager.get_all_tools_as_callables()` for tool access
   - `MemoryManager` provides retrieval context via `build_retrieval_context(question)`

3. **Refresh Pipeline:**
   - `refresh_main()` scans project with optional `--quick` and `--failed-only` flags → calls `refresh_pipeline()`
   - ModuleAgent cluster (in hub submodule) likely uses `MemoryManager` to persist findings

4. **Memory Persistence:**
   - Agents add entries via `MemoryManager.add_entry()` with role, content, metadata
   - `get_context_window()` compresses old history via checkpoint + summarizer when building prompts
   - Markdown files serve as durable audit log and summary checkpoint store

---

## Configuration

**Environment Variables (via .env or direct):**
- `WORKSPACE_PATH` — Project root; used by CLI and Settings defaults
- `OPENAI_API_KEY` — API key for LLM
- `OPENAI_BASE_URL` — Base URL for OpenAI-compatible endpoint
- `OPENAI_MODEL` — Model name (default: gpt-4o-mini)
- `STREAM_ENABLED` — Enable LLM streaming (default: False)
- `DEBUG_MODE` — Enable debug output
- `MCP_ENABLED` — Enable MCP integration (default: False)
- `MCP_TOOL_PREFIX` — Prefix for tool names (default: "mcp_")

**Configuration Files:**
- `.env` — Environment variable overrides, resolved from PROJECT_ROOT
- `mcp_servers.json` — MCP server definitions, loaded by MCPClientManager

**Constants/Defaults:**
- Agent name: "AntigravityAgent"
- Memory file: `memory/agent_memory.md` (resolved from PROJECT_ROOT)
- Summary file: `memory/agent_summary.md` (resolved from PROJECT_ROOT)
- Artifacts dir: `artifacts/` (resolved from PROJECT_ROOT)
- Antigravity dir: `.antigravity/` (resolved from PROJECT_ROOT)
- MCP connection timeout: 30 seconds
- Default LLM model: "gpt-4o-mini"