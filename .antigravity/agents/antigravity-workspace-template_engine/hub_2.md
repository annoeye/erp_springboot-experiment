# antigravity-workspace-template_engine Module Knowledge Document

## Overview

The **hub_2** group contains three core modules that work together to scan, analyze, and map project structure. `scanner.py` performs filesystem traversal and project metadata extraction, `semantic_index.py` coordinates language-aware semantic analysis of source files, and `structure.py` generates Markdown file trees. These modules form the foundation of the antigravity workspace analysis pipeline, providing both quick snapshots and comprehensive project understanding without requiring LLM inference.

---

## File: scanner.py

### Purpose
Pure Python project scanner that performs filesystem traversal and extracts project metadata (languages, frameworks, dependencies, git history) into a `ScanReport`. Serves as the entry point for project analysis.

### Key Classes

#### `ScanReport`
**Location:** Lines 52–76

Dataclass representing complete project scan results.

**Fields:**
- `root: Path` — project root directory
- `languages: dict[str, int]` — language → file count (sorted by frequency)
- `frameworks: list[str]` — detected framework names
- `top_dirs: list[str]` — top-level directories (non-skipped)
- `file_count: int` — total files analyzed
- `has_tests: bool` — test directory detected during walk
- `has_pytest: bool` — pytest markers found
- `has_ci: bool` — CI workflows detected (.github/workflows or .gitlab-ci.yml)
- `has_docker: bool` — Docker files detected
- `readme_snippet: str` — first 10 lines of README
- `config_contents: dict[str, str]` — well-known config file contents (truncated)
- `entry_points: dict[str, str]` — detected entry point files
- `git_summary: str` — git history and contributors
- `walked_file_count: int` — files examined before stopping
- `type_distribution: dict[str, int]` — file type counts (code, documentation, data, media, binary, other)
- `scan_elapsed_seconds: float` — execution time
- `timed_out: bool` — whether scan hit limits
- `scan_stopped_reason: str` — "completed", "timeout", or "max_files_reached"
- `scanned_file_samples: list[str]` — sample of file paths (up to `AG_SCAN_SAMPLE_FILES`)
- `file_metadata: dict[str, dict[str, object]]` — per-file type/size/mime/binary info

### Key Functions

#### `full_scan(root: Path) → ScanReport`
**Location:** Lines 266–322

Performs complete project scan using single `os.walk` pass with in-place directory pruning. Detects test directories, pytest markers, frameworks, and collects file statistics.

**Parameters:**
- `root` — project root directory

**Configuration (environment variables):**
- `AG_SCAN_TIMEOUT_SECONDS` (default 30.0, min 0.0)
- `AG_SCAN_MAX_FILES` (default 5000, min 1)
- `AG_SCAN_SAMPLE_FILES` (default 50, min 1)

**Returns:** `ScanReport` with complete project metadata

#### `quick_scan(root: Path, since_sha: str) → ScanReport`
**Location:** Lines 430–471

Analyzes only files changed since a git commit SHA. Falls back to `full_scan` if git is unavailable.

**Parameters:**
- `root` — project root
- `since_sha` — git commit SHA to diff against

**Returns:** `ScanReport` limited to changed files

#### `detect_modules(root: Path) → list[str]`
**Location:** Lines 329–389

Language-agnostic module detection using directory structure. Auto-splits top-level directories with ≥ `_AUTO_SPLIT_THRESHOLD` code-bearing subdirectories. Supports two-level resolution: when a top-level dir contains exactly one code-bearing child, descends and checks for auto-split there.

**Returns:** Module identifiers (simple names or `"parent_child"` for two-level; includes `WORKSPACE_ROOT_MODULE_ID` if root has source files)

**Configuration:**
- `AG_AUTO_SPLIT_THRESHOLD` (default 6) — minimum subdirectories to trigger auto-split

#### `resolve_module_path(root: Path, module_id: str) → Path`
**Location:** Lines 414–451

Resolves module identifier to filesystem directory. Handles simple modules (`root / module_id`), two-level modules (`"parent_child"` → `root/parent/<inner>/child`), and workspace root.

**Parameters:**
- `root` — project root
- `module_id` — module identifier from `detect_modules`

**Returns:** Absolute path to module directory

