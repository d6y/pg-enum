import slick.jdbc.PostgresProfile.api._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Example {

  import java.time.DayOfWeek
  case class Shift(day: DayOfWeek, duration: Int)

  implicit val dowMapper = MappedColumnType.base[DayOfWeek, String](
    day => day.toString.take(3).toLowerCase, // MONDAY -> mon etc
    str => str match {
      case "mon" => DayOfWeek.MONDAY
      case "tue" => DayOfWeek.TUESDAY
      case "wed" => DayOfWeek.WEDNESDAY
      case "thu" => DayOfWeek.THURSDAY
      case "fri" => DayOfWeek.FRIDAY
      case "sat" => DayOfWeek.SATURDAY
      case "sun" => DayOfWeek.SUNDAY
    }
  )

  class ShiftTable(tag: Tag) extends Table[Shift](tag, "shifts") {
    def day = column[DayOfWeek]("day", O.SqlType("weekday"))
    def duration = column[Int]("duration")
    def * = (day, duration).mapTo[Shift]
  }

  lazy val shifts = TableQuery[ShiftTable]

  def main(args: Array[String]): Unit = {

    def schema = DBIO.seq(
      sqlu" create type WeekDay as ENUM('sun','mon','tue','wed','thu','fri','sat') ",
      sqlu" create table shifts( duration INTEGER, day WeekDay ) ",
      sqlu" CREATE CAST (character varying AS WeekDay) WITH INOUT AS ASSIGNMENT ",
    )

    val program = for {
      _ <- schema.asTry
      _ <- shifts.delete
      _ <- shifts += Shift(DayOfWeek.MONDAY, 1)
      _ <- shifts += Shift(DayOfWeek.TUESDAY, 2)
      _ <- shifts.filter(_.day === DayOfWeek.TUESDAY).update(Shift(DayOfWeek.WEDNESDAY, 3))
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
