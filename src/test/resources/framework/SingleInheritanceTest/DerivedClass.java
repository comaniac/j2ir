public class DerivedClass extends BaseClass {
		
		public DerivedClass(int v) {
			super(v);
		}
		
		@Override
		public int calc() {
			return val + 5;
		}
}