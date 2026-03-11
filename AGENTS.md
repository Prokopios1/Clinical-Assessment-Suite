# Multi-Agent Development Ecosystem

This is a multi-agent development environment. Development tasks are handled by a specialized team of agents, with execution and code generation primarily delegated to the asynchronous `jules` engine.

## The Team

### 1. Systems Architect
Responsible for high-level technical decisions, architectural patterns, and data modeling. Ensures that the system remains modular, scalable, and maintainable.
- Constraints: `.agent/rules/systems_architect.md`

### 2. Senior Developer
Responsible for detailed implementation of logic, algorithms, and UI components. Follows the architecture laid out by the Architect and adheres strictly to project coding standards.
- Constraints: `.agent/rules/senior_developer.md`

### 3. QA Tester
Responsible for monitoring runtime health, analyzing ADB/system logs (specifically for `PERMISSION_DENIED` and `FirebaseError`), and validating the functionality after patches are applied. 
- Constraints: `.agent/rules/qa_tester.md`

### 4. Firebase Administrator
Responsible for backend infrastructure, Firebase configuration, and drafting Firestore security rules to resolve access denials uncovered by the QA Tester.
- Constraints: `.agent/rules/firebase_administrator.md`

---

## 🚀 Global Execution Protocol: The Jules Engine
**CRITICAL INSTRUCTION FOR ALL AGENTS:** 
This environment utilizes `jules` as the primary **Execution Agent**.

Whenever a code implementation, refactoring, large bug fix, or security rule patch is designed and approved by the team:
1. Do not manually apply hundreds of lines of code.
2. Delegate the implementation to Jules by running: `jules new "Your detailed prompt here"`
3. The prompt to Jules must contain the exact files to modify and the specific design pattern or logic to apply.

Jules operates asynchronously. Once Jules completes the task, pull and apply the changes to the workspace using `jules remote pull --session <ID> --apply`.
