# Tests Group Knowledge Document

## Overview

The **tests** group contains 20 test files (~31,590 tokens) covering the antigravity-workspace-template engine module. Tests span CLI entry points, LLM agent construction, code analysis, graph-based retrieval, sandbox execution, MCP integration, and knowledge graph generation. The test suite validates both Python-specific and polyglot (Go, TypeScript, JavaScript) code analysis, provider failover, content-hash change detection, and project refresh/ask pipelines.

---

## File: conftest.py

**Purpose**: Pytest configuration helper ensuring the engine root is on `sys.path` so tests can import `antigravity_engine` regardless of pytest invocation method.

**Key Functions**:
- Root path calculation: `ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))`
- Conditional sys.path insertion at module load time

**Design Pattern**: Setup fixture (no test functions; runs at collection time)

---

## File: test_agent_repo_init_skill.py

**Purpose**: Tests for the agent-repo-init skill, validating project scaffolding, environment setup, and portable script execution.

**Key Classes/Functions**:
- `_load_skill_module()` → Dynamically loads `antigravity_engine/skills/agent-repo-init/tools.py` using `spec_from_file_location`
- `module.init_agent_repo(project_name, destination_root, mode, enable_mcp, enable_swarm, sandbox_runtime, init_git)` → Initializes a new agent project, returns dict with `project_name`, `project_path`, `mode`, `next_steps`

**Test Coverage**:
- `test_init_agent_repo_creates_clean_project` → Verifies basic scaffolding (no `.git`, no `agent_memory.json`)
- `test_init_agent_repo_full_mode_writes_profile` → Validates `.env` (MCP_ENABLED, SANDBOX_TYPE), runtime profile, report generation
- `test_init_agent_repo_rejects_invalid_project_name` → ValueError on unsupported characters
- `test_init_agent_repo_rejects_destination_inside_template_repo` → Prevents nested initialization
- `test_portable_script_runs_with_template_override` → Tests standalone `scripts/init_project.py` with `--template-root`, validates JSON stdout output

**Data Flow**: CLI args → `init_agent_repo()` → scaffold files → write `.env`, `.context/agent_runtime_profile.md`, `artifacts/logs/agent_repo_init_report.md` → return next steps

**Dependencies**: `pathlib.Path`, `subprocess`, `json`, `types.ModuleType`, `importlib.util`

---

## File: test_ask_fallback_detection.py

**Purpose**: Regression tests for fallback-knowledge detection on the ask path, ensuring degraded docs (from failed LLM analysis) are never silently presented as factual.

**Key Functions**:
- `antigravity_engine.hub.refresh_pipeline._build_agent_md_fallback(module_name, group_id, group)` → Returns markdown with AGENT_MD_FALLBACK_MARKER and AGENT_MD_FALLBACK_SENTINEL
- `antigravity_engine.hub.ask_pipeline._is_fallback_doc(markdown_text)` → Returns True if fallback markers present
- `antigravity_engine.hub.ask_pipeline._prepend_degradation_banner(answer, degraded_modules)` → Prepends blockquote warning if degraded_modules non-empty

**Test Coverage**:
- `test_fallback_doc_is_marked_and_detected` → Validates marker/sentinel insertion and detection
- `test_real_doc_not_detected_as_fallback` → Real docs without markers return False
- `test_legacy_sentinel_only_doc_is_detected` → Backward compat: old sentinel-only docs still detected
- `test_degradation_banner_only_when_degraded` → Banner only prepended when degraded_modules list non-empty

**Constants**: `AGENT_MD_FALLBACK_MARKER`, `AGENT_MD_FALLBACK_SENTINEL` from `antigravity_engine.hub._constants`

---

## File: test_ask_tools.py

**Purpose**: Tests for hub.ask_tools — code exploration tools for the ask swarm (search_code, read_file, list_directory, git_file_history).

