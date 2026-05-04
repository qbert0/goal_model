# GOAL Plugin Architecture

## 1. Scope

This document defines the architecture of the `goal` plugin as a multi-language pipeline integrated into USE.

The plugin now treats every supported language through the same conceptual stages:

`source file -> parser -> AST -> metamodel (MM) -> semantic processing -> view / analysis`

This rule applies to:

- GOAL
- OCL embedded in GOAL
- BPMN (simplified process language for operational verification)
- USE models remain provided by USE itself and are consumed as external semantic context

The structure is intentionally aligned with the style used by USE core under:

- `org.tzi.use.parser.use`
- `org.tzi.use.parser.ocl`
- `org.tzi.use.parser.soil`

## 2. Architectural Principles

### 2.1 One language, one pipeline

Each language must expose:

1. a parser/compiler entry point
2. an AST package
3. an MM package
4. a semantic step
5. a view/reporting step

### 2.2 AST is syntax-oriented

AST nodes represent language structure after parsing and before runtime semantics.

AST nodes:

- keep source-oriented structure
- preserve names, clause structure, and declaration shape
- are allowed to keep tokens or source text for diagnostics
- must not be the final runtime semantic model

### 2.3 MM is runtime-oriented

MM nodes are semantic runtime objects used by:

- validators
- analyzers
- views
- operational verification

### 2.4 Semantic processing happens after MM construction

Parsing success alone is not enough.

A language is only considered successfully loaded when:

1. AST was built
2. MM was built from AST
3. semantic checks finished
4. resulting semantics can be shown in the UI

## 3. Language Pipelines

### 3.1 GOAL pipeline

`*.goal`

1. `GOALCompiler`
2. ANTLR parse tree
3. `org.vnu.sme.goal.ast`
4. `org.vnu.sme.goal.mm`
5. `GoalSemanticPipelineSkeleton`, `GoalSemanticAnalyzer`, `OclSemanticAnalyzer`, `GoalOclCompilationValidator`
6. `GoalDiagramView`, status/design/BPMN verification reports

### 3.2 OCL-in-GOAL pipeline

Embedded OCL expressions inside GOAL clauses now follow an explicit internal pipeline:

1. grammar expression subtree from `GOAL.g4`
2. `OclExpressionBuilder`
3. `org.vnu.sme.goal.ast.ocl`
4. `OclModelBuilder`
5. `org.vnu.sme.goal.mm.ocl`
6. semantic checks in `OclSemanticAnalyzer`
7. external compilation/evaluation through USE `OCLCompiler`
8. exposure in validation views and status tables

This means OCL is no longer hidden only inside `mm.ocl`. It now has a visible AST stage with dedicated syntax nodes such as binary, unary, iterator, aggregate, feature-call, and literal expressions.

### 3.3 BPMN pipeline

`*.bpmn`

1. `BpmnCompiler`
2. ANTLR parse from `BPMN.g4`
3. `org.vnu.sme.goal.ast.bpmn`
4. `BpmnModelFactory`
5. `org.vnu.sme.goal.mm.bpmn`
6. semantic / proof validation in `GoalBpmnValidator` and `GoalBpmnProofEngine`
7. verification report through `GoalDiagramView` and plugin actions

## 4. Package Layout

### 4.1 Parser layer

- `org.vnu.sme.goal.parser`
- `org.vnu.sme.goal.bpmn` for BPMN parsing utilities

Responsibilities:

- compile input files
- build AST
- build MM from AST
- coordinate language loading

### 4.2 AST layer

- `org.vnu.sme.goal.ast`
- `org.vnu.sme.goal.ast.ocl`
- `org.vnu.sme.goal.ast.bpmn`

Responsibilities:

- represent parsed declarations and expression structure
- keep language-local structure visible
- feed semantic table building and MM factories

### 4.3 MM layer

- `org.vnu.sme.goal.mm`
- `org.vnu.sme.goal.mm.ocl`
- `org.vnu.sme.goal.mm.bpmn`

Responsibilities:

- runtime semantic objects
- bidirectional model links
- objects consumed by analyzers and views

### 4.4 Semantic / validation layer

- `org.vnu.sme.goal.parser.semantic`
- `org.vnu.sme.goal.parser.semantic.symbols`
- `GoalOclCompilationValidator`
- `org.vnu.sme.goal.validator`

Responsibilities:

