package domain.entity
import scalikejdbc._

import domain.valueobject.Todo.{TodoID, 作成日, 内容}

case class Todo(id: TodoID, 内容: 内容, 作成日: 作成日)

object Todo extends SQLSyntaxSupport[Todo]{
  override val tableName = "Todo"

  def apply(u: ResultName[Todo])(rs: WrappedResultSet): Todo = Todo(
    TodoID(rs.string("uuidv4")),
    内容(rs.string("内容")),
    作成日(rs.localDateTime("created_at"))
  )
}
