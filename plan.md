# Project Description

> - A **console-based prototype system** for TAR UMT Resorts 
> - Full CLI, we don't do GUI
> - **Must implement `Interface`**

## Modules Break Down

> Total 6 modules

| *No.* | *Modules* | *Suggest ADT* | *Assigned to* | *Reason* |
|------|---|---|---|---|
| M1 | Walk-In Registration & Standard Booking | Deque | YZ | FIFO is the business rule itself (chronological processing), all core ops O(1); `addFront` lets a called guest rejoin the front; tightly coupled with M2 -> same owner |
| M2 | VIP & Loyalty Tier-Priority Room Allocation | Max-Heap (**team ADT**) | YZ | not covered in the course (originality marks); insert reorganizes automatically so the highest priority is always at the root — exactly the spec wording; priority = static key `tier*W - arrivalSeq*r`, so waiting-time aging needs no re-heapify |
| M3 | Housekeeping and Task Log | 2 * LinkedStack | Pujin | LIFO matches undo semantics; O(1) push/pop = spec's "roll back instantly"; second stack enables redo (cleared on new action) |
| M4 | Front-Desk Service | Binary Search Tree (BST) / harder: AVL Tree | QW | tutor advised against hash; O(log n) search on the unique comparable confirmationNo; in-order traversal gives a sorted listing for free |
| M5 | Loyalty and Rewards Service | Doubly Linked List | KY | no single dominant operation -> general-purpose collection; O(1) insert/remove after locating + two-way traversal, supports the many member features |
| M6 | Summary Reports | - | All Members | algorithms instead of a new ADT: each member hand-writes one sort + search/filter for their own module's report |

> notes: the ADT is just suggestion, you may change it yourself if you think the other ADT is better, but remember each member should not take the same ADT

## Timeline

> Submission Date: Week 10 Friday 11.59 pm.

- Freeze shared data contracts (Booking fields, Room Status, tier values): Week 3
- Code Writing: Week 3 - Week 8
- Early Code Integration: Week 6
- Final Code Integration & Documentation (NetBeans project + ReadMe.txt): Week 9
- Last Checking: Week 10
- Demo Prep (each member: own ADT reasoning + complexity): Week 10 - Week 11

## Spec Compliance Checklist (for report / code review)

- [ ] **No Java Collections Framework**: never use `java.util` ArrayList / HashMap / LinkedList / Stack etc.; for collections outside your scope, use a teammate's ADT or the course sample code
- [ ] ADTs not written by you / adapted: **acknowledge the source** at the top of the Java interface (spec requirement)
- [ ] Author name as a comment at the top of every class you wrote
- [ ] Utility classes may contain static methods + static variables only (check InputHelper / OutputHelper)
- [ ] ECB constraints: Boundary <-> actor/control; Control <-> boundary/entity/other controls; **Entity may only know other entities**
- [ ] Only validations that invoke ADT methods are required (spec wording); UI earns no marks — don't over-invest
- [ ] Deliverables: NetBeans project + data files + ReadMe.txt + AI Usage Disclosure Form
- [ ] AI policy is Yellow: no AI-generated modules/core code; rubric "Overall Solution" penalizes AI usage; write core ADT + module logic yourself and be able to explain every line at the demo
- [ ] Team component submits and is assessed on **ONE** ADT only -> submit the Max-Heap
- [ ] Java naming convention (Camel Case)

---

# Project Structure

## ECB

> Make it as Entity-Control-Boundary (ECB) architecture

- ECB (Entity-Control-Boundary) — a pattern that splits the system into three layers:
    - Boundary: UI layer. Performs user I/O handling and interacts with the user.
    - Control: Logic layer. Handles business logic and coordinates the workflow.
    - Entity: Data layer. Stores the data and the data structure operations.

- Flow: Boundary -> Control -> Entity

- ECB Architecture UML

```
                                 ┌─────────────────┐
                                 │ Utility Classes │
                                 └─────────────────┘
                                          │
                                      used by
                                          │
         ┌────────────────────────────────┼──────────────────────────────┐
         │                                │                              │
         │                                │                              │
┌──────────────────┐             ┌─────────────────┐             ┌────────────────┐
│ Boundary Classes │ ──access──> │ Control Classes │ ──access──> │ Entity Classes │
└──────────────────┘             └─────────────────┘             └────────────────┘
```

## File Structure

> Notes: The naming is just for reference, free to change it if you want to

```
src/
├── Main.java          // Entry point
├── adt/               // ADT interfaces + implementations (team ADT: MaxHeap)
├── entity/            // Data classes (Serializable)
├── boundary/          // UI layer - console I/O only
├── control/           // Logic layer - business rules, module menus, reports
├── dao/               // Hardcode data needed to RAM
└── utility/           // Static-only helpers
```

## Shared Data Definitions (共享数据定义)

> 我先放一些，有要改的提意见

### Room Status

- who handle: module 3 (housekeeping) (pujin)
- read by: module 2 (allocation) and module 1 (booking) for room allocation 
- occupancy: `Available, Occupied, Out-of-Service`
- housekeeping pipeline (spec wording): `Dirty, Cleaning In Progress, Inspected, Ready for Check-In`

### Room Types

- data: `Single, Suite, Deluxe`

### Booking / Reservation

- who handle: module 1 (booking) (yz)
- read by: module 4 (front-desk), report
- fields: `confirmationNo, guestName, roomNo, checkIn, checkOut, status`
- `confirmationNo` is an 8-digit number, generate randomly (sequential keys would degenerate M4's BST into a linked list)

### Booking Status

- who handle: module 1 (booking) (yz)
- data: `Pending, Confirmed, Checked-in, Checked-out, Cancelled`

### Confirmation Number

- who handle: module 1 (booking) (yz)
- read by: module 4 (front-desk) for BST search (O(log n) average)
- format: 8-digit numeric, unique -> e.g. `10042087`

### Member

- who handle: module 5 (loyalty) (ky)
- read by: module 2 (allocation) for tier priority
- fields: `memberId, name, tier, points`

### Member ID

- who handle: module 5 (loyalty) (ky)
- format: `M` + 5 digits -> e.g. `M00231`

### Loyalty Tier

- who handle: module 5 (loyalty) (ky)
- read by: module 2 (allocation) for priority ordering
- data: `Silver, Gold, Platinum` (priority 1, 2, 3 -> higher wins, consistent with M2's additive priority key) (Platinum = 3)

### Guest

- who handle: module 1 (booking) (yz)
- note: walk-in guest, NOT a loyalty member (no tier)
- fields: `name, icOrPassport, contactNo`

### Format Conventions

- Room No.: `zone-floorRoom` -> e.g. `A-1203`
- Date: `dd-MM-yyyy` -> e.g. `26-06-2026`
- Money: `RM` + 2 decimals floating point number -> e.g. `RM 250.00`

# Implementation Details

## Menu Design

### Main Menu

```
=== TARUMT Resorts ===
(^_^)/ Welcome!

[0] Exit
[1] Walk-In Registration & Standard Booking Procedure
[2] Vip & Loyalty Tier-Priority Room Allocation
[3] Housekeeping and Task Log
[4] Front-Desk Service
[5] Loyalty and Rewards Service

Please Select > 
```

> modules 的 menu 你们自己设计

### 一点小设定

> 有意见可以提

- use `===` to wrap title
    - e.g. `=== VIP & Loyalty Tier-Priority Room Allocation ===`
- use `[n]` for option
- must provide `[0]` for exit/go back
- use `>` for prompt