# antigravity-workspace-template_scripts Module Knowledge Document

## Overview

The **main** group contains four utility scripts for the antigravity-workspace-template project. These scripts enforce repository contracts (version alignment, documentation standards, Python requirements), provide demonstration tooling for agent capabilities, and manage GitHub metadata and social preview assets. Together they support quality assurance, developer onboarding, and project visibility.

---

## File: check_repo_contract.py

**Purpose**: Validates repository structure, configuration alignment, and compliance with project standards across multiple dimensions (versions, Python requirements, LLM configuration, positioning, productization, workflows, governance).

### Key Functions

| Function | Parameters | Returns | Purpose |
|----------|-----------|---------|---------|
| `fail(message: str)` | Error message string | None (exits with code 1) | Prints error to stderr and terminates execution |
| `read_text(path: str)` | Relative file path | `str` | Reads UTF-8 file content relative to ROOT |
| `read_json(path: str)` | Relative JSON file path | `dict` | Parses JSON file content |
| `require_contains(path: str, needle: str)` | File path, required substring | None (or fails) | Asserts substring exists in file |
| `require_absent(path: str, pattern: str)` | File path, regex pattern | None (or fails) | Asserts regex pattern does NOT match file |
| `check_plugin_versions()` | None | None | Validates all plugin versions (Claude, Codex, engine) are aligned across `.claude-plugin/plugin.json`, `.claude-plugin/marketplace.json`, `.codex-plugin/plugin.json`, `engine/pyproject.toml`, `engine/antigravity_engine/__init__.py` |
| `check_python_contract()` | None | None | Enforces Python 3.10+ in `engine/pyproject.toml`, `cli/pyproject.toml`, and quick-start docs (en/zh/es) |
| `check_llm_configuration_docs()` | None | None | Ensures OpenAI configuration (OPENAI_BASE_URL, OPENAI_API_KEY) is documented and Gemini/Google references are removed from docs, env files, and code |
| `check_positioning_contract()` | None | None | Validates brand messaging in README.md, PHILOSOPHY.md, mission.md, VERSIONING.md |
| `check_productization_contract()` | None | None | Verifies install scripts mention ag-refresh/ag-ask, Python 3.10, and removes Docker Sandbox references; checks QUICK_START and MCP docs include AG_RETRIEVAL_MODE, AG_ALLOW_MCP settings |
| `check_workflows()` | None | None | Enforces GitHub Actions use checkout@v5, setup-python@v6, tests Python 3.10/3.11/3.12; requires repo-hygiene.yml runs check_repo_contract.py |
| `check_governance_assets()` | None | None | Asserts existence of CONTRIBUTING.md, SECURITY.md, dependabot.yml, and issue templates |
| `main()` | None | None | Orchestrates all checks in sequence and prints success message |

### Constants & Globals

- **ROOT** (line 8): Project root directory, computed as `pathlib.Path(__file__).resolve().parents[1]` — one level up from `scripts/`

### Design Patterns

- **Defensive Programming**: Uses `require_contains()` and `require_absent()` helpers to encode business rules declaratively
- **Contract Testing**: Each check function validates a specific "contract" (versions, Python version, documentation standards)
- **Fail-Fast**: Exits immediately on first violation via `fail()`

### Public API

The module exports via `if __name__ == "__main__": main()`. Intended to be run as:
```
python scripts/check_repo_contract.py
```

### Configuration

No environment variables. Hard-coded paths reference:
- `.claude-plugin/plugin.json`, `.claude-plugin/marketplace.json`, `.codex-plugin/plugin.json`
- `engine/pyproject.toml`, `engine/antigravity_engine/__init__.py`, `engine/.env.example`
- `docs/{en,zh,es}/QUICK_START.md`, `docs/{en,zh,es}/PHILOSOPHY.md`
- `docker-compose.yml`, `commands/ag-setup.md`, `mission.md`, `VERSIONING.md`
- `.github/workflows/test.yml`, `.github/workflows/repo-hygiene.yml`
- `README.md`, `README_CN.md`, `README_ES.md`, `CONTRIBUTING.md`, `SECURITY.md`, `Dockerfile.sandbox`

---

## File: demo_tools.py

**Purpose**: Demonstrates tool invocation both directly and through a GeminiAgent registry, with deterministic execution avoiding external API calls.

### Key Functions

| Function | Parameters | Returns | Purpose |
|----------|-----------|---------|---------|
| `demo_direct_calls()` | None | None (prints to stdout) | Calls example tools directly: calculate_math, get_weather, send_email, web_search, get_stock_price |
| `demo_via_agent_registry()` | None | None (prints to stdout) | Instantiates GeminiAgent, accesses available_tools dict, calls tools via agent.available_tools[name](...) |
| `main()` (implicit via `if __name__`) | None | None | Runs both demo functions sequentially |

### Dependencies

