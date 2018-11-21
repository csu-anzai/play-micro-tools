package microtools.evolutions

import play.api.db.Database
import play.api.db.evolutions._
import play.api.db.evolutions.Evolutions

import scala.util.Try

object H2Evolutions {
  private val PATTERN = """--h2:(.*)""".r

  def applyEvolutions(database: Database,
                      evolutionsReader: EvolutionsReader = ThisClassLoaderEvolutionsReader,
                      autocommit: Boolean = true,
                      schema: String = ""): Unit = {
    val evolutions = patchEvolutions(database, evolutionsReader)

    Evolutions.applyEvolutions(
      database = database,
      evolutionsReader = SimpleEvolutionsReader.forDefault(evolutions: _*),
      autocommit = autocommit,
      schema = schema
    )
  }

  def patchEvolutions(database: Database, evolutionsReader: EvolutionsReader): Seq[Evolution] = {
    evolutionsReader.evolutions(database.name).map { evolution =>
      val upMatches   = matches(evolution.sql_up)
      val downMatches = matches(evolution.sql_down)

      (upMatches, downMatches) match {
        case (Nil, Nil)                => evolution
        case (ups: Seq[String], Nil)   => evolution.copy(sql_up = ups.mkString("\n"))
        case (Nil, downs: Seq[String]) => evolution.copy(sql_down = downs.mkString("\n"))
        case (ups: Seq[String], downs: Seq[String]) =>
          evolution.copy(sql_up = ups.mkString("\n"), sql_down = downs.mkString("\n"))
      }
    }
  }

  private def matches(statements: String): Seq[String] = {
    val matches = for (m <- PATTERN.findAllIn(statements).matchData)
      yield Try(m.group(1)).toOption

    matches.flatten.toSeq
  }
}
