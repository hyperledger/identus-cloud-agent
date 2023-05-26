# Data isolation for multi-tenancy

- Status: draft
- Deciders: Benjamin Voiturier, Yurii Shynbuiev, Shailesh Patil 
- Date: 2023-05-10
- Tags: multi-tenancy, data-isolation, PostgreSQL

Technical Story:

The PRISM platform must support the multi-tenancy, so the data of the tenants must be isolated from each other and the access control policies must be applied to the data of each tenant.

This ADR is about the data isolation for multi-tenancy that must be implemented in the PRISM Agent.

## Context and Problem Statement

In a multi-tenant architecture, where multiple clients or tenants share the same infrastructure or application, data isolation is crucial to ensure the privacy and security of each tenant's data. 

The specific requirements for data isolation may vary depending on the system and its specific needs. However, here are some common requirements for achieving data isolation in a multi-tenant architecture:

### Logical Separation

Tenants' data should be logically separated from each other, meaning that each tenant should have their own isolated environment within the system. This can be achieved through logical partitions, such as separate databases, schemas, or tables.

### Physical Separation 

In addition to logical separation, physical separation can provide an extra layer of isolation. This involves segregating tenants' data onto separate physical resources, such as servers, storage devices, or networks. Physical separation helps prevent data leakage or unauthorized access between tenants.

### Access Controls

Robust access controls should be implemented to restrict access to each tenant's data. This includes authentication mechanisms to verify the identity of users or applications accessing the data and authorization rules to define what actions they are allowed to perform. Each tenant's access should be limited to their own data and not extend beyond their boundaries.

### Data Encryption

To protect data at rest and in transit, encryption techniques should be employed. This ensures that even if unauthorized access occurs, the data remains unreadable without the appropriate decryption keys. Encryption can be applied at various levels, including the application, database, or file system.

### Tenant-Specific Configuration

Each tenant may have specific configuration requirements, such as custom data schemas, access controls, or data retention policies. The system should allow for flexible configuration options to accommodate these individual tenant needs while maintaining isolation.

### Auditing and Logging

Comprehensive logging and auditing mechanisms should be in place to track and monitor access to tenants' data. This helps in detecting any unauthorized activities or potential breaches and provides an audit trail for forensic analysis if needed.

### Data Backup and Recovery

Robust data backup and recovery processes should be implemented to ensure the availability and integrity of tenants' data. Regular backups should be taken and securely stored, with mechanisms in place to restore data in the event of data loss or system failures.

### Performance and Scalability

Data isolation mechanisms should be designed to minimize performance impacts and provide scalability. The architecture should be able to handle increasing numbers of tenants and their data without sacrificing performance or compromising isolation.

### Technology Stack

The PRISM platform heavily uses the relational database PostgreSQL. Even having the abstraction as a Data Access Layer (DAL), introducing the alternative solution implies a lot of engineering efforts for refactoring and is not recommended at the current phase of the platform development.

## Decision Drivers

- Logical Separation: Tenants' data should be logically separated from each other, meaning that each tenant should have their own isolated space within the system. This can be achieved through logical partitions, such as separate databases, schemas, tables or Row Security Policies (RSP in the PostgreSQL)

- Physical Separation: In addition to logical separation, physical separation can provide an extra layer of isolation. This involves segregating tenants' data into separate physical resources, such as servers, storage devices, or networks. Physical separation helps prevent data leakage or unauthorized access between tenants.

Logical and Physical Separations define the level of `isolation` for storage, connection pool, and computation resources: CPU, RAM, and Disk I/0.

- Access Controls: Robust access controls should be implemented to restrict access to each tenant's data. This includes authentication mechanisms to verify the identity of users or applications accessing the data and authorization rules to define what actions they are allowed to perform. Each tenant's access should be limited to their data and not extend beyond their boundaries.

- Data Encryption: To protect data at rest and in transit, encryption techniques should be employed. This ensures that even if unauthorized access occurs, the data remains unreadable without the appropriate decryption keys. Encryption can be applied at various levels, including the application, database, or file system. Data encryption at rest and in transit is a matter of infrastructure configuration and shouldn't play a significant role in the following ADR.

