# "returning" in update queries

Postgres' `returning` keyword is an extension to the SQL standard not supported everywhere.
EntityFramework uses a temporary table to store the written rows to, and then references it in a join afterwards:

- https://stackoverflow.com/a/40411643/13485494

```tsql
DECLARE @generated_keys table
                        (
                            [Id] uniqueidentifier
                        )

INSERT INTO Customers(FirstName)
OUTPUT inserted.CustomerID INTO @generated_keys
VALUES ('bob');

SELECT t.[CustomerID]
FROM @generated_keys AS g
         JOIN dbo.Customers AS t
              ON g.Id = t.CustomerID
WHERE @@ROWCOUNT > 0
```
