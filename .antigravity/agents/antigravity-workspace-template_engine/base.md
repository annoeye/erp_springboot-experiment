# antigravity-workspace-template_engine: base Group Knowledge Document

## Overview

The **base** group provides two foundational subsystems for the antigravity engine: (1) language adapter protocols and semantic models for extracting code structure from multiple programming languages, and (2) a pluggable sandbox execution layer that supports local, Microsandbox, and E2B runtimes with configurable resource limits and graceful degradation.

---

## Language Adapters: Semantic Models & Protocol

### File: `antigravity_engine/hub/language_adapters/base.py`

**Purpose**: Defines language-neutral semantic models and the `LanguageAdapter` protocol contract for analyzing source files across multiple languages.

#### Key Classes & Data Models

**`SymbolDef`** (Pydantic BaseModel)
- **Purpose**: Language-neutral representation of a symbol definition (function, class, struct, etc.)
- **Fields**:
  - `name` (str): Short symbol name
  - `kind` (str): Language-neutral symbol kind (e.g., `function`, `struct`, `class`)
  - `qualified_name` (str | None): Fully qualified name when available
  - `line` (int | None): 1-based definition line number
  - `signature` (str | None): Compact declaration summary
  - `receiver` (str | None): Owning type for methods
  - `bases` (list[str]): Base types or implemented interfaces
  - `is_entrypoint` (bool): Whether symbol is an execution entrypoint

**`SignatureSummary`** (Pydantic BaseModel)
- **Purpose**: Compact signature-oriented summary for hub splitting context
- **Fields**:
  - `rel_path` (str): Workspace-relative file path
  - `content` (str): Summary text suitable for sibling-agent context

**`SemanticEdge`** (Pydantic BaseModel)
- **Purpose**: Language-neutral semantic relation for graph construction
- **Fields**:
  - `source` (str): Source graph node identifier
  - `target` (str): Target graph node identifier
  - `edge_type` (str): Semantic relation type (e.g., `calls`, `imports`, `extends`)

**`FileSemantics`** (Pydantic BaseModel)
- **Purpose**: Complete semantic summary for one analyzed source file
- **Fields**:
  - `rel_path` (str): Workspace-relative file path
  - `language` (str): Human-readable language label
  - `adapter_name` (str): Name of adapter that produced semantics
  - `package_name` (str | None): Language-level package/namespace
  - `package_identity` (str | None): Shared dependency identity for grouping
  - `module_name` (str | None): Per-file module identity
  - `provided_modules` (list[str]): Import keys this file/package satisfies
  - `imports` (list[str]): Imported dependency identities
  - `symbols` (list[SymbolDef]): Top-level symbol definitions
  - `entrypoints` (list[str]): Entry symbols detected
  - `is_test_file` (bool): Whether file is a test file
  - `test_targets` (list[str]): Package/module identities covered by test
  - `signature_summary` (str): Compact signature-oriented text summary
  - `parse_error` (str | None): Non-fatal parse failure details

#### Protocol: `LanguageAdapter`

**Purpose**: Contract that all language-specific semantic analyzers must implement.

**Required Attributes**:
- `name` (str): Adapter identifier
- `supported_suffixes` (frozenset[str]): File extensions the adapter handles (e.g., `.py`, `.ts`)

**Method: `analyze()`**
```
analyze(workspace: Path, abs_path: Path, rel_path: str, content: str) -> FileSemantics
```
- **Purpose**: Extract semantic information from a source file
- **Parameters**:
  - `workspace`: Root workspace directory
  - `abs_path`: Absolute file path
  - `rel_path`: Workspace-relative path
  - `content`: Decoded file contents
- **Returns**: `FileSemantics` object with extracted structure; adapters degrade gracefully on parse errors
- **Design Pattern**: Protocol-based adapter pattern allows pluggable language support

#### Dependencies
- `pathlib.Path`: For file path handling
- `pydantic`: BaseModel for validated data structures with field descriptions

#### Data Flow
Language-specific adapters (not in this file) implement the `LanguageAdapter` protocol. Each adapter's `analyze()` method processes a single source file and returns a `FileSemantics` object containing extracted symbols, dependencies, and entrypoints. This feeds into downstream hub analysis for code splitting and dependency resolution.

#### Public API
- Export `SymbolDef`, `SignatureSummary`, `SemanticEdge`, `FileSemantics` for type hints
- Export `LanguageAdapter` protocol for adapter implementations

---

## Sandbox Execution Layer

The sandbox subsystem provides an abstraction over multiple code execution backends with a unified interface.

### File: `antigravity_engine/sandbox/base.py`

**Purpose**: Defines the base protocol and result dataclass for all sandbox implementations.

#### Key Classes

