# Multi-Agent Workflow Protocol — 3-File Pattern

<Overview>
3 `.md` files consumed by 2 different AI agents, coordinated via 1 JSON state file on disk.
</Overview>

---

## File Layout

```
.agent/<DOMAIN>/
  <domain>-workflow.md           # File 1 — Workflow
  <domain>-state.json            # File 2 — State (on disk)
  <domain>-test.md               # File 3 — Test
```

---

## File 1 (`<domain>-workflow.md`)

<Critical type="format">This file is a `.md` task prompt consumed by the Workflow AI Agent.</Critical>

<Rule sequence="1">**MUST** start with rules / skills / context if any.</Rule>
<Rule sequence="2">**MUST** describe the problem: input X, expected output Y.</Rule>
<Rule sequence="3">**MUST** provide file paths to relevant source code (service, repository, entity, dto).</Rule>
<Rule sequence="4">**MUST** break the business logic into the smallest possible tasks.</Rule>
<Rule sequence="5">**MUST** loop: pick 1 task → execute → log → if issue → log to issues → pick next task.</Rule>
<Rule sequence="6">**MUST** save log + state to File 2 after ALL tasks complete.</Rule>

<Important>Workflow only executes. It MUST NOT assert, verify, or decide what "correct" means.</Important>

---

## File 2 (`<domain>-state.json`)

<Critical type="format">This is the **sole communication channel** between Workflow and Test agents.</Critical>

<Rule sequence="1">**MUST** use state machine: `IDLE → PROCESSING → DONE → VERIFIED` (happy) or `→ FAILED`.</Rule>
<Rule sequence="2">File 1 **MUST** write: state, input, output, issues, executionLog.</Rule>
<Rule sequence="3">File 3 **MAY** read: state, output, issues. File 3 **MUST** write: testResult, issues (on fail), state = VERIFIED | FAILED.</Rule>
<Rule sequence="4">`issues` list **MUST** be append-only. Never delete or overwrite existing entries.</Rule>

<Important>Both Workflow and Test append to issues. Human reviews issues to decide next action.</Important>

---

## File 3 (`<domain>-test.md`)

<Critical type="format">This file is a `.md` task prompt consumed by the Test AI Agent.</Critical>

<Rule sequence="1">**MUST** open with: wait 40 seconds, then every 10 seconds check the state file.</Rule>
<Rule sequence="2">**MUST** only proceed to test when `state == "DONE"`.</Rule>
<Rule sequence="3">Test specification **MUST** be extremely detailed: exact input X passed, exact output Y expected, every field asserted with concrete values.</Rule>

<Critical type="prohibition">
  <Rule sequence="4">**MUST NOT** read File 1 (workflow). Test must be completely independent.</Rule>
  <Rule sequence="5">**MUST NOT** fix any code. Only investigate and log issues.</Rule>
</Critical>

<Rule sequence="6">**MUST** investigate source code (service, repository, entity — NOT workflow) to find root cause when test fails.</Rule>
<Rule sequence="7">**MUST** write found issues into `issues[]` in File 2. Never modify the codebase.</Rule>
