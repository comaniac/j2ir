package org.apache.j2ir.model;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.Type;
import org.apache.j2ir.utils.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassModel extends Model {
	private final String name;
	private final Map<String, MethodModel> methods = new HashMap<>();
	private final Map<String, FieldModel> fields = new HashMap<>();
	private final List<ClassModel> bases =  new ArrayList<>();
	private MethodModel kernelMethod = null;

	public ClassModel(ClassOrInterfaceDeclaration n) {
		name = n.getName();
		decl = n;
	}

	public ClassModel(String n) {
		name = n;
		decl = null;
	}

	public String getName() {
		return name;
	}

	public void setDecl(ClassOrInterfaceDeclaration n) {
		decl = n;
	}

	public ClassOrInterfaceDeclaration getDecl() {
		return (ClassOrInterfaceDeclaration) decl;
	}

	public boolean isEntryClass() {
		return !(kernelMethod == null);
	}

	public void addBaseClass(ClassModel m) {
		bases.add(m);
	}

	public List<ClassModel> getBaseClasses() {
		return bases;
	}

	public boolean isKernelMethod(MethodDeclaration n) {
		return kernelMethod != null && kernelMethod.getName().equals(n.getName());
	}

	public MethodModel getKernelMethod() {
		return kernelMethod;
	}

	public Map<String, MethodModel> getMethods() {
		return methods;
	}

	public MethodModel getMethod(String sig) {
		return methods.get(sig);
	}

	public boolean hasMethod(String sig) {
		return methods.containsKey(sig);
	}

	public MethodModel addMethod(Expression n, Map<String, Type> typeEnv) {
		String sig = Util.getMethodSig(n, typeEnv, this);
		return addMethod(sig);
	}

	public MethodModel addMethod(MethodDeclaration n, boolean isKernel) {
		String sig = Util.getMethodSig(n);
		MethodModel method = addMethod(sig);
		method.setDecl(n);

		if (isKernel) {
			kernelMethod = method;
			method.setAsKernel();
		}
		return method;
	}

	public MethodModel addMethod(String sig) {
		MethodModel method = new MethodModel(this, sig);
		methods.put(sig, method);
		return method;
	}

	public boolean hasField(String fieldName) {
		return fields.containsKey(fieldName);
	}

	public Map<String, FieldModel> getFields() {
		return fields;
	}

	public void addField(Type t, String n) {
		fields.put(n, new FieldModel(t, n));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Class name: ").append(this.name).append("\n");
		sb.append("- Fields:\n");
		for (String n : fields.keySet()) {
			if (fields.get(n) != null)
				sb.append("  ").append(fields.get(n).toString()).append("\n");
			else
				sb.append("  ? ").append(n).append("\n");
		}
		sb.append("- Methods:\n");
		for (String n : methods.keySet())
			sb.append("  ").append(methods.get(n).toString()).append("\n");

		return sb.toString();
	}
}