**Key Functions**:
- `create_ask_tools(workspace_path)` → Returns dict of tool callables: `search_code`, `read_file`, `list_directory`, `git_file_history`
- `search_code(query, file_pattern=None)` → Searches workspace for pattern, skips node_modules, returns formatted results or "No results"
- `read_file(path, start_line=None, end_line=None)` → Reads file with line-range support, rejects path traversal, returns numbered output
- `list_directory(path)` → Lists dir contents, skips `__pycache__`, rejects traversal
- `git_file_history(path)` → Returns git log or "No git history" message
- `record_retrieval_graph(workspace, tool_name, input_data, output_data)` → Persists retrieval evidence, redacts secrets

**Test Coverage**:
- **search_code**: finds pattern, respects file_pattern, skips node_modules, errors on empty query
- **read_file**: supports line ranges, rejects traversal (`../../etc/passwd`), errors on missing
- **list_directory**: basic listing, skips `__pycache__`, rejects traversal
- **git_file_history**: works with/without git repo
- **retrieval_graph**: writes JSONL nodes/edges (compact mode), .json artifacts (full mode), redacts secrets in both raw values and secret-keyed JSON fields
- `test_search_code_records_retrieval_graph_artifacts` → Validates JSONL storage and secret redaction across compact/full modes
- `test_retrieval_graph_redacts_secret_input_fields` → Secrets in input keys (`api_key`, `password`) redacted even if values opaque

**Dependencies**: `pathlib.Path`, `subprocess` (git), redaction logic via `antigravity_engine.hub.retrieval_graph`

**Public API**: `create_ask_tools(workspace_path) → dict[str, Callable]`

---

## File: test_cli_entry.py

**Purpose**: Tests for engine CLI entrypoint dispatch.

**Key Functions**:
- `antigravity_engine._cli_entry.engine_main(argv=None)` → Dispatches to `ask_main`, `mcp_main` based on subcommand, exits on unknown
- `antigravity_engine._cli_entry.hub_main(argv=None)` → Dispatches to `refresh_main` based on subcommand

**Test Coverage**:
- `test_engine_main_dispatches_ask` → `engine_main(["ask", "Where is auth?"])` calls `ask_main(["Where is auth?"])`
- `test_engine_main_dispatches_mcp` → `engine_main(["mcp", "--workspace", "/tmp/project"])` calls `mcp_main([...])`
- `test_hub_main_dispatches_refresh` → `hub_main(["refresh", "--quick"])` calls `refresh_main(["--quick"])`
- `test_engine_main_rejects_unknown_subcommand` → SystemExit code 2 on unknown command

**Design Pattern**: Dispatcher pattern using argparse

---

## File: test_create_model.py

**Purpose**: Tests for hub.agents.create_model() LLM backend resolution.

**Key Functions**:
- `create_model(settings)` → Returns model identifier string based on LLM configuration
  - Returns raw model name if OPENAI_API_KEY set without OPENAI_BASE_URL
  - Routes through litellm if OPENAI_BASE_URL present (e.g., Ollama, NVIDIA)
  - Format: `litellm/openai/{model}` for custom base URLs
  - Raises ValueError if no LLM configured

**Test Coverage**:
- `test_openai_key_only_returns_model_name` → `"gpt-4o-mini"` (raw model)
- `test_base_url_routes_through_litellm` → `"litellm/openai/gpt-4o-mini"` for Ollama/localhost
- `test_base_url_with_key_routes_through_litellm` → `"litellm/openai/moonshotai/kimi-k2.5"` for NVIDIA
- `test_ag_setup_openai_config_ignores_unrelated_provider_attrs` → Ignores GOOGLE_API_KEY when OPENAI configured
- `test_no_config_raises_value_error` → ValueError: "No LLM configured"

**Dependencies**: Settings object with OPENAI_API_KEY, OPENAI_BASE_URL, OPENAI_MODEL attributes

---

## File: test_execution_tool.py

**Purpose**: Tests for run_python_code execution tool.

**Key Functions**:
- `run_python_code(code, timeout)` → Executes Python string, returns result with exit code

**Test Coverage**:
- `test_run_python_code_success` → `"hi"` in output on success
- `test_run_python_code_error` → "Error (exit_code=" in output on exception

---

