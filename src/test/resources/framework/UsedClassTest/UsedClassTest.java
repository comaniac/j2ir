public class UsedClassTest {

	public static void main(String[] args) {
		System.out.println(compute(10));
	}

	public static int compute(int N) {
		ClassLv1 obj = new ClassLv1(N);
		return obj.calc();
	}
}