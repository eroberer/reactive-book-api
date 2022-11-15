# Simple Book API (Helidon SE)

The project provides simple book crud rest api that uses the dbclient API with an in-memory H2 database, helidon se (reactive) and java 17+.

> Helidon SE: Transparent "no magic" development experience; pure java application development with no annotations and no dependency injections.
-> [Helidon Doc](https://helidon.io/docs/v3/#/se/introduction)


## Build and run

With JDK17+
```bash
mvn package
java -jar target/reactive-book-api.jar
```

## The application endpoints

### Book List
```
curl -X GET http://localhost:8080/books

-- response
{
    "status":"SUCCESS", 
    "body":[
        {
            "author":"Ahmet Hamdi Tanpınar",
            "isbn":"9759952378",
            "language":"Türkçe",
            "name":"Saatleri Ayarlama Enstitüsü"
        }...
    ]
}
```

### Get Book By ISBN
```
curl -X GET http://localhost:8080/books/9759952378

-- response
{
    "status":"SUCCESS", 
    "body":{
        "author":"Ahmet Hamdi Tanpınar",
        "isbn":"9759952378",
        "language":"Türkçe",
        "name":"Saatleri Ayarlama Enstitüsü"
    }
}
```

### Insert Book
```
curl -H "Content-Type: application/json" --request POST --data '{"isbn":"23131299", "author":"test", "name":"Test", "language":"EN"}' http://localhost:8080/books/

-- response
{
    "status":"SUCCESS",
    "body":"Inserted: 1 values"
}
```

### Delete Book
```
curl -X DELETE http://localhost:8080/books/9759952378

-- response
{
    "status":"SUCCESS",
    "body":"Deleted: 1 values"
}
```

### Update Book
```
curl -H "Content-Type: application/json" --request PUT --data '{"isbn":"231312992", "author":"test22", "name":"Test321", "language":"EN2"}' http://localhost:8080/books/23131299

-- response
{
    "status":"SUCCESS",
    "body":"Updated: 1 values"
}
```