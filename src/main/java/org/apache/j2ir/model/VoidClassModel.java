package org.apache.j2ir.model;

public class VoidClassModel extends ClassModel {

	public VoidClassModel(String n) {
		super(n);
	}

	@Override
	public boolean isBuilt() {
		return true;
	}
}
