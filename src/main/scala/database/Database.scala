package database

import models.Models.{Mobile, MobileForm}
import scalikejdbc.{DB, DBSession, SQL}

import scala.util.{Failure, Success, Try}

object Database {

  implicit val session: DBSession = Connection.session

  def createNewMobile(mobile: MobileForm): List[Mobile] = {
    val result: List[Mobile] = DB readOnly { implicit session =>
      SQL(BaseQuery.insertQuery(mobile)).map(rs => Mobile(rs.int("id"), rs.string("name"), rs.string("model"), rs.double("price"))).list()
    }
    result
  }

  def getMobiles: List[Mobile] = {
    val result: List[Mobile] = DB readOnly { implicit session =>
      println("inside the fetchData method")
      val res = SQL(BaseQuery.selectAllQuery).map(rs => Mobile(rs.int("id"), rs.string("name"), rs.string("model"), rs.double("price")))
      res.list()
    }
    result
  }

  def getMobileById(id: Int): List[Mobile] = {
    val result: List[Mobile] = DB readOnly { implicit session =>
      println("inside the fetchData method")
      val res = SQL(BaseQuery.selectByIdQuery(id)).map(rs => Mobile(rs.int("id"), rs.string("name"), rs.string("model"), rs.double("price")))
      res.list()
    }
    result
  }

  def deleteById(id: Int): Option[String] = {
    Try(DB autoCommit { implicit session =>
      println("Inside delete by id method.")
      SQL(BaseQuery.deleteByIdQuery(id)).executeUpdate()
    }) match {
      case Failure(ex) =>
        println("**************" + ex.getCause + "******" + ex.getClass + " ********" + ex.getMessage)
        None
      case Success(value) => if (value != 0) Some(s"$value row Deleted!") else Some("No record exists!")
    }

  }


  private object BaseQuery {

    private type InsertQuery = String
    private type SelectQuery = String
    private type UpdateQuery = String
    private type DeleteQuery = String

    def insertQuery(mobileForm: MobileForm): InsertQuery = {
      s"""INSERT INTO mobiles(name, model, price)
         |VALUES (
         |'${mobileForm.name}',
         |'${mobileForm.model}',
         |'${mobileForm.price}'
         |)
         |RETURNING *
         |""".stripMargin
    }

    def selectAllQuery: SelectQuery = {
      """SELECT
        |id,
        |name,
        |model,
        |price
        |FROM mobiles;
        |""".stripMargin
    }

    def selectByIdQuery(id: Int): SelectQuery = {
      s"""SELECT
         |id,
         |name,
         |model,
         |price
         |FROM mobiles
         |where id = '$id';
         |""".stripMargin
    }

    def deleteByIdQuery(id: Int): DeleteQuery = {
      s"""DELETE FROM mobiles WHERE id = '$id';
         |""".stripMargin
    }
  }
}
