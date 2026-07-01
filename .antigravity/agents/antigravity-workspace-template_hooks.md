# antigravity-workspace-template Hooks Module — Knowledge Document

## Overview

The **main** group within the hooks module provides a cross-platform SessionStart hook for AWS Code that ensures the `ag-mcp` (AntiGravity Model Context Protocol) engine is installed and available on PATH. The module implements an idempotent, silent-on-success installation strategy that prioritizes `pipx` as the package manager but falls back to pip if needed. All logic is centralized in Python for reusability across platform-specific entry points (Python script, Bash wrapper, and Windows batch file).

---

## File: `install_engine.py`

**Purpose:** Core installation logic for the `ag-mcp` engine. Handles multi-strategy provisioning (pipx primary, pip fallback), version verification, and PATH manipulation across macOS, Linux, and Windows.

### Key Functions

#### `log(msg: str) -> None`
- Outputs diagnostic messages to stderr (preserves stdout for AWS Code hook context injection)
- Enables silent operation on success path

#### `has(cmd: str) -> bool`
- Uses `shutil.which()` to check if a command exists on PATH
- Returns `True` if found, `False` otherwise

#### `user_scripts_bin() -> Path | None`
- Resolves the Python user-level scripts directory via `site.USER_BASE`
- Returns `Scripts` on Windows (`nt`), `bin` on POSIX systems
- Returns `None` if the directory does not exist

#### `prepend_path(p: Path) -> None`
- Adds a directory to the front of the current process's PATH environment variable
- Splits PATH by `os.pathsep`, deduplicates, and updates `os.environ["PATH"]`
- No-op if path already present or directory does not exist

#### `run(cmd: list[str]) -> int`
- Executes a shell command with stderr/stdout redirected to stderr
- Returns the exit code; catches `FileNotFoundError` and returns 127
- Used internally for `brew`, `pip`, and `pipx` invocations

#### `ensure_pipx() -> bool`
- Installs `pipx` if missing, returning `True` if available afterwards
- **Strategy:** Homebrew on macOS (if `brew` exists), else `pip install --user pipx`
- Calls `prepend_path()` for user-base bin directory if pip install is used
- Checks both command availability and Python module availability via `_has_pipx_module()`

#### `_has_pipx_module() -> bool`
- Tests if `pipx` can be invoked via `python -m pipx --version`
- Used as a fallback when `pipx` command is not on PATH but the module is installed

#### `pipx(args: list[str]) -> int`
- Wraps pipx invocation; uses `pipx` command if available, else `python -m pipx`
- Accepts argument list and returns subprocess exit code

#### `read_project_version(engine_dir: Path) -> str | None`
- Parses `pyproject.toml` in the bundled engine directory
- Uses regex `^version\s*=\s*"([^"]+)"` to extract semantic version string
- Returns version string or `None` if file not found or regex does not match

#### `get_installed_engine_version() -> str | None`
- Runs `ag-mcp --version` with 15-second timeout
- Extracts version from stdout/stderr using regex `(\d+\.\d+\.\d+(?:[-+][A-Za-z0-9.]+)?)`
- Returns version string or `None` if `ag-mcp` not on PATH, command fails, or timeout occurs

#### `needs_engine_install_or_upgrade(installed: str | None, expected: str | None) -> bool`
- Compares installed version against bundled (expected) version
- Returns `True` (install/upgrade needed) unless both are non-None and identical