**Internal**:
- `src.tools.example_tool`: Exports `calculate_math`, `get_weather`, `send_email`, `web_search`, `get_stock_price`
- `src.agent`: Exports `GeminiAgent` class

### Data Flow

1. **Direct calls** (line 17–22): Tool functions invoked standalone with example arguments
2. **Registry calls** (line 25–42): GeminiAgent instantiated → `agent.available_tools` dict accessed → tool functions called through dict lookup

### Design Patterns

- **Tool Registry**: Agent exposes `available_tools` dict mapping tool name (str) to callable
- **Deterministic Testing**: Avoids real API calls by calling registered functions directly

### Public API

Runs as CLI script:
```
python3 scripts/demo_tools.py
```

Outputs both direct and agent-registry call results to stdout.

### Configuration

No environment variables. Tool invocations use hard-coded example inputs (e.g., math expressions, city names, stock tickers, email addresses).

---

## File: render-social-preview.sh

**Purpose**: Converts SVG social preview graphic to PNG at GitHub's recommended 1280×640 resolution using svgexport.

### Key Steps

1. **Validation** (line 12–15): Checks that `docs/assets/social-preview.svg` exists
2. **Conversion** (line 17): Invokes `npx -y svgexport "$SVG" "$PNG" 1280:640`
3. **Output** (line 18): Prints file size of generated PNG

### External Dependencies

- **svgexport**: NPM package (installed on-demand via `npx -y`)

### Data Flow

- **Input**: `docs/assets/social-preview.svg`
- **Output**: `docs/assets/social-preview.png` (1280×640 px)

### Configuration

Hard-coded paths:
- SVG source: `docs/assets/social-preview.svg`
- PNG destination: `docs/assets/social-preview.png`
- Dimensions: `1280:640` (GitHub standard)

### Error Handling

- `set -euo pipefail`: Exits on error, undefined variable, or pipe failure
- Explicit check for SVG file existence with error message

---

## File: setup-github-metadata.sh

**Purpose**: Configures GitHub repository metadata (description, homepage, topics) in one command using the gh CLI.

### Key Variables

| Variable | Value | Purpose |
|----------|-------|---------|
| **REPO** | `study8677/antigravity-workspace-template` | Target repository identifier |
| **DESCRIPTION** | Multi-line string | Repository "About" description (100+ chars) |
| **HOMEPAGE** | `https://deepwiki.com/study8677/antigravity-workspace-template` | Homepage URL |
| **TOPICS** | Array of 20 strings | GitHub topic slugs (ordered by discovery value) |

### Key Steps

1. **Edit repo metadata** (line 17–19): Calls `gh repo edit` with description and homepage
2. **Set topics** (line 21–24): Builds JSON payload via `jq`, calls `gh api PUT /repos/.../topics`
3. **Verify** (line 26–28): Prints confirmation URL and manual step reminder

### External Dependencies

- **gh CLI**: GitHub command-line tool (must be authenticated with `repo` scope)
- **jq**: JSON query tool (for payload construction)

### Topics (20 max, ordered)

`ai-agent`, `claude-code`, `codex-cli`, `cursor`, `windsurf`, `mcp`, `mcp-server`, `llm`, `rag`, `code-search`, `developer-tools`, `knowledge-graph`, `multi-agent`, `python`, `openai`, `agents-sdk`, `ide-plugin`, `copilot`, `code-intelligence`, `codebase-qa`

### Data Flow

- **Input**: Hard-coded REPO, DESCRIPTION, HOMEPAGE, TOPICS
- **Output**: GitHub repo metadata updated; manual social preview upload reminder printed

### Error Handling

- `set -euo pipefail`: Exits on error or undefined variable
- Assumes `gh` is authenticated; no runtime check

### Configuration

Manual edits required in script:
- REPO: Target repository
- DESCRIPTION: About text
- HOMEPAGE: URL
- TOPICS: Discovery keywords

---

## Cross-File Dependencies & Integration

| File | Depends On | Purpose |
|------|-----------|---------|
| `check_repo_contract.py` | filesystem only | CI/CD validation (runs in `.github/workflows/repo-hygiene.yml`) |
| `demo_tools.py` | `src.tools`, `src.agent` | Developer demonstration; not production code |
| `render-social-preview.sh` | svgexport (npm) | Asset generation; runs manually or in CI |
| `setup-github-metadata.sh` | gh, jq | One-time setup; runs manually after repo initialization |

---

## Summary Table: Key Responsibilities

| Script | Role | Trigger | Audience |
|--------|------|---------|----------|
| `check_repo_contract.py` | Contract enforcement | CI/CD (repo-hygiene workflow) | Developers, CI |
| `demo_tools.py` | Tool showcase | Manual/onboarding | New developers |
| `render-social-preview.sh` | Asset generation | Manual/pre-release | Release engineers |
| `setup-github-metadata.sh` | Repo configuration | Manual (one-time) | Repository maintainers |