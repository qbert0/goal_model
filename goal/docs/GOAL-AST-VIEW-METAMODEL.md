# GOAL AST va View Meta-model

## 1. Pham vi

Tai lieu nay chot lai 3 diem:

- doi chieu nhanh package `mm` voi meta-model runtime dang co trong code;
- dac ta lai meta-model cua package `ast`;
- dac ta lai projection model va quy uoc render cua package `view`.

Tai lieu nay lay code hien tai lam nguon su that. Khong co anh meta-model duoc dinh kem trong repo, vi vay doi chieu `mm` duoc thuc hien truc tiep tu cac class trong package `org.vnu.sme.goal.mm`.

## 2. Ket qua doi chieu package `mm`

### 2.1 Muc do khop voi meta-model

Package `mm` hien tai van khop ve y nghia domain:

- `GoalModel` la aggregate root.
- `Actor`, `Agent`, `Role` bieu dien actor hierarchy.
- `IntentionalElement` la goc cho `Goal`, `Task`, `Quality`, `Resource`.
- `GoalClause` va `Clause` giu cac bieu thuc OCL cho goal/task.
- `Refinement`, `Contribution`, `Dependency` giu cac quan he chinh.

### 2.2 Diem can luu y

Cau truc code co 2 sai khac nho so voi cach ve meta-model "ly tuong":

1. `GoalModel` khong luu san danh sach `Contribution` va `Refinement` nhu field rieng.
   No suy ra chung qua:
   - `getContributions()` tu `IntentionalElement.getOutgoingContributions()`;
   - `getRefinements()` tu `GoalTaskElement.getChildRefinements()` va `getParentRefinements()`.

2. `Quality` va `Resource` giu quan he dac thu tren chinh class cua chung:
   - `Quality.getQualifiedElements()`;
   - `Resource.getNeededByTasks()`.

Nghia la package `mm` van dung theo meta-model, nhung mot so quan he duoc suy dan tu lien ket hai chieu thay vi luu tap trung o root.

### 2.3 Cay runtime model cua `mm`

```text
GoalModel
|- actors: List<Actor>
|- dependencies: List<Dependency>
|- actorMap: Map<String, Actor>
`- elementMap: Map<String, IntentionalElement>

Actor
|- Agent
`- Role

IntentionalElement
|- GoalTaskElement
|  |- Goal
|  `- Task
|- Quality
`- ConcreteIntentionalElement
   `- Resource

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

## 3. Meta-model cua package `ast`

### 3.1 Vai tro

`ast` la lop abstract syntax da thoat khoi parse tree ANTLR nhung chua thanh semantic runtime model.

No duoc thiet ke theo huong:

- giu token goc de bao loi va semantic check;
- luu quan he duoi dang `OutgoingLink` chua resolve;
- luu OCL duoi dang `mm.ocl.Expression` ngay tai AST, de semantic va factory khong phai quay lai parse tree.

### 3.2 Cay AST chinh

```text
GoalModelCS
|- actorDeclsCS: List<ActorDeclCS>
|- ieDeclsCS: List<IntentionalElementCS>
`- relationDeclsCS: List<RelationCS>

DescriptionContainerCS
|- ActorDeclCS
|  |- ActorCS
|  |- AgentCS
|  `- RoleCS
`- IntentionalElementCS
   |- GoalCS
   |- TaskCS
   |- QualityCS
   `- ResourceCS

RelationCS
`- DependencyCS

OutgoingLink
`- Kind
   |- REFINE_AND
   |- REFINE_OR
   |- CONTRIB_MAKE
   |- CONTRIB_HELP
   |- CONTRIB_HURT
   |- CONTRIB_BREAK
   |- QUALIFY
   `- NEEDED_BY
