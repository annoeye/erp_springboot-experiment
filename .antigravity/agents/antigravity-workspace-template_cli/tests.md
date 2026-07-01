# antigravity-workspace-template CLI Tests Module — Knowledge Document

## Group Overview

The **tests** group provides comprehensive test coverage for the CLI module, validating core functionality around workspace initialization, report logging, decision tracking, hub command discovery, and context reading. Tests use `typer.testing.CliRunner` for CLI invocation and `pathlib.Path` for filesystem operations in isolated temporary directories.

---

## File: `__init__.py`

**Purpose:** Package marker file. No implementation.

---

## File: `test_cli_force.py`

**Purpose:** Validates `ag init` command behavior with and without the `--force` flag, ensuring template files are correctly preserved or overwritten.

### Key Functions

**`test_init_skips_existing_without_force(tmp_path: Path) -> None`**
- Verifies that running `ag init` twice without `--force` preserves custom modifications to existing files
- Invokes CLI twice: first creates template structure, second run should not overwrite
- Asserts custom marker text ("CUSTOM CONTENT") remains in `.antigravity/*.md` files

**`test_init_overwrites_with_force(tmp_path: Path) -> None`**
- Verifies that `ag init --force` overwrites existing files with fresh template content
- Captures original content, modifies a file, re-initializes with `--force`
- Asserts file is restored to original template state

**`test_init_bootstrap_files_defer_to_agents_md(tmp_path: Path) -> None`**
- Validates that bootstrap files (`CLAUDE.md`, `.cursorrules`, `CONTEXT.md`, `.antigravity/rules.md`) contain references to `AGENTS.md` as authoritative source
- Reads content from 5 generated files
- Asserts specific strings indicating deference to `AGENTS.md` appear in each

### Data Flow

- Uses `runner.invoke(app, ["init", ...])` to execute CLI commands
- Creates `.antigravity` directory with markdown files
- Modifies file content via `Path.write_text()` and validates with `Path.read_text()`

### Dependencies

- `pathlib.Path` — filesystem operations
- `typer.testing.CliRunner` — CLI invocation harness
- `ag_cli.cli.app` — Typer CLI application instance

### Public API

Tests verify:
- `ag init <path>` creates template structure
- `ag init <path>` without `--force` preserves existing files
- `ag init <path> --force` overwrites files
- Bootstrap files reference `AGENTS.md` as authority

---

## File: `test_cli_report.py`

**Purpose:** Integration tests for report logging (`ag report`) and decision logging (`ag log-decision`) commands.

### Key Functions

**`test_report_creates_file(tmp_path: Path) -> None`**
- Invokes `ag report "Found a critical bug" --workspace <tmp_path>`
- Asserts exit code is 0 and output contains "Logged report"
- Verifies `.antigravity/memory/reports.md` file is created and contains report text

**`test_log_decision_creates_file(tmp_path: Path) -> None`**
- Invokes `ag log-decision "Use PostgreSQL" "Team has deep expertise" --workspace <tmp_path>`
- Asserts exit code is 0 and output contains "Logged decision"
- Verifies `.antigravity/decisions/log.md` file exists with both decision and rationale text

### Data Flow

- CLI receives message and optional rationale arguments
- Commands delegate to writer functions (not shown in tests) that persist data to workspace-relative paths
- Output confirms successful logging

### Dependencies

- `pathlib.Path` — filesystem operations
- `typer.testing.CliRunner` — CLI invocation
- `ag_cli.cli.app` — CLI application

### Public API

Validates:
- `ag report <message> --workspace <path>` creates timestamped entry in `.antigravity/memory/reports.md`
- `ag log-decision <decision> <rationale> --workspace <path>` creates entry in `.antigravity/decisions/log.md`
- Both commands return exit code 0 on success

---

## File: `test_hub_discovery.py`

**Purpose:** Tests the `_run_hub()` discovery mechanism that locates and invokes engine-backed commands (`ag ask`, `ag refresh`) via console scripts or fallback Python module execution.

### Key Functions

**`test_run_hub_ask_console_script_found() -> None`**
- Mocks `shutil.which()` to return `/usr/local/bin/ag-ask`
- Calls `_run_hub(Path("/tmp/project"), "ask", "What?")`
- Asserts `subprocess.run()` called with command starting with `/usr/local/bin/ag-ask`
- Verifies `--workspace` flag passed to subprocess

**`test_run_hub_refresh_console_script_found() -> None`**
- Similar to above but tests `ag-refresh` with `--quick` argument
- Verifies console script invocation path

**`test_run_hub_fallback_to_python_m(tmp_path: Path) -> None`**
- Mocks `shutil.which()` to return `None` (no console script)
- Checks `_REPO_ROOT / "engine" / "antigravity_engine" / "hub" / "__main__.py"` exists
- Calls `_run_hub()` and asserts fallback uses `python -m antigravity_engine` command
- Returns exit code 0

