public class ClassLv1 {
	private int val;
	
	public ClassLv1(int a) {
		val = a;
	}
	
	public int calc() {
		return realCalc();
	}
	
	public int realCalc() {
		ClassLv2 obj = new ClassLv2(val + 5);
		return obj.calc();		
	}
}