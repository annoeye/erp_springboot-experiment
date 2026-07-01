# Antigravity CLI Module – Knowledge Document

## Overview

The **antigravity-workspace-template_cli** module (group: main) is a command-line interface for injecting portable AI repository context into any project directory. It provides commands to initialize Antigravity context files, query projects via LLM, refresh project analysis, log decisions/reports, and clean agent artifacts. The module acts as an entry point for the broader Antigravity cognitive architecture system.

---

## File: `__init__.py`

**Purpose:** Package initialization and version declaration.

**Responsibilities:** Exports the module version string (`1.0.0`).

**Public API:**
- `__version__`: Version string used by other modules and CLI commands.

---

## File: `cli.py`

**Purpose:** Main CLI application definition and command implementations using Typer framework.

**Responsibilities:**
- Scaffold Antigravity context directory structure in target projects
- Delegate to engine for LLM-backed commands (ask, refresh)
- Log reports and architectural decisions to `.antigravity/` memory
- Clean temporary artifacts and logs
- Handle version reporting

### Key Functions

#### `_get_templates_dir() -> Path`
Locates the bundled `templates/` directory using `importlib.resources`. Works in both editable and wheel installations. Returns an absolute `Path` object.

#### `_copy_tree(src: Path, dst: Path, force: bool = False) -> list[str]`
Recursively copies directory tree from `src` to `dst`, preserving dotfiles and skipping `__pycache__`. Returns list of relative paths created. Respects `force` flag to control overwrite behavior.

#### `_run_hub(workspace: Path, *args: str) -> int`
Delegates engine commands (`ask`, `refresh`, `mcp`) to either:
1. Installed script via `shutil.which()` (if available)
2. Local engine module via `subprocess` and `sys.executable`
3. Fails with helpful error if engine unavailable

Returns exit code from delegated process.

### Commands

#### `init_cmd(target_dir: str, force: bool = False)`
- **Arguments:** `target_dir` (required), `--force/-f` (optional)
- **Behavior:**
  - Creates target directory if missing
  - Copies templates from bundled `templates/` directory
  - Scaffolds `artifacts/logs/` directory with `.gitkeep` files
  - Displays success tree and next-steps panel
- **Data Flow:** `_get_templates_dir()` → `_copy_tree()` → rich console output

#### `version_cmd()`
- **Behavior:** Prints current CLI version from `ag_cli.__version__`

#### `ask_cmd(question: str, workspace: str = ".")`
- **Arguments:** `question` (required), `--workspace/-w` (optional)
- **Behavior:** Delegates to `_run_hub()` with `ask` command

#### `refresh_cmd(workspace: str = ".", quick: bool = False, failed_only: bool = False)`
- **Arguments:** `--workspace/-w`, `--quick`, `--failed-only` (optional)
- **Behavior:** Delegates to `_run_hub()` with `refresh` command and optional flags

#### `report_cmd(message: str, workspace: str = ".")`
- **Arguments:** `message` (required), `--workspace/-w` (optional)
- **Behavior:** Calls `append_to_memory()` to log report to `reports.md`

#### `log_decision_cmd(decision: str, rationale: str, workspace: str = ".")`
- **Arguments:** `decision` (required), `rationale` (required), `--workspace/-w` (optional)
- **Behavior:** Calls `append_decision()` to log decision to `.antigravity/decisions/log.md`

#### `clean_cmd(workspace: str = ".", force: bool = False)`
- **Arguments:** `--workspace/-w`, `--force/-f` (optional)
- **Behavior:**
  - Prompts for confirmation (unless `--force` used)
  - Cleans `artifacts/logs/`, `.antigravity/memory/`, and legacy `memory/` directories
  - Preserves `.gitkeep` files
  - Returns count of cleaned items

### Constants & Configuration

- `_REPO_ROOT`: Resolved from file path (3 levels up from `cli.py`)
- `_ENGINE_SCRIPTS`: Maps command names (`ask`, `refresh`, `mcp`) to script executables (`ag-ask`, `ag-refresh`, `ag-mcp`)
- `app`: Typer application instance with:
  - `name="ag"`
  - `rich_markup_mode="rich"` (enables subcommand grouping)
  - `no_args_is_help=True`

### Dependencies

- `typer`: CLI framework
- `rich`: Console formatting (Panel, Tree, Text, Console)
- `importlib.resources`: Package resource discovery
- `shutil`: File/directory operations and path discovery
- `pathlib.Path`: Cross-platform path handling
- `subprocess`: Engine process delegation
- `ag_cli.reader`: Memory/decision logging utilities
- `time`: Spinner delays for UX

### Design Patterns

- **Delegation Pattern:** Engine commands (`ask`, `refresh`) are delegated to subprocess rather than implemented in CLI
- **Template Injection Pattern:** Bundled templates are copied into user projects to bootstrap context
- **Rich Output Pattern:** Uses rich library for styled console output and structured tree display

### Public API

CLI entry point: `ag` command with subcommands:
- `ag init <target_dir> [--force]`
- `ag version`
- `ag ask <question> [--workspace]`
- `ag refresh [--workspace] [--quick] [--failed-only]`
- `ag report <message> [--workspace]`
- `ag log-decision <decision> <rationale> [--workspace]`
- `ag clean [--workspace] [--force]`

---

## File: `reader.py`

**Purpose:** Pure pathlib-based file reader for `.antigravity/` context and memory management. No engine dependency.

**Responsibilities:**
- Read context files from `.antigravity/` directory
- Append timestamped entries to memory files
- Log architectural decisions with rationale

### Key Functions

#### `read_antigravity_context(workspace: Path) -> dict[str, str]`
- **Parameters:** `workspace` (Path to project root)
- **Returns:** Dictionary mapping filename → file content
- **Behavior:** Reads all `.md` files from `.antigravity/` directory; silently skips OSError exceptions
- **Data Flow:** Discovers `.md` files via `glob()`, reads with UTF-8 encoding

#### `append_to_memory(workspace: Path, filename: str, entry: str) -> Path`
- **Parameters:** 
  - `workspace`: Project root
  - `filename`: Target memory file (e.g., `reports.md`)
  - `entry`: Text to append
- **Returns:** Path to memory file
- **Behavior:**
  - Creates `.antigravity/memory/` if missing
  - Appends block with UTC timestamp header and entry text
  - Block format: `\n## YYYY-MM-DD HH:MM:SS UTC\n\n{entry}\n`

#### `append_decision(workspace: Path, decision: str, rationale: str) -> Path`
- **Parameters:**
  - `workspace`: Project root
  - `decision`: Decision summary
  - `rationale`: Why decision was made
- **Returns:** Path to `.antigravity/decisions/log.md`
- **Behavior:**
  - Creates `.antigravity/decisions/` if missing
  - Appends decision block with UTC timestamp
  - Block format: `\n## YYYY-MM-DD HH:MM:SS UTC\n\n**Decision:** {decision}\n\n**Rationale:** {rationale}\n`

### Dependencies

- `pathlib.Path`: Cross-platform path handling
- `datetime`: UTC timestamp generation

### Design Patterns

- **Append-Only Pattern:** Memory and decision logs are append-only with timestamped blocks
- **Stateless Reader Pattern:** No state maintained; all operations are pure path-based

### Public API

- `read_antigravity_context(workspace)` → context dictionary
- `append_to_memory(workspace, filename, entry)` → memory file Path
- `append_decision(workspace, decision, rationale)` → decisions log Path

### Configuration

- Hardcoded directory structure: `.antigravity/memory/` and `.antigravity/decisions/`
- UTC timezone enforced for all timestamps
- UTF-8 encoding for all file I/O