#include <math.h>
#include <string.h>
class BaseClass {
	public:

	int val;

	BaseClass(int v) {
		this.val = v;
	}


}
class DerivedClass : public BaseClass {
	public:

	DerivedClass(int v) {
		super(v);
	}

	int calc() {
		return this.val + 5;
	}
}
