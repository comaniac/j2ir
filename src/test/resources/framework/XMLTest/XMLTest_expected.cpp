#include <math.h>
#include <string.h>

int** compute(int N, int* a) {
	int** b = new int[20][ 30];
	for (int i = 0; i < N; ++i) {
		for (int j = 0; j < N + 10; ++j) {
			b[i][j] = a[i] + 5 + j;
		}
	}
	return b;
}
