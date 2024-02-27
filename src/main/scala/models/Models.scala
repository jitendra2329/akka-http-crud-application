package models

object Models {
  case class Mobile(id: Int, name: String, model: String, price: Double)

  case class MobileForm(name: String, model: String, price: Double)

  case class MobileUpdateForm(price: Double)
}
