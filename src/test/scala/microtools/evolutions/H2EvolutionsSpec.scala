package microtools.evolutions

import java.sql.Connection

import javax.sql.DataSource
import microtools.evolutions.H2EvolutionsSpec.TestDatabase
import org.scalatest.{MustMatchers, WordSpec}
import play.api.db.evolutions.{Evolution, SimpleEvolutionsReader}
import play.api.db.Database

class H2EvolutionsSpec extends WordSpec with MustMatchers {
  "H2Evolutions" should {
    "not patch evolutions" in {
      val evolutions = Seq(
        Evolution(
          revision = 1,
          sql_up = """CREATE UNIQUE INDEX "email_unique_idx" on Logins (LOWER("login"));""",
          sql_down = """DROP INDEX "email_unique_idx";"""
        )
      )

      val patchedEvolutions =
        H2Evolutions.patchEvolutions(new TestDatabase,
                                     SimpleEvolutionsReader.forDefault(evolutions: _*))

      patchedEvolutions.head.sql_up mustBe """CREATE UNIQUE INDEX "email_unique_idx" on Logins (LOWER("login"));"""
      patchedEvolutions.head.sql_down mustBe """DROP INDEX "email_unique_idx";"""
    }

    "patch evolutions" in {
      val evolutions = Seq(
        Evolution(
          revision = 1,
          sql_up = """CREATE UNIQUE INDEX "email_unique_idx" on Logins (LOWER("login"));
              |--h2: CREATE UNIQUE INDEX "email_unique_idx" on Logins ("login");
            """.stripMargin,
          sql_down = """DROP INDEX "email_unique_idx";
              |--h2: DROP INDEX "email_unique_idx";
            """.stripMargin
        ),
        Evolution(
          revision = 2,
          sql_up = """CREATE UNIQUE INDEX "email_unique_idx" on Logins (LOWER("login"));
              |--h2:
            """.stripMargin,
          sql_down = """DROP INDEX "email_unique_idx";
              |--h2:
            """.stripMargin
        ),
        Evolution(
          revision = 3,
          sql_up = """CREATE UNIQUE INDEX "email_unique_idx" on Logins (LOWER("login"));
              |--h2:CREATE TABLE Logins (
              |--h2:  login            VARCHAR(50) CONSTRAINT pk PRIMARY KEY,
              |--h2:  hashed_password  VARCHAR(50) NULL
              |--h2:);
              |--h2:CREATE UNIQUE INDEX "email_unique_idx" on Logins ("login");""".stripMargin,
          sql_down = """DROP INDEX "email_unique_idx";
              |--h2: DROP INDEX "email_unique_idx";
              |--h2: DROP TABLE Logins;""".stripMargin
        )
      )

      val patchedEvolutions =
        H2Evolutions.patchEvolutions(new TestDatabase,
                                     SimpleEvolutionsReader.forDefault(evolutions: _*))

      patchedEvolutions.head.sql_up mustBe """ CREATE UNIQUE INDEX "email_unique_idx" on Logins ("login");"""
      patchedEvolutions.head.sql_down mustBe """ DROP INDEX "email_unique_idx";"""

      patchedEvolutions(1).sql_up mustBe ""
      patchedEvolutions(1).sql_down mustBe ""

      patchedEvolutions(2).sql_up mustBe
        """CREATE TABLE Logins (
        |  login            VARCHAR(50) CONSTRAINT pk PRIMARY KEY,
        |  hashed_password  VARCHAR(50) NULL
        |);
        |CREATE UNIQUE INDEX "email_unique_idx" on Logins ("login");""".stripMargin

      patchedEvolutions(2).sql_down mustBe
        """ DROP INDEX "email_unique_idx";
          | DROP TABLE Logins;""".stripMargin
    }
  }
}

object H2EvolutionsSpec {
  class TestDatabase extends Database {
    override def name: String                                                      = "default"
    override def dataSource: DataSource                                            = ???
    override def url: String                                                       = ???
    override def getConnection(): Connection                                       = ???
    override def getConnection(autocommit: Boolean): Connection                    = ???
    override def withConnection[A](block: Connection => A): A                      = ???
    override def withConnection[A](autocommit: Boolean)(block: Connection => A): A = ???
    override def withTransaction[A](block: Connection => A): A                     = ???
    override def shutdown(): Unit                                                  = ???
  }
}
