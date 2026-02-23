package pmeig.spring.libraries.test

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
//@EnablePmeigSecurity
class TestApplication

fun main(args: Array<String>) {
  runApplication<TestApplication>(*args)
}
