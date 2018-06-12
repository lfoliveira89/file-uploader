FROM openjdk

VOLUME /tmp

ADD maven/file-uploader.jar fileuploader.jar

RUN sh -c 'touch /fileuploader.jar'

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/fileuploader.jar"]
