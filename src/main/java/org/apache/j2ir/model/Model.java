package org.apache.j2ir.model;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.type.Type;
import org.apache.j2ir.visitor.TypeEnvBuilder;

import java.util.HashMap;
import java.util.Map;

public abstract class Model {
	private Map<String, Type> typeEnv = new HashMap<String, Type>();
	protected BodyDeclaration decl;

	public boolean isBuilt() {
		if (decl == null)
			return false;
		return true;
	}

	public void setTypeEnv(Map<String, Type> n) {
		typeEnv = n;
	}

	public Map<String, Type> getTypeEnv() {
		return typeEnv;
	}

	public void buildOrUpdateTypeEnv() {
		assert (decl != null);
		new TypeEnvBuilder(decl).build(typeEnv);
		return;
	}

	public void dumpTypeEnv() {
		System.out.println("Type env.");
		for (String var : typeEnv.keySet())
			System.out.println(var + ": " + typeEnv.get(var).toString());
		return;
	}
}
