# Local authentication testing

No user is created by Flyway. For an explicit local identity:

1. Copy `database/development/seed-local-identity.template.sql` to an untracked local file.
2. In IntelliJ, run the test-scope main class
   `com.flakomencia.agendaflow.identity.tools.LocalPasswordHashTool` with terminal emulation enabled.
3. Enter and confirm the password interactively; do not pass it on the command line.
4. Replace every visible placeholder, including `__BCRYPT_HASH__` and `__ROLE_NAME__`.
5. Review the target database and execute the copied SQL manually after Flyway V1/V2.

The template is outside `db/migration`, is never automatic, and is reasonably idempotent for its
email, tax identifier, membership and role assignment. Never commit the populated copy, a real
password, or a usable credential hash.

Set local token variables from `.env.example` through the IDE or shell. The repository defaults are
explicit local placeholders and production-like profiles reject them.
