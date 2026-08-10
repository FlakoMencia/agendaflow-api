# Scheduling domain

Phase 4 separates recurring rules from date-time exceptions:

- `availability_schedules` describes weekly working intervals for one specialist and branch.
- `schedule_blocks` describes bounded exceptions such as vacation, meeting or maintenance.

An active availability interval conflicts only when organization, specialist, branch and weekday
match, its wall-clock interval intersects, and its optional validity range intersects. Adjacent time
ranges are allowed. Inactive rules do not participate in overlap checks. The SQL schema has no
overlap constraint, so the transactional application service owns this validation; a future booking
phase must revisit concurrency requirements before using these rules to generate slots.

`LocalTime` represents wall-clock hours, `LocalDate` represents validity dates, and `OffsetDateTime`
represents block and audit timestamps. Weekdays retain the SQL `SMALLINT` domain `0..6` rather than
introducing a different persistence convention.

This domain currently performs no appointment lookup, capacity allocation, slot generation or
booking concurrency control.
