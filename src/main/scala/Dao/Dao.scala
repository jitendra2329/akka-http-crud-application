package Dao

import models.Models.{Mobile, MobileForm}

trait Dao {

  def createNewMobile(mobile: MobileForm): List[Mobile]

  def getMobiles: List[Mobile]

  def getMobileById(id: Int): List[Mobile]

  def deleteById(id: Int): Option[String]

  def deleteAll(): Option[String]

  def updateById(id: Int, newPriceToUpdate: Double): Option[String]

}
