# Kien truc Plugin GOAL trong USE

## 1. Muc tieu tai lieu

Tai lieu nay mo ta kien truc cua plugin `goal` trong du an USE, tap trung vao:

- luong xu ly tu file DSL `.goal` den mo hinh runtime;
- vai tro cua package `parser`, `ast`, `mm`, `mm/ocl`, `view`, `gui`;
- trach nhiem cua cac file trong `parser` va `gui`;
- cau truc meta-model cua `mm`, `mm/ocl`, va cach `view` chieu mo hinh ra giao dien;
- vai tro cua ma duoc sinh trong `target/generated-sources/antlr4`.

## 2. Tong quan luong xu ly

Plugin di theo mot pipeline ro rang:

1. Nguoi dung kich action trong USE.
2. `ActionOpenGOAL` mo form nap file.
3. `GoalModelForm` cho chon file `.goal`.
4. `GoalLoader` goi compiler va sau do mo diagram view.
5. `GOALCompiler` dung ANTLR de parse file.
6. `GoalAstBuilder` chuyen parse tree thanh AST trong package `ast`.
7. `OclExpressionBuilder` chuyen cac bieu thuc OCL-like thanh cay expression trong `mm/ocl`.
8. `GoalModelFactory` chuyen AST thanh semantic model trong `mm` va resolve tham chieu.
9. `GoalDiagramView` dung `mm` de ve node/edge va goi `OclUseValidator` de doi chieu voi USE model.

Co the tom tat bang so do:

```text
.goal
  -> ANTLR lexer/parser
  -> parse tree
  -> ast
  -> mm + mm/ocl
  -> view/gui
```

## 3. Entry points cua plugin

### `Main.java`

- La diem vao theo giao dien `IPlugin`.
- Hien tai chi khai bao ten plugin.
- `run(...)` dang de trong, nghia la plugin hien duoc kich hoat chu yeu qua action khai bao trong `useplugin.xml`.

### `useplugin.xml`

- Dang ky action `GoalModel Compiler`.
- Gan action voi class `org.vnu.sme.goal.actions.ActionOpenGOAL`.
- Day la cau hinh de USE dua plugin vao menu va toolbar.

### `ActionOpenGOAL.java`

- Lay `Session` va `MainWindow` tu moi truong USE.
- Tao `GoalModelForm` de nguoi dung chon file GOAL.

## 4. Package `parser`

Package nay la trung tam cua qua trinh bien dich DSL. No khong chi parse cu phap, ma con ket noi parse tree, AST, OCL AST, va semantic model.

### Trach nhiem tong quan

- goi ANTLR lexer/parser;
- xay parse tree;
- chuyen parse tree sang AST;
- chuyen OCL subtree sang expression model;
- chuyen AST sang semantic model;
- bao loi syntax va loi build model.

### `GOALCompiler.java`

Vai tro:

- Facade cho toan bo qua trinh compile file `.goal`.
- Tao `GOALLexer`, `GOALParser`, parse rule goc `goalModel`.
- Gan `BaseErrorListener` de bien syntax error thanh exception de xu ly tap trung.
- Sau khi parse xong, goi `GoalAstBuilder` va `GoalModelFactory`.

Trach nhiem chinh:

- doc file;
- parse;
- bao loi;
- tra ve `GoalModel`.

Day la diem vao logic parser ma `GoalLoader` su dung.

### `GoalAstBuilder.java`

Vai tro:

- Visitor di qua parse tree cua `GOALParser`.
- Chuyen `ParserRuleContext` sang AST trong package `ast`.

No lam gi:

- tao `GoalModelCS`;
- tao `ActorCS`, `AgentCS`, `RoleCS`;
- tao `GoalCS`, `TaskCS`, `QualityCS`, `ResourceCS`;
- tao `DependencyCS`;
- thu thap relation inline nhu `&>`, `|>`, `++>`, `+>`, `->`, `-->`, `=>`, `<>`;
- goi `OclExpressionBuilder` cho:
  - `achieve`;
  - `maintain`;
  - `avoid`;
  - `pre`;
  - `post`.

Dieu quan trong:

