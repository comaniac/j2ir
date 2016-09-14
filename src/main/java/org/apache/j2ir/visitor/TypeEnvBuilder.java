package org.apache.j2ir.visitor;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.Map;

public class TypeEnvBuilder extends VoidVisitorAdapter<Map<String, Type>> {

	// Scope of the program that we will build the type environment.
	// Must be either class or method declaration.
	private final BodyDeclaration scope;

	public TypeEnvBuilder(Node n) {
		if (n instanceof ClassOrInterfaceDeclaration)
			scope = (ClassOrInterfaceDeclaration) n;
		else if (n instanceof BodyDeclaration)
			scope = (BodyDeclaration) n;
		else
			throw new RuntimeException("Invalid scope: " + n.toString());
	}

	public void build(Map<String, Type> env) {

		// Add synthetic variable "SYNTHETIC_MODULE" for singleton object accessing.
		if (!env.containsKey("SYNTHETIC_MODULE"))
			env.put("SYNTHETIC_MODULE", new ClassOrInterfaceType("SYNTHETIC_MODULE"));

		if (scope instanceof ClassOrInterfaceDeclaration)
			visit((ClassOrInterfaceDeclaration) scope, env);
		else if (scope instanceof MethodDeclaration)
			visit((MethodDeclaration) scope, env);
		else
			visit((ConstructorDeclaration) scope, env);
	}

	@Override
	public void visit(ClassOrInterfaceDeclaration n, Map<String, Type> env) {
		if (n != scope)
			return;

		// Visit body
		for (final BodyDeclaration member : n.getMembers())
			member.accept(this, env);
	}

	@Override
	public void visit(MethodDeclaration n, Map<String, Type> env) {
		if (scope != n)
			return;

		// Visit parameters
		if (n.getParameters() != null) {
			for (final Parameter p : n.getParameters())
				p.accept(this, env);
		}

		// Visit body
		if (n.getBody() != null)
			n.getBody().accept(this, env);
	}

	@Override
	public void visit(FieldDeclaration n, Map<String, Type> env) {
		for (final VariableDeclarator var : n.getVariables())
			env.put(var.getId().getName(), n.getType());
	}

	@Override
	public void visit(Parameter n, Map<String, Type> env) {
		env.put(n.getId().getName(), n.getType());
	}

	@Override
	public void visit(VariableDeclarationExpr n, Map<String, Type> env) {
		for (final VariableDeclarator var : n.getVars())
			env.put(var.getId().getName(), n.getType());
	}

}
