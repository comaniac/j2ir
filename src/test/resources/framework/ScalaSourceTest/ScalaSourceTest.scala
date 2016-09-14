class ScalaSourceTest {

  def main(args: Array[String]) = {
    println(compute(10))
  }

  def compute(N: Int): Int = {
    val obj = new ClassLv1(N)
    obj.calc
  }
}