#include <math.h>
#include <string.h>

int compute(int N) {
	DerivedClass obj = new DerivedClass(N);
	return obj.calc();
}
