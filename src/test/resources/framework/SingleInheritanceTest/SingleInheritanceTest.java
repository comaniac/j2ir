public class SingleInheritanceTest {

	public static void main(String[] args) {
		System.out.println(compute(10));
	}

	public static int compute(int N) {
		BaseClass obj = new DerivedClass(N);
		return obj.calc();
	}
}