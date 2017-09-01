FROM java:openjdk-8u91-jdk
COPY ./build /etc/daacs-api
EXPOSE 8080
VOLUME ["/etc/daacs-api/lightside/saved"]

CMD java $CONFIG_PARAMS -jar /etc/daacs-api/daacs/daacsapi.jar