**`ExecutionResult`** (Dataclass)
- **Purpose**: Structured result of code execution
- **Fields**:
  - `stdout` (str): Standard output text
  - `stderr` (str): Standard error text
  - `exit_code` (int): Process exit code
  - `duration` (float): Execution time in seconds
  - `meta` (Dict[str, object]): Runtime-specific metadata (e.g., `runtime`, `truncated`, `timed_out`, `resource_limits`)

**`CodeSandbox`** (Protocol)
- **Purpose**: Abstract interface for all execution environments
- **Method: `execute()`**
  ```
  execute(code: str, language: str = "python", timeout: int = 30) -> ExecutionResult
  ```
  - **Purpose**: Execute code synchronously with timeout and output capture
  - **Parameters**:
    - `code`: Source code to execute
    - `language`: Language identifier (default: `"python"`)
    - `timeout`: Maximum execution time in seconds (default: 30)
  - **Returns**: `ExecutionResult` with captured output and metadata
  - **Design Pattern**: Protocol-based strategy pattern for pluggable runtimes

#### Dependencies
- `dataclasses`: For result structure
- `typing.Protocol`: For abstract interface definition

---

### File: `antigravity_engine/sandbox/factory.py`

**Purpose**: Factory that returns the configured sandbox implementation based on environment variables.

#### Key Function

**`get_sandbox() -> CodeSandbox`**
- **Purpose**: Obtain the configured executor with fallback to local sandbox
- **Logic**:
  1. Read `SANDBOX_TYPE` environment variable (default: `"local"`)
  2. If `"microsandbox"`: attempt to import and return `MicrosandboxSandbox`; on failure, warn to stderr and fallback to `LocalSandbox`
  3. If `"e2b"`: attempt to import and return `E2BSandbox`; on failure, warn to stderr and fallback to `LocalSandbox`
  4. If unknown or `"local"`: return `LocalSandbox`
- **Behavior**: Graceful degradation with explicit stderr warnings so callers know execution is not isolated
- **Design Pattern**: Factory pattern with runtime plugin loading

#### Dependencies
- `os`: Environment variable access
- `sys`: stderr output
- `CodeSandbox, LocalSandbox`: Sandbox abstractions
- Conditional imports: `MicrosandboxSandbox`, `E2BSandbox` (lazy-loaded)

#### Configuration
- **Environment Variable**: `SANDBOX_TYPE` (supported: `local`, `microsandbox`, `e2b`)

---

### File: `antigravity_engine/sandbox/local.py`

**Purpose**: Local subprocess-based sandbox that executes Python code in an isolated temporary directory with timeout and output truncation.

#### Key Classes & Functions

**Helper: `_truncate_output(text: str, max_bytes: int) -> Tuple[str, bool]`**
- **Purpose**: Truncate UTF-8 output to maximum byte length
- **Parameters**:
  - `text`: Output text to truncate
  - `max_bytes`: Maximum byte length allowed
- **Returns**: Tuple of (truncated_text, was_truncated_flag)
- **Logic**: Encodes to UTF-8, slices to max_bytes, appends `"... (output truncated)"` suffix if trimmed

**Class: `LocalSandbox(CodeSandbox)`**
- **Purpose**: Execute Python code in subprocess inside isolated temp directory
- **Method: `execute(code, language="python", timeout=30)`**
  - **Supported languages**: Python only; returns error for other languages
  - **Execution flow**:
    1. Write code to `main.py` in temporary directory
    2. Run `subprocess.run([sys.executable, script_path], cwd=tmpdir, capture_output=True, timeout=timeout)`
    3. Capture stdout, stderr, exit code
    4. Handle `subprocess.TimeoutExpired`: set `timed_out=True`, exit_code=-1, stderr=timeout message
    5. Handle other exceptions: exit_code=1, stderr=error message
  - **Output handling**:
    - Read `SANDBOX_MAX_OUTPUT_KB` env var (default: 10 KB)
    - Truncate stdout and stderr independently to this limit
  - **Returns**: `ExecutionResult` with meta containing:
    - `runtime`: `"local"`
    - `truncated`: Whether output was truncated
    - `timed_out`: Whether execution timed out
    - `resource_limits`: Dict with `timeout_sec` and `max_output_kb`

#### Dependencies
- `os`, `sys`, `time`, `tempfile`, `subprocess`: System execution and temp directory management
- `CodeSandbox, ExecutionResult`: Base protocol and result type

#### Configuration
- **Environment Variable**: `SANDBOX_MAX_OUTPUT_KB` (default: 10)

#### Design Pattern
- Clean separation of concerns: temp directory cleanup handled by context manager
- Graceful output truncation prevents memory exhaustion from runaway output

---

### File: `antigravity_engine/sandbox/microsandbox_exec.py`

