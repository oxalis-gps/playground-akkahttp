package domain.valueobject

import java.time.LocalDateTime

object Todo {
  case class TodoID(value: String)
  case class 内容(value: String)
  case class 作成日(value: LocalDateTime)
}
