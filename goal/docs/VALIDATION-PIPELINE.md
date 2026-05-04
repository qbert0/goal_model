# GOAL Validation Pipeline

## 1. Two Different Pipelines

The plugin now distinguishes two different concerns:

- `runtime evaluation`
- `formal proof / validation`

These must never be confused.

## 2. Runtime Evaluation

This answers:

`Is a goal expression true or false on the current USE system state?`

Pipeline:

1. `GoalDiagramView.showGoalStatusTable`
2. read goal OCL text from `Goal`
3. normalize `self` through `GoalOclService`
4. compile with USE `OCLCompiler`
5. evaluate on the current `MSystemState`
6. derive goal status from goal type

This pipeline is state-based.

## 3. GOAL + BPMN Proof Validation

This answers:

`Does the BPMN process, together with the GOAL task contracts, prove the goal?`

Pipeline:

1. `GoalDiagramView.showBpmnVerificationReport`
2. `GoalBpmnValidator.analyze`
3. `GoalBpmnProofEngine.analyze`
4. enumerate complete BPMN traces for each agent pool
5. map BPMN tasks to GOAL tasks by exact name
6. generate proof obligations
7. discharge obligations with the symbolic proof layer
8. collect goal-level and obligation-level truth values
9. render the proof report in the GOAL diagram

## 4. Proof Obligations

The current proof layer generates these obligation kinds:

- `task-coverage`
- `post-implies-pre`
- `post-implies-goal`
- `post-preserves-goal`
- `post-avoids-goal`

Examples:

- `post(ProcessOrder) => pre(AssignDriver)`
- `post(DeliverOrder) => goal DeliverPackage`
- `post(T) => goalOcl` for `maintain`
- `post(T) => not(goalOcl)` for `avoid`

## 5. Proof Layer

The proof layer is separated into:

- `org.vnu.sme.goal.validator.GoalBpmnProofEngine`
- `org.vnu.sme.goal.validator.proof.ProofObligation`
- `org.vnu.sme.goal.validator.proof.ProofObligationKind`
- `org.vnu.sme.goal.validator.proof.OclEntailmentChecker`

Important:

- this proof pipeline does **not** use runtime evaluation on a concrete state
- it works over `mm.ocl` expressions
- it is conservative: if the checker cannot derive the consequent from the antecedent, the obligation is `FALSE`

## 6. Meaning of TRUE and FALSE

In BPMN verification:

- `TRUE` means all required obligations for that goal were discharged
- `FALSE` means at least one required obligation was not discharged

So `FALSE` should be read as:

`not proved by the current model`

not as:

`observed false in one concrete runtime state`
