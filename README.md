# Spring Boot - jQuery File Upload

This is a Spring Boot web application configured with H2 in-memory database that supports chunked file uploads.<br />
Graphic interface was built using jQuery.

## Getting Started

### Prerequisites
* Git
* JDK 8 or later
* Maven 3.0 or later

### Clone
To get started you can simply clone this repository using git:
```
git clone https://github.com/lfoliveira89/file-uploader.git
cd file-uploader
```

### Quick start
You can run the tests from the command line using:
```
mvn test
```
You can run the application from the command line using:
```
mvn spring-boot:run
```
Now open a browser and visit [http://localhost:8080/index.html](http://localhost:8080/index.html).

## API

### Project Structure
The project is organized as follow:

    file-uploader/
     └── src/test/java/fileuploader/            # test files
     └── src/main/
         └── java/fileuploader/
             ├── configuration/                 # configuration files
             ├── controller/                    # controller files
             ├── domain/                        # domain files
             ├── enums/                         # enum files
             ├── exceptions/                    # exception files
             ├── projection/                    # projection files
             ├── repositories/                  # repository files
             ├── services/                      # service files
             ├── utils/                         # util files
             └── FileUploadApplication.java     # Starts application
         └── resources                          # resources files
         └── webapp                             # web application files

### Documentation
API documentation is being generated by Swagger and can be accessed at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).
<br />
<br />
Sample:
<br />
<br />
| URL               | Method    | Example                       | Description                                               |
| ----------------- | --------- | ----------------------------- | --------------------------------------------------------- |
| /api/files        | GET       | localhost:8080/api/files      | Returns a complete list of uploaded files                 |
| /api/files/{id}   | GET       | localhost:8080/api/files/1    | Returns an uploaded file by its id                        |
| /api/files        | POST      | localhost:8080/api/files      | Uploads a file either via multipart or chunked transfer   |

### Health check
This project uses Spring actuator in order to implement health check which can be accessed at [http://localhost:8080/health](http://localhost:8080/health).

## Tips
Depending on the size of the uploaded file it may be required to increase the JVM heap size. For instance:
```
mvn spring-boot:run -Drun.jvmArguments="-Xmx6144m"
```