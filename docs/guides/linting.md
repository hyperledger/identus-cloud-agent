# Linting

Megalinter is used as the default linter and is run at the point of creating a PR by Github Actions. It is also included as a pre-commit hook.

## Configuration

The default configuration for megalinter is found at the root of the repository in `.mega-linter.yml`.

This configuration forms the base and additional properties are overridden in the Github Action workflow file `./github/workflows/lint.yml`.

The Github Action workflow file only impacts the configuration at the point it runs within the continuous integration (CI) environment.

This means that megalinter will behave differently when run locally as part of the pre-commit hook [or enacted manually] compared to when run in an automated way in Github Actions as part of CI.

**Linting is currently in an early adoption phase and due to adoption of Scala 3, it only runs in a mode which does not fail PR checks if errors are found for various linters.**

> At the end of this markdown document is a section for changelog, please leave feedback in this section so that we can enable and refine the linting configuration over time

### Base

- All linters enabled except:
  - Disabled Entirely
    - REPOSITORY_DEVSKIM
    - REPOSITORY_GITLEAKS
    - DOCKERFILE_HADOLINT
    - REPOSITORY_TRIVY
    - REPOSITORY_CHECKOV
    - REPOSITORY_SECRETLINT
    - SCALA-SCALAFIX
  - Enabled but pass even with error
    - KOTLIN_KTLINT
    - PROTOBUF_PROTOLINT
    - OPENAPI_SPECTRAL
- Linters with customisation
  - MARKDOWN_MARKDOWN_LINK_CHECK
    - Excluded Files
      - Exclude CHANGELOG.md(s)
  - MARKDOWN_MARKDOWNLINT
    - Excluded Files
      - Exclude CHANGELOG.md(s)
- Applied only to changed source files [not all files]

### Running Locally

- Fixes applied automatically

### Running in CI Environment (GitHub Actions)

- Fixes not applied automatically

## Linting Feedback

Please add any changes made to the linting rules in the below changelog section. This allows us to capture changes to linters over time which allows us to understand the good and the bad with respect to linting.

The objective with this feedback is to strike the balance of linting being useful for improving quality but not at the cost of developer velocity [it should not cause pain]

The initial configuration has been created by disabling linters which were previously disabled and then changing linters which seem incompatible with our current work to "Enabled but pass even with error"

Please use the following format

```text
---

#### Linter Name

Date Added:

Author:

Date Actioned:

Linter - Current Status: Enabled | Disabled | Enabled but pass even with error

Linter - Suggested Change:  Enable | Disable | Enabled but pass even with error | Customise

*Change detail*

Insert suggestion / description of what it did well, what it didn't do well and what should be changed

```

## Changelog

#### YAMLLINT checker configuration file is added explicitly and rule line-length is set to 600
Date Added: 2023-02-15

Author: Yurii Shynbuiev

Date Actioned: 2023-02-15

Linter - Current Status: Enabled

Linter - Suggested Change: Customise

*Change detail*

yamllint checker reports the error for OAS files for lines longer than 500 characters.

The configuration file for the yamllint checker was added to the root of the project `.yamllint.yml`.

The rule for the line-length was set to 600

---

#### BASH_SHELLCHECK_DISABLE_ERRORS

Date Added: 2023-01-20

Author: Yurii Shynbuiev

Date Actioned: 2023-01-20

Linter - Current Status: Enabled

Linter - Suggested Change: Customise

*Change detail*

`BASH_SHELLCHECK_DISABLE_ERRORS: true` turns the error into the warning for the following cases:
```bash
  In /github/workspace/infrastructure/dev/get-versions.sh line 6:
  cd ${SCRIPT_DIR}
  ^--------------^ SC2164 (warning): Use 'cd ... || exit' or 'cd ... || return' in case cd fails.
     ^-----------^ SC2086 (info): Double quote to prevent globbing and word splitting.

  Did you mean:
  cd "${SCRIPT_DIR}" || exit


  In /github/workspace/infrastructure/dev/get-versions.sh line 8:
  export PRISM_AGENT_VERSION=$(cd ../../prism-agent/service && sbt "project server" -Dsbt.supershell=false -error "print version")
         ^-----------------^ SC2155 (warning): Declare and assign separately to avoid masking return values.


  In /github/workspace/infrastructure/dev/get-versions.sh line 11:
  export MERCURY_MEDIATOR_VERSION=$(cd ../../mercury/mercury-mediator && sbt "project mediator" -Dsbt.supershell=false -error "print version")
         ^----------------------^ SC2155 (warning): Declare and assign separately to avoid masking return values.


  In /github/workspace/infrastructure/dev/get-versions.sh line 14:
  export IRIS_SERVICE_VERSION=$(cd ../../iris/service && sbt "project server" -Dsbt.supershell=false -error "print version")
         ^------------------^ SC2155 (warning): Declare and assign separately to avoid masking return values.

  For more information:
    https://www.shellcheck.net/wiki/SC2155 -- Declare and assign separately to ...
    https://www.shellcheck.net/wiki/SC2164 -- Use 'cd ... || exit' or 'cd ... |...
    https://www.shellcheck.net/wiki/SC2086 -- Double quote to prevent globbing ...
```

