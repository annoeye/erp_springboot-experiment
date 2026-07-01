# Language Adapters Module — Knowledge Document

## Overview

The `language_adapters` group provides a pluggable, language-agnostic semantic analysis system for the Antigravity engine's hub knowledge graph. It normalizes code structure (imports, symbols, metadata) across Python, Go, TypeScript/JavaScript, and unsupported languages into a unified `FileSemantics` representation. This enables consistent dependency tracking, grouping, and semantic queries regardless of source language.

---

## Module Architecture

### Public API (`__init__.py`)

**File:** `antigravity-workspace-template/engine/antigravity_engine/hub/language_adapters/__init__.py`

**Purpose:** Registry and factory for language-specific adapters.

**Key Export:**
- `get_language_adapter(path: Path) -> LanguageAdapter` — Returns the appropriate adapter for a file suffix, or falls back to `GenericLanguageAdapter`.

**Exported Types:**
- `FileSemantics` — Complete normalized semantic record for a source file
- `LanguageAdapter` — Base protocol for analyzer implementations
- `SemanticEdge`, `SignatureSummary`, `SymbolDef` — Shared data models

**Registry Implementation:**
- `_ADAPTERS` tuple holds instances: `PythonLanguageAdapter()`, `GoLanguageAdapter()`, `TypeScriptLanguageAdapter()`
- `_ADAPTER_BY_SUFFIX` dict maps file extensions (`.py`, `.go`, `.ts`, `.tsx`, `.js`, `.jsx`) to adapter instances
- `_GENERIC_ADAPTER` fallback for unknown suffixes

---

## Core Semantic Data Model (`base.py` — referenced, not provided)

The following classes are imported and re-exported; they define the normalized output:

- **`FileSemantics`** — Container for analyzed file metadata:
  - `rel_path: str` — Workspace-relative path
  - `language: str` — Human-readable language name
  - `adapter_name: str` — Adapter identifier (e.g., "python", "go")
  - `package_name: str | None` — Logical package/module grouping
  - `package_identity: str` — Unique module identity (import-style)
  - `module_name: str` — Primary module identifier
  - `provided_modules: list[str]` — Alternative module names this file satisfies
  - `imports: list[str]` — Imported module/package identifiers
  - `symbols: list[SymbolDef]` — Top-level declarations
  - `entrypoints: list[str]` — Entry function/method names
  - `is_test_file: bool` — Test file detection flag
  - `test_targets: list[str]` — Inferred modules under test
  - `signature_summary: str` — Compact markdown summary for splitting context
  - `parse_error: str | None` — Optional parser error message

- **`SymbolDef`** — Represents a top-level declaration:
  - `name: str` — Symbol name
  - `kind: str` — Category: "function", "class", "method", "type", "struct", "interface", "enum", "constant", "variable"
  - `qualified_name: str` — Full name (e.g., "ReceiverType.MethodName" for Go methods)
  - `line: int | None` — 1-based line number
  - `signature: str` — Full declaration text
  - `receiver: str | None` — Go method receiver type
  - `bases: list[str] | None` — Base class/interface names
  - `is_entrypoint: bool` — Whether this is an executable entry point

- **`SemanticEdge`**, **`SignatureSummary`** — Additional shared types (contracts not detailed in provided files)

---

## Language-Specific Adapters

### Generic Adapter (`generic_adapter.py`)

**Purpose:** Graceful fallback for unsupported file types; never raises exceptions.

**Key Methods:**

| Method | Purpose | Returns |
|--------|---------|---------|
| `analyze(workspace, abs_path, rel_path, content)` | Parse unsupported file into minimal semantics | `FileSemantics` |
| `_build_signature_summary(rel_path, language, content)` | Extract first 20 non-empty lines as preview | `str` |
| `_is_test_file(filename)` | Pattern-match test files: `test_*`, `*_test`, `.test.`, `.spec.` | `bool` |

**Behavior:**
- `package_identity` derived from `rel_path` with `/` replaced by `.`
- `provided_modules` empty (no specific imports tracked)
- `language` looked up from `LANG_MAP` constant or defaults to "Unknown"
- Signature summary includes path, language, and file preview

---

### Python Adapter (`python_adapter.py`)

**Purpose:** Parse Python files via stdlib `ast` module for precise symbol extraction.

**Key Methods:**

| Method | Purpose | Parameters | Returns |
|--------|---------|------------|---------|
| `analyze()` | Main entry point; uses `ast.parse()` | workspace, abs_path, rel_path, content | `FileSemantics` |
| `_module_name(rel_path)` | Convert path to import-style name (`.py` stripped, `/` → `.`) | rel_path: str | str |
| `_provided_modules(module_name, rel_path)` | Build alternative module aliases (e.g., strip `src.` prefix) | module_name, rel_path | list[str] |
| `_is_test_file(rel_path)` | Detect test files: `test_*`, `*_test`, `/tests/`, `/test/` | rel_path: str | bool |
| `_has_main_guard(tree)` | Find `if __name__ == '__main__'` | ast.Module | bool |
| `_build_signature_summary()` | Compact markdown with classes, methods, functions, docstrings | rel_path, content, tree | str |
| `_extract_symbols()` via AST walk | Extract top-level classes and functions | content parsed to AST | list[SymbolDef] |

