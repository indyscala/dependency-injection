package org.indyscala.di

import scala.io.Source
import scalaz.{Traverse, OptionT}
import scalaz.concurrent.Task
import scalaz.syntax.traverse._
import scalaz.std.vector._

/*** Domain ***/
case class User(
  name: String,
  language: String
)

object Test extends App {
  def run(greeter: Greeter) =
    (1 to 3).toVector.traverse(greeter.greet).run
}

/*** Services ***/
trait Greeter {
  def greet(userId: Int): Task[Unit]
}

trait GreetingService {
  def greeting(user: User): Task[String]
}

trait LanguageSelector {
  def language(user: User): String
}

trait Translator {
  def translate(message: String, language: String): Task[String]
}

trait UserDao {
  def findUser(id: Int): Task[User]
}

/*** Implementations ***/
class ConsoleGreeter(userDao: UserDao, greetingService: GreetingService) extends Greeter {
  def greet(userId: Int) =
    userDao.findUser(userId)
      .flatMap(greetingService.greeting)
      .handle { case e: Exception => e.printStackTrace(); "<error greeting user>" }
      .map(println)
}

class HelloUserGreetingService extends GreetingService {
  def greeting(user: User): Task[String] =
    Task.now(s"Hello, ${user.name}")
}

class MultilingualGreetingService(greetingService: GreetingService,
                                  languageSelector: LanguageSelector,
                                  translator: Translator) extends GreetingService {
  def greeting(user: User): Task[String] =
    for {
      greeting <- greetingService.greeting(user)
      language = languageSelector.language(user)
      translation <- translator.translate(greeting, language)
    } yield translation
}

class UserLanguageSelector extends LanguageSelector {
  def language(user: User) = user.language
}

class StubUserDao extends UserDao {
  override def findUser(id: Int): Task[User] =
    Task.now(User(s"User #${id.toString}", "en"))
}

class JsonUserDao(resource: String) extends UserDao {
  import io.circe._
  import io.circe.generic.auto._

  private val map = {
    // Not a work of parsing art.
    val source = Source.fromInputStream(getClass.getResourceAsStream(resource))
    val rawJson = source.mkString
    val json = jawn.parse(rawJson)
    json.flatMap(_.as[Map[String, User]])
      .valueOr { e => e.printStackTrace(); Map.empty }
      .map { case (k, v) => k.toInt -> v }
  }

  override def findUser(id: Int): Task[User] =
    Task.now(map(id))
}

class StubTranslator extends Translator {
  def translate(message: String, language: String) =
    Task.now(s"$message in $language")
}

class MicrosoftTranslator(clientSecret: String) extends Translator {
  import org.http4s._
  import org.http4s.Http4s._
  import org.http4s.client._
  import org.http4s.circe._
  import io.circe._
  private val client = org.http4s.client.blaze.defaultClient

  def translate(message: String, language: String) =
    fetchToken.flatMap { token =>
      val req = Method.GET(
        uri("http://api.microsofttranslator.com/v2/Http.svc/Translate")
          .withQueryParam("text", message)
          .withQueryParam("from", "en")
          .withQueryParam("to", language))
        .putHeaders(headers.Authorization(OAuth2BearerToken(token)))
      client(req).as[String].map(_.replaceAll("<[^>]*>", ""))
    }

  private def fetchToken: Task[String] =
    client(Method.POST(uri("https://datamarket.accesscontrol.windows.net/v2/OAuth2-13"))
      .withBody(UrlForm(
        "client_id" -> "indyscala-di",
        "client_secret" -> clientSecret,
        "scope" -> "http://api.microsofttranslator.com",
        "grant_type" -> "client_credentials"
      )))
      .as[Json]
      .map { json => json.cursor
        .downField("access_token")
        .flatMap(_.as[String].toOption)
        .getOrElse(sys.error(s"Could not obtain access token: $json"))
      }
}