- File nay chua resolve tham chieu thanh doi tuong that.
- No moi chi chuyen cu phap thanh AST o muc "da du thong tin de hieu file".

### `OclExpressionBuilder.java`

Vai tro:

- Chuyen subtree `expression` trong grammar `GOAL.g4` thanh cay expression trong `org.vnu.sme.goal.mm.ocl`.

No xu ly:

- logic operators: `implies`, `or`, `and`;
- relational operators: `=`, `<`, `<=`, `>`, `>=`;
- arithmetic operators: `+`, `-`, `*`, `/`;
- unary operators: `not`, unary `-`;
- `self`, `@pre`;
- literal: string, int, real, boolean, null, enum literal;
- path/property call;
- operation call;
- iterator call: `exists`, `forAll`, `collect`, `select`, `reject`, `any`, `one`, `isUnique`, `sortedBy`, `closure`.

No khong validate voi USE model. Viec validate do `OclUseValidator` ben package `view` dam nhiem.

### `GoalModelFactory.java`

Day la file semantic quan trong nhat trong package `parser`.

Vai tro:

- Nhan AST trong package `ast`.
- Tao semantic model trong package `mm`.
- Resolve tham chieu bang ten thanh lien ket doi tuong thuc.

No thuc hien 3 viec lon:

1. Tao actor va intentional elements tu AST.
2. Thu thap cac relation tam thoi thanh `PendingRelation`.
3. Materialize relation thanh cau truc domain that:
   - refinement;
   - contribution;
   - qualification;
   - needed-by;
   - dependency.

Chi tiet:

- `createActor(...)` map `ActorCS`, `AgentCS`, `RoleCS` sang `Actor`, `Agent`, `Role`.
- `createElement(...)` map `GoalCS`, `TaskCS`, `QualityCS`, `ResourceCS` sang runtime object.
- Goal clause duoc doi thanh `Achieve`, `Maintain`, `Avoid`.
- Task clause duoc doi thanh `Pre`, `Post`.
- Relation duoc resolve sau khi toan bo element da duoc tao, tranh loi forward reference.

Y nghia kien truc:

- `GoalAstBuilder` tao AST.
- `OclExpressionBuilder` tao expression tree cho AST.
- `GoalModelFactory` la "nha may" kiem soat viec tao domain object va noi ket giua chung.

Do do, neu can mo rong DSL, day la file trung tam de bo sung semantic moi.

### `GoalLoader.java`

Vai tro:

- Lop glue giua parser va giao dien.
- Goi `GOALCompiler.compileSpecification(...)`.
- Neu compile thanh cong thi tao `GoalDiagramView` va mo trong `ViewFrame`.

No dung `session.system().model()` de:

- dua USE model vao view;
- cho phep validator OCL doi chieu goal model voi system model hien tai.

## 5. Package `gui`

Package nay chiu trach nhiem giao dien nap file, validate input, va thong bao ket qua cho nguoi dung.

### `ModelForm.java`

- Interface mo ta contract chung cho form nap model.
- Dinh nghia cac thao tac:
  - lay/set file;
  - lay/set model name;
  - parse;
  - close;
  - validate;
  - hien thi loi/thanh cong.

### `ModelFormAbs.java`

- Abstract class dung chung cho form.
- Giu:
  - `Session`;
  - `MainWindow`;
  - `PrintWriter`;
  - `selectedFile`;
  - `modelName`;
  - text field hien thi duong dan file.

No chiu trach nhiem:

- validate file null/khong ton tai/rong;
- validate ten model;
- hien thi dialog loi va dialog thanh cong.

### `GoalModelForm.java`

- Form cu the de nap file `.goal`.
- Tao `JFileChooser` voi filter `.goal`.
- Xu ly nut chon file, parse, close.
- Goi `GoalLoader` de thuc thi luong load model.

Luu y:

- Form nay hien tai van goi `showParseSuccess()` sau `loader.run()` ma khong dua ket qua `success` de quyet dinh. Neu can chat che hon, cho nay co the duoc sua.

## 6. Package `ast`

Package `ast` la tang trung gian giua parse tree va semantic model.

### Trach nhiem

