# TARUMT Resorts — Console Prototype

BMCS2063 Data Structures and Algorithms — Assignment 202605.

A console-based reservation and room-optimization prototype for TARUMT Resorts,
a luxury hospitality chain. The system is driven by one shared Master Guest
Registry and demonstrates both linear and non-linear Abstract Data Types (ADTs)
together with explicit searching and sorting algorithms.

> No Java Collections Framework is used — all collection ADTs are custom
> implementations of the interfaces in `shared/adt`.

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
| 1 | Walk-In Registrations & Standard Booking | Queue (FIFO) | Pujin |
| 2 | VIP & Loyalty Tier-Priority Room Allocation | Heap | YZ |
| 3 | Housekeeping and Task Log | Stack (undo/redo) | QW |
| 4 | Front-Desk Service | Hash (O(1) search) | YZ |
| 5 | Loyalty and Rewards Service | List | KY |
| 6 | Summary Reports (Revenue & Occupancy) | Search & Sort | All |

## Project Structure

```
src/
├── Main.java          // Entry point
├── shared/            // Generic ADT interfaces (adt/) and helpers (util/)
├── booking/           // Module 1 - Queue
├── allocation/        // Module 2 - Heap
├── housekeeping/      // Module 3 - Stack
├── frontdesk/         // Module 4 - Hash
└── loyalty/           // Module 5 - List
```

Each module follows ECB with `boundary/`, `control/`, `entity/` and `report/`
sub-packages.

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
| Pujin  | 1 (Booking) |
| YZ     | 2 (Allocation), 4 (Front-Desk) |
| QW     | 3 (Housekeeping) |
| KY     | 5 (Loyalty) |

Each member also delivers the report (search & sort) for their own module(s).