## File: test_factory.py

**Purpose**: Tests for sandbox factory (get_sandbox).

**Key Functions**:
- `get_sandbox()` → Returns sandbox instance based on SANDBOX_TYPE env var

**Test Coverage**:
- `test_factory_default_local` → Returns LocalSandbox when SANDBOX_TYPE unset
- `test_factory_microsandbox_resolution` → Returns MicrosandboxSandbox when SANDBOX_TYPE=microsandbox
- `test_factory_legacy_docker_value_falls_back_to_local` → SANDBOX_TYPE=docker → LocalSandbox + stderr warning
- `test_factory_unavailable_runtime_warns_before_local_fallback` → SANDBOX_TYPE=e2b (unavailable) → LocalSandbox + stderr warning

**Dependencies**: `antigravity_engine.sandbox.factory`, `.local.LocalSandbox`, `.microsandbox_exec.MicrosandboxSandbox`

---

## File: test_graph_skills.py

**Purpose**: Tests for graph-retrieval and knowledge-layer skills integration.

**Key Functions**:
- `_load_skill_tools_module(skill_name)` → Dynamically loads `antigravity_engine/skills/{skill_name}/tools.py`
- **graph-retrieval skill**:
  - `query_graph(query, workspace)` → Returns dict with `summary`, `triples` (node-edge-node tuples), `evidence` (retrieval_id + tool_name)
  - Falls back to `knowledge_graph.json` when JSONL files missing
  - Rejects workspaces outside WORKSPACE_PATH root
- **knowledge-layer skill**:
  - `refresh_filesystem(workspace, quick)` → Delegates to async `refresh_pipeline`, reports generated artifacts
  - `ask_filesystem(question, workspace)` → Delegates to async `ask_pipeline`, returns answer string
  - Rejects workspaces outside root

**Test Coverage**:
- `test_query_graph_returns_relevant_subgraph` → Reads nodes.jsonl/edges.jsonl, returns matching triples with evidence
- `test_query_graph_after_refresh_without_retrieval_graph_jsonl` → Falls back to knowledge_graph.json when JSONL missing
- `test_refresh_filesystem_reports_generated_artifacts` → Mocks async refresh_pipeline, validates artifact listing
- `test_ask_filesystem_delegates_to_pipeline` → Mocks async ask_pipeline, validates question passed through
- `test_graph_retrieval_rejects_workspace_outside_root` → ValueError: "workspace must be inside"
- `test_knowledge_layer_rejects_workspace_outside_root` → ValueError: "workspace must be inside"

**Data Format**: Nodes: `{"schema":"antigravity-graph-node-v1","retrieval_id":"r1","tool_name":"search_code","node":{...}}`, Edges: similar

---

## File: test_hub_agents_import.py

**Purpose**: Tests for hub agent ImportError handling, area detection, and reasoning effort passthrough.

**Key Functions**:
- `build_refresh_swarm(model)` → Raises ImportError with "OpenAI Agent SDK not found" if agents module missing
- `build_ask_swarm(model)` → Raises ImportError with "OpenAI Agent SDK not found" if agents module missing
- `_detect_areas(workspace_root)` → Returns list of directories containing real source code (excludes .git, node_modules, docs-only dirs)

**Test Coverage**:
- `test_build_refresh_swarm_import_error` → ImportError when agents module None
- `test_build_ask_swarm_import_error` → ImportError when agents module None
- `test_reasoning_effort_is_passed_through_model_settings_extra_body` → AG_REASONING_EFFORT env var → ModelSettings.extra_body["reasoning_effort"]
- `test_detect_areas_finds_source_dirs` → Finds engine/, cli/, skips docs/ (markdown-only)
- `test_detect_areas_skips_hidden_and_skip_dirs` → Skips .git, node_modules, .hidden

**Constants**: Areas exclude: `.git`, `node_modules`, `__pycache__`, `.hidden`, egg-info

---

## File: test_hub_merkle.py

**Purpose**: Tests for hub._merkle content-hash change detection.

