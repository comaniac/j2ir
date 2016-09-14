package org.apache.j2ir;

import org.junit.Test;

public class CppUnitTest extends UnitTest {
	@Test public void testXML() { doTest("framework/XMLTest"); }
	@Test public void testUsedClass() { doTest("framework/UsedClassTest"); }

	@Test
	public void testScalaSource() {
		doTest("framework/ScalaSourceTest");
	}

	@Test
	public void testSingleInheritance() {
		doTest("framework/SingleInheritanceTest");
	}
}