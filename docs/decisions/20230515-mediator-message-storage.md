# Mediator message storage 

- Status: draft [ accepted | deprecated | superseded by [xxx](yyyymmdd-xxx.md)]
- Deciders: Yurii Shynbuiev, Benjamin Voiturier, Shailesh Patil, Fabio Pinheiro , David Poltorak
- Date: [2023-05-09] 
- Tags: storage, db, message, mongo, postgres, sql

## Context and Problem Statement
Mediator storage
Relational databases like PostgreSQL store data in structured tables, with rows and columns that help establish relationships between various tables and entities.
SQL is used in PostgreSQL to save, retrieve, access, and manipulate the database data.
While PostgreSQL may be ideal for managing structured data streams, it tends to struggle when dealing with big unstructured data, as maintaining such relations can increase time complexity significantly.
Postgres SQL relies on relational data models that need to defined in advance
Change in any field in table requires lengthy process of migration scripts run and to maintain but it works there are tools available and we also use it PrismAgent, 
But maintenance cost of software is higher.
Contrastingly, non-relational databases like MongoDB excel in these scenarios because data isn't constrained to a single table. 
This approach permits massive data streams to be imported directly into the system without the burden of setting up increasingly complex relationships and keys. 
MongoDB typically stores data within documents using JSON (JavaScript Object Notation) or BSON (Binary JavaScript Object Notation), which simplifies the handling of big data and complex data streams.
document database supports a rapid, iterative cycle of development the way that a document database turns data into code.
MongoDB is faster at inserting and for queries that use nested references instead of joins
In Mediator the data which we send or receive is json message and to process json message we can avoid the unnecessary serialization and deserialization by having it stored in document database.
Postgres SQL by default in vertically scalable where as Mongo can scale horizontally, This can help in scalability problem
Mediator messages store in simple and strigh forward write there is no transactional workflow involved so we don't gain much by using relational db like postgres.

Below are the 2 options which we can use to reduce infrastructure management 
MongoDB Atlas. Fully managed MongoDB in the cloud which can reduce the infrastructure management https://www.mongodb.com/atlas/database
Amazon DocumentDB (with MongoDB compatibility)  https://aws.amazon.com/documentdb/


## Refrences used 
https://www.plesk.com/blog/various/mongodb-vs-postgresql/
https://www.dbvis.com/thetable/json-vs-jsonb-in-postgresql-a-complete-comparison/
https://severalnines.com/blog/overview-json-capabilities-within-postgresql/
https://www.mongodb.com/docs/manual/core/schema-validation/
https://www.mongodb.com/compare/mongodb-dynamodb
https://www.projectpro.io/article/dynamodb-vs-mongodb/826


