#include <math.h>
#include <string.h>
class ClassLv1 {
	public:

	int val;

	ClassLv1(int a) {
		this.val = a;
	}

	int calc() {
		return this.realCalc();
	}

	int realCalc() {
		ClassLv2 obj = new ClassLv2(this.val + 5);
		return obj.calc();
	}
}
class ClassLv2 {
	public:

	int val;

	ClassLv2(int a) {
		this.val = a;
	}

	int calc() {
		return this.realCalc();
	}

	int realCalc() {
		return this.val + 10;
	}
}
