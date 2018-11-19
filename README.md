# example
[![Build Status](https://travis-ci.org/xuybin/fc-graphql-spring-example.svg?branch=master)](https://travis-ci.org/xuybin/fc-graphql-spring-example)

1. 在build.gradle.kts引入com.github.xuybin:fc-graphql-spring和必要配置
2. 在resources下新建schema目录,并在HelloGraphql.graphqls内定义好graphql模型
3. 编写HelloGraphql类,根据模型实现对应的接口，且提供与模型相同的同名fun
4. 在resources/META-INF/services下新建对应的接口全称的文件,在内部写入实现类的规范名称
5. 本地调试./gradlew run 访问http://localhost:8080/graphiql.html
6. Aliyun部署,设置环境变量后执行打包并部署即可
``` bash
export ACCOUNT_ID=xxxxxxxx
export DEFAULT_REGION=cn-shenzhen
export ACCESS_KEY_ID=xxxxxxxxxxxx
export ACCESS_KEY_SECRET=xxxxxxxxxx
./gradlew assemble && ./gradlew funDeploy --info
```