**Symbol Extraction:**
- Classes: kind="class", bases extracted via `ast.unparse()` if available
- Functions: kind="function", marked entrypoint if `main()` with main guard
- Imports: tracked as module names (both `import X` and `from X import Y`)

**Error Handling:**
- On `SyntaxError`, returns `FileSemantics` with `parse_error` set and empty symbols

---

### Go Adapter (`go_adapter.py`)

**Purpose:** Lightweight regex-based Go parser; avoids heavy AST dependencies.

**Key Methods:**

| Method | Purpose | Parameters | Returns |
|--------|---------|------------|---------|
| `analyze()` | Main entry; orchestrates parsing | workspace, abs_path, rel_path, content | `FileSemantics` |
| `_package_name(content)` | Extract `package <name>` declaration | content: str | str \| None |
| `_extract_imports(content)` | Parse single and block imports (`import (...)`) | content: str | list[str] |
| `_extract_symbols(content, package_name)` | Find funcs, methods, type decls | content, package_name | list[SymbolDef] |
| `_package_identity()` | Resolve Go module path from `go.mod`; build import-style identity | workspace, abs_path, rel_path | str |
| `_find_go_module()` | Locate nearest `go.mod` and parse module declaration | workspace, abs_path | tuple[str \| None, Path \| None] (cached) |
| `_receiver_type(receiver)` | Extract type name from method receiver (e.g., `*Type` → `Type`) | receiver: str | str |
| `_collect_declaration()` | Gather multi-line func/type decl until signature closes | lines, start_index | str |
| `_build_signature_summary()` | Organize package, imports, types, functions, methods into sections | rel_path, package_name, ... | str |

**Regex Patterns:**
- `_PACKAGE_RE` — `package <name>`
- `_IMPORT_SINGLE_RE` — `import "path"`
- `_IMPORT_BLOCK_START_RE` — `import (`
- `_FUNC_RE` — `func [receiver] name(...)`
- `_TYPE_RE` — `type name (struct|interface)`
- `_TYPE_FALLBACK_RE` — `type name` (fallback)

**Symbol Detection:**
- Methods: `is_entrypoint = name in {"main", "init"}`
- Entrypoint logic: `init` always, `main` only if package=="main"
- Methods vs. functions distinguished by receiver presence

**Module Identity:**
- If `go.mod` found: `<module_path>/<rel_dir_from_module>`
- Otherwise: `go:<rel_dir>` or file stem

---

### TypeScript/JavaScript Adapter (`typescript_adapter.py`)

**Purpose:** Regex-based ES module and CommonJS parser for TS/JS files.

**Key Methods:**

| Method | Purpose | Parameters | Returns |
|--------|---------|------------|---------|
| `analyze()` | Main entry; orchestrates parsing | workspace, abs_path, rel_path, content | `FileSemantics` |
| `_extract_import_refs()` | Find ES6 imports, dynamic imports, requires, re-exports | content, rel_path | list[_ImportRef] |
| `_extract_symbols()` | Parse top-level classes, functions, types, interfaces, enums, variables | content | list[SymbolDef] |
| `_parse_top_level_symbol()` | Convert declaration string into SymbolDef(s) | declaration, line, has_main_guard | list[SymbolDef] \| None |
| `_module_name()` | Strip module suffixes (`.ts`, `.tsx`, `.js`, `.jsx`) and normalize | rel_path: str | str |
| `_package_name()` | Return parent directory or None | rel_path: str | str \| None |
| `_provided_modules()` | Build import aliases (e.g., `src/foo` → `foo`, `foo/index` → `foo`) | module_name | list[str] |
| `_normalize_import()` | Resolve relative imports to repo-relative module identities | raw, rel_path | str |
| `_is_test_file()` | Detect `.test.`, `.spec.`, `__tests__/` | rel_path: str | bool |
| `_test_targets()` | Infer modules under test from colocated names and relative imports | rel_path, import_refs | list[str] |
| `_collect_declaration()` | Gather declaration up to 12 lines or until `{` or `;` | lines, start_index | str |
| `_strip_modifiers()` | Remove `export`, `declare`, `abstract`, `default` | normalized: str | tuple[str, bool] (subject, exported) |
| `_build_signature_summary()` | Organize imports, types, interfaces, classes, functions, constants | rel_path, module_name, ... | str |
| `_strip_comments()` | Remove `//` and `/* */` while preserving strings | content: str | str |
| `_mask_non_code()` | Replace strings/comments with spaces for depth tracking | content: str | str |
| `_has_main_guard()` | Detect `require.main === module` or `import.meta.main` | content: str | bool |

**Import Regex Patterns:**
- `_IMPORT_FROM_RE` — ES6 `import ... from "..."`
- `_IMPORT_SIDE_EFFECT_RE` — `import "..."`
- `_DYNAMIC_IMPORT_RE` — `import("...")`
- `_REQUIRE_RE` — `require("...")`
- `_EXPORT_FROM_RE` — `export ... from "..."`

