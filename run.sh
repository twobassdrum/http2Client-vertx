#/bin/bash
mvn clean package
java -jar ./target/http2Client-vertx-1.0-SNAPSHOT-fat.jar -conf ./src/main/conf/Http2Client.json