- luu thong tin o muc abstract syntax;
- giam phu thuoc vao parse tree ANTLR;
- giu token ten, description, relation refs, goal/task clauses;
- chuan bi du lieu cho `GoalModelFactory`.

### Dac diem

- chua co reference object that su;
- chua co domain semantics day du;
- phu hop cho buoc chuyen doi tiep theo, khong phai cho view.

### Cay AST chinh

```text
DescriptionContainerCS
|- GoalModelCS
|- ActorDeclCS
|  |- ActorCS
|  |- AgentCS
|  `- RoleCS
|- IntentionalElementCS
|  |- GoalCS
|  |- TaskCS
|  |- QualityCS
|  `- ResourceCS
`- DependencyCS
```

### So luong nhom bieu thuc OCL duoc bieu dien tu AST

Plugin hien bieu dien OCL trong 5 loai clause:

- goal: `achieve`
- goal: `maintain`
- goal: `avoid`
- task: `pre`
- task: `post`

Tat ca cac clause tren deu duoc dua ve cay expression trong `mm/ocl`.

## 7. Package `mm`

Package `mm` la semantic domain model cua plugin. Moi thao tac hien thi, phan tich, va relation traversal deu dua vao day.

### Trach nhiem

- bieu dien actor, intentional element, relation, clause;
- giu lien ket doi tuong da resolve;
- cung cap API runtime cho view va validator.

### Meta-model cua `mm`

```text
GoalModel

Actor
|- Agent
`- Role

IntentionalElement
|- ConcreteIntentionalElement
|  |- GoalTaskElement
|  |  |- Goal
|  |  `- Task
|  `- Resource
`- Quality

Clause
|- GoalClause
|  |- Achieve
|  |- Maintain
|  `- Avoid
|- Pre
`- Post

Refinement
|- AndRefinement
`- OrRefinement

Contribution
ContributionType
Dependency
```

### Quan he chinh trong meta-model

```text
GoalModel
|- actors: List<Actor>
|- dependencies: List<Dependency>
`- elementMap: ten -> IntentionalElement

Actor
|- isAActor: Actor
|- participatesInActor: Actor
`- wantedElements: List<IntentionalElement>

Goal
`- goalClause: GoalClause

Task
|- pre: Pre
|- post: Post
`- neededResources: List<Resource>

Quality
`- qualifiedElements: List<ConcreteIntentionalElement>

Resource
`- neededByTasks: List<Task>

Refinement
|- parent: GoalTaskElement
`- children: List<GoalTaskElement>

Contribution
|- source: IntentionalElement
`- target: IntentionalElement

Dependency
|- depender: Actor
|- dependee: Actor
|- dependumElement: IntentionalElement
|- dependerElement: IntentionalElement?
`- dependeeElement: IntentionalElement?
```

## 8. Package `mm/ocl`

Package nay la expression meta-model dung cho OCL-like logic trong GOAL DSL.

### Trach nhiem

- luu cay bieu thuc da parse;
- tach bieu thuc khoi parse tree ANTLR;
- lam nen cho validation, phan tich, hoac chuyen doi tiep theo.

### Meta-model cua `mm/ocl`

```text
Expression
|- BinaryExp
|- UnaryExp
|- VariableExp
|  `- SelfExp
|- AtPreExp
|- IteratorExp
|- FeatureCallExp
|  |- PropertyCallExp
|  `- OperationCallExp
|- LiteralExp
|  |- StringLiteralExp
|  |- IntegerLiteralExp
|  |- RealLiteralExp
|  |- BooleanLiteralExp
|  |- NullLiteralExp
|  `- EnumLiteralExp
`- OpaqueExpression

