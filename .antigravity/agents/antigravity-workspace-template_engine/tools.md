# Tools Group Knowledge Document

## Overview

The **tools** group provides a modular collection of utility functions and integrations for the Antigravity agent ecosystem. These tools enable code execution, external API interactions (web search, weather, stock prices), mathematical evaluation, memory management, and MCP (Model Context Protocol) server integration. The group supports both legacy single-agent patterns and current multi-agent architecture (v0.2+), with most tools designed as zero-config plugins that can be discovered and bound dynamically.

---

## File: `__init__.py`

**Purpose:** Package initialization and legacy documentation. Declares this as a legacy tools package retained for reference.

**Context:** The old single-agent architecture (GeminiAgent) used auto-discovery of `.py` files in this directory. Current multi-agent architecture (v0.2+) defines code-exploration tools in `hub/ask_tools.py` and binds them per-workspace. Files here may be integrated into ModuleAgents in future releases.

---

## File: `demo_tool.py`

**Purpose:** Demonstration of dynamic tool discovery. Shows how new tools can be added without modifying agent code.

### Key Functions

| Function | Parameters | Returns | Purpose |
|----------|-----------|---------|---------|
| `greet_user(name: str)` | `name` — user's name | `str` — greeting message | Friendly greeting with emoji |
| `reverse_text(text: str)` | `text` — input string | `str` — reversed text | Text reversal utility |

**Design Pattern:** Zero-config plugin discovery — drop file in `tools/` directory and functions become available automatically.

**Public API:** `greet_user()`, `reverse_text()`

---

## File: `example_tool.py`

**Purpose:** Reference implementations of common tool patterns (web search, stock prices, safe math evaluation, weather, email).

### Key Functions

| Function | Parameters | Returns | Purpose |
|----------|-----------|---------|---------|
| `web_search(query: str)` | `query` — search terms | `str` — formatted results | Mock web search (placeholder for real API) |
| `get_stock_price(ticker: str)` | `ticker` — stock symbol | `float` — price | Mock stock price lookup |
| `calculate_math(expression: str)` | `expression` — math string | `float` — evaluated result | Safe math evaluation using AST |
| `get_weather(city: str)` | `city` — location | `dict` — temp, condition, city | Mock weather data |
| `send_email(to: str, body: str)` | `to` — recipient, `body` — content | `str` — confirmation | Mock email sending |

### Design Patterns

**Safe AST-based evaluation** (`calculate_math`): Parses expression string via Python's `ast` module, validates node types, and maps only whitelisted operators (`Add`, `Sub`, `Mult`, `Div`, `Pow`, `Mod`, `FloorDiv`, `UAdd`, `USub`). Prevents code injection compared to `eval()`. Raises `ValueError` for unsupported nodes or operators.

### Dependencies

- `requests` — imported but not used in current implementation
- `ast` — AST parsing for math expressions
- `operator` — whitelisted mathematical operations

### Public API

All five functions are public; mock implementations log DEBUG output before returning placeholder values.

---

## File: `execution_tool.py`

**Purpose:** Execute arbitrary Python code in a sandboxed environment with timeout protection.

### Key Functions

| Function | Parameters | Returns | Purpose |
|----------|-----------|---------|---------|
| `run_python_code(code: str, timeout: Optional[int] = None)` | `code` — Python source, `timeout` — max seconds | `str` — stdout or error message | Safe code execution via sandbox |

### Data Flow

1. Calls `get_sandbox()` from `antigravity_engine.sandbox.factory`
2. Resolves effective timeout from parameter, env var `SANDBOX_TIMEOUT_SEC`, or default 30 seconds
3. Calls `sandbox.execute(code, language="python", timeout)`
4. Returns stdout on success (exit_code=0) or formatted error message on failure

### Dependencies

- `antigravity_engine.sandbox.factory.get_sandbox` — retrieves configured sandbox instance
- `os` — reads `SANDBOX_TIMEOUT_SEC` environment variable

### Configuration

- `SANDBOX_TIMEOUT_SEC` env var — default execution timeout (default: 30 seconds)

---

## File: `mcp_tools.py`

**Purpose:** Utilities for discovering, listing, and managing MCP (Model Context Protocol) servers and their tools within the Antigravity agent ecosystem.

### Key Functions

| Function | Parameters | Returns | Purpose |
|----------|-----------|---------|---------|
| `list_mcp_servers()` | None | `str` — formatted status report | List all configured MCP servers and connection status |
| `list_mcp_tools(server_name: Optional[str] = None)` | `server_name` — filter by server | `str` — formatted tool list | Discover available MCP tools; optionally filter by server |
| `get_mcp_tool_help(tool_name: str)` | `tool_name` — tool identifier | `str` — detailed documentation | Get help for specific MCP tool, including input schema |
| `mcp_health_check()` | None | `str` — health status report | Perform connectivity check on all MCP servers |
| `_set_mcp_manager(manager)` | `manager` — MCPClientManager instance | None | Internal: set global manager (called by agent) |
| `_get_mcp_manager()` | None | MCPClientManager or None | Internal: retrieve global manager instance |

