package org.apache.j2ir.model;

import com.github.javaparser.ast.type.Type;

public class FieldModel {
	private final String name;
	private final Type type;

	public FieldModel(Type t, String n) {
		type = t;
		name = n;
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	@Override
	public String toString() {
		return type.toString() + " " + name;
	}
}
