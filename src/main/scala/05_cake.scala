package org.indyscala.di
package cake

import scalaz.concurrent.Task

trait GreeterComponent {
  def greeter: Greeter
}
trait GreetingServiceComponent {
  def greetingService: GreetingService
}
trait UserDaoComponent {
  def userDao: UserDao
}
trait TranslatorComponent {
  def translator: Translator
}
trait LanguageSelectorComponent {
  def languageSelector: LanguageSelector
}

trait ConsoleGreeterComponent
  extends GreeterComponent // extend what you provide
  with UserDaoComponent // with your dependencies
  with GreetingServiceComponent
{
  def greeter = new ConsoleGreeter(userDao, greetingService)
}

trait CakeGreetingServiceComponent
  extends GreetingServiceComponent
{
  def greetingService = new GreetingService {
    def greeting(user: User): Task[String] =
      Task.now("Hey, {username}.  Here's a cake.".replace("{username}", user.name))
  }
}

trait MultilingualGreetingServiceComponent
  extends GreetingServiceComponent
  with LanguageSelectorComponent
  with TranslatorComponent
{
  def baseGreetingService: GreetingService

  lazy val greetingService = new MultilingualGreetingService(
    baseGreetingService,
    languageSelector,
    translator
  )
}

trait JsonUserDaoComponent {
  def userResource: String
  lazy val userDao: UserDao = new JsonUserDao(userResource)
}

trait MicrosoftTranslatorComponent
  extends TranslatorComponent
{
  def apiKey: String
  lazy val translator: Translator = new MicrosoftTranslator(apiKey)
}

trait FixedLanguageSelectorComponent {
  def language: String
  lazy val languageSelector: LanguageSelector = new LanguageSelector {
    override def language(user: User): String =
      FixedLanguageSelectorComponent.this.language
  }
}

trait CakeApp extends App
  with ConsoleGreeterComponent
  with MultilingualGreetingServiceComponent
  with FixedLanguageSelectorComponent
  with MicrosoftTranslatorComponent
  with JsonUserDaoComponent
{
  lazy val userResource = "/users.json"
  lazy val apiKey = sys.env("MS_TRANSLATE_API_KEY")

  // It gets ugly when you have two of the same thing.
  lazy val baseGreetingService = new CakeGreetingServiceComponent {}.greetingService

  Test.run(greeter)
}

object FrenchApp extends CakeApp {
  lazy val language = "fr"
}

object RussianApp extends CakeApp {
  lazy val language = "ru"
}


