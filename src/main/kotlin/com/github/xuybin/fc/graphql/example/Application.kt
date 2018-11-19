package com.github.xuybin.fc.graphql.example

import com.github.xuybin.fc.graphql.GApp
import com.github.xuybin.fc.graphql.GRequest
import com.github.xuybin.fc.graphql.GSchema
import com.google.gson.Gson
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import kotlin.reflect.KClass

@Configuration
@ComponentScan
class Application

class AppMain:GApp{

    private var appContext : ConfigurableApplicationContext?=null
    override fun init( args: Array<String>) {
        appContext = SpringApplication.run(Application::class.java, *args)
    }

    override fun <T : Any> getBean(clazz: KClass<T>): T {
        return requireNotNull(appContext){"call ${this.javaClass.canonicalName}.init() befor use."}.getBean(clazz.java)
    }

    override fun <T : Any> getBean(clazz: KClass<T>, name: String): T {
        return requireNotNull(appContext){"call ${this.javaClass.canonicalName}.init() befor use."}.getBean(name,clazz.java)
    }

    private val gson = Gson()
    override fun toJson(src: Any?): String {
        return gson.toJson(src)
    }

    override fun fromJson(queryJson: String): GRequest {
        return gson.fromJson(queryJson,GRequest::class.java)
    }

    override fun getGSchema(): List<GSchema> {
        return listOf<GSchema>(getBean(HelloGraphql::class))
    }
}