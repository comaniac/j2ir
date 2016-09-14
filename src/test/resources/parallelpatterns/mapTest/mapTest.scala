class mapTest {

  def main(args: Array[String]) = {
    val radius = Array(1.0, 2.0, 3.0, 4.0, 5.0)
    val area = radius.map(e => e * e * 3.14)
    println(area(0))
  }
}