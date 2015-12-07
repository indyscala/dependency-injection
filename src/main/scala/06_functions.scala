package org.indyscala.di
package functions

import scalaz.concurrent.Task

object JsonUserDao {
  val findUser: String => Int => Task[User] =
    userResource => userId => new JsonUserDao(userResource).findUser(userId)
}

object MicrosoftTranslator {
  val translate: String => (String, String) => Task[String] =
    clientSecret => (message, language) => new MicrosoftTranslator(clientSecret).translate(message, language)
}

object UserLanguageSelector {
  val language: User => String =
    user => user.language
}

object HelloUserGreetingService {
  val greeting: User => Task[String] =
    user => new HelloUserGreetingService().greeting(user)
}

object MultilingualGreetingService {
  val greeting: (User => Task[String], User => String, (String, String) => Task[String]) => User => Task[String] =
    (greetingService, languageSelector, translator) => user =>
      for {
        greeting <- greetingService(user)
        language = languageSelector(user)
        translation <- translator(greeting, language)
      } yield translation
}

object ConsoleGreeter {
  val greet: (Int => Task[User], User => Task[String]) => Int => Task[Unit] =
    (userDao, greetingService) => userId =>
      userDao(userId)
        .flatMap(user => greetingService(user))
        .handle { case e: Exception => e.printStackTrace(); "<error greeting user>" }
        .map(message => println(message))
}

object FunctionalApp extends App {
  lazy val userResource = "/users.json"
  lazy val clientSecret = sys.env("MS_TRANSLATE_API_KEY")

  def greet(userId: Int): Task[Unit] = {
    val userDao = JsonUserDao.findUser(userResource)
    val translator = MicrosoftTranslator.translate(clientSecret)
    val languageSelector = UserLanguageSelector.language
    val monoglotGreetingService = HelloUserGreetingService.greeting
    val polyglotGreetingService = MultilingualGreetingService.greeting(monoglotGreetingService, languageSelector, translator)
    val greeter = ConsoleGreeter.greet(userDao, polyglotGreetingService)
    greeter(userId)
  }

  import scalaz.std.vector._
  import scalaz.syntax.traverse._
  (1 to 3).toVector.traverse(greet).run
}



