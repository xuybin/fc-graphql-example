# example
[![Build Status](https://travis-ci.org/xuybin/fc-graphql-example.svg?branch=master)](https://travis-ci.org/xuybin/fc-graphql-example)

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
git clone https://github.com/xuybin/fc-graphql-example.git
cd fc-graphql-example
docker-compose up
```
### Visit
``` bash
curl -H "Content-Type: application/json" -d '{"query":"{\n  version\n}\n","variables":null}' http://fc.gshbzw.com/example-service
```