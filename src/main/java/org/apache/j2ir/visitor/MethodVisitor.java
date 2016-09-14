package org.apache.j2ir.visitor;

import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.apache.j2ir.model.ClassModel;
import org.apache.j2ir.model.MethodModel;
import org.apache.j2ir.model.VoidClassModel;
import org.apache.j2ir.utils.J2IRLogger;
import org.apache.j2ir.utils.Util;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class MethodVisitor extends VoidVisitorAdapter<MethodModel> {

	private final static Logger logger = (new J2IRLogger()).logger;
	private final Map<String, ClassModel> usedClasses;

	public MethodVisitor(Map<String, ClassModel> usedClasses) {
		this.usedClasses = usedClasses;
	}

	private ClassModel getOrAddClass(String className) {
		ClassModel model;
		if (!usedClasses.containsKey(className)) {
			if (className.equals("SYNTHETIC_MODULE"))
				model = new VoidClassModel("SYNTHETIC_MODULE");
			else
				model = new ClassModel(className);
			usedClasses.put(className, model);
		} else
			model = usedClasses.get(className);

		return model;
	}

	private void addMethod(String className, Expression n, Map<String, Type> typeEnv) {
		ClassModel classModel = getOrAddClass(className);
		classModel.addMethod(n, typeEnv);
	}

	@Override
	public void visit(Parameter n, MethodModel model) {

		// Collect class type to be built
		if (n.getType() instanceof ClassOrInterfaceType) {
			String className = ((ClassOrInterfaceType) n.getType()).getName();
			getOrAddClass(className);
		}

		if (n.getAnnotations() != null) {
			for (final AnnotationExpr a : n.getAnnotations())
				a.accept(this, model);
		}
		n.getType().accept(this, model);
		n.getId().accept(this, model);
	}

	@Override
	public void visit(FieldAccessExpr n, MethodModel model) {
		String name = n.getFieldExpr().getName();
		Type type = model.getTypeEnv().get(name);
		if (type == null) {
			// Try to find the field from base classes
			for (ClassModel base : model.getClassModel().getBaseClasses()) {
				type = base.getTypeEnv().get(name);
				if (type != null) {
					base.addField(type, name);
					break;
				}
			}
			if (type == null) {
				throw new RuntimeException("Cannot find field " + name + " in "
						+ model.getName() + " and its base classes");
			}
		} else
			model.getClassModel().addField(type, name);

		n.getScope().accept(this, model);
		n.getFieldExpr().accept(this, model);
	}

	@Override
	public void visit(VariableDeclarationExpr n, MethodModel model) {

		// Collect class type to be built
		if (n.getType() instanceof ClassOrInterfaceType) {
			String className = ((ClassOrInterfaceType) n.getType()).getName();
			getOrAddClass(className);
		}

		if (n.getAnnotations() != null) {
			for (final AnnotationExpr a : n.getAnnotations())
				a.accept(this, model);
		}
		n.getType().accept(this, model);
		for (final VariableDeclarator v : n.getVars())
			v.accept(this, model);
	}

	@Override
	public void visit(final ObjectCreationExpr n, final MethodModel model) {
		Type type = n.getType();

		if (type instanceof ClassOrInterfaceType) {
			String className = ((ClassOrInterfaceType) type).getName();
			addMethod(className, n, model.getTypeEnv());
		}
	}

	@Override
	public void visit(ExplicitConstructorInvocationStmt n, MethodModel model) {
		String sig;
		List<Expression> args;
		ClassModel classModel = model.getClassModel();

		// Setup method name (class name)
		if (n.isThis())
			sig = classModel.getName();
		else {
			if (n.getExpr() != null) {
				// TODO: Multiple inheritance
				throw new RuntimeException("Not support multiple inheritance");
			}
			else {
				classModel = model.getClassModel().getBaseClasses().get(0);
				sig = classModel.getName();
			}
		}

		// Setup arguments for sig
		args = n.getArgs();
		if (args.size() != 0) {
			for (final Expression e : args) {
				Type type = Util.getExpType(e, model.getTypeEnv(), model.getClassModel());
				if (type == null)
					throw new RuntimeException("Cannot infer type for " + e.toString());
				sig += "-" + type.toString();
			}
		}
		classModel.addMethod(sig);
	}

	@Override
	public void visit(MethodCallExpr n, MethodModel model) {
		Expression caller = n.getScope();

		// Deal with series method calls
		while (caller != null && caller instanceof MethodCallExpr) {
			MethodCallExpr expr = (MethodCallExpr) caller;
			if (expr.getScope() == null)
				break;
			caller = expr.getScope();
		}

		if (caller == null || caller instanceof ThisExpr) {
			// Method in the same class
			model.getClassModel().addMethod(n, model.getTypeEnv());
		} else {
			// Method in the other class
			String varName;

			if (caller instanceof NameExpr) {
				// Method in local variable's type class
				varName = caller.toString();
			} else if (caller instanceof FieldAccessExpr) {
				// Method in field's type class
				varName = ((FieldAccessExpr) caller).getFieldExpr().toString();
			} else
				throw new RuntimeException("Expect FieldAccessExpr, but found " + caller.toString());

			Type type = model.getTypeEnv().get(varName);
			if (type == null)
				throw new RuntimeException("Cannot find variable " + varName + " in "
						+ model.getName());
			addMethod(type.toString(), n, model.getTypeEnv());
		}

		if (n.getScope() != null)
			n.getScope().accept(this, model);
		if (n.getTypeArgs() != null) {
			for (final Type t : n.getTypeArgs())
				t.accept(this, model);
		}
		n.getNameExpr().accept(this, model);
		if (n.getArgs() != null) {
			for (final Expression e : n.getArgs())
				e.accept(this, model);
		}
	}
}
