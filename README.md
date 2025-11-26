# TableRating

Media collection manager (Games, Movies, Books, TV Shows) implemented using raw Java Servlets and JDBC.

## Project Description

This application was developed to demonstrate the implementation of a web architecture without using high-level frameworks.

### Architecture
* **Custom MVC**: Implementation of the Model-View-Controller pattern using `HttpServlet`.
* **Dependency Injection**: Custom DI container (`AppContextListener`) for managing component lifecycle.
* **Data Access**: `AbstractMediaDao` base class implementing generic CRUD operations.
* **Transaction Management**: `DaoHelper` wrapper for handling JDBC transactions.

### Testing
* **JUnit 5 & Mockito**: Backend unit and integration tests.
* **Jest**: Frontend JavaScript testing.

## Tech Stack

* **Java**: 17
* **Web Server**: Apache Tomcat 10 
* **Database**: MySQL 8.0
* **Build Tool**: Maven 3.9
* **Testing**: JUnit 5, Mockito, H2 Database, Jest
* **Libraries**: HikariCP, BCrypt, Logback, Gson, Jakarta JSTL.


### Configuration
Create `src/main/resources/application.properties`:
```
db.url=jdbc:mysql://localhost:3306/your_db_name
db.user=your_user
db.password=your_password
```

## Project Structure
```src/main/
├── java/org/criticizer/
│   ├── constants/
│   ├── dao/
│   ├── entity/
│   ├── exceptions/
│   ├── filter/
│   ├── listener/
│   ├── service/
│   ├── servlets/
│   └── util/
├── resources/
└── webapp/
    ├── css/
    ├── js/
    ├── jsp/
    └── WEB-INF/
        ├── admin/      
        ├── fragments/  
        └── templates/  
```