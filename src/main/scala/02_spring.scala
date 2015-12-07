package org.indyscala.di
package spring

import org.springframework.context.support.ClassPathXmlApplicationContext

// n.b. I had to run this seven times to get the XML right. Don't do this.
object SpringApp extends App {
  val applicationContext = new ClassPathXmlApplicationContext("/applicationContext.xml")
  val greeter = applicationContext.getBean(classOf[Greeter])
  Test.run(greeter)
}

