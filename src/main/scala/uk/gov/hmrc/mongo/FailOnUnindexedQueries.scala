package uk.gov.hmrc.mongo


import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Failed, Outcome, Suite}
import reactivemongo.api.DefaultDB
import reactivemongo.bson.BSONDocument
import reactivemongo.core.commands.RawCommand
import reactivemongo.core.errors.ReactiveMongoException

trait FailOnUnindexedQueries extends BeforeAndAfterAll with ScalaFutures {
  this: Suite =>

  def mongo: () => DefaultDB
  protected def databaseName: String

  import scala.concurrent.ExecutionContext.Implicits.global

  override protected def beforeAll() = {
    super.beforeAll()
    mongo().connection.db(databaseName).drop().futureValue
    mongo().connection.db("admin").command(RawCommand(BSONDocument("setParameter" -> 1, "notablescan" -> 1))).futureValue
  }

  override protected def afterAll() = {
    super.afterAll()
    mongo().connection.db("admin").command(RawCommand(BSONDocument("setParameter" -> 1, "notablescan" -> 0))).futureValue
  }

  abstract override def withFixture(test: NoArgTest): Outcome = {
    super.withFixture(test) match {
      case Failed(e: ReactiveMongoException) if e.getMessage() contains "No query solutions" =>
        Failed("Mongo query could not be satisfied by an index:\n" + e.getMessage())
      case other => other
    }
  }
}
