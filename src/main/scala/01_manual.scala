package org.indyscala.di
package manual

import scalaz.concurrent.Task

object MockApp extends App {
  lazy val userDao = new StubUserDao
  lazy val greetingService = new HelloUserGreetingService
  lazy val greeter = new ConsoleGreeter(userDao, greetingService)
  Test.run(greeter)
}

object ProductionApp extends App {
  lazy val userDao = new JsonUserDao("/users.json")
  lazy val translator = new MicrosoftTranslator(sys.env("MS_TRANSLATE_API_KEY"))
  lazy val languageSelector = new UserLanguageSelector
  lazy val greetingService = new HelloUserGreetingService
  lazy val multilingualGreetingService = new MultilingualGreetingService(greetingService, languageSelector, translator)
  lazy val greeter = new ConsoleGreeter(userDao, multilingualGreetingService)
  Test.run(greeter)
}



