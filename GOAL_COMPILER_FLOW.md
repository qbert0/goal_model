# GOAL Compiler Call Tree (From `GOALCompiler.java`)

File gốc bắt đầu:

- `goal/src/main/java/org/vnu/sme/goal/parser/GOALCompiler.java`

---

## 1) Cây gọi hàm từ trên xuống

```text
GOALCompiler.compileSpecification(inName, err, model)
├─ parse input stream bằng ANTLR
│  ├─ new GOALLexer(input)
│  ├─ new GOALParser(tokenStream)
│  └─ root = parser.goalModel()
├─ (optional) dump parse tree
│  └─ root.toStringTree(parser)            [khi -Dgoal.dump.parsetree=true]
├─ build AST
│  └─ ast = GoalModelBuildingVisitor.build(root)
├─ (optional) dump AST
│  └─ GoalAstPrinter.dump(ast)             [khi -Dgoal.dump.ast=true]
├─ chạy semantic pipeline (always run)
│  └─ new GoalSemanticPipelineSkeleton().run(ast, model, err)
│     ├─ createEmptySymbolTable(ast)
│     │  └─ return new GoalSymbolTable(modelName)
│     ├─ declarationPass(ast, table, err)
│     │  └─ GoalSymbolTableBuilder.runDeclarationPass(ast, table)
│     │     ├─ for each ActorDeclCS: registerActor(...)
│     │     │  ├─ map kind ActorCS/AgentCS/RoleCS -> ActorKind
│     │     │  ├─ put ActorSymbol vào table.actorsByName
│     │     │  └─ for each IntentionalElementCS: registerElement(...)
│     │     │     ├─ map kind Goal/Task/Quality/Resource -> ElementKind
│     │     │     ├─ put vào actor.elementTable (local scope)
│     │     │     ├─ put vào elementsByQualifiedName (global qualified index)
│     │     │     └─ collect OutgoingLink -> RelationEntry(raw target token)
│     │     └─ for each RelationCS là DependencyCS: registerDependency(...)
│     │        ├─ create DependencySymbol(raw depender/dependee refs)
│     │        ├─ create dependum ElementSymbol inline (owner ảo)
│     │        └─ put vào table.dependenciesByName
│     ├─ resolutionPass(ast, table, err)
│     │  └─ GoalSymbolTableBuilder.runResolutionPass(ast, table)
│     │     ├─ resolveRelationTargets(table)
│     │     │  └─ for each relation:
│     │     │     ├─ nếu target có dấu "." -> lookup qualified
│     │     │     └─ nếu target thường -> lookup local actor scope
│     │     ├─ resolveDependencyReferences(table)
│     │     │  └─ resolve depender/dependee từ elementsByQualifiedName
│     │     └─ recomputeLeafFlags(table)
│     │        ├─ reset all element.leaf = true
│     │        └─ relation REFINE_AND/REFINE_OR => target.leaf = false
│     ├─ computeDerivedFlags(table, err)
│     │  └─ hiện chỉ log info (leaf đã tính ở resolution pass)
│     ├─ validateSemanticRules(table, model, err)
│     │  ├─ merged = builder.getIssues()   (S1 undeclared, S5 duplicate)
│     │  └─ check thêm S3: dependency depender/dependee phải là leaf
│     └─ printSemanticIssues(merged, err)
│        └─ in lỗi semantic (nếu bật log flag)
└─ in summary compile
   └─ "[GOAL] model=... actors=... dependencies=..."
```

---

## 2) Ý nghĩa từng cụm chính

- **Parse**: biến text `.goal` -> parse tree (`GoalModelContext`).
- **AST build**: parse tree -> object model `GoalModelCS`.
- **Declaration pass (Pass 1)**: thu thập tất cả khai báo vào symbol table.
- **Resolution pass (Pass 2)**: nối các tham chiếu raw (`targetRef`, `depender`, `dependee`) vào symbol thực.
- **Derived flags**: tính thuộc tính suy ra như `leaf`.
- **Validation**: gom lỗi duplicate/undeclared + check leaf cho dependency.

---

## 3) Các file chính trong nhánh gọi

- Entry:
  - `goal/src/main/java/org/vnu/sme/goal/parser/GOALCompiler.java`
- AST:
  - `goal/src/main/java/org/vnu/sme/goal/parser/GoalModelBuildingVisitor.java`
  - `goal/src/main/java/org/vnu/sme/goal/parser/debug/GoalAstPrinter.java`
- Pipeline:
  - `goal/src/main/java/org/vnu/sme/goal/parser/semantic/pipeline/GoalSemanticPipelineSkeleton.java`
- Symbol table build:
  - `goal/src/main/java/org/vnu/sme/goal/parser/semantic/symbols/GoalSymbolTableBuilder.java`
  - `goal/src/main/java/org/vnu/sme/goal/parser/semantic/symbols/GoalSymbolTable.java`
  - `goal/src/main/java/org/vnu/sme/goal/parser/semantic/symbols/ActorSymbol.java`
  - `goal/src/main/java/org/vnu/sme/goal/parser/semantic/symbols/ElementSymbol.java`
  - `goal/src/main/java/org/vnu/sme/goal/parser/semantic/symbols/RelationEntry.java`
  - `goal/src/main/java/org/vnu/sme/goal/parser/semantic/symbols/DependencySymbol.java`
  - `goal/src/main/java/org/vnu/sme/goal/parser/semantic/symbols/SemanticIssue.java`
