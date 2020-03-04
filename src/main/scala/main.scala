import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import java.time.DayOfWeek

object CustomProfile extends CustomProfile

trait CustomProfile extends  slick.jdbc.PostgresProfile {
  import java.sql.{ResultSet, PreparedStatement}
  override val columnTypes = new JdbcTypes

  class JdbcTypes extends super.JdbcTypes {

    class DayOfWeekJdbcType extends DriverJdbcType[DayOfWeek] {
      def sqlType = java.sql.Types.OTHER
      def setValue(v: DayOfWeek, p: PreparedStatement, idx: Int) = p.setObject(idx, v, sqlType)
      def getValue(r: ResultSet, idx: Int) = DayOfWeek.valueOf(r.getString(idx))
      def updateValue(v: DayOfWeek, r: ResultSet, idx: Int) = r.updateObject(idx, v)
    }

    val dowCol = new DayOfWeekJdbcType()
  }

  override val api = CustomAPI

  object CustomAPI extends API {
    implicit def dowColumnType: BaseColumnType[DayOfWeek] = columnTypes.dowCol
  }
}

object Example {

  import CustomProfile.api._

  case class Shift(day: DayOfWeek, duration: Int)

  class ShiftTable(tag: Tag) extends Table[Shift](tag, "shifts") {
    def day = column[DayOfWeek]("day", O.SqlType("WeekDay"))
    def duration = column[Int]("duration")
    def * = (day, duration).mapTo[Shift]
  }

  lazy val shifts = TableQuery[ShiftTable]

  def main(args: Array[String]): Unit = {

    def schema = DBIO.seq(
      sqlu" create type WeekDay as ENUM('SUNDAY','MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY') ",
      sqlu" create table shifts( duration INTEGER, day WeekDay ) ",
    )

    val program = for {
      _ <- schema.asTry
      _ <- shifts.delete
      _ <- shifts += Shift(DayOfWeek.MONDAY, 1)
      _ <- shifts += Shift(DayOfWeek.TUESDAY, 2)
      _ <- shifts.filter(_.day === DayOfWeek.TUESDAY).update(Shift(DayOfWeek.SATURDAY, 3))
      rows <- shifts.result
    } yield rows

    val db = Database.forConfig("example")
    try 
      println(
        Await.result(db.run(program), 2.seconds)
      )
    finally db.close
  }
}
