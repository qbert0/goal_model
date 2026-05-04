# Verification Usage

## 1. Goal

The plugin supports three distinct checks and they must not be confused.

## 2. Checks

### 2.1 OCL validation

Use:

- menu: `Validate GOAL OCL`
- shortcut: `F6`

Purpose:

- checks whether GOAL-embedded OCL is syntactically and semantically valid against the current USE model

### 2.2 Goal runtime status

Use:

- menu: `Goal Status Table`
- shortcut: `F7`

Purpose:

- evaluates GOAL OCL on the current USE `SystemState`
- reports `TRUE`, `FALSE`, `UNDEFINED`, or `ERROR`

### 2.3 GOAL + BPMN operational verification

Use:

1. load `.goal`
2. load `.bpmn`
3. run `Verify GOAL With BPMN`
4. or use shortcut `F9` from the GOAL diagram

Purpose:

- checks whether BPMN paths operationally support GOAL refinements and tasks
- matches BPMN task names to GOAL task names by exact name

## 3. Simplified BPMN Language

Current syntax:

```text
bpmn SmartDeliveryFlow

process DeliveryProcess {
    start Start
    task ProcessOrder
    task AssignDriver
    task DeliverOrder
    end Done

    flow Start -> ProcessOrder
    flow ProcessOrder -> AssignDriver
    flow AssignDriver -> DeliverOrder
    flow DeliverOrder -> Done
}
```

Supported declarations:

- `start`
- `end`
- `task`
- `xor`
- `and`
- `flow A -> B`

## 4. Interpretation

The current verifier is intentionally conservative.

It proves only structural operational support:

- whether GOAL tasks are covered by BPMN
- whether BPMN contains start-to-end paths
- whether a goal refinement structure can be supported by at least one BPMN path

It is not yet a full theorem prover or full state-space model checker.