#### `extract_git_insights(root: Path) → str`
**Location:** Lines 476–522

Generates Markdown document with git history, module change frequency (3 months), recently modified files, and contributor activity.

**Returns:** Markdown string or empty string if git unavailable

#### `list_root_module_files(root: Path) → list[Path]`
**Location:** Lines 245–262

Returns analyzable source files directly under root (not in subdirectories).

**Parameters:**
- `root` — project root

**Returns:** Sorted list of direct child files with source code extensions

### Helper Functions

| Function | Purpose | Parameters | Returns |
|----------|---------|-----------|---------|
| `_is_venv_dir(path)` | Detect venv by presence of `pyvenv.cfg` | `path: Path` | `bool` |
| `_find_venv_dirs(root)` | Discover venv names up to 2 levels deep | `root: Path` | `set[str]` |
| `_should_skip(path, extra_skip)` | Check if path is in skippable directory | `path: Path`, `extra_skip: set[str] \| None` | `bool` |
| `_classify_file(path)` | Classify file into type (code/documentation/data/media/binary/other) with mime and binary flag | `path: Path` | `tuple[str, str, bool]` |
| `_update_scan_stats(report, rel, item, sample_limit)` | Update file-level counters and metadata | `report, rel: Path, item: Path, sample_limit: int` | `None` |
| `_finalize_scan_report(report, root, venv_dirs)` | Fill derived report fields after scan loop | `report, root: Path, venv_dirs: set[str]` | `None` |
| `_read_file_head(path, max_lines)` | Read first N lines of text file | `path: Path, max_lines: int` | `str \| None` |
| `_read_config_files(root)` | Read well-known config files (pyproject.toml, package.json, etc.) | `root: Path` | `dict[str, str]` |
| `_read_entry_points(root, config_contents)` | Detect entry-point files from config or common patterns | `root: Path, config_contents: dict[str, str]` | `dict[str, str]` |
| `_extract_git_summary(root)` | Extract recent git log and contributors | `root: Path` | `str` |
| `_find_single_code_child(top_dir, venv_dirs, skip)` | Find single primary code-bearing child directory | `top_dir: Path, venv_dirs: set[str], skip: set[str]` | `Path \| None` |
| `_detect_sub_modules_any_lang(parent_dir, parent_name, venv_dirs, skip)` | Detect sub-modules by scanning children for code (language-agnostic) | `parent_dir: Path, parent_name: str, venv_dirs: set[str], skip: set[str]` | `list[str]` |
| `_dir_has_code(directory, venv_dirs)` | Check if directory contains at least one source file | `directory: Path, venv_dirs: set[str]` | `bool` |

### Constants

| Name | Value | Purpose |
|------|-------|---------|
| `_CONFIG_LINE_LIMIT` | 200 | Max lines per config file |
| `_CONFIG_TOTAL_LIMIT` | 30,000 | Total bytes for all config files |
| `_ENTRY_POINT_LINE_LIMIT` | 50 | Max lines per entry point |
| `_DEFAULT_SCAN_TIMEOUT_SECONDS` | 30.0 | Default timeout |
| `_DEFAULT_SCAN_MAX_FILES` | 5000 | Default max files |
| `_DEFAULT_SCAN_SAMPLE_FILES` | 50 | Default sample size |
| `_AUTO_SPLIT_THRESHOLD` | 6 (from env) | Subdir count for auto-split |
| `_MODULE_SKIP_DIRS` | `.git`, `node_modules`, `__pycache__`, etc. | Dirs never treated as modules |
| `_CODE_EXTS` | `SOURCE_CODE_EXTS` | Analyzable extensions |
| `_CONFIG_FILES` | pyproject.toml, package.json, Cargo.toml, etc. | Well-known config files |
| `_COMMON_ENTRY_FILES` | main.py, app.py, index.ts, etc. | Entry point candidates |
| `_NON_MODULE_DIR_NAMES` | tests, test, docs, examples, etc. | Non-primary code dirs |
| `_TEXT_EXTS` | `LANG_MAP ∪ DOCUMENTATION_EXTS ∪ DATA_EXTS ∪ {".env", ".log"}` | Text file extensions |

### Re-Exports (Backward Compatibility)