**Purpose**: Microsandbox server-backed sandbox that executes code in containerized runtime instances via JSON-RPC API.

#### Key Classes & Functions

**Helper: `_truncate_output(text: str, max_bytes: int) -> Tuple[str, bool]`**
- Same truncation logic as `local.py`

**Class: `MicrosandboxSandbox(CodeSandbox)`**
- **Purpose**: Remote sandbox execution via Microsandbox server
- **Constructor: `__init__()`**
  - Initializes from environment variables:
    - `MSB_SERVER_URL` (default: `"http://127.0.0.1:5555"`): Microsandbox server URL
    - `MSB_API_KEY` (default: `""`): Optional Bearer token for authentication
    - `MSB_IMAGE` (default: `"microsandbox/python"`): Container image to run
    - `MSB_MEMORY_MB` (default: 512): Memory limit in MB
    - `MSB_CPU_LIMIT` (default: 1.0): CPU limit (fractional cores)
    - `MSB_START_TIMEOUT_SEC` (default: 30): Timeout for sandbox startup

- **Method: `_headers() -> Dict[str, str]`**
  - Builds HTTP headers with optional Bearer token authorization
  - Returns: Headers with `Content-Type: application/json` and optional `Authorization` header

- **Method: `_post_json_rpc(path, payload, timeout_sec) -> Tuple[Optional[Dict], Optional[str]]`**
  - Sends JSON-RPC 2.0 request to Microsandbox server
  - **Handles**: Connection timeouts, HTTP errors, JSON parsing, RPC errors
  - **Returns**: Tuple of (parsed_response_data, error_message); one is None

- **Method: `_start_sandbox(sandbox_name, timeout) -> Optional[str]`**
  - Calls `sandbox.start` RPC method
  - **Payload**: Includes sandbox name, image, memory, CPU config
  - **Returns**: Error string on failure; None on success

- **Method: `_stop_sandbox(sandbox_name) -> Optional[str]`**
  - Calls `sandbox.stop` RPC method with 10s timeout
  - **Returns**: Error string on failure; None on success

- **Method: `execute(code, language="python", timeout=30) -> ExecutionResult`**
  - **Supported languages**: Python only
  - **Execution flow**:
    1. Generate unique sandbox name `ag-msb-{uuid}`
    2. Start sandbox via `_start_sandbox()`. If fails, return error result immediately
    3. Call `sandbox.repl.run` RPC with code
    4. Parse response output lines (stream-tagged stdout/stderr)
    5. Check status: if error/exception/failed, set exit_code=1
    6. In finally block: stop sandbox; if cleanup fails and execution succeeded, surface as exit_code=1
  - **Output handling**: Same truncation as local sandbox
  - **Returns**: `ExecutionResult` with meta containing:
    - `runtime`: `"microsandbox"`
    - `timed_out`: Detected from error message
    - `truncated`: Whether output was truncated
    - `resource_limits`: Dict with `timeout_sec` and `max_output_kb`
    - `server_url`: Server URL for debugging

#### Dependencies
- `os`, `time`, `uuid`, `tempfile`: Environment and unique naming
- `requests`: HTTP client for RPC calls
- `CodeSandbox, ExecutionResult`: Base protocol

#### Configuration
- **Environment Variables**: `MSB_SERVER_URL`, `MSB_API_KEY`, `MSB_IMAGE`, `MSB_MEMORY_MB`, `MSB_CPU_LIMIT`, `MSB_START_TIMEOUT_SEC`, `SANDBOX_MAX_OUTPUT_KB`

#### Design Patterns
- JSON-RPC 2.0 protocol for server communication
- Lifecycle management: start sandbox → execute → stop (in finally for reliability)
- Best-effort cleanup: sandbox stop errors only surface if execution succeeded

---

## Cross-Cutting Concerns

### Resource Limits
- **Output truncation**: Configurable via `SANDBOX_MAX_OUTPUT_KB` (default 10 KB), applied uniformly across all runtimes
- **Execution timeout**: Per-call parameter, stored in result metadata
- **Memory/CPU** (Microsandbox only): Configurable at startup

### Error Handling & Degradation
- Factory provides fallback from unavailable backends (Microsandbox, E2B) to local sandbox with stderr warning
- All implementations gracefully degrade on unsupported languages (return error result, not exception)
- Parse failures in adapters captured in `FileSemantics.parse_error` field without aborting analysis

### Data Flow Summary
1. **Analysis Phase**: Language-specific adapters (implementing `LanguageAdapter` protocol) analyze source files → produce `FileSemantics`
2. **Execution Phase**: Factory returns configured `CodeSandbox` → callers invoke `execute()` → receive `ExecutionResult` with captured output and metadata