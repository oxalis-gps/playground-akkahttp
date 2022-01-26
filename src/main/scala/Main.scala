import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.coding.Coders
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives
import domain.entity.Todo
import domain.valueobject.Todo.{TodoID, 作成日, 内容}
import scalikejdbc._
import scalikejdbc.config._
import spray.json._

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val localDateTimeFormat = new JsonFormat[LocalDateTime] {
    private val iso_date_time = DateTimeFormatter.ISO_DATE_TIME
    def write(x: LocalDateTime) = JsString(iso_date_time.format(x))
    def read(value: JsValue) = value match {
      case JsString(x) => LocalDateTime.parse(x, iso_date_time)
      case x => throw new RuntimeException(s"Unexpected type ${x.getClass.getName} when trying to parse LocalDateTime")
    }
  }

  implicit val todoIdFormat = jsonFormat(TodoID.apply _, "value")
  implicit val 内容Format = jsonFormat(内容.apply _, "value")
  implicit val createdAtFormat = jsonFormat(作成日.apply _, "value")
  implicit val todoFormat = jsonFormat(Todo.apply, "id", "内容", "created_at")
}


object Main extends App with Directives with JsonSupport {
  implicit val system = ActorSystem(Behaviors.empty, "my-system")
  implicit val executionContext = system.executionContext

  DBs.setupAll()

  val route = concat(
    pathSingleSlash {
      get {
        encodeResponseWith(Coders.Gzip) {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>hello, world.</h1>"))
        }
      }
    },

    path("todo") {
      concat(
        get {
          val todoList = DB readOnly { implicit session =>
            sql"SELECT * FROM Todo".map { rs =>
              Todo(
                TodoID(rs.get("uuidv4")),
                内容(rs.get("内容")),
                作成日(rs.get("created_at"))
              )
            }.list.apply()
          }

          complete(todoList)
        },
        post {
          val a = DB localTx { implicit session =>
            val uuid: String = java.util.UUID.randomUUID.toString
            val todo = Todo(TodoID(uuid), 内容("テスト"), 作成日(LocalDateTime.now))

            sql"""
                 INSERT INTO Todo (
                                   UUIDv4,
                                   内容,
                                   created_at) VALUES (
                                             ${todo.id.value},
                                             ${todo.内容.value},
                                             ${todo.作成日.value})
                 """.update.apply()
          }
          complete("created")
        }
      )
    },
  )

  val host = sys.props.get("http.host") getOrElse "0.0.0.0"
  val port = sys.props.get("http.port").fold(8080) { _.toInt }

  val bindingFuture = Http().newServerAt(host, port).bind(route)
  println(s"Server now online. Please navigate to http://${host}:${port}/\nPress RETURN to stop...")

  // Await.ready(bindingFuture, Duration.Inf)
  StdIn.readLine() // let it run until user presses return

  bindingFuture
    .flatMap(_.unbind())
    .onComplete { _ =>
      DBs.closeAll()
      system.terminate()
    }
}
