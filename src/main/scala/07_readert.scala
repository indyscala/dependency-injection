package org.indyscala.di
package reader

import scalaz.{ReaderT, Kleisli, Reader}
import scalaz.concurrent.Task
import scalaz.syntax.kleisli._

trait Config {
  def userResource: String
  def clientSecret: String
  def userDao: UserDaoR
  def languageSelector: LanguageSelectorR
  def translator: TranslatorR
  def monoglotGreetingService: GreetingServiceR
  def polyglotGreetingService: GreetingServiceR
  def consoleGreeter: GreeterR
}

trait UserDaoR {
  def findUser(userId: Int): ReaderT[Task, Config, User]
}
trait TranslatorR {
  def translate(message: String, language: String): ReaderT[Task, Config, String]
}
trait LanguageSelectorR {
  def language(user: User): Reader[Config, String]
}
trait GreetingServiceR {
  def greeting(user: User): ReaderT[Task, Config, String]
}
trait GreeterR {
  def greet(userId: Int): ReaderT[Task, Config, Unit]
}

object JsonUserDao extends UserDaoR {
  override def findUser(userId: Int): ReaderT[Task, Config, User] = Kleisli { cfg =>
    new JsonUserDao(cfg.userResource).findUser(userId)
  }
}

object MicrosoftTranslator extends TranslatorR {
  override def translate(message: String, language: String): ReaderT[Task, Config, String] = Kleisli { cfg =>
    new MicrosoftTranslator(cfg.clientSecret).translate(message, language)
  }
}

object UserLanguageSelector extends LanguageSelectorR {
  override def language(user: User): Reader[Config, String] =
    user.language.liftReader[Config]
}

object HelloUserGreetingService extends GreetingServiceR {
  override def greeting(user: User): ReaderT[Task, Config, String] =
    new HelloUserGreetingService().greeting(user).liftReaderT[Config]
}


object MultilingualGreetingService extends GreetingServiceR {
  override def greeting(user: User): ReaderT[Task, Config, String] =
    for {
      cfg <- Kleisli.ask[Task, Config]
      greeting <- cfg.monoglotGreetingService.greeting(user)
      language <- cfg.languageSelector.language(user).lift[Task]
      translation <- cfg.translator.translate(greeting, language)
    } yield translation
}

object ConsoleGreeter extends GreeterR {
  override def greet(userId: Int): ReaderT[Task, Config, Unit] = {
    for {
      cfg <- Kleisli.ask[Task, Config]
      user <- cfg.userDao.findUser(userId)
      greeting <- cfg.polyglotGreetingService.greeting(user).mapK { _.handle {
        case e: Exception => e.printStackTrace(); "<error getting user>"
      }}
    } yield println(greeting)
  }
}

object ProdConfig extends Config {
  lazy val userResource = "/users.json"
  lazy val clientSecret = sys.env("MS_TRANSLATE_API_KEY")
  lazy val userDao: UserDaoR = JsonUserDao
  lazy val languageSelector: LanguageSelectorR = UserLanguageSelector
  lazy val translator: TranslatorR = MicrosoftTranslator
  lazy val monoglotGreetingService: GreetingServiceR = HelloUserGreetingService
  lazy val polyglotGreetingService: GreetingServiceR = MultilingualGreetingService
  lazy val consoleGreeter: GreeterR = ConsoleGreeter
}

object ReaderTApp extends App {
  import ProdConfig._
  import scalaz.std.vector._
  import scalaz.syntax.traverse._
  (1 to 3).toVector.traverse(consoleGreeter.greet(_).run(ProdConfig)).run
}

