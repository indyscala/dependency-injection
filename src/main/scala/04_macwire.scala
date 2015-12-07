package org.indyscala.di
package macwire

import com.softwaremill.macwire._
import com.softwaremill.tagging._

object MacWireApp extends App {
  implicit lazy val userDao = new JsonUserDao("/users.json")
  implicit lazy val translator = new MicrosoftTranslator(sys.env("MS_TRANSLATE_API_KEY"))
  lazy val languageSelector = wire[UserLanguageSelector]
  lazy val greetingService = wire[HelloUserGreetingService].taggedWith[Monoglot]
  lazy val multilingualGreetingService = wire[TaggedMultilingualGreetingService].taggedWith[Polyglot]
  lazy val greeter = wire[TaggedConsoleGreeter]
  Test.run(greeter)
}

// Marker traits to disambiguate
trait Monoglot
trait Polyglot

// I'm just reusing code.  In the real world, you'd flatten this out,
// and take the tagged dependencies in the constructor.
class TaggedMultilingualGreetingService(greetingService: GreetingService @@ Monoglot,
                                        languageSelector: LanguageSelector,
                                        translator: Translator)
  extends MultilingualGreetingService(greetingService, languageSelector, translator)

class TaggedConsoleGreeter(userDao: UserDao,
                           greetingService: GreetingService @@ Polyglot)
  extends ConsoleGreeter(userDao, greetingService)