**Symbol Kinds:**
- Functions, classes, interfaces, types, enums, constants, variables
- Entrypoint detection: `main()` with main guard

**_ImportRef Dataclass (frozen):**
- `raw: str` — Original import spec
- `normalized: str` — Repo-relative module identity
- `is_relative: bool` — Whether spec starts with `.`

**Test Target Inference:**
- Colocated: `foo.test.ts` → targets `foo`
- `__tests__/foo.test.ts` → targets `src/foo` (guessed from directory structure)
- Explicit: relative imports from test file

**Language Detection:**
- `.ts` → "TypeScript"
- `.tsx` → "TypeScript (React)"
- `.js` → "JavaScript"
- `.jsx` → "JavaScript (React)"

---

## Data Flow

```
File (abs_path, rel_path, content)
    ↓
get_language_adapter(path.suffix)
    ↓
LanguageAdapter.analyze()
    ├─ Extract package/module identity
    ├─ Extract imports (and normalize relative specs)
    ├─ Extract top-level symbols (classes, functions, types, etc.)
    ├─ Detect test files and infer test targets
    ├─ Determine entrypoints (main, init, or main-guarded functions)
    └─ Build signature summary (markdown)
    ↓
FileSemantics (normalized output)
    ↓
[Used by hub for knowledge graph construction, grouping, and dependency analysis]
```

---

## Key Design Patterns

1. **Adapter Pattern** — Language-specific implementations conforming to `LanguageAdapter` protocol; registry-based selection via file suffix.

2. **Fallback Strategy** — `GenericLanguageAdapter` ensures graceful degradation for unsupported languages; never raises.

3. **Regex-Based Parsing** — Go and TypeScript adapters use compiled regex patterns for lightweight, repo-friendly analysis; avoids heavy parser dependencies.

4. **AST Parsing (Python)** — Stdlib `ast.parse()` for precision where available; syntax errors caught and reported.

5. **Normalization** — All adapters normalize relative imports, module names, and symbols into a unified `FileSemantics` schema.

6. **Caching** — Go adapter caches `go.mod` lookups via `@lru_cache(maxsize=64)` on `_find_go_module()`.

---

## Configuration & Dependencies

### External Imports
- **`pathlib.Path`** — File path manipulation (standard library)
- **`ast` (Python only)** — AST parsing for Python files (standard library)
- **`re`** — Regex pattern matching (all adapters; standard library)
- **`functools.lru_cache` (Go only)** — Caching for module lookups
- **`posixpath` (TypeScript only)** — POSIX path normalization for module identity

### Internal Dependencies
- **`antigravity_engine.hub._constants.LANG_MAP`** — Language suffix-to-name mapping (used by `GenericLanguageAdapter`)
- **`antigravity_engine.hub.language_adapters.base`** — Shared data models: `FileSemantics`, `LanguageAdapter`, `SymbolDef`, `SemanticEdge`, `SignatureSummary`

### Constants (Language-Specific)

**Go Adapter:**
- Regex patterns: `_PACKAGE_RE`, `_IMPORT_*_RE`, `_FUNC_RE`, `_TYPE_RE`

**TypeScript Adapter:**
- Regex patterns: `_IMPORT_FROM_RE`, `_REQUIRE_RE`, `_DYNAMIC_IMPORT_RE`, `_EXPORT_FROM_RE`, `_TEST_FILE_RE`
- `_MODULE_SUFFIXES` tuple: `(".tsx", ".ts", ".jsx", ".js", ".mjs", ".cjs")`
- `_LANGUAGE_BY_SUFFIX` dict: maps suffix to display name
- `_IDENTIFIER_RE` pattern: `[A-Za-z_$][A-Za-z0-9_$]*`

**Python Adapter:**
- None (uses stdlib `ast`)

**Generic Adapter:**
- Depends on `LANG_MAP` from constants

---

## Signature Summaries

Each adapter generates a `signature_summary` string (markdown) for hub context splitting:

- **Python:** Class definitions with docstrings, method signatures, function signatures
- **Go:** Package name, imports, entrypoints, types (struct/interface), functions, methods organized by category
- **TypeScript/JavaScript:** Module name, package, imports, entrypoints, types, interfaces, enums, classes, functions, constants, variables organized by category
- **Generic:** File path, language, first 20 non-empty lines

---

## Public API Summary

**Module-level exports** (`__init__.py`):
- `get_language_adapter(path: Path) -> LanguageAdapter` — Main factory function
- `FileSemantics`, `LanguageAdapter`, `SemanticEdge`, `SignatureSummary`, `SymbolDef` — Data model types

**Per-adapter `analyze()` signature:**
```python
def analyze(
    self,
    workspace: Path,
    abs_path: Path,
    rel_path: str,
    content: str,
) -> FileSemantics:
```

All adapters expose `.name` (str) and `.supported_suffixes` (frozenset[str]) class attributes for registration.