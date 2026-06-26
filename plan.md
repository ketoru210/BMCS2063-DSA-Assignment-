# Project Description

> A **console-based prototype system** for TAR UMT Resorts 
> Full CLI, we don't do GUI
> **Must implement `Interface`**

## Modules Break Down

> Total 6 modules

| *Modules* | *Data Structure* | *Reason* | *Description* | *Assigned to* |
|---|---|---|---|---|
| Walk-In Registrations & Standard Booking Procedure | Queue | Obvious, a queue must be FIFO | When there are rooms available and walk-in is available, process standard guests as a queue | Pujin | 
| VIP & Loyalty Tier-Priority Room Allocation | Heap | To create a binary tree to dynamically reorganize the data (ensuring VIP always at the root) | Customer with higher loyalty tier can bypass lower loaylty tier, room allocation must prioritize high loaylty tier member | YZ |
| Housekeeping and Task Log | Stack | To perform "undo / redo" | Housekeeping supervisor updates room status sequentially, if any mistake occur, the schedule must roll back instantly | QW |
| Front-Desk Service | Hash | To preform o(1) search | Search by 8-digit confirmation number | YZ |
| Loyalty and Rewards Service | List | Member has many attributes, and random access is needed | Manage the member enrollment, points accumulation and redemption, tier progression process / track redemption requests. Also notifications reminders for expiring points, redemption requets, or tier upgrades | KY |
| Summary Reports - Executive Revenue & Occupancy Reports | - | Search & Sort just consider on algorithms | Generate reports, should able to filter matching criteria | All Members, each member do 1 per their own module(s) |

> notes: this module description is just a short one. For more information, refer to assignment question

## Submission Date

**Week 10 Friday 11.59 pm.**

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
├── Main.java                         // Entry point
│
├── shared/                           // Shared by everyone (notice in group when changed)
│   ├── adt/                          // Generic ADT interfaces
│   │   ├── QueueInterface.java
│   │   ├── HeapInterface.java
│   │   ├── StackInterface.java
│   │   ├── HashInterface.java
│   │   └── ListInterface.java
│   └── util/                         // Small helpers
│       └── InputHelper.java
│
├── booking/                          // Module 1 - Queue
│   ├── boundary/
│   │   └── BookingUI.java            // CLI menu and user I/O for booking
│   ├── control/
│   │   └── BookingManager.java       // Booking business logic
│   ├── entity/
│   │   ├── Booking.java              // Booking data object
│   │   └── BookingQueue.java         // implements QueueInterface
│   └── report/
│       └── BookingReport.java        // Search & sort + report for this module
│
├── allocation/                       // Module 2 - Heap
│   ├── boundary/
│   │   └── AllocationUI.java
│   ├── control/
│   │   └── AllocationManager.java
│   ├── entity/
│   │   ├── VipGuest.java
│   │   └── PriorityHeap.java         // implements HeapInterface
│   └── report/
│       └── AllocationReport.java
│
├── housekeeping/                     // Module 3 - Stack
│   ├── boundary/
│   │   └── HousekeepingUI.java
│   ├── control/
│   │   └── HousekeepingManager.java
│   ├── entity/
│   │   ├── Task.java
│   │   └── TaskStack.java            // implements StackInterface
│   └── report/
│       └── HousekeepingReport.java
│
├── frontdesk/                        // Module 4 - Hash
│   ├── boundary/
│   │   └── FrontDeskUI.java
│   ├── control/
│   │   └── FrontDeskManager.java
│   ├── entity/
│   │   ├── Reservation.java
│   │   └── ConfirmationHashMap.java  // implements HashInterface
│   └── report/
│       └── FrontDeskReport.java
│
└── loyalty/                          // Module 5 - List
    ├── boundary/
    │   └── LoyaltyUI.java
    ├── control/
    │   └── LoyaltyManager.java
    ├── entity/
    │   ├── Member.java
    │   └── MemberList.java           // implements ListInterface
    └── report/
        └── LoyaltyReport.java
```

## Shared Data Definitions (共享数据定义)

> 我先放一些，有要改的提意见

### Room Status

- who handle: module 3 (housekeeping) (qw)
- read by: module 2 (allocation) and module 1 (booking) for room allocation 
- data: `Available, Occupied, Dirty, Clean, Out-of-Service`

### Room Types

- data: `Single, Suite, Deluxe`

### Booking / Reservation

- who handle: module 1 (booking) (pujin)
- read by: module 4 (front-desk), report
- fields: `confirmationNo, guestName, roomNo, checkIn, checkOut, status`

### Booking Status

- who handle: module 1 (booking) (pujin)
- data: `Pending, Confirmed, Checked-in, Checked-out, Cancelled`

### Confirmation Number

- who handle: module 1 (booking) (pujin)
- read by: module 4 (front-desk) for O(1) search
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
- data: `Silver, Gold, Platinum` (priority 2, 1, 0 -> lower wins) (platinum = 0)

### Guest

- who handle: module 1 (booking) (pujin)
- note: walk-in guest, NOT a loyalty member (no tier)
- fields: `name, icOrPassport, contactNo`

### Format Conventions

- Room No.: `zone-floorRoom` -> e.g. `A-1203`
- Date: `dd-MM-yyyy` -> e.g. `26-06-2026`
- Money: `RM` + 2 decimals floating point number -> e.g. `RM 250.00`