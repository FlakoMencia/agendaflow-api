# Concurrency and booking consistency

Every create/reschedule transaction acquires a PostgreSQL pessimistic write lock on the tenant-scoped specialist row. After obtaining that stable database lock, it recalculates schedules, blocks, buffers and current appointments, then inserts only if overlapping occupancy remains below `simultaneous_capacity`.

This serializes booking decisions for one specialist across all future API instances. A Java `synchronized` block or local cache would protect only one process and would allow double booking when multiple instances handle requests.

Capacity is counted using the standard half-open overlap rule. Cancelled and rescheduled statuses do not consume capacity. Capacity 1 concurrent tests assert one success and one `409`; capacity 2 tests assert two successes and the next conflict. No V3 migration is necessary because the existing specialist row is the lock anchor and existing capacity/buffer columns are sufficient.
