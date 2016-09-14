class ClassLv1(a: Int) {

  def calc(): Int = {
    realCalc
  }

  def realCalc(): Int = {
    val obj = new ClassLv2(a + 5)
    obj.calc
  }
}