**Key Classes/Functions**:
- `compute_content_hash(text_or_bytes)` → SHA256 hash (deterministic for str/bytes)
- `build_tree(module_dict)` → Constructs MerkleTree from `{module_id: {file: hash}}`
- `build_workspace_tree(workspace_root)` → Walks filesystem, computes content hashes, builds tree
- `diff_trees(prev_tree, cur_tree)` → Returns diff with `added`, `modified`, `removed`, `changed_modules`, `is_empty`
- `save_snapshot(tree, path)` → Writes JSON with version, root, modules
- `load_snapshot(path)` → Reads snapshot, validates version, returns tree or None

**Test Coverage**:
- **hashing**: str/bytes agree, order-independent determinism, hash changes on content change
- **tree building**: root changes on file/module add, module hash stable if unchanged
- **diff**: marks everything added when prev=None, detects modified/added/removed, empty on identical
- **snapshot**: round-trip persistence, version mismatch returns None, bad JSON returns None, missing file returns None
- **workspace**: detects content change, structure unchanged → only changed_modules marked

**Constants**: `SNAPSHOT_VERSION` validated on load

---

## File: test_hub_module_grouping.py

**Purpose**: Tests for hub.module_grouping multi-language loading and semantic grouping.

**Key Functions**:
- `load_module_files(module_path, workspace_root)` → Returns list of SourceFile objects (rel_path, content, language)
  - Workspace-root module: loads only direct files (no recursion)
  - Non-Python modules (Go, Rust, Java, Kotlin): detected and loadable
- `group_files(files, workspace_root, token_budget)` → Groups files by semantic import edges
  - Max MAX_FILES_PER_GROUP per group
  - TS/JS: local import edges keep related files together
  - Go: package peers and imported packages together
  - Returns list of Group objects with `files`, `name`
- `format_group_context(group)` → Formats group for LLM context

**Test Coverage**:
- `test_load_module_files_supports_detected_non_python_modules` → Go/Rust/Java/Kotlin files loadable
- `test_load_module_files_limits_workspace_root_to_direct_files` → Workspace root excludes subdirs
- `test_typescript_grouping_uses_local_import_edges` → Related TS files (imports from "./") group together, unrelated stay separate
- `test_group_files_chunks_all_test_modules` → Test modules split across groups if > MAX_FILES_PER_GROUP
- `test_resolve_module_path_handles_underscore_parent_names` → Module ID "docs_src_additional_responses" → path "docs_src/additional_responses"

**Constants**: `MAX_FILES_PER_GROUP`, `WORKSPACE_ROOT_MODULE_ID`

**Design Pattern**: Semantic grouping via import graph analysis

---

## File: test_hub_pipeline.py

**Purpose**: Tests for hub.pipeline — mocked Runner, refresh/ask pipeline initialization.

**Key Functions**:
- `_format_scan_report(report)` → Formats ScanReport (languages, frameworks, dirs, file count, readme, config, entry points, git summary, CI workflows)
- `_get_head_sha(workspace)` → Returns git HEAD SHA or None
- `_build_ask_context(workspace)` → Aggregates project docs (conventions.md, CONTEXT.md, AGENTS.md, memory/reports.md)
- `_load_project_context(ag_dir, map_content, max_chars)` → Loads project-wide context (conventions + registry + map) with per-source/total budgets
- `_is_retryable_ask_error(exc)` → Classifies transient errors (ServiceUnavailable, Connection, Timeout)
- **async** `refresh_pipeline(workspace, quick)` → Full refresh: writes conventions.md, knowledge_graph.json/.md/.mmd, document_index.md, data_overview.md, media_manifest.md
- **async** `ask_pipeline(workspace, question)` → Answer generation from project context + code tools

