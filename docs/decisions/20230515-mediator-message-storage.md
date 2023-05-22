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
Mediator messages store in simple and straight forward write there is no transactional workflow involved so we don't gain much by using relational db like postgres.

Below are the 2 options which we can use to reduce infrastructure management 
MongoDB Atlas. Fully managed MongoDB in the cloud which can reduce the infrastructure management https://www.mongodb.com/atlas/database
Amazon DocumentDB (with MongoDB compatibility)  https://aws.amazon.com/documentdb/

## Decision Drivers
- DIDCOMM messages are json based
- flexibility to store the data
- Reduce serialisation deserilisation of the data
- Easy to write the json queries
- scalability
- low maintainance

## Considered Options
- PostgresSQL (Storing unstructured data (JSON) and quering data (JSON), scalability)
- MongoDB (Storing unstructured data (JSON) and quering data (JSON), scalability)
- Kafka Stream (Storing unstructured data (JSON) and quering data (JSON), scalability and streaming)

## Decision Outcome

Chosen option: MongoDB because of storing unstructured json data and json queries that requires minimal changes to the existing code and provides the benefits for the current use cases.
Is a NoSQL database that uses a document-oriented data model. Data is stored in a semi-structured format (BSON, similar to JSON), which can easily accommodate changes in data structure. This makes MongoDB particularly suitable for large volumes of data that may not be easily modeled in a relational schema.
Kafka Stream was also considered but current usecases are more towards storage of the messages, streaming is not the usecase for all the scenarios and the  mediator pickup protocol (https://didcomm.org/pickup/3.0/) requirements need the flexibily to delete the messages read.

### Positive Consequences

- flexility to store unstructured data in json format
- easy work with storage of unstrutured data and json queries
- low cost for with no sql migrations required.
- Uses a flexible, JSON-like query language. While this provides great flexibility
- Is designed for easy horizontal scalability, which is achieved through sharding.

### Negative Consequences

- Extra infrastructure and additional resource to be maintained  

## Pros and Cons of the Options

### MongoDB

MongoDB provides flexibility with json storage and queries 

- Good, because it is horizontally scalabale 
- Good, because typically performs better with large, unstructured datasets and write-heavy applications
- Good, because have strong communities and extensive support materials, so you can expect to find help when you encounter issues.
- Bad, Is not full ACID compliance
- Bad, Doesn't natively support complex joins like a relational database


## Refrences used 
https://www.plesk.com/blog/various/mongodb-vs-postgresql/
https://www.dbvis.com/thetable/json-vs-jsonb-in-postgresql-a-complete-comparison/
https://severalnines.com/blog/overview-json-capabilities-within-postgresql/
https://www.mongodb.com/docs/manual/core/schema-validation/
https://www.mongodb.com/compare/mongodb-dynamodb
https://www.projectpro.io/article/dynamodb-vs-mongodb/826