- Tenant-Specific Configuration: Each tenant may have specific configuration requirements, such as custom data schemas, access controls, or data retention policies. The system should allow for flexible configuration options to accommodate these individual tenant needs while maintaining isolation. For simplification, in the scope of the following ADR, all tenants will not have the custom configuration, but it can be implemented by having one of the following physical separations, such as separated schemas or instances of the database.

- Auditing and Logging: Comprehensive logging and auditing mechanisms should be in place to track and monitor access to tenants' data. This helps in detecting any unauthorized activities or potential breaches and provides an audit trail for forensic analysis if needed. Auditing and Logging should provide visibility if the tenant tried to access the data that don't belong to it and the basic statistic to determine resource unitisation by the tenant (requests/sec, request latency).

- Data Backup and Recovery: Robust data backup and recovery processes should be implemented to ensure the availability and integrity of tenants' data. Regular backups should be taken and securely stored, with mechanisms in place to restore data in the event of data loss or system failures. The current aspect corresponds to the SRE area and will not be taken into account in the following ADR and can be guaranteed by the infrastructure layer.

- Performance and Scalability: Data isolation mechanisms should be designed to minimize performance impacts and provide scalability. The architecture should be able to handle increasing numbers of tenants and their data without sacrificing performance or compromising isolation.

- The Complexity of the Implementation: It's essential to build the multi-tenancy capability for the PRISM platform without the introduction of unnecessary complexity at the application layer, operation layer, and maintenance, in a way that allows evolving the platform naturally along with the growth of the users, scalability requirements, and real business needs.

## Considered Options

All considered options are built using the PostgreSQL database

- Row Security Policies (RSP) - Shared Database, Shared Schemas, Shared Tables, Separate Rows
- Table per Tenant - Shared Instance, Shared Database, Shared Schemas, Separate Tables
- Schema per Tenant - Shared Instance, Shared Database, Separate Schemas
- Database per Tenant - Shared Instance, Separate Database
- Instance per Tenant - Separate Instance,
- Citus extension (sharding) - Sharded Instances
- AWS (sharding) - Sharded Instances

The first five options are the common architecture patterns for multi-tenancy built using PostgreSQL

The level of isolation grows up from the 1st to the 5th option

The maintenance cost grows up from the 1st to the 5th option

The infrastructure cost grows significantly using the 5th option

Sharding options (Citus extension and AWS sharding) must be used with the combination of one of the first five options to provide the scalability and performance aspects by sharding the instances of the database. These options must be considered for SaaS solutions when the number of tenants grows to millions and cannot be managed by a single instance of the database because of resource constraints: disk space, CPU, RAM, disk I/O, and network I/O.

## Decision Outcome

The `Row Security Policies` option is the easiest for implementation at the current phase of the Atala PRISM development.
A single instance of the PostgreSQL database can keep the data and handle requests of hundreds of thousands of tenants leveraging the Row Security Policies without additional operation and infrastructure costs.

At the same time, the PRISM Agent architecture can support `Instance per Tenant` or `Database per Tenant` configuration by isolating the DAL under the repository interface. So, these options also can be considered for organizations with a lot of tenants to provide better isolation and data protection guarantees.
These two options are excellent for a group of tenants under a single organisation or can be considered for tenants that require geographical separation of data, but should not be used for a single tenant.

Moreover, for the SaaS application to manage thousands of organizations and millions of tenants, the `Row Security Policies` option will not be enough because of the resource limitations and amount of requests to the database. In this case, one of the PostgreSQL sharding options is required together with `Row Security Policies`. So, either `Citus extension` or Amazon RDS sharding should be used. `Citus extension` is a preferred way for an on-premise environment, but, it probably, can be used in AWS as well.

### Out of the Scope

- Access Controls - must be enforced by the access control at the application level by implementing a policies enforcement point (PEP) and resolving the tenant.
- Auditing and Logging - must be implemented at the application layer, but basic statistics can be implemented at the database level (by using triggers on the tables)
- Performance and Scalability - are not covered by this option and must be implemented on top of it. Performance and Load tests at the particular infrastructure configuration must be performed in order to know the limitations of the PostgreSQL instance.

### Positive Consequences