### Data Flow

1. Public functions retrieve global `_global_mcp_manager` via `_get_mcp_manager()`
2. Call manager methods (`get_status()`, `get_all_tools()`) to fetch server and tool metadata
3. Format and return results as human-readable strings
4. Agent initializes manager and sets it via `_set_mcp_manager()` during startup

### Dependencies

- `antigravity_engine.mcp_client.MCPClientManager` — manages MCP server connections
- `antigravity_engine.config.settings` — reads `MCP_ENABLED`, `MCP_TOOL_PREFIX`, and other config
- `json` — formats tool schemas

### Configuration

- `MCP_ENABLED` — enable/disable MCP integration
- `MCP_TOOL_PREFIX` — prefix applied to tool names (e.g., "mcp_")
- `mcp_servers.json` — MCP server configuration file

### Design Patterns

**Global singleton manager:** `_global_mcp_manager` is set once by agent and reused by all tool functions. Decouples tool discovery from manager lifecycle.

**Tool normalization:** `get_mcp_tool_help()` accepts tool names with or without prefix and searches for matches.

**Graceful degradation:** Each function returns informative error messages rather than raising exceptions (MCP disabled, not initialized, library missing, connection failed).

### Public API

- `list_mcp_servers()` — report server status
- `list_mcp_tools(server_name=None)` — enumerate available tools
- `get_mcp_tool_help(tool_name)` — fetch tool documentation
- `mcp_health_check()` — connectivity diagnostics

---

## File: `memory_tools.py`

**Purpose:** Read and search markdown memory files using ripgrep (with Python fallback), allowing agents to inspect and query persistent memory.

### Key Functions

| Function | Parameters | Returns | Purpose |
|----------|-----------|---------|---------|
| `read_memory_md(max_chars: int = 12000, memory_file: Optional[str] = None)` | `max_chars` — truncation limit (≤0 = no limit), `memory_file` — override path | `str` — file content (possibly truncated) | Read memory markdown with optional character limit |
| `search_memory_md(query: str, max_results: int = 20, case_insensitive: bool = True, memory_file: Optional[str] = None)` | `query` — search pattern, `max_results` — line limit, `case_insensitive` — ignore case, `memory_file` — override path | `str` — matching lines with line numbers | Search memory file using ripgrep or Python fallback |
| `_resolve_memory_file(memory_file: Optional[str] = None)` | `memory_file` — optional override | `Path` — absolute file path | Resolve target memory file location |

### Data Flow

1. `_resolve_memory_file()` normalizes path: use explicit `memory_file` or default `settings.memory_file_path`
2. `read_memory_md()` reads entire file, truncates to `max_chars` if needed
3. `search_memory_md()` attempts ripgrep (`rg`) command; falls back to Python line-by-line search if ripgrep not installed
4. Returns matches formatted as `line_number:line_content`

### Dependencies

- `pathlib.Path` — file path handling
- `subprocess` — execute ripgrep command
- `antigravity_engine.config.settings` — `memory_file_path` and `resolve_path()`

### Configuration

- `settings.memory_file_path` — default memory markdown location
- Ripgrep (`rg`) command — must be installed for optimal performance; Python fallback available

### Design Patterns

**Graceful degradation:** Ripgrep attempt → Python fallback if not installed or fails.

**Path resolution:** Centralizes logic in `_resolve_memory_file()` to support both default and explicit paths.

### Public API

- `read_memory_md(max_chars=12000, memory_file=None)` — retrieve memory content
- `search_memory_md(query, max_results=20, case_insensitive=True, memory_file=None)` — search memory

---

## File: `ollama_local.py`

**Purpose:** Call local Ollama-style LLM endpoints for inference from within the Antigravity agent.

### Key Functions

| Function | Parameters | Returns | Purpose |
|----------|-----------|---------|---------|
| `call_local_ollama(prompt: str, model: str = "qwen3:0.6b", host: str = "http://127.0.0.1:11434", stream: bool = False, options: Optional[Dict[str, Any]] = None)` | `prompt` — input text, `model` — model ID, `host` — server URL, `stream` — enable streaming, `options` — extra backend options | `str` — generated response text | Call `/api/generate` endpoint on local Ollama server |

### Data Flow

1. Construct POST request to `{host}/api/generate`
2. Payload includes `model`, `prompt`, `stream`, and optional `options`
3. Send request with 60-second timeout
4. Extract `response` or `output` field from JSON response
5. Return as string; convert non-string responses to JSON

### Dependencies

- `requests` — HTTP POST request
- `json` — parse/serialize responses

### Configuration

- `model` — default "qwen3:0.6b"; can be overridden per call
- `host` — default "http://127.0.0.1:11434"
- `stream` — default False

### Error Handling

Returns error message string (not exception) on request failure, JSON decode error, or missing response field.

### Public API

- `call_local_ollama(prompt, model="qwen3:0.6b", host="http://127.0.0.1:11434", stream=False, options=None)`

---

## File: `openai_proxy.py`

**Purpose:** Thin wrapper around OpenAI-compatible chat completion APIs (OpenAI, Azure OpenAI, or self-hosted providers like Ollama/Llama.cpp).

