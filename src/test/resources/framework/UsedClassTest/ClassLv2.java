public class ClassLv2 {
	private int val;
	
	public ClassLv2(int a) {
		val = a;
	}
	
	public int calc() {
		return realCalc();
	}
	
	public int realCalc() {
		return val + 10;
	}
}