**`test_run_hub_neither_found(tmp_path: Path) -> None`**
- Patches `shutil.which()` to return `None` and `_REPO_ROOT` to empty path
- Calls `_run_hub()`
- Asserts returns exit code 1 (failure)

**`test_help_lists_supported_commands_only() -> None`**
- Invokes `app --help`
- Asserts supported commands (`ask`, `refresh`, `report`, `log-decision`) appear in help output
- Asserts internal commands (`start-engine`) do not appear

### Data Flow

1. `_run_hub()` first attempts `shutil.which(command_name)` to locate console script
2. If found, builds command array and invokes via `subprocess.run()`
3. If not found, checks for monorepo structure at `_REPO_ROOT / "engine" / antigravity_engine / hub / __main__.py`
4. If module found, invokes `python -m antigravity_engine <command> ...`
5. Returns subprocess exit code, or 1 if neither method available

### Dependencies

- `pathlib.Path` — path operations
- `unittest.mock` (MagicMock, call, patch) — mocking filesystem/subprocess calls
- `pytest` — test framework
- `typer.testing.CliRunner` — CLI testing
- `shutil.which` — console script discovery
- `subprocess.run` — subprocess execution
- `ag_cli.cli._run_hub`, `_REPO_ROOT` — internal hub runner and repo root constant

### Design Patterns

- **Discovery Pattern:** Attempts multiple strategies (console script → Python module → failure)
- **Fallback Chain:** Graceful degradation from installed executable to source-based execution

### Public API

- `_run_hub(workspace: Path, command: str, *args: str) -> int` — discovers and runs hub command, returns exit code
- Public `ask` and `refresh` commands available in help; internal `start-engine` hidden

---

## File: `test_reader.py`

**Purpose:** Tests the `ag_cli.reader` module functions for reading workspace context and appending log entries to memory and decision files.

### Key Functions

**`test_read_antigravity_context_empty(tmp_path: Path) -> None`**
- Calls `read_antigravity_context(tmp_path)` when `.antigravity/` does not exist
- Asserts returns empty dict `{}`

**`test_read_antigravity_context_reads_md(tmp_path: Path) -> None`**
- Creates `.antigravity/rules.md` and `.antigravity/conventions.md`
- Creates `.antigravity/not-md.txt` (should be ignored)
- Calls `read_antigravity_context(tmp_path)`
- Asserts returned dict has keys `"rules.md"` and `"conventions.md"` with file contents
- Asserts `"not-md.txt"` is not in result (only `.md` files loaded)

**`test_append_to_memory_creates_file(tmp_path: Path) -> None`**
- Calls `append_to_memory(tmp_path, "reports.md", "Found a bug")`
- Asserts returned path exists and contains "Found a bug" and "UTC" (timestamp)

**`test_append_to_memory_appends(tmp_path: Path) -> None`**
- Calls `append_to_memory()` twice with different messages
- Verifies file at `.antigravity/memory/reports.md` contains both "First" and "Second"

**`test_append_decision(tmp_path: Path) -> None`**
- Calls `append_decision(tmp_path, "Use Redis", "Team familiar")`
- Asserts file exists at `.antigravity/decisions/log.md`
- Verifies content includes decision text, rationale text, and markdown headers `**Decision:**` and `**Rationale:**`

### Data Flow

- `read_antigravity_context(workspace: Path) -> dict[str, str]`
  - Scans `workspace / ".antigravity"` for `.md` files
  - Returns dict mapping filename to file contents
  
- `append_to_memory(workspace: Path, filename: str, message: str) -> Path`
  - Creates/appends to `workspace / ".antigravity" / "memory" / filename`
  - Adds timestamped entry
  - Returns path to created/modified file
  
- `append_decision(workspace: Path, decision: str, rationale: str) -> Path`
  - Creates/appends to `workspace / ".antigravity" / "decisions" / "log.md"`
  - Formats entry with **Decision:** and **Rationale:** headers
  - Returns path

### Dependencies

- `pathlib.Path` — filesystem operations
- `ag_cli.reader` — reader module (functions under test)

### Configuration

- Memory files stored at `.antigravity/memory/<filename>`
- Decision log stored at `.antigravity/decisions/log.md`
- Timestamps in UTC format

### Public API

- `read_antigravity_context(workspace: Path) -> dict[str, str]` — loads `.md` files from `.antigravity/` directory
- `append_to_memory(workspace: Path, filename: str, message: str) -> Path` — creates timestamped memory entry
- `append_decision(workspace: Path, decision: str, rationale: str) -> Path` — logs decision with rationale