### Key Functions

| Function | Parameters | Returns | Purpose |
|----------|-----------|---------|---------|
| `call_openai_chat(prompt: str, system: Optional[str] = None, model: Optional[str] = None, temperature: float = 0.7, max_tokens: int = 512)` | `prompt` — user message, `system` — system prompt, `model` — model override, `temperature` — sampling temp, `max_tokens` — generation limit | `str` — LLM response or error message | Call OpenAI-compatible chat completion API |

### Data Flow

1. Read configuration: `OPENAI_BASE_URL`, `OPENAI_API_KEY`, `OPENAI_MODEL` from settings
2. Validate required values (`base_url`, `model`)
3. Construct messages array: optional system message, then user prompt
4. POST to `{base_url}/chat/completions` with headers (auth if API key present)
5. Parse response, extract `choices[0].message.content`
6. Return content or error message on failure

### Dependencies

- `requests` — HTTP POST
- `antigravity_engine.config.settings` — `OPENAI_BASE_URL`, `OPENAI_API_KEY`, `OPENAI_MODEL`

### Configuration

- `OPENAI_BASE_URL` — API endpoint URL (required)
- `OPENAI_API_KEY` — authentication token (optional; added to Authorization header if present)
- `OPENAI_MODEL` — default model name (required)
- `temperature` — default 0.7
- `max_tokens` — default 512

### Error Handling

Returns error string (not exception) on request failure, JSON decode error, or missing response fields.

### Public API

- `call_openai_chat(prompt, system=None, model=None, temperature=0.7, max_tokens=512)`

---

## Cross-File Dependencies Summary

| Import | Used By | Purpose |
|--------|---------|---------|
| `antigravity_engine.sandbox.factory.get_sandbox` | `execution_tool.py` | Retrieve configured sandbox |
| `antigravity_engine.mcp_client.MCPClientManager` | `mcp_tools.py` | Manage MCP server connections |
| `antigravity_engine.config.settings` | `mcp_tools.py`, `memory_tools.py`, `openai_proxy.py` | Read configuration |
| `requests` | `example_tool.py`, `ollama_local.py`, `openai_proxy.py` | HTTP requests |
| `ast`, `operator` | `example_tool.py` | Safe math evaluation |
| `subprocess` | `memory_tools.py` | Execute ripgrep |
| `pathlib.Path` | `memory_tools.py` | File path handling |
| `json` | `mcp_tools.py`, `ollama_local.py` | JSON serialization |

---

## Configuration & Environment Variables

| Variable | Used By | Purpose | Default |
|----------|---------|---------|---------|
| `SANDBOX_TIMEOUT_SEC` | `execution_tool.py` | Code execution timeout | 30 seconds |
| `MCP_ENABLED` | `mcp_tools.py` | Enable MCP integration | (from settings) |
| `MCP_TOOL_PREFIX` | `mcp_tools.py` | MCP tool name prefix | (from settings) |
| `OPENAI_BASE_URL` | `openai_proxy.py` | Chat completion API URL | (required) |
| `OPENAI_API_KEY` | `openai_proxy.py` | Authentication token | (optional) |
| `OPENAI_MODEL` | `openai_proxy.py` | Default model | (required) |

---

## Design Patterns

1. **Zero-config plugins** (`demo_tool.py`, `example_tool.py`) — drop `.py` files in `tools/` for automatic discovery
2. **Safe evaluation** (`example_tool.py`) — AST-based math with whitelisted operators prevents code injection
3. **Graceful degradation** (`mcp_tools.py`, `memory_tools.py`, `ollama_local.py`, `openai_proxy.py`) — return error strings instead of raising exceptions
4. **Global singleton manager** (`mcp_tools.py`) — `_global_mcp_manager` set once, reused by all functions
5. **Sandboxed execution** (`execution_tool.py`) — delegate to configurable sandbox factory
6. **Mock implementations** (`example_tool.py`) — demonstrate expected tool signatures with placeholder responses

---

## Public API Surface

### Code Execution
- `run_python_code(code, timeout=None)` → `str`

### LLM Integration
- `call_local_ollama(prompt, model="qwen3:0.6b", host="...", stream=False, options=None)` → `str`
- `call_openai_chat(prompt, system=None, model=None, temperature=0.7, max_tokens=512)` → `str`

### MCP Management
- `list_mcp_servers()` → `str`
- `list_mcp_tools(server_name=None)` → `str`
- `get_mcp_tool_help(tool_name)` → `str`
- `mcp_health_check()` → `str`

### Memory Management
- `read_memory_md(max_chars=12000, memory_file=None)` → `str`
- `search_memory_md(query, max_results=20, case_insensitive=True, memory_file=None)` → `str`

### Demo / Reference
- `greet_user(name)` → `str`
- `reverse_text(text)` → `str`
- `web_search(query)` → `str` (mock)
- `get_stock_price(ticker)` → `float` (mock)
- `calculate_math(expression)` → `float`
- `get_weather(city)` → `dict` (mock)
- `send_email(to, body)` → `str` (mock)