**Test Coverage**:
- `test_format_scan_report_basic` → Python/JavaScript counts, pyproject.toml, src/tests, readme snippet
- `test_format_scan_report_empty` → Empty dir → "Total files: 0"
- `test_get_head_sha_no_git` → Returns None in non-git dir
- `test_refresh_initializes_antigravity_scaffold` → Creates .antigravity/ with agents, modules, graph, retrieval_graphs, memory, decisions, logs subdirs; idempotent
- `test_refresh_initialization_refuses_blocking_file` → RuntimeError if .antigravity exists as file (not dir)
- `test_refresh_pipeline_creates_conventions` → Mocked Runner.run, validates conventions.md + knowledge artifacts
- `test_ask_pipeline_returns_answer` → Mocked Runner.run, validates answer string returned
- `test_build_ask_context_includes_root_and_memory_docs` → Aggregates conventions + CONTEXT.md + AGENTS.md + memory/* files
- `test_load_project_context_includes_conventions_and_registry` → Surfaces conventions.md + module_registry.md + module map
- `test_load_project_context_returns_empty_when_no_sources` → Empty string if no conventions/map/registry
- `test_ask_tools_can_be_wrapped_for_answer_agent` → `_wrap_tools(tools)` produces FunctionTool objects for SDK
- `test_load_project_context_respects_total_budget` → Per-source cap + total cap enforced
- `test_ask_retry_classifier_handles_litellm_service_unavailable` → ServiceUnavailableError classified as retryable
- `test_format_scan_report_includes_config` → Config file contents in report
- `test_format_scan_report_includes_entry_points` → Entry point code snippets in report
- `test_format_scan_report_includes_git` → Git commit history in report
- `test_build_module_registry_entries_humanizes_workspace_root` → Workspace-root module → "__workspace_root__" display name

**Architecture**: Mocked agents.Runner.run, async pipeline coordination

---

## File: test_hub_providers.py

**Purpose**: Tests for hub._providers multi-provider LLM failover.

**Key Classes/Functions**:
- `ProviderConfig(model, base_url, api_key, label)` → Provider configuration
- `get_provider_chain(settings)` → Parses AG_LLM_FALLBACKS env (JSON array), inherits primary settings for fallbacks
- `is_retryable_provider_error(exc)` → True for transient (ServiceUnavailable, Connection, Timeout)
- **async** `run_with_provider_failover(operation, providers, label)` → Executes operation across provider chain, retries transient errors

**Test Coverage**:
- `test_is_retryable_provider_error_true` → ServiceUnavailable, HTTP 503, Connection refused, TimeoutError → True
- `test_is_retryable_provider_error_false` → ValueError, KeyError, RuntimeError → False
- `test_get_provider_chain_without_fallbacks_is_single` → Single primary provider
- `test_get_provider_chain_parses_and_inherits` → AG_LLM_FALLBACKS JSON parsed, inheritance of api_key/model/label
- `test_get_provider_chain_degrades_on_bad_json` → Falls back to single provider
- `test_failover_single_provider_is_passthrough` → No env activation, runs once
- `test_failover_switches_provider_on_transient_error` → Tries primary, fails, activates backup, succeeds
- `test_failover_does_not_retry_non_transient_error` → ValueError raised immediately, no failover

**Data Flow**: Settings → parse AG_LLM_FALLBACKS → build chain → execute with transient retry + provider switch

---

## File: test_hub_scanner.py

**Purpose**: Tests for hub.scanner — pure Python code analysis (no LLM).

**Key Classes/Functions**:
- `ScanReport(root, file_count, languages, frameworks, top_dirs, has_tests, has_ci, has_docker, readme_snippet, config_contents, entry_points, git_summary, ...)`
- `full_scan(workspace)` → Walks filesystem, detects languages, frameworks, tests, CI, docker, readme, configs, entry points, git history
- `quick_scan(workspace, sha)` → Attempts git-based scan, falls back to full_scan
- `detect_modules(workspace)` → Returns list of module IDs (dirs with source files + WORKSPACE_ROOT_MODULE_ID)
- `resolve_module_path(workspace, module_id)` → Maps module ID to filesystem path
- `extract_structure(workspace)` → Generates text summary of project structure (nested dirs, line counts, language labels)

**Test Coverage**:
- `test_full_scan_empty_dir` → Empty ScanReport
- `test_full_scan_detects_languages` → Python, JavaScript, CSS → languages dict
- `test_full_scan_detects_frameworks` → pyproject.toml, Dockerfile detected
- `test_full_scan_detects_tests` → tests/ directory → has_tests=True
- `test_full_scan_reads_readme` → README.md snippet extracted
- `test_full_scan_skips_node_modules` → node_modules excluded from count
- `test_full_scan_top_dirs` → src, tests included; .hidden excluded
- `test_full_scan_skips_custom_venv` → pyvenv.cfg marker detected, dir excluded
- `test_full_scan_excludes_egg_info_from_top_dirs` → .egg-info skipped
- `test_full_scan_detects_nested_tests` → engine/tests/ detected
- `test_full_scan_detects_pytest` → conftest.py/pytest.ini → has_pytest=True
- `test_quick_scan_falls_back_to_full` → Git failure → full_scan fallback
- `test_detect_modules_finds_go_directories` → cmd/, internal/ detected
- `test_detect_modules_adds_workspace_root_module_for_root_code` → Root-level .go files → WORKSPACE_ROOT_MODULE_ID
- `test_detect_modules_combines_dirs_and_workspace_root` → Both top-level modules + root files possible
- `test_full_scan_reads_config_files` → pyproject.toml, package.json parsed
- `test_full_scan_config_truncates_long_files` → Lines capped at 200
- `test_full_scan_reads_ci_workflows` → .github/workflows/*.yml read
- `test_full_scan_detects_entry_points_from_pyproject` → [project.scripts] parsed
- `test_full_scan_detects_common_entry_files` → main.py, app.py detected
- `test_full_scan_git_summary_no_git` → Empty string if not git repo
- `test_full_scan_git_summary_with_repo` → Commit history included
- `test_extract_structure_*` → Python/JS/Go files with line counts, nested dirs, node_modules skipped

**Constants**: Skip dirs: `node_modules`, `__pycache__`, `.git`, `.hidden`, `.egg-info`, venvs (pyvenv.cfg marker)

**Design Pattern**: Walk + detection rules (language patterns, framework config files, marker files)

---

## File: test_hub_semantic_graph.py

**Purpose**: Tests for shared semantic graph generation across Python, Go, TypeScript/JavaScript, and fallback adapters.

**Key Functions**:
- `analyze_source_file(workspace, file_path)` → Returns SemanticAnalysis with language, adapter_name, imports, symbols, package_identity, entrypoints, test_targets, signature_summary, is_test_file
- `build_knowledge_graph(workspace, scan_report)` → Constructs full knowledge graph with nodes (files, modules, symbols), edges (imports, defines, calls, tests, entrypoints)
- `group_files(files, workspace, token_budget)` → Semantic grouping via import edges

**Adapters**:
- **Python**: Imports from ast parse, definitions via ast walk, fallback to generic regex
- **Go**: Imports from AST, package identity from go.mod, struct/interface/function symbols, test files (suffix _test.go), entrypoints (init, main)
- **TypeScript/JavaScript**: Imports (ES6 static, dynamic, require, require.resolve, export..from), symbols (interface, type, enum, class, function, const, variable), test files (*.test.{ts,tsx,js,jsx})
- **Generic fallback**: Regex-based imports, function/class/def detection

**Test Coverage**:
- `test_python_semantic_graph_preserves_imports_and_definitions` → os, typing imports + Worker/run symbols
- `test_go_adapter_extracts_package_imports_symbols_entrypoints_and_tests` → Package identity from go.mod, imports (stdlib + package imports), struct/interface/function symbols, methods with signature summary, test file markers (package service_test), test targets
- `test_typescript_adapter_extracts_imports_symbols_and_tests` → React/TS imports, interface/type/enum/function/class/const/variable symbols, signature summary with headers, test file detection + test targets
- `test_javascript_adapter_extracts_commonjs_and_export_symbols` → require/export syntax, symbols extracted
- `test_typescript_adapter_ignores_import_like_text_inside_strings` → String literals not parsed as imports
- `test_go_knowledge_graph_contains_semantic_edges` → declares_package, imports, defines edges in graph
- `test_typescript_knowledge_graph_contains_adapter_summary_and_import_edges` → Adapter diagnostics, import edges
- `test_go_module_grouping_uses_semantic_package_and_import_signals` → Package peers (same package) + imported packages grouped together
- `test_mixed_language_workspace_builds_stable_semantic_graph` → Python + Go coexist with stable output
- `test_unsupported_language_semantics_degrade_gracefully` → Scala files → generic adapter (Unknown language), grouping continues
- `test_realistic_go_refresh_pipeline_emits_semantic_diagnostics` → Full refresh, validates semantic_files/edges, adapter counts, node metadata (semantic_package_identity)
- `test_mixed_language_refresh_pipeline_normalizes_nested_go_modules` → Nested Go module (goapp/go.mod) identities normalized correctly, imports resolved
- `test_generic_fallback_files_appear_in_graph_diagnostics` → Unsupported files flagged in diagnostics, generic_fallback=True on nodes

**Data Structure**: Nodes: `{id, type, label, language, semantic_adapter, semantic_package_identity, generic_fallback}`, Edges: `{from, to, type}`

**Edge Types**: imports, defines, declares_package, entrypoint, calls, tests

---

## File: test_install_engine_hook.py

**Purpose**: Tests for Claude plugin engine install hook (pipx/pip management).

**Key Functions**:
- `read_project_version(engine_dir)` → Parses version from engine_dir/pyproject.toml
- `get_installed_engine_version()` → Runs `ag-mcp --version`, parses version string
- `needs_engine_install_or_upgrade(installed_version, project_version)` → True if either None or versions differ
- `main()` → Orchestrates install/upgrade: ensures pipx, checks versions, runs pipx install/upgrade or pip fallback

**Test Coverage**:
- `test_read_project_version` → Parses [project] version from pyproject.toml
- `test_get_installed_engine_version_parses_ag_mcp_version` → Mocks subprocess, extracts version from "ag-mcp 0.2.1\n"
- `test_needs_engine_install_or_upgrade` → 0.2.0 vs 0.2.1 = True, matching = False, None cases
- `test_existing_old_ag_mcp_triggers_pipx_upgrade` → pipx install --force called for old version
- `test_missing_ag_mcp_uses_first_install_path` → First install: pipx ensurepath, then install
- `test_existing_matching_ag_mcp_skips_install` → No upgrade if versions match
- `test_pipx_unavailable_falls_back_to_pip_user_upgrade` → pip install --user --upgrade if pipx unavailable
- `test_pipx_unavailable_pip_failure_returns_nonzero_and_prints_manual_upgrade` → Error message with manual command on pip failure

**Environment**: CLAUDE_PLUGIN_ROOT env var

---

## File: test_local_sandbox.py

**Purpose**: Tests for LocalSandbox execution model.

**Key Functions**:
- `LocalSandbox.execute(code, timeout)` → Executes Python code in temp dir, returns SandboxResult with exit_code, stdout, stderr, duration, meta

**Test Coverage**:
- `test_success_execution` → exit_code=0, stdout captured, meta["runtime"]="local"
- `test_timeout_enforcement` → Infinite loop × 1s timeout → exit_code=-1, "timed out" in stderr
- `test_non_zero_exit_code` → Exception raises → non-zero exit, ValueError in stderr, duration measured
- `test_output_truncation` → SANDBOX_MAX_OUTPUT_KB env var enforced, meta["truncated"]=True
- `test_stderr_capture` → sys.stderr.write() captured
- `test_working_dir_isolation` → Temp dir cwd, no project-root pollution

**Configuration**: SANDBOX_MAX_OUTPUT_KB (default unlimited)

---

## File: test_mcp.py

**Purpose**: Tests for MCP (Model Context Protocol) integration, configuration loading, and tool management.

**Key Classes/Functions**:
- `MCPServerConfig(name, transport, command, args, url, env, enabled)` → Configuration dataclass
- `MCPClientManager(config_path)` → Manages MCP connections
  - `_load_server_configs()` → Parses JSON config, filters by enabled flag
  - `_