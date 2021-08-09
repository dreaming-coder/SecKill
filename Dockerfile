FROM java:8

MAINTAINER ice

ADD seckill.jar /usr/local/docker/seckill.jar

CMD java -jar /usr/local/docker/seckill.jar

EXPOSE 8080