Lines 24–32 re-export from other modules:
- `build_knowledge_graph` → `antigravity_engine.hub.knowledge_graph`
- `render_knowledge_graph_markdown` → `antigravity_engine.hub.knowledge_graph`
- `render_knowledge_graph_mermaid` → `antigravity_engine.hub.knowledge_graph`
- `extract_structure` → `antigravity_engine.hub.structure`
- `generate_module_context` → `antigravity_engine.hub.structure`

### Data Flow

1. **Entry:** User calls `full_scan(root)` or `quick_scan(root, sha)`
2. **Scan Loop:** `os.walk` prunes skipped dirs in-place, collects file stats
3. **Test/Pytest Detection:** During walk, checks `dirnames` and `filenames` for test patterns
4. **Finalization:** `_finalize_scan_report` derives fields (sorted languages, top dirs, readme, config)
5. **Config/Entry Points:** Read well-known files and entry-point detection
6. **Git Summary:** Subprocess calls to `git log` and `git shortlog`
7. **Module Detection:** `detect_modules` analyzes directory structure for module boundaries

### Dependencies

- `pathlib.Path` — filesystem operations
- `os.walk` — directory traversal
- `subprocess` — git commands
- `json` — package.json parsing
- `tomllib`/`tomli` — pyproject.toml parsing (optional)
- `mimetypes` — MIME type detection
- `dataclasses` — `ScanReport` definition
- `antigravity_engine.hub._constants` — `SKIP_DIRS`, `LANG_MAP`, `SOURCE_CODE_EXTS`, etc.
- `antigravity_engine.hub._utils` — `env_float`, `env_int`
- `antigravity_engine.hub.knowledge_graph` — re-exported functions
- `antigravity_engine.hub.structure` — re-exported functions

### Design Patterns

- **Dataclass for Report:** Immutable result container
- **Lazy Finalization:** Defer derived field computation until after main walk
- **In-Place Pruning:** Modify `dirnames[:]` in `os.walk` to avoid redundant traversals
- **Fallback Chains:** Git unavailable → use full_scan; no tomllib → skip pyproject parsing
- **Language-Agnostic Detection:** No language-specific markers required; check file extensions and directory structure only
- **Two-Level Resolution:** Auto-detect nested package structures (e.g., `engine/antigravity_engine/`)

### Public API

**Main Functions:**
- `full_scan(root) → ScanReport`
- `quick_scan(root, since_sha) → ScanReport`
- `detect_modules(root) → list[str]`
- `resolve_module_path(root, module_id) → Path`
- `extract_git_insights(root) → str`
- `list_root_module_files(root) → list[Path]`

**Re-Exported:**
- `build_knowledge_graph`, `render_knowledge_graph_markdown`, `render_knowledge_graph_mermaid`
- `extract_structure`, `generate_module_context`

---

## File: semantic_index.py

### Purpose
Coordinates language-aware semantic analysis of source files. Provides a unified interface for analyzing individual files and building indexes across the workspace, delegating language-specific work to registered adapters.

### Key Classes

#### `SemanticIndex`
**Location:** Lines 13–27

Pydantic model holding semantic analysis results for a set of source files.

**Fields:**
- `files: list[FileSemantics]` — semantic records for analyzed files (default: empty list)

**Methods:**
- `by_rel_path() → dict[str, FileSemantics]` — index records by relative path

### Key Functions

#### `analyze_source_file(workspace, abs_path, *, rel_path=None, content=None) → FileSemantics`
**Location:** Lines 30–53

Analyzes a single source file using the registered language adapter for its file type.

**Parameters:**
- `workspace: Path` — workspace root directory
- `abs_path: Path` — absolute path to source file
- `rel_path: str | None` — optional workspace-relative path (computed if not provided)
- `content: str | None` — optional pre-read file contents (read if not provided)

**Returns:** Language-neutral `FileSemantics` object

**Raises:** `OSError` if file cannot be read and `content` not provided

#### `build_semantic_index(root, *, candidate_rel_paths=None, max_files=None, skip_dirs=None) → SemanticIndex`
**Location:** Lines 56–85

Analyzes a set of workspace source files into a shared semantic index. File-level failures degrade gracefully.

