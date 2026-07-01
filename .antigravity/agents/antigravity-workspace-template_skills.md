# Agent Repo Init Module Knowledge Document

## Overview

The `init_project.py` module is a portable CLI tool that initializes new projects from the Antigravity template. It handles template copying, environment configuration, git initialization, and generates runtime profile documentation for agent-based workflows. The module supports two initialization modes (quick and full) with configurable MCP, swarm, and sandbox runtime settings.

## File: `antigravity-workspace-template/skills/agent-repo-init/scripts/init_project.py`

**Purpose:** Standalone Python script that scaffolds new projects by copying the template repository, configuring environment variables, and setting up agent runtime profiles.

### Key Functions

#### `_is_within(child: Path, parent: Path) -> bool`
- **Purpose:** Validates path containment to prevent recursive template copies
- **Parameters:** `child` (Path to check), `parent` (parent directory)
- **Returns:** True if child resolves under parent
- **Logic:** Uses `Path.relative_to()` to test containment; catches ValueError if child is not under parent

#### `_count_entries(path: Path) -> int`
- **Purpose:** Recursively counts filesystem entries for reporting
- **Parameters:** `path` (directory to count)
- **Returns:** Total count of files and directories
- **Logic:** Uses `path.rglob("*")` to traverse all descendants

#### `_upsert_env_var(lines: List[str], key: str, value: str) -> List[str]`
- **Purpose:** Updates or appends environment variables in .env file
- **Parameters:** `lines` (existing file lines), `key` (variable name), `value` (new value)
- **Returns:** Updated list of lines
- **Logic:** Uses regex to find existing key assignment (skipping comments), replaces in-place or appends if not found

#### `_configure_env_file(target_path: Path, project_name: str, mode: str, enable_mcp: bool, sandbox_runtime: str) -> None`
- **Purpose:** Creates or updates .env file with configuration
- **Parameters:** Target project path, project name, mode ("quick" or "full"), MCP flag, sandbox runtime mode
- **Returns:** None
- **Logic:** 
  - Copies .env.example to .env if needed
  - In "quick" mode, returns early
  - In "full" mode, upserts MCP_ENABLED, SANDBOX_TYPE, and AGENT_NAME variables

#### `_write_full_mode_files(target_path: Path, mode: str, project_name: str, enable_mcp: bool, enable_swarm: bool, sandbox_runtime: str, init_git: bool) -> None`
- **Purpose:** Generates mission statement and runtime profile documentation for full-mode projects
- **Parameters:** Target path, mode, project name, MCP flag, swarm flag, sandbox runtime, git init flag
- **Returns:** None
- **Creates:** Three files when mode="full":
  - `mission.md` — High-level project mission and runtime configuration
  - `.context/agent_runtime_profile.md` — Agent runtime settings and LLM setup instructions
  - `artifacts/logs/agent_repo_init_report.md` — Initialization report with all configuration

#### `_init_git_repo(target_path: Path) -> None`
- **Purpose:** Initializes a git repository in the target directory
- **Parameters:** `target_path` (project directory)
- **Returns:** None
- **Raises:** OSError if `git init` fails
- **Logic:** Runs `git init` subprocess; captures and re-raises stderr on failure

#### `_build_parser() -> argparse.ArgumentParser`
- **Purpose:** Constructs CLI argument parser
- **Returns:** Configured ArgumentParser
- **Arguments:**
  - `--project-name` (required): Project identifier
  - `--destination-root` (default="."): Parent output directory
  - `--mode` (default="quick"): "quick" or "full"
  - `--enable-mcp`: Boolean flag for MCP support
  - `--disable-swarm`: Boolean flag to disable swarm workflows
  - `--sandbox-runtime` (default="local"): "local" or "microsandbox"
  - `--init-git`: Boolean flag to initialize git
  - `--template-root` (optional): Explicit template path

#### `main() -> int`
- **Purpose:** Main CLI entry point; orchestrates entire initialization workflow
- **Returns:** Exit code (0 on success)
- **Raises:** ValueError (invalid arguments, path conflicts), OSError (filesystem/git failures)
- **Workflow:**
  1. Parse CLI arguments
  2. Validate project name (alphanumeric, dots, underscores, hyphens only)
  3. Resolve template root (from --template-root or default to 3 levels up from script)
  4. Create destination parent directory
  5. Validate target path doesn't exist and isn't inside template
  6. Copy template with ignore patterns (git, cache, venv, artifacts)
  7. Create `artifacts/logs` directory
  8. Configure .env file
  9. Write full-mode documentation if applicable
  10. Initialize git if requested
  11. Print JSON result with next steps

### Data Flow

1. **CLI Arguments** → `_build_parser()` → `main()` validates and routes
2. **Template Copy** → `shutil.copytree()` with ignore patterns → Target directory
3. **Environment Setup** → `_configure_env_file()` → reads/writes `.env`
4. **Profile Generation** → `_write_full_mode_files()` → creates mission.md, runtime profile, report
5. **Git Init** → `_init_git_repo()` subprocess → initialized .git
6. **Output** → JSON report with project path and next steps

### Dependencies

- **Standard Library:**
  - `argparse` — CLI argument parsing
  - `json` — Output serialization
  - `re` — Regex for project name validation and env var pattern matching
  - `shutil` — Template directory copy with ignore patterns
  - `subprocess` — Git initialization
  - `pathlib.Path` — Cross-platform filesystem operations
  - `typing` — Type hints (Dict, List, annotations)

### Design Patterns

- **Utility Functions Pattern:** Decomposed into focused, single-responsibility functions (_is_within, _count_entries, _upsert_env_var, etc.)
- **Ignore Patterns:** Uses `shutil.ignore_patterns()` to exclude non-essential files during copy
- **Lazy Template Root Resolution:** Defaults to script location relative path but allows explicit override
- **Conditional File Generation:** Full-mode behavior gates documentation creation

### Configuration & Constants

- **VALID_MODES:** {"quick", "full"} — Initialization depth
- **VALID_SANDBOX_RUNTIMES:** {"local", "microsandbox"} — Execution environment
- **Ignore Patterns:** `.git`, `.pytest_cache`, `__pycache__`, `venv`, `.venv`, `antigravity_workspace_template_venv`, `agent_memory.json`, `artifacts`, `*.pyc`
- **Environment Variables (full mode only):**
  - `MCP_ENABLED` — Boolean (true/false)
  - `SANDBOX_TYPE` — Runtime mode
  - `AGENT_NAME` — Project name

### Public API

The module exposes:
- **`main() -> int`** — Primary CLI entry point; prints JSON result, exits with status code
- **CLI Interface:** Command-line arguments for project initialization
- **Output Format:** JSON object containing:
  - `project_name` — Project identifier
  - `project_path` — Absolute path to initialized project
  - `mode` — "quick" or "full"
  - `copied_entries` — Count of filesystem entries
  - `next_steps` — List of recommended commands (varies by mode)

### Error Handling

- **ValueError:** Invalid project name format, target path exists, destination inside template, argument validation
- **OSError:** Git initialization failures
- Errors are raised and propagated to caller; script exits with SystemExit