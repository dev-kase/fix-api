
            title Next. Trade System

            set CLASSPATH=%CLASSPATH%;sample-app.jar
            set CLASSPATH=%CLASSPATH%;lib/nextfix.jar
            set CLASSPATH=%CLASSPATH%;lib/mina-core-1.1.7.jar
            set CLASSPATH=%CLASSPATH%;lib/slf4j-jdk14-1.6.3.jar
            set CLASSPATH=%CLASSPATH%;lib/slf4j-api-1.6.3.jar

            java -cp %CLASSPATH% ${main.class} %1 %2 %3

            pause
        