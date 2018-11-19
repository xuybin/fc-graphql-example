# example
[![Build Status](https://travis-ci.org/xuybin/fc-graphql-spring-example.svg?branch=master)](https://travis-ci.org/xuybin/fc-graphql-spring-example)

### debug
``` bash
./gradlew run
```

### Serverless Cloud Function Deploy
``` bash
export ACCOUNT_ID=xxxxxxxx
export DEFAULT_REGION=cn-shenzhen
export ACCESS_KEY_ID=xxxxxxxxxxxx
export ACCESS_KEY_SECRET=xxxxxxxxxx
./gradlew assemble && ./gradlew funDeploy --info
```

### Microservice Deploy
``` bash
./gradlew assemble

```