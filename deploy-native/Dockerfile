FROM registry.access.redhat.com/ubi9/ubi-minimal:9.3
WORKDIR /work/
COPY application /work/application
RUN chmod 775 /work /work/application
EXPOSE 8080
CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]
