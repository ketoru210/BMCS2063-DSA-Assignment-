# TARUMT Resorts — Console Prototype

BMCS2063 Data Structures and Algorithms — Assignment 202605.

A console-based reservation and room-optimization prototype for TARUMT Resorts,
a luxury hospitality chain. The whole system is built around **one self-defined
team ADT** — `CollectionInterface<T>`, a *policy-ordered collection* whose
`remove()` / `getFirst()` operate on the element each implementation's
organizing policy designates as *first* — implemented differently by every
module, alongside hand-written searching and sorting algorithms.

> No Java Collections Framework is used — every collection in the system is an
> implementation of the single team ADT interface in `adt/`

> 内部设计细节、接口契约、数据约定与分工，见 [plan.md](plan.md)。

## Architecture

Built on the **Entity–Control–Boundary (ECB)** pattern:

- **Boundary** — UI layer, handles console I/O with the user.
- **Control** — logic layer, business rules and workflow coordination.
- **Entity** — data layer, stores data and the data structure operations.

Flow: `Boundary → Control → Entity`, with shared utility classes used across layers.

## Modules

| # | Module | Team-ADT implementation | Policy ("first" element) | Owner |
|---|--------|-------------------------|--------------------------|-------|
| 2 | VIP & Loyalty Tier-Priority Room Allocation | `MaxHeap` | highest priority | YZ |
| 3 | Housekeeping and Task Log | 2 × `LinkedStack` (undo/redo) | most recently pushed | Pujin |
| 4 | Front-Desk Service | `BinarySearchTree` (O(log n) search) | smallest key | QW |
| 5 | Loyalty and Rewards Service | `DoublyLinkedList` | head (insertion order) | KY |
| 6 | Summary Reports (Revenue & Occupancy) | hand-written search & sort | — | All |

> Module 1 (Walk-In Registration & Standard Booking) was dropped —
> booking data is seeded to RAM via `dao/` instead.

## Project Structure

```
src/
├── Main.java          // Entry point
├── adt/               // Team ADT interface (CollectionInterface) + 4 implementations
├── entity/            // Data classes (POJO, Serializable)
├── boundary/          // UI layer - console I/O only
├── control/           // Logic layer - business rules, module menus, reports
├── dao/               // Hardcoded seed data to RAM (incl. bookings)
└── utility/           // Static-only helpers
```

Packages are organized by ECB layer, following the course's ECBDemo reference
project; every class carries an `@author` tag (the ADT interface is co-authored
by the whole team).

## Build & Run

This is a NetBeans (Ant) project.

```bash
ant clean jar          # compile and package
java -jar dist/*.jar   # run

# or from the IDE: open the project and Run (F6)
```

## Team

| Member | Module |
|--------|--------|
| YZ     | 2 (Allocation) |
| Pujin  | 3 (Housekeeping) |
| QW     | 4 (Front-Desk) |
| KY     | 5 (Loyalty) |

Each member implements one class of the team ADT and also delivers the
report (search & sort) for their own module.
