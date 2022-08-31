javac src/main/java/jess/*java -d target/classes -verbose -deprecation
jar  -cfe jess-8.0b1.jar jess.Main target/classes/Main.class  -C target/classes jess