- Logical Separation - PostgreSQL RSP allows to separate of the tenant data at the database level end and enforces the ACL using the policies
- The Complexity of the Implementation - this option can be implemented on top of the current codebase without significant refactoring of the codebase and additional work for infrastructure engineers.


### Negative Consequences 

- Physical Separation - is not covered by this option

## Pros and Cons of the Options

### Row Security Policies (RSP)

In addition to the SQL-standard privilege system available through GRANT, tables can have row security policies that restrict, on a per-user basis, which rows can be returned by normal queries or inserted, updated, or deleted by data modification commands. This feature is also known as Row-Level Security. By default, tables do not have any policies, so if a user has access privileges to a table according to the SQL privilege system, all rows within it are equally available for querying or updating.

Having the Wallet abstraction and the `tenantId` that is resolved from JWT issued by PEP, it's possible to logically isolate the rows of the table in the database.

- Good, because logical isolation is performed at the application and database layer
- Good, because it's easy to implement based on the current architecture
- Good, because no additional overhead for DevOps engineers
- Good, because the migration is going to be executed within the same time as for the single tenant solution
- Good, because the cost of the solution is going to be the same as for single-tenant
- Bad, because `noisy neighbors` issue might occur when some tenant is actively using the Wallet and occupies the resources
- Bad, because it might not fit the clients that require a higher level of data isolation.

### Table per Tenant and Schema per Tenant

In this option, the data are logically isolated either in the table or the schema that belongs to the tenant.

For each Wallet abstraction, the tenant must have the table or the schema with the `suffix` as a `tenantId` and routing to the concrete tenant is resolved at the application layer.

- Good, because logical isolation is performed at the application layer
- Good, because it's possible to implement it based on the current architecture (but more work is required compared to option 1)
- Good, because no additional overhead for DevOps engineers
- Good, because the cost of the solution is going to be the same as for single-tenant
- Bad, because all isolation must be figured out at the application layer, so all components must use the same approach to the data isolation
- Bad, because the migration time and maintenance complexity is going to grow with the number of tenants
- Bad, because `noisy neighbors` issue might occur when some tenant is actively using the Wallet and occupies the resources


### Database per Tenant and Instance per Tenant

In this option, the data are physically isolated by using the database or the server instance per tenant.
The logic for data isolation must be implemented at the application layer, so per each tenant, the table with the mapping of the tenant to the database or the instance must be configured.

- Good, because of the physical data isolation
- Good, because of granular control over the resources per each tenant, so `noisy neighbors` issue will not happen in this option
- Bad, because the cost of running the additional database or server instance with grow with the number of tenants
- Bad, because the migration time and maintenance complexity is going to grow with the number of tenants
- Bad, because more work is required for engineers to implement this solution

### Citus extension and AWS - Sharded Instances

Current options must be applied for SaaS solutions with a high number of tenants which requires sharding of the databases.

Both options serve the same goal - horizontal scaling of the instances of PostgreSQL

The main advantages of Citus:
- fits for on-premise deployments
- provides additional monitoring and statistics to manage the tenants
- routing to the shard is managed by Citus using the `hash` of the table index (compared to AWS sharding option, the routing is done at the application layer and the system table contains the information about the mapping of the tenant to the instance of the database)

One of the previously described options `should` be implemented behind these options. 

- Good, because can manage millions of tenants
- Good, because can manage the isolation (logical or physical bases on the configured mapping of the tenant to the database)
- Good, `noisy neighbors` issue can be monitored and solved (Citus)
- Bad, because additional work is required from engineers
- Bad, because the cost of the solution is higher compared to the single-tenant

## Links

- [Strategies for Using PostgreSQL as a Database for Multi-Tenant Services](https://dev.to/lbelkind/strategies-for-using-postgresql-as-a-database-for-multi-tenant-services-4abd)
- [Multi‑Tenant Data Architecture](https://renatoargh.files.wordpress.com/2018/01/article-multi-tenant-data-architecture-2006.pdf)
- [PostgreSQL Row Security Policies](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)
- [Sharding with Amazon Relational Database Service](https://aws.amazon.com/blogs/database/sharding-with-amazon-relational-database-service/)
- [Citusdata](https://www.citusdata.com/)
- [Citusdata Multi-Tenant Applications](https://www.citusdata.com/use-cases/multi-tenant-apps)