---

#### SQL_SQL_LINT_ARGUMENTS

Date Added: 2023-01-18

Author: Pat Losoponkul

Date Actioned: 2023-01-18

Linter - Current Status: Enabled

Linter - Suggested Change:  Customise

*Change detail*

`sql-lint` postgres ALTER grammar is still quite limited as observed
in [postgres alter grammar](https://github.com/joereynolds/sql-lint/blob/0908c5b19e5275be9de339e2d26d3057526687f1/src/lexer/statements/postgres/alter.ts#L8).
Suggest we ignore this check for now as we use some ALTER TYPE in migration script.

```yaml
SQL_SQL_LINT_ARGUMENTS: -d postgres --ignore-errors=postgres-invalid-alter-option
```

---

#### SQL_TSQLLINT

Date Added: 2023-01-18

Author: Pat Losoponkul

Date Actioned: 2023-01-18

Linter - Current Status: Enabled

Linter - Suggested Change:  Disable

*Change detail*

As we use Postgres as our main database, this linter should be configured to skip `.sql` files
intended for postgres or disabled entirely if we are not using T-SQL.

---

#### SCALA_SCALAFIX

Date Added: 2023-01-16

Author: David Poltorak

Date Actioned: 2023-01-16

Linter - Current Status: Enabled

Linter - Suggested Change: Disabled

*Change detail*

SCALA_SCALAFIX is not compatible with Scala3. The errors it reports cannot be fixed. Suggest that we disable this and rely on scalafmt only

---

#### MARKDOWN_MARKDOWN_LINK_CHECK

Date Added: 2023-01-16

Author: David Poltorak

Date Actioned: 2023-01-16

Linter - Current Status: Enabled

Linter - Suggested Change: Customise and Enabled but pass even with error

*Change detail*

MARKDOWN_MARKDOWN_LINK_CHECK detects lots of broken links in automatically generated files. Suggest change configuration to ignore `CHANGELOG.md` which don't conform to the rules.

MARKDOWN_MARKDOWN_LINK_CHECK cannot traverse links to websites which require authentication. This means that links to protected github pages fail checks. Suggest change to configuration to enable but pass even with error until repository is public.

---

#### PROTOBUF_PROTOLINT

Date Added: 2023-01-16

Author: David Poltorak

Date Actioned: 2023-01-16

Linter - Current Status: Enabled

Linter - Suggested Change: Enabled but pass even with error

*Change detail*

PROTOBUF_PROTOLINT generates lots of errors for files which are quite large and may block small changes to these files. Suggest change to enable but pass even with error to not block velocity. Suggest to re enabled when files can be reviewed as a specific task

---

Date Added: 2023-03-27

Author: Pat Losoponkul

Date Actioned: 2023-03-27

Linter - Current Status: Enabled

Linter - Suggested Change:  Customise

*Change detail*

Add `.protolintrc.yml` used by `protolint` linter and configure it to disable the following rules
- `REPEATED_FIELD_NAMES_PLURALIZED`
- `ENUM_FIELD_NAMES_PREFIX`
- `ENUM_FIELD_NAMES_ZERO_VALUE_END_WITH`
- `FIELD_NAMES_LOWER_SNAKE_CASE`

These rules should be disabled since they try to rename some fields deviating from the spec submitted to W3C.

---

#### OPENAPI_SPECTRAL

Date Added: 2023-01-16

Author: David Poltorak

Date Actioned: 2023-01-16

Linter - Current Status: Enabled

Linter - Suggested Change: Enabled but pass even with error

*Change detail*

OPENAPI_SPECTRAL generates lots of errors for files which are quite large and have been generated by the openapi generator which is going to be replaced with Tapir in future work. Suggest change to enable but pass even with error. Can be re enabled when files can be reviewed as a specific task / they get removed