VariableDeclaration
OclModel
```

### Nhom bieu thuc ma plugin dang bieu dien

- binary expressions;
- unary expressions;
- variable/self expressions;
- property va operation call;
- iterator expressions;
- `@pre`;
- cac literal co kieu;
- opaque string expression de tuong thich nguoc.

## 9. Package `view`

Package `view` la tang projection. No lay semantic model trong `mm` va bieu dien thanh diagram trong USE.

### Trach nhiem

- tao node/edge tu `GoalModel`;
- quan ly visible/hidden diagram data;
- xu ly input keo tha, resize;
- validate text OCL voi USE model;
- hien thi bao cao validation.

### Thanh phan chinh

- `GoalDiagramView`: lop trung tam cua diagram.
- `GoalDiagramData`: chua node/edge hien thi va an.
- `GoalDiagramInputHandling`: xu ly drag/resize.
- `GoalDiagramOptions`: tuy chinh options diagram.
- `OclUseValidator`: validator doi chieu expression voi `MModel`.
- `OclValidationReport`: tap hop message loi.
- `view/nodes`: cac node cho actor, goal, task, quality, resource.
- `view/edges`: cac edge cho contribution, refinement, dependency, qualification.

### Meta-model projection cua `view`

`view` khong phai domain model doc lap. No la lop chieu cua `mm` len UI. Co the xem no nhu mot projection model:

```text
GoalDiagramView
|- GoalDiagramData visibleData
|- GoalDiagramData hiddenData
|- actorNodeMap: Actor -> ActorNode
|- actorBoundaryMap: Actor -> ActorBoundaryNode
|- elementNodeMap: IntentionalElement -> PlaceableNode
`- validationReports: IntentionalElement -> OclValidationReport

GoalDiagramData
|- nodes: Set<PlaceableNode>
`- edges: Set<EdgeBase>

Nodes
|- ActorBoundaryNode
|- ActorNode
|  |- AgentNode
|  `- RoleNode
|- GoalNode
|- TaskNode
|- QualityNode
`- ResourceNode

Edges
|- ContributionEdge
|- RefinementEdge
|- DependencyEdge
`- QualificationEdge
```

### Cach `GoalDiagramView` su dung meta-model `mm`

- `Actor`, `Agent`, `Role` -> actor nodes + actor boundaries
- `Goal`, `Task`, `Quality`, `Resource` -> element nodes
- `Contribution` -> `ContributionEdge`
- `Refinement` -> `RefinementEdge`
- `Dependency` -> `DependencyEdge`
- `Quality` qualification relation -> `QualificationEdge`

Noi cach khac, `view` khong tao y nghia moi, ma chi anh xa y nghia da duoc `GoalModelFactory` materialize.

## 10. Package generated `target/generated-sources/antlr4`

Thu muc nay chua ma sinh boi `antlr4-maven-plugin` tu:

- `src/main/resources/grammars/GOAL.g4`
- `src/main/resources/grammars/goal12.g4`

### Loai file duoc sinh

- lexer classes;
- parser classes;
- visitor interfaces va base visitor;
- listener interfaces va base listener;
- `.tokens`;
- `.interp`.

### Cach dung trong du an

- `GOALLexer` va `GOALParser` duoc `GOALCompiler` dung truc tiep.
- `GOALBaseVisitor` la lop co so ma `GoalAstBuilder` ke thua.
- cac `ParserRuleContext` trong `GOALParser` duoc `GoalAstBuilder` va `OclExpressionBuilder` dung de trich thong tin.

### Co the tai su dung gi tu ma generate

- debug grammar khi parse fail;
- viet them visitor/listener moi ma khong sua vao parser sinh ra;
- viet unit test parser;
- tra cuu token/rule names khi mo rong DSL.

### Nguyen tac bao tri

- khong sua truc tiep file trong `target/generated-sources/antlr4`;
- moi thay doi ngon ngu phai bat dau tu file `.g4`;
- sau khi sua grammar phai build lai va kiem tra cac visitor tu viet con khop voi parse contexts.

## 11. Ket luan

Kien truc cua plugin GOAL co the hieu gon nhu sau:

- `parser` dieu phoi toan bo viec bien DSL thanh model;
- `GoalAstBuilder` va `OclExpressionBuilder` chiu trach nhiem chuyen doi cu phap thanh cau truc trung gian;
- `GoalModelFactory` chiu trach nhiem tao va noi cac doi tuong semantic;
- `mm` va `mm/ocl` la lop meta-model runtime;
- `view` va `gui` la lop giao dien va projection;
- ma generate ANTLR la tang ho tro parser, khong phai noi sua truc tiep.