#### `main() -> int`
- Orchestrates the full installation workflow
- **Lines 108–114:** Resolves plugin root from `CLAUDE_PLUGIN_ROOT` env var or relative to script location; resolves engine and cli subdirectory paths
- **Lines 117–124:** Prepends common user bin directories to PATH before version check (compensates for AWS Code's minimal hook PATH)
- **Lines 126–130:** Fast-path: exits 0 if `ag-mcp` is on PATH and version matches bundled version
- **Lines 132–145:** Attempts pipx-based install/upgrade; calls `pipx ensurepath` and prepends bin directories, then `pipx install --force` on engine directory
- **Lines 147–159:** Fallback to `pip install --user --upgrade` on both engine and cli directories
- **Lines 161–177:** On total failure, prints detailed manual-install instructions for all three platforms and exits 1
- Returns 0 on success, 1 on failure

### Data Flow

1. **Version Check:** `read_project_version()` extracts bundled version; `get_installed_engine_version()` queries installed version
2. **Fast Path:** If versions match and `ag-mcp` is on PATH, exit early (idempotent, no-op behavior)
3. **Dependency Resolution:** `ensure_pipx()` provisions pipx if needed, with platform-specific strategy
4. **Installation:** `pipx install --force` (primary) or `pip install --user` (fallback) targets engine directory
5. **PATH Update:** `prepend_path()` is called multiple times to ensure newly installed shims are discoverable

### Dependencies

- **Built-in:** `os`, `re`, `shutil`, `subprocess`, `sys`, `pathlib.Path`, `site`
- **External:** None (pure stdlib)
- **Platform Detection:** `os.name` ("nt" for Windows, else POSIX), `sys.platform` ("darwin" for macOS)

### Design Patterns

- **Multi-Strategy Fallback:** Tries pipx (primary, most robust), then pip (fallback), then manual instructions
- **Idempotent:** Version check ensures repeated runs are no-ops if already at target version
- **Silent-on-Success:** All output to stderr; stdout preserved for AWS Code hook protocol
- **Cross-Platform Abstraction:** Single Python script handles macOS (Homebrew), Linux, and Windows via conditional logic

### Public API

- **Entry Point:** `main() -> int` — returns 0 on success, 1 on failure
- **Environment Variable:** `CLAUDE_PLUGIN_ROOT` — optional override for plugin root directory
- **Exit Codes:** 0 = success or already up-to-date; 1 = install/upgrade failed
- **Side Effects:** Modifies `os.environ["PATH"]` for current process; installs packages via pipx or pip

### Configuration

- **Version Source:** `<plugin_root>/engine/pyproject.toml` (semver string from `version =` key)
- **Installation Target:** `<plugin_root>/engine` (and `<plugin_root>/cli` for pip fallback)
- **PATH Directories Checked/Prepended:**
  - `~/.local/bin` (POSIX user scripts)
  - `site.USER_BASE/bin` (POSIX) or `site.USER_BASE/Scripts` (Windows)
  - `~/AppData/Roaming/Python/Scripts` (Windows pipx location)

---

## File: `install_engine.sh`

**Purpose:** Thin Bash wrapper for manual invocation of the Python install script. Provides POSIX-compliant entry point that attempts Python 3, then falls back to Python 2 (legacy support).

### Key Functions

None — this is a shell wrapper script with inline execution.

### Implementation

- **Lines 1–5:** Bash header with strict mode (`set -u` for undefined variable error)
- **Lines 6:** Resolves script directory using `cd` and `dirname $0`
- **Lines 7:** Executes `install_engine.py` via `python3`, with fallback to `python`
- **Line 7:** Redirects stderr to `/dev/null` during execution (silent operation)

### Data Flow

1. Resolves its own directory via shell builtins
2. Calls `install_engine.py` with Python 3 (or Python 2 fallback)
3. Inherits all environment variables from parent shell (including `CLAUDE_PLUGIN_ROOT` if set)
4. Exits with the Python script's exit code

### Dependencies

- **External:** `python3` or `python` (invokes `install_engine.py`)

### Public API

- **Invocation:** `bash install_engine.sh` from command line or AWS Code hook system
- **Exit Codes:** Inherited from `install_engine.py` (0 or 1)

---

## Summary: Cross-Module Integration

- **install_engine.py** contains all portable logic; **install_engine.sh** is a lightweight POSIX wrapper
- A Windows `.bat` wrapper (not shown) would similarly delegate to `install_engine.py`
- The hooks system invokes via `install_engine.sh` (or `.bat` on Windows) during AWS Code SessionStart
- Version verification and PATH manipulation ensure `ag-mcp` is always available for subsequent tool calls
- All user-facing messages guide manual fallback installation if automation fails