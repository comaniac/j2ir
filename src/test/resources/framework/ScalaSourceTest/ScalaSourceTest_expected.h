#include <math.h>
#include <string.h>
class ClassLv1 {
	public:

	int a;

	int calc() {
		return this.realCalc();
	}

	int realCalc() {
		ClassLv2 obj = new ClassLv2(this.a + 5);
		return obj.calc();
	}

	ClassLv1(int a) {
		this.a = a;
	}
}
class ClassLv2 {
	public:

	int a;

	int calc() {
		return this.realCalc();
	}

	int realCalc() {
		return this.a + 10;
	}

	ClassLv2(int a) {
		this.a = a;
	}
}
