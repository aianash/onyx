package onyx
package core
package datasets
package alt


sealed trait Intent {
  def value: String
  def intentType: String
  def copy(value: String): Intent
  override def toString = value
}

case class Activity(value: String) extends Intent {
  val intentType = "activity"
  def copy(value: String) = Activity(value)
}

case class Look(value: String) extends Intent {
  val intentType = "look"
  def copy(value: String) = Look(value)
}

case class TimeWeather(value: String) extends Intent {
  val intentType = "timeWeather"
  def copy(value: String) = TimeWeather(value)
}

case class ALT(activity: Activity, look: Look, timeWeather: TimeWeather) {
  override def toString = s"${activity}:${look}:${timeWeather}"
}