**Parameters:**
- `root: Path` — workspace root directory
- `candidate_rel_paths: list[str] | None` — explicit file list to analyze (if None, walk entire root)
- `max_files: int | None` — optional hard limit on analyzed files
- `skip_dirs: set[str] | None` — extra directory names to skip during walking

**Returns:** `SemanticIndex` with analyzed files

#### `iter_semantic_candidates(root, *, candidate_rel_paths=None, skip_dirs=None) → list[tuple[Path, str]]`
**Location:** Lines 88–125

Returns source files eligible for semantic analysis, either from explicit list or by walking the root.

**Parameters:**
- `root: Path` — workspace root directory
- `candidate_rel_paths: list[str] | None` — explicit file list to analyze
- `skip_dirs: set[str] | None` — extra directory names to skip

**Returns:** Stable sorted list of `(abs_path, rel_path)` tuples for analyzable files

**Logic:**
- If `candidate_rel_paths` provided, validate and return those files
- Otherwise, walk `root`, skip directories in `SKIP_DIRS ∪ skip_dirs`, collect files with extensions in `SOURCE_CODE_EXTS`
- Return sorted by relative path

### Dependencies

- `pathlib.Path` — filesystem operations
- `os.walk` — directory traversal
- `pydantic.BaseModel`, `pydantic.Field` — data model and validation
- `antigravity_engine.hub._constants` — `SKIP_DIRS`, `SOURCE_CODE_EXTS`
- `antigravity_engine.hub.language_adapters` — `FileSemantics`, `get_language_adapter`

### Design Patterns

- **Pydantic Model:** Type-safe, serializable semantic index
- **Language Adapter Pattern:** Delegate analysis to language-specific adapters via `get_language_adapter`
- **Graceful Degradation:** Individual file failures don't halt indexing
- **Lazy Walking:** Only walk root if explicit file list not provided

### Data Flow

1. **Entry:** User calls `build_semantic_index(root, ...)`
2. **Candidate Collection:** `iter_semantic_candidates` returns list of analyzable files
3. **Per-File Analysis:** Loop calls `analyze_source_file` for each file
4. **Adapter Dispatch:** `analyze_source_file` calls `get_language_adapter(abs_path).analyze(...)`
5. **Error Handling:** `OSError` caught, file skipped, indexing continues
6. **Result:** `SemanticIndex` aggregates all `FileSemantics` records

### Public API

- `SemanticIndex` — data model for semantic analysis results
- `analyze_source_file(workspace, abs_path, *, rel_path=None, content=None) → FileSemantics`
- `build_semantic_index(root, *, candidate_rel_paths=None, max_files=None, skip_dirs=None) → SemanticIndex`
- `iter_semantic_candidates(root, *, candidate_rel_paths=None, skip_dirs=None) → list[tuple[Path, str]]`

---

## File: structure.py

### Purpose
Generates Markdown file trees with line counts for projects and individual modules. Language-agnostic structural overview without AST parsing; detailed code analysis deferred to LLM agents.

### Constants

| Name | Value | Purpose |
|------|-------|---------|
| `_STRUCTURE_LIMIT` | 50,000 | Max character output |

### Key Functions

#### `extract_structure(root: Path) → str`
**Location:** Lines 23–67

Generates a file tree map of the entire project organized by directory, with file counts and line totals.

**Parameters:**
- `root: Path` — project root directory

**Returns:** Markdown string suitable for writing to `.antigravity/structure.md`

**Logic:**
1. Walk `root`, skip directories using `_should_skip` and venvs
2. Collect source files (extensions in `LANG_MAP`) grouped by directory
3. For each directory, count files and lines, format as Markdown
4. Stop output if total exceeds `_STRUCTURE_LIMIT` (50,000 chars)
5. Return header + directory sections

**Output Format:**
```
# Project Structure Map

Auto-generated by `ag refresh`. Do not edit manually.

## (root)/  (X files, Y lines)
- `path/to/file.py` [Python] (123 lines)
...

## dirpath/  (X files, Y lines)
- `path/to/file.ts` [TypeScript] (456 lines)
...
```

#### `generate_module_context(root: Path, module_name: str) → str`
**Location:** Lines 70–114

Generates a file tree for a single module.

