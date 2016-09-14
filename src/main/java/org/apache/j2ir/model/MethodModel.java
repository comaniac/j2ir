package org.apache.j2ir.model;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import org.apache.j2ir.visitor.TypeEnvBuilder;

import java.lang.reflect.Constructor;
import java.util.List;

public class MethodModel extends Model {
	private final String name;
	private final ClassModel classModel;
	private boolean iskernel = false;
	private boolean isconstructor = false;

	public MethodModel(ClassModel m, String n) {
		name = n;
		decl = null;
		classModel = m;
	}

	public String getName() {
		return name;
	}

	public void setAsKernel() {
		iskernel = true;
	}

	public boolean isKernel() {
		return iskernel;
	}

	public boolean isConstructor() { return isconstructor; }

	public ClassModel getClassModel() {
		return classModel;
	}

	public void setDecl(BodyDeclaration n) {
		if (n instanceof ConstructorDeclaration)
			isconstructor = true;
		decl = n;
	}

	public BodyDeclaration getDecl() {
		return decl;
	}

	@Override
	public void buildOrUpdateTypeEnv() {
		setTypeEnv(classModel.getTypeEnv());
		new TypeEnvBuilder(getDecl()).build(getTypeEnv());
		return;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (!isBuilt())
			sb.append(name);
		else {
			String retType = "";
			List<Parameter> params;

			if (isConstructor())
				params = ((ConstructorDeclaration) getDecl()).getParameters();
			else {
				retType = ((MethodDeclaration) getDecl()).getType().toString();
				params = ((MethodDeclaration) getDecl()).getParameters();
			}

			sb.append(retType + " ");
			sb.append(name + "(");
			if (params != null) {
				boolean first = true;
				for (final Parameter p : params) {
					if (!first)
						sb.append(", ");
					sb.append(p.getType().toString() + " " + p.getId().getName());
					first = false;
				}
			}
			sb.append(")");
		}
		return sb.toString();
	}
}
