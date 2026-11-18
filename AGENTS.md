# SPEC-Driven Development 约束文档
- 参考 Notion Page ID: 37d0ec7a4b9280bd8c84c922f57f6d3e

# 全局研发环境与行为准则
- 参考 Notion Page ID: 37d0ec7a4b928005aee8d863af5b2518

# 开源车联网研发与行为准则
- 参考 Notion Page ID: 37d0ec7a4b92802d8915c7c310e24486

# 当前项目需求文档
- 参考 Notion Page ID: 4170ec7a4b9280f485a3cce2ccab8eae
- 变更记录 Notion Page ID: 4170ec7a4b928078b413f0f81b0d1d6d

# 当前项目设计文档
- 参考 Notion Page ID: 4170ec7a4b9280ac9347de777d69fbdc
- 变更记录 Notion Page ID: 4170ec7a4b928035a0f5f850d64f928f

# 当前项目系统架构文档
- 参考 Notion Page ID: 4170ec7a4b9280189720e3b40541e44d

# 当前项目功能架构文档
- 参考 Notion Page ID: 4170ec7a4b92805291dacd87fadc009c

# 当前项目数据库文档
- 参考 Notion Page ID: 4170ec7a4b9280e3aa01e4877dca6719

# graphify

This project has a graphify knowledge graph at graphify-out/.

Rules:
- Before answering architecture or codebase questions, read graphify-out/GRAPH_REPORT.md for god nodes and community structure
- If graphify-out/wiki/index.md exists, navigate it instead of reading raw files
- For cross-module "how does X relate to Y" questions, prefer `graphify query "<question>"`, `graphify path "<A>" "<B>"`, or `graphify explain "<concept>"` over grep — these traverse the graph's EXTRACTED + INFERRED edges instead of scanning files
- After modifying code files in this session, run `graphify update .` to keep the graph current (AST-only, no API cost)
