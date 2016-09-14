public class XMLTest {

	public static void main(String[] args) {
		int[] a = new int[10];
		for (int i = 0; i < 10; i++)
			a[i] = i;

		int[][] b = compute(10, a);
	}

	public static int[][] compute(int N, int[] a) {
		int[][] b = new int[N][N + 10];
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N + 10; j++)
				b[i][j] = a[i] + 5 + j;
		}
		return b;
	}
}