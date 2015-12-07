package org.indyscala.di
package implicitz

import scalaz.{Tag, @@}
import scalaz.concurrent.Task

// Tags to resolve ambiguous implicits
trait UserResource
trait ClientSecret
trait Monoglot

// This looks nice enough...
object ImplicitzApp extends App {
  implicit val userResource = Tag.of[UserResource]("/users.json")
  implicit val clientSecret = Tag.of[ClientSecret](sys.env("MS_TRANSLATE_API_KEY"))
  implicit val userDao = JsonUserDao
  implicit val translator = MicrosoftTranslator
  implicit val languageSelector = UserLanguageSelector
  implicit val monoglotGreetingService: GreetingService @@ Monoglot = Tag.of[Monoglot](HelloUserGreetingService)
  implicit val polyglotGreetingService = MultilingualGreetingService
  implicit val greeter = ConsoleGreeter

  import scalaz.std.vector._
  import scalaz.syntax.traverse._
  (1 to 3).toVector.traverse(greeter.greet).run
}

// ... but this degrades quickly.

object JsonUserDao {
  def findUser(userId: Int)(implicit userResource: String @@ UserResource): Task[User] =
    new JsonUserDao(Tag.unwrap(userResource)).findUser(userId)
}

object MicrosoftTranslator {
  def translate(message: String, language: String)(implicit clientSecret: String @@ ClientSecret): Task[String] =
    new MicrosoftTranslator(Tag.unwrap(clientSecret)).translate(message, language)
}

object UserLanguageSelector extends UserLanguageSelector

object HelloUserGreetingService extends HelloUserGreetingService

object MultilingualGreetingService {
  def greeting(user: User)(implicit greetingService: GreetingService @@ Monoglot,
                           languageSelector: LanguageSelector,
                           translator: MicrosoftTranslator.type,
                           clientSecret: String @@ ClientSecret): Task[String] =
    for {
      greeting <- Tag.unwrap(greetingService).greeting(user)
      language = languageSelector.language(user)
      translation <- translator.translate(greeting, language)
    } yield translation
}

object ConsoleGreeter {
  // EEK!  JsonUserDao has ceased to be a UserDao because of its own implicit parameters.  Now we have
  // to hardcode a concrete dependency, or add more boilerplate to adapt it!
  def greet(userId: Int)(implicit userDao: JsonUserDao.type,
                         greetingService: MultilingualGreetingService.type,
                         // dependencies of our dependencies, oh my!
                         userResource: String @@ UserResource,
                         monoglotGreetingService: GreetingService @@ Monoglot,
                         languageSelector: LanguageSelector,
                         translator: MicrosoftTranslator.type,
                         clientSecret: String @@ ClientSecret): Task[Unit] =
    userDao.findUser(userId)
      .flatMap(greetingService.greeting)
      .handle { case e: Exception => e.printStackTrace(); "<error greeting user>" }
      .map(message => println(message))
}




