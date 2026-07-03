# TARUMT Resorts — Console Prototype

BMCS2063 Data Structures and Algorithms — Assignment 202605.

A console-based reservation and room-optimization prototype for TARUMT Resorts,
a luxury hospitality chain. The system is driven by one shared Master Guest
Registry and demonstrates both linear and non-linear Abstract Data Types (ADTs)
together with explicit searching and sorting algorithms.

> No Java Collections Framework is used — all collections implement the ADT interfaces in `adt/`

> 内部设计细节、数据约定与分工，见 [plan.md](plan.md)。

## Architecture

Built on the **Entity–Control–Boundary (ECB)** pattern:

- **Boundary** — UI layer, handles console I/O with the user.
- **Control** — logic layer, business rules and workflow coordination.
- **Entity** — data layer, stores data and the data structure operations.

Flow: `Boundary → Control → Entity`, with shared utility classes used across layers.

## Modules

| # | Module | ADT | Owner |
|---|--------|-----|-------|
| 1 | Walk-In Registrations & Standard Booking | Deque (FIFO) | YZ |
| 2 | VIP & Loyalty Tier-Priority Room Allocation | Max-Heap (**team ADT**) | YZ |
| 3 | Housekeeping and Task Log | 2 × LinkedStack (undo/redo) | Pujin |
| 4 | Front-Desk Service | BST (O(log n) search) | QW |
| 5 | Loyalty and Rewards Service | Doubly Linked List | KY |
| 6 | Summary Reports (Revenue & Occupancy) | Search & Sort | All |

## Project Structure

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

Packages are organized by ECB layer, following the course's ECBDemo reference
project; every class carries an `@author` tag.

## Build & Run

This is a NetBeans (Ant) project.

```bash
ant clean jar          # compile and package
java -jar dist/*.jar   # run

# or from the IDE: open the project and Run (F6)
```

## Team

| Member | Modules |
|--------|---------|
| YZ     | 1 (Booking), 2 (Allocation) |
| QW     | 4 (Front-Desk) |
| KY     | 5 (Loyalty) |
| Pujin  | 3 (Housekeeping) |

Each member also delivers the report (search & sort) for their own module(s).