```

### 3.3 Trach nhiem cua cac node AST

#### `GoalModelCS`

- giu ten model;
- giu danh sach actor declaration;
- giu relation declaration cap model, hien tai la `DependencyCS`;
- la root cho parser, debug printer, symbol builder, semantic analyzer, va `GoalModelFactory`.

#### `ActorDeclCS`

- giu ten actor (`fName`);
- giu cac quan he actor-level:
  - `isARefs`;
  - `participatesInRefs`;
  - `wantsRefs`;
- giu toan bo `IntentionalElementCS` nam trong actor do.

#### `IntentionalElementCS`

- giu ten element;
- giu danh sach `OutgoingLink`;
- khong resolve target, chi giu token dich.

#### `GoalCS`

- bo sung `GoalType` gom `ACHIEVE`, `MAINTAIN`, `AVOID`;
- giu `Expression oclExpression`.

#### `TaskCS`

- giu `Expression preExpression`;
- giu `Expression postExpression`.

#### `DependencyCS`

- la relation-level declaration;
- giu tham chieu thuan van ban:
  - `dependerQualifiedName`;
  - `dependeeQualifiedName`;
- giu `dependumElement` duoi dang AST element khai bao inline.

### 3.4 Rang buoc thiet ke cua AST hien tai

AST hien tai da duoc viet lai theo huong phu hop voi semantic pipeline:

- actor va element van la declaration tree;
- relation noi bo cua element duoc chuan hoa ve `OutgoingLink`;
- dependency cap model duoc tach rieng thanh `RelationCS`/`DependencyCS`;
- token van duoc giu lai de semantic bao loi theo line/column.

Day la hinh dang AST phu hop de:

- dung symbol table;
- validate operator matrix;
- check leaf dependency;
- chuyen sang `mm` ma khong can doc lai parse tree.

## 4. Projection model cua package `view`

### 4.1 Vai tro

`view` khong tao semantics moi. No la lop projection tu `mm` sang `DiagramView` cua USE.

No chiu trach nhiem:

- tao node va boundary tu actor/element;
- tao edge tu contribution/refinement/qualification/needed-by/dependency;
- validate OCL cua goal va task voi `MModel` cua USE;
- cung cap popup menu, refresh, va bo loc hien thi.

### 4.2 Cay projection model

```text
GoalDiagramView
|- visibleData: GoalDiagramData
|- hiddenData: GoalDiagramData
|- actorNodeMap: Actor -> ActorNode
|- actorBoundaryMap: Actor -> ActorBoundaryNode
|- nodeOwnerMap: PlaceableNode -> Actor
|- elementNodeMap: IntentionalElement -> PlaceableNode
|- validationReports: IntentionalElement -> OclValidationReport
|- displayMode: ALL | ACTORS_ONLY | DEPENDENCY_OVERVIEW | ACTOR_FOCUS
`- focusedActor: Actor?

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
|- RefinementEdge
|- ContributionEdge
|- QualificationEdge
|- NeededByEdge
`- DependencyEdge
```

### 4.3 Quy uoc render quan he

View hien tai duoc chot theo quy uoc sau:

- `AND refinement`: duong thang va co thanh ngang o dau dich.
- `OR refinement`: duong thang va co mui ten to dam o dau dich.
- `Contribution`: duong thang mui ten lien net, nhan la ten dong gop:
  - `make`
  - `help`
  - `hurt`
  - `break`
  - `some+`
  - `some-`
  - `unknown`
- `Qualification`: duong net dut.
- `Needed-by`: duong lien net, co cham den o dau nguon resource.
- `Dependency`: van la canh ba ngoi, di qua node dependum.

### 4.4 Bo loc va popup menu

`GoalDiagramView` hien co cac che do hien thi:

- `Show full GOAL model`
- `Show actors only`
- `Show actors + dependencies`
- `Focus selected actor`
- `Focus actor` theo menu con cho tung actor
- `Refresh GOAL diagram`
- `Validate GOAL OCL`

Popup menu nay duoc gan vao co che `unionOfPopUpMenu()` cua `DiagramView`, nen no di dung cach mo menu chuot phai cua USE.

### 4.5 Anh xa tu `mm` sang `view`

```text
Actor / Agent / Role         -> ActorBoundaryNode + ActorNode / AgentNode / RoleNode
Goal                         -> GoalNode
Task                         -> TaskNode
Quality                      -> QualityNode
Resource                     -> ResourceNode
Contribution                 -> ContributionEdge
Refinement                   -> RefinementEdge
Quality.qualifies(...)       -> QualificationEdge
Resource.neededByTasks(...)  -> NeededByEdge
Dependency                   -> DependencyEdge
```

## 5. Ket luan

Sau khi doi chieu code hien tai:

- `mm` van dung voi runtime meta-model cua plugin;
- `ast` da duoc chuan hoa quanh `OutgoingLink` va `DependencyCS`, phu hop hon cho semantic pipeline;
- `view` can duoc hieu nhu projection layer co filter mode, popup menu, va quy uoc render quan he rieng;
- loi "khong hien thi het relation" nam o `view`, khong nam o `mm`.