- symbol construction
- semantic constraints
- language cross-checks
- proof / validation logic

### 4.5 View layer

- `org.vnu.sme.goal.view`
- `org.vnu.sme.goal.view.nodes`
- `org.vnu.sme.goal.view.edges`

Responsibilities:

- show semantic results
- show validation state
- show design analysis
- show BPMN-based operational support report

## 5. Loading and Verification Flow

### 5.1 GOAL loading

1. `ActionOpenGOAL`
2. `GoalModelForm`
3. `GoalLoader`
4. `GOALCompiler`
5. AST build
6. MM build
7. semantic validation
8. `GoalDiagramView`

### 5.2 BPMN loading

1. `ActionOpenBPMN`
2. `BpmnModelForm`
3. `BpmnLoader`
4. `BpmnCompiler`
5. BPMN AST build
6. BPMN MM build
7. registry update in `GoalViewRegistry`
8. later use by verification action

### 5.3 GOAL + BPMN verification

1. open USE model
2. load GOAL model
3. load BPMN model
4. run `Verify GOAL With BPMN`
5. validator matches BPMN pools to GOAL agents by exact name
6. validator matches BPMN tasks to GOAL tasks inside the same agent
7. proof engine enumerates complete BPMN traces
8. proof engine generates proof obligations such as:
   `post(T1) => pre(T2)`
   `post(Tn) => goalOcl`
   `post(Ti) => not(goalOcl)` for avoid-goals
9. obligations are checked by a symbolic entailment checker over `mm.ocl`
10. report is shown from the active GOAL view

## 6. Current Position of OCL AST

The plugin now exposes OCL AST explicitly in:

- `org.vnu.sme.goal.ast.ocl.OclExpressionCS`
- `org.vnu.sme.goal.ast.ocl.OclVariableDeclarationCS`

The current implementation uses a real syntax AST under `ast.ocl`, and conversion to runtime OCL MM happens only in `OclModelBuilder`.

## 7. Reference Style from USE Core

The plugin should continue to follow USE core conventions:

- AST classes live in parser-side packages
- runtime semantic classes live outside parser-side AST
- semantic checking is explicit and separate from parsing
- views consume semantic models, not raw parse trees

The main reference packages are:

- `org.tzi.use.parser.use`
- `org.tzi.use.parser.ocl`
- `org.tzi.use.parser.soil`

## 8. Concrete Runtime Pipelines

### 8.1 GOAL runtime status table

This pipeline answers:

`Is the current goal expression true or false on the current USE system state?`

Pipeline:

1. `GoalDiagramView.showGoalStatusTable`
2. read goal OCL text from `Goal`
3. `GoalOclService.validateExpression`
4. `GoalOclService.evaluateBooleanExpression`
5. derive goal status from goal type:
   - `achieve`, `maintain`: expression `TRUE` means satisfied
   - `avoid`: expression `FALSE` means satisfied
6. show result in the status table

This is a runtime state evaluation pipeline.

### 8.2 GOAL + BPMN proof validation

This pipeline answers:

`Does the BPMN process operationalize the goal model, and do generated obligations hold?`

Pipeline:

1. `GoalDiagramView.showBpmnVerificationReport`
2. `GoalBpmnValidator.analyze`
3. `GoalBpmnProofEngine.analyze`
4. build BPMN trace set per agent/pool
5. match refinements, sub-goals, and tasks
6. generate proof obligations:
   - task coverage
   - `post(Ti) => pre(Ti+1)`
   - `post(Tn) => goalOcl`
   - `post(Ti) => goalOcl` for maintain-goals
   - `post(Ti) => not(goalOcl)` for avoid-goals
7. check each obligation with the symbolic proof layer in `org.vnu.sme.goal.validator.proof`
8. collect:
   - goal-level `TRUE/FALSE`
   - obligation-level `TRUE/FALSE`
   - trace explanations
9. render result in the GOAL diagram view

This is a proof / validation pipeline, not the same as runtime status evaluation.
It must not depend on a concrete USE runtime state to decide whether a proof obligation is discharged.

## 9. Status

As of the current implementation:

- GOAL has AST, MM, semantic processing, and view
- OCL embedded in GOAL has AST, MM, semantic processing, and validation/evaluation view
- BPMN has AST, MM, loading pipeline, and proof/reporting view

This is the baseline architecture all future language extensions must follow.
