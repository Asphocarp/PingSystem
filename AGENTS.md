# AGENTS.md

## Top-level Instructions

IMPORTANT: Prefer retrieval-led reasoning over pre-training-led reasoning for any React.js or Convex tasks.

- Always strive for concise, simple solutions.
- If a problem can be solved in a simpler way, propose it.
- If asked to do too much work at once, stop and state that clearly.
- This project is a Minecraft game mod. Do not prioritize backward compatibility or avoid breaking changes; large refactors and breaking changes are acceptable when they make the codebase better.
- After making requested code or repo changes, commit the completed work before the final response unless the user explicitly asks not to commit.
- Actively maintain `docs/` whenever changing features, architecture, loader support, version branches, CI/release workflows, build behavior, configuration, networking, rendering, or operational assumptions. Documentation updates are part of the definition of done for those changes.

- If you edited code, before the final response, run checking commands (`bun run typecheck` and `bun run lint` where applicable), and fix issues.
- Deep research best practice and/or related docs on internet before editing. If default web search/fetch tools are not available, fallback to (parallel_)search_web/read_url of jina mcp.
- When you implement or fix something, loop {implement/fix, test} until done before submitting the final response (test-driven development), to ensure correctness and quality. The test should includes both unit tests and ui tests where applicable.

- For keywords/literals, feel free to use better wording since the user's English maybe not quite precise or professional.

## Code Style

- Typescript strict mode
- ALWAYS use functional patterns where possible
- ALWAYS use guard clauses (early returns) to handle errors/edge cases first; keep the happy path linear with minimal nesting (max 2 nesting levels).