**Parameters:**
- `root: Path` — project root directory
- `module_name: str` — name of top-level module directory

**Returns:** Markdown string with module's file tree

**Logic:**
1. Construct `module_path = root / module_name`
2. Walk module, skip directories and venvs
3. Collect source files grouped by directory
4. Format as Markdown with file counts and line totals

**Output Format:**
```
# Module: engine

## dirpath/
- `path/to/file.py` [Python] (123 lines)
...
```

### Dependencies

- `pathlib.Path` — filesystem operations
- `antigravity_engine.hub._constants` — `LANG_MAP`
- `antigravity_engine.hub.scanner` — `_find_venv_dirs`, `_should_skip` (imported locally within functions)

### Design Patterns

- **Language-Agnostic:** Uses extension-based classification only; no AST or regex
- **Deferred Analysis:** Structural overview only; detailed code analysis handled by agents
- **Character Limit:** Truncate output if limit exceeded to prevent oversized documents
- **Grouped Output:** Files grouped by directory for clarity

### Data Flow

1. **Entry:** User calls `extract_structure(root)` or `generate_module_context(root, module)`
2. **Collection:** Walk filesystem, collect source files grouped by directory
3. **Counting:** For each file, read text and count lines; sum per directory
4. **Formatting:** Generate Markdown sections with file paths, language tags, line counts
5. **Limit Check:** Stop if total output exceeds `_STRUCTURE_LIMIT`
6. **Return:** Markdown string

### Public API

- `extract_structure(root: Path) → str` — entire project file tree
- `generate_module_context(root: Path, module_name: str) → str` — single module file tree

---

## Cross-Module Integration

### Data Flow Summary

```
User Request
    ↓
scanner.full_scan(root)  ← Entry point
    ├→ os.walk(root)  [prune venvs, SKIP_DIRS]
    ├→ Collect file stats, detect tests/pytest/frameworks
    ├→ Read config files, entry points, git history
    └→ Return ScanReport
         ├→ Used by: structure.extract_structure, semantic_index.build_semantic_index
         └→ Contains: languages, frameworks, file_count, has_tests, etc.

scanner.detect_modules(root)  ← Module discovery
    ├→ Scan top-level dirs for code
    ├→ Auto-split if ≥ 6 code-bearing subdirs (configurable)
    ├→ Two-level resolution for nested packages
    └→ Return list[str]  [module_id, ...]

scanner.resolve_module_path(module_id)  ← Module resolution
    └→ Map module_id to filesystem path

semantic_index.build_semantic_index(root)  ← Semantic analysis
    ├→ iter_semantic_candidates(root)
    ├→ For each file: analyze_source_file(abs_path)
    │   └→ get_language_adapter(abs_path).analyze(...)
    └→ Return SemanticIndex

structure.extract_structure(root)  ← File tree generation
    ├→ Collect files grouped by directory
    ├→ Count lines per file
    └→ Return Markdown

structure.generate_module_context(root, module)  ← Module file tree
    └→ Return Markdown for single module
```

### Shared Constants

All modules import from `antigravity_engine.hub._constants`:
- `SKIP_DIRS` — always-skipped directories (node_modules, __pycache__, etc.)
- `LANG_MAP` — file extension → language name mapping
- `SOURCE_CODE_EXTS` — extensions counted as source code
- `DOCUMENTATION_EXTS` — documentation extensions (.md, .rst, etc.)
- `DATA_EXTS` — data file extensions
- `MEDIA_EXTS` — image/video extensions
- `FRAMEWORK_MARKERS` — file/dir names indicating framework presence
- `WORKSPACE_ROOT_MODULE_ID` — string constant for workspace root as module

---

## Configuration & Environment Variables

| Variable | Module | Default | Min | Purpose |
|----------|--------|---------|-----|---------|
| `AG_SCAN_TIMEOUT_SECONDS` | scanner | 30.0 | 0.0 | Full scan timeout |
| `AG_SCAN_MAX_FILES` | scanner | 5000 | 1 | Max files to scan |
| `AG_SCAN_SAMPLE_FILES` | scanner | 50 | 1 | File samples in report |
| `AG_AUTO_SPLIT_THRESHOLD` | scanner | 6 | — | Subdir count for auto-split |