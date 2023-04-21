# Quill library for SQL statement generation and validation

- Status: draft [ accepted | deprecated | superseded by [xxx](yyyymmdd-xxx.md)]
- Deciders: Yurii Shynbuiev, Fabio Pinheiro, Benjamin Voiturier 
- Date: [2023-01-17] 
- Tags: DAL, SQL, Postrgresql, Typesafe

## Context and Problem Statement

PostgreSQL is essential to the Atala PRISM technology stack, where most entities are stored.

Backend services: PRISM Agent, PRISM Mediator, and PRISM Node use PostgreSQL.

[Doobie](https://tpolecat.github.io/doobie/index.html) library is currently used in Scala code to communicate with Postgresql. Quotes from the website

```
Doobie is a pure functional JDBC layer for Scala and Cats. It is not an ORM, nor is it a relational algebra; 
it simply provides a functional way to construct programs (and higher-level libraries) that use JDBC
doobie is a Typelevel project. 
This means we embrace pure, typeful, functional programming, and provide a safe and friendly environment for teaching, learning, and contributing as described in the Scala Code of Conduct.
```
Doobie is a good choice for DAL, and this ADR is about something other than replacing it.

Writing the SQL statement and mapping the row to the case class is a boilerplate and error-prone activity that the Quill library can optimize.

**Writing the code for mapping a table row to a case class and writing the low-level SQL statement is an error-prone and boilerplate thing**

**Using the [Quill](https://getquill.io/) library on top of Doobie can optimize and improve these things.**
Quote from the website:

```
Quill provides a Quoted Domain Specific Language (QDSL) to express queries in Scala and execute them in a target language. The library’s core is designed to support multiple target languages, currently featuring specializations for Structured Query Language (SQL) and Cassandra Query Language (CQL).

1. Boilerplate-free mapping: The database schema is mapped using simple case classes.
2. Quoted DSL: Queries are defined inside a quote block. Quill parses each quoted block of code (quotation) at compile time and translates them to an internal Abstract Syntax Tree (AST)
3. Compile-time query generation: The ctx.run call reads the quotation’s AST and translates it to the target language at compile time, emitting the query string as a compilation message. As the query string is known at compile time, the runtime overhead is very low and similar to using the database driver directly.
4. Compile-time query validation: If configured, the query is verified against the database at compile time, and the compilation fails if it is not valid. The query validation does not alter the database state.
```

There are [Slick](https://scala-slick.org/) and [ScalikeJDBC](http://scalikejdbc.org/) libraries as well. 

Comparison of these libraries is not a goal of this ADR, but it's essential to know the differences.

There are good references to take a look at in the [Links](#links) section.

Overall, all libraries have differences in the following aspects:

- Metamodel (how to define the schema and type mapping)
- Static SQL statement (how and where does the SQL statement is written/generated)
- Dynamic SQL statement (how and where does the dynamic SQL statement written/generated)
- Connection Management (thread and connection pooling)
- Asynchronous API (the high-level API to execute queries blocking or non-blocking)
- Asynchronous IO (is IO operation blocking or asynchronous)
- Effect library that is used (free-monad, Future, Task, ZIO)

## Decision Drivers

- Generate and validate SQL statement based on the convention-over-configuration approach in compile time (type-safe queries)
- Reduce boilerplate and error-prone code
- Easy to write the dynamic queries

## Considered Options

- Doobie (Quill for the connection pooling, SQL statement execution, and SQL statement writing)
- Doobie + Quill (Quill for the connection pooling, SQL statement execution, and SQL statement writing + Quill for the SQL statement generation)
- Quill (Quill for the connection pooling, SQL statement execution, and SQL statement writing and generation)

## Decision Outcome

Chosen option: "Doobie + Quill" because it's the simplest solution that requires minimal changes to the existing code and brings the benefits of automatic SQL statement generation and validation in compile time (see below).

### Positive Consequences

- convention-over-configuration approach for the generation and validation of SQL statements using macros in the compile time
- easy work with dynamic queries
- backward compatible solution (minimum changes are required for the current code base)

### Negative Consequences

- DTO case classes are required for each table to generate the SQL statement based on the convention

## Pros and Cons of the Options

### Doobie

Doobies library is used as it is right now without any changes

- Good, because it is a solid FP library for Postgresql
- Good, because it has good documentation and a large community of developers who contribute to the library
- Good, because it is built using Free Monad, which makes it composable and easy to integrate with any popular effects library
- Bad, because it has a low-level API for writing the SQL statement (boilerplate and error-prone code)
- Bad, because it uses blocking IO at the network level

### Doobie+Quill

Doobie library is used as it is right now, and Quill library is used for SQL statement generation and validation in compile time

- Good, because it ss a solid FP library for Postgresql
- Good, because it has good documentation and a large community of developers who contribute to the library
- Good, because it is built using Free Monad, which makes it composable and easy to integrate with any popular effects library
- Good, because Quill library is used for SQL statement generation at the compile time
- Good, because Quill library extends the current solution, and no changes to the code base are required
- Bad, because the DTO case class must be created for each table
- Bad, because it uses blocking IO at the network level

### Quill

Quill is used instead of Doobie

- Good, because it is a solid FP library for Postgresql
- Good, because it has good documentation and a large community of developers who contribute to the library
- Good, because it is built using Free Monad, which makes it composable and easy to integrate with any widespread effects library
- Good, because it is used for SQL statement generation at the compile time instead of using Doobie low-level API
- Good, because it can be configured to use non-blocking IO at the network level
- Good, because it get rid of the `cats` ecosystem that comes with `doobie` (simplify the dependency management)
- Bad, because significant refactoring of all DAL is required
- Bad, because the DTO case class must be created for each table

## Examples

### Doobie

```
import doobie._
import doobie.implicits._
import doobie.postgres._

case class Person(id: Int, name: String)

val q = sql"SELECT id, name FROM person WHERE id = 1".query[Person]

val result: ConnectionIO[List[Person]] = q.to[List].transact(Transactor.fromDriverManager[IO](
  "org.postgresql.Driver", "jdbc:postgresql:world", "username", "password"
))
```

### Quill

```
import io.getquill._

val ctx = new SqlMirrorContext(PostgresDialect, "ctx")

case class Person(id: Int, name: String)

val q = quote {
  query[Person].filter(p => p.id == 1)
}

val result: List[Person] = ctx.run(q)
```

### Slick

```
import slick.jdbc.PostgresProfile.api._

val db = Database.forConfig("database")

case class Person(id: Int, name: String)

val q = TableQuery[Person].filter(_.id === 1)

val result: Future[Seq[Person]] = db.run(q.result)
```

#### Two more real example of Doobie and Quill usage are in the [Links](#links) section.

## Links

- [Comparing Scala relational database access libraries](https://softwaremill.com/comparing-scala-relational-database-access-libraries/)
- [Comparison with Alternatives](https://scala-slick.org/docs/compare-alternatives)
- [Doobie vs Quill](https://www.libhunt.com/compare-doobie-vs-zio-quill)
- [Slick vs Doobie](https://www.libhunt.com/compare-slick--slick-vs-doobie?ref=compare)
- [Database access libraries in Scala](https://medium.com/@takezoe/database-access-libraries-in-scala-7aa7590aa3db)
- [Typechecking SQL queries with doobie](https://godatadriven.com/blog/typechecking-sql-queries-with-doobie/)
- [Typechecking SQL in Slick and doobie](https://underscore.io/blog/posts/2015/05/28/typechecking-sql.html)
- [Doobie example in the Pollux library](https://github.com/input-output-hk/atala-prism-building-blocks/blob/pollux-v0.17.0/pollux/lib/sql-doobie/src/main/scala/io/iohk/atala/pollux/sql/repository/JdbcCredentialRepository.scala)
- [Quill example in the Pollux library](https://github.com/input-output-hk/atala-prism-building-blocks/blob/pollux-v0.17.0/pollux/lib/sql-doobie/src/main/scala/io/iohk/atala/pollux/sql/model/VerifiableCredentialSchema.scala)
