package org.apache.j2ir.visitor;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.*;
import org.apache.j2ir.model.ClassModel;
import org.apache.j2ir.model.MethodModel;
import org.apache.j2ir.utils.J2IRLogger;
import org.apache.j2ir.utils.Util;

import java.util.Map;
import java.util.logging.Logger;

public class TypeVisitor extends GenericVisitorAdapter<Type, Map<String, Type>> {
	private final static Logger logger = (new J2IRLogger()).logger;
	private final ClassModel classModel;

	public TypeVisitor(ClassModel model) {
		classModel = model;
	}

	public Type visit(final Expression n, final Map<String, Type> arg) {
		if (n instanceof ArrayAccessExpr)
			return visit((ArrayAccessExpr) n, arg);
		else if (n instanceof ArrayCreationExpr)
			return visit((ArrayCreationExpr) n, arg);
		else if (n instanceof ArrayInitializerExpr)
			return visit((ArrayInitializerExpr) n, arg);
		else if (n instanceof AssignExpr)
			return visit((AssignExpr) n, arg);
		else if (n instanceof BinaryExpr)
			return visit((BinaryExpr) n, arg);
		else if (n instanceof BooleanLiteralExpr)
			return visit((BooleanLiteralExpr) n, arg);
		else if (n instanceof CastExpr)
			return visit((CastExpr) n, arg);
		else if (n instanceof CharLiteralExpr)
			return visit((CharLiteralExpr) n, arg);
		else if (n instanceof ClassExpr)
			return visit((ClassExpr) n, arg);
		else if (n instanceof ConditionalExpr)
			return visit((ConditionalExpr) n, arg);
		else if (n instanceof DoubleLiteralExpr)
			return visit((DoubleLiteralExpr) n, arg);
		else if (n instanceof EnclosedExpr)
			return visit((EnclosedExpr) n, arg);
		else if (n instanceof FieldAccessExpr)
			return visit((FieldAccessExpr) n, arg);
		else if (n instanceof InstanceOfExpr)
			return visit((InstanceOfExpr) n, arg);
		else if (n instanceof IntegerLiteralExpr)
			return visit((IntegerLiteralExpr) n, arg);
		else if (n instanceof LongLiteralExpr)
			return visit((LongLiteralExpr) n, arg);
		else if (n instanceof MarkerAnnotationExpr)
			return visit((MarkerAnnotationExpr) n, arg);
		else if (n instanceof MethodCallExpr)
			return visit((MethodCallExpr) n, arg);
		else if (n instanceof QualifiedNameExpr)
			return visit((QualifiedNameExpr) n, arg);
		else if (n instanceof NameExpr)
			return visit((NameExpr) n, arg);
		else if (n instanceof NormalAnnotationExpr)
			return visit((NormalAnnotationExpr) n, arg);
		else if (n instanceof NullLiteralExpr)
			return visit((NullLiteralExpr) n, arg);
		else if (n instanceof ObjectCreationExpr)
			return visit((ObjectCreationExpr) n, arg);
		else if (n instanceof StringLiteralExpr)
			return visit((StringLiteralExpr) n, arg);
		else if (n instanceof SuperExpr)
			return visit((SuperExpr) n, arg);
		else if (n instanceof ThisExpr)
			return visit((ThisExpr) n, arg);
		else if (n instanceof UnaryExpr)
			return visit((UnaryExpr) n, arg);
		else if (n instanceof VariableDeclarationExpr)
			return visit((VariableDeclarationExpr) n, arg);
		else if (n instanceof LambdaExpr)
			return visit((LambdaExpr) n, arg);
		else if (n instanceof MethodReferenceExpr)
			return visit((MethodReferenceExpr) n, arg);
		else if (n instanceof TypeExpr)
			return visit((TypeExpr) n, arg);

		logger.warning("Cannot infer expression type: " + n.toString());
		return null;
	}

	@Override
	public Type visit(final ArrayAccessExpr n, final Map<String, Type> arg) {
		if (!arg.containsKey(n.getName().toString())) {
			logger.severe("Cannot find array " + n.getName() + " in the type environment");
			return new VoidType();
		}
		return arg.get(n.getName().toString());
	}

	@Override
	public Type visit(final ArrayCreationExpr n, final Map<String, Type> arg) {
		return n.getType().accept(this, arg);
	}

	@Override
	public Type visit(final ArrayInitializerExpr n, final Map<String, Type> arg) {
		assert (n.getValues() != null);
		return n.getValues().get(0).accept(this, arg);
	}

	@Override
	public Type visit(final AssignExpr n, final Map<String, Type> arg) {
		return new VoidType();
	}

	@Override
	public Type visit(final BinaryExpr n, final Map<String, Type> arg) {
		Type tLeft = n.getLeft().accept(this, arg);
		Type tRight = n.getRight().accept(this, arg);
		if (tLeft == tRight)
			return tLeft;
		else if (tLeft.toString().equals("double") || tLeft.toString().equals("double"))
			return new PrimitiveType(PrimitiveType.Primitive.Double);
		else if (tLeft.toString().equals("float") || tLeft.toString().equals("float"))
			return new PrimitiveType(PrimitiveType.Primitive.Float);
		else if (tLeft.toString().equals("long") || tLeft.toString().equals("long"))
			return new PrimitiveType(PrimitiveType.Primitive.Long);
		else if (tLeft.toString().equals("int") || tLeft.toString().equals("int"))
			return new PrimitiveType(PrimitiveType.Primitive.Int);
		logger.severe("Not support operator overloading for classes.");
		return null;
	}

	@Override
	public Type visit(final BooleanLiteralExpr n, final Map<String, Type> arg) {
		return new PrimitiveType(PrimitiveType.Primitive.Boolean);
	}

	@Override
	public Type visit(final CastExpr n, final Map<String, Type> arg) {
		return n.getType().accept(this, arg);
	}

	@Override
	public Type visit(final CharLiteralExpr n, final Map<String, Type> arg) {
		return new PrimitiveType(PrimitiveType.Primitive.Char);
	}

	@Override
	public Type visit(final ClassExpr n, final Map<String, Type> arg) {
		return n.getType().accept(this, arg);
	}

	@Override
	public Type visit(final ClassOrInterfaceType n, final Map<String, Type> arg) {
		return n;
	}

	@Override
	public Type visit(final ConditionalExpr n, final Map<String, Type> arg) {
		return new PrimitiveType(PrimitiveType.Primitive.Boolean);
	}

	@Override
	public Type visit(final DoubleLiteralExpr n, final Map<String, Type> arg) {
		return new PrimitiveType(PrimitiveType.Primitive.Double);
	}

	@Override
	public Type visit(final EnclosedExpr n, final Map<String, Type> arg) {
		return n.getInner().accept(this, arg);
	}

	@Override
	public Type visit(final FieldAccessExpr n, final Map<String, Type> arg) {
		if (!arg.containsKey(n.getField())) {
			logger.severe("Cannot find field " + n.getField() + " in the type environment");
			return new VoidType();
		}
		return arg.get(n.getField());
	}

	@Override
	public Type visit(final InstanceOfExpr n, final Map<String, Type> arg) {
		return new PrimitiveType(PrimitiveType.Primitive.Boolean);
	}

	@Override
	public Type visit(final IntegerLiteralExpr n, final Map<String, Type> arg) {
		return new PrimitiveType(PrimitiveType.Primitive.Int);
	}

	@Override
	public Type visit(final IntegerLiteralMinValueExpr n, final Map<String, Type> arg) {
		return new PrimitiveType(PrimitiveType.Primitive.Int);
	}

	@Override
	public Type visit(final LongLiteralExpr n, final Map<String, Type> arg) {
		return new PrimitiveType(PrimitiveType.Primitive.Long);
	}

	@Override
	public Type visit(final LongLiteralMinValueExpr n, final Map<String, Type> arg) {
		return new PrimitiveType(PrimitiveType.Primitive.Long);
	}

	@Override
	public Type visit(final MarkerAnnotationExpr n, final Map<String, Type> arg) {
		return new VoidType();
	}

	@Override
	public Type visit(final MemberValuePair n, final Map<String, Type> arg) {
		return n.getValue().accept(this, arg);
	}

	@Override
	public Type visit(final MethodCallExpr n, final Map<String, Type> arg) {
		if (classModel == null)
			throw new RuntimeException("Type inference for method call expr needs to set up the scope.");
		String sig = Util.getMethodSig(n, arg, classModel);
		MethodModel model = classModel.getMethod(sig);
		if (model == null)
			throw new RuntimeException("Cannot find method " + sig + " from " + classModel.getName());
		if (model.isConstructor())
			return new UnknownType();
		return ((MethodDeclaration) model.getDecl()).getType();
	}

	@Override
	public Type visit(final NameExpr n, final Map<String, Type> arg) {
		Type type = arg.get(n.getName());
		if (type == null) {
			logger.severe("Cannot find variable " + n.getName() + " in the type environment");
		}
		return type;
	}

	@Override
	public Type visit(final NormalAnnotationExpr n, final Map<String, Type> arg) {
		return new VoidType();
	}

	@Override
	public Type visit(final NullLiteralExpr n, final Map<String, Type> arg) {
		return new ClassOrInterfaceType("null");
	}

	@Override
	public Type visit(final ObjectCreationExpr n, final Map<String, Type> arg) {
		return n.getType().accept(this, arg);
	}


	@Override
	public Type visit(final PrimitiveType n, final Map<String, Type> arg) {
		return n;
	}

	@Override
	public Type visit(final QualifiedNameExpr n, final Map<String, Type> arg) {
		return new ClassOrInterfaceType("String");
	}

	@Override
	public Type visit(final ReferenceType n, final Map<String, Type> arg) {
		return n.getType().accept(this, arg);
	}

	@Override
	public Type visit(final IntersectionType n, final Map<String, Type> arg) {
		logger.warning("Not supported intersection type");
		return null;
	}

	@Override
	public Type visit(final UnionType n, final Map<String, Type> arg) {
		logger.warning("Not supported union type");
		return null;
	}

	@Override
	public Type visit(final StringLiteralExpr n, final Map<String, Type> arg) {
		return new ClassOrInterfaceType("String");
	}

	@Override
	public Type visit(final SuperExpr n, final Map<String, Type> arg) {
		return n.getClassExpr().accept(this, arg);
	}

	@Override
	public Type visit(final ThisExpr n, final Map<String, Type> arg) {
		assert (n.getClassExpr() != null);
		return n.getClassExpr().accept(this, arg);
	}

	@Override
	public Type visit(final UnaryExpr n, final Map<String, Type> arg) {
		return n.getExpr().accept(this, arg);
	}

	@Override
	public Type visit(final UnknownType n, final Map<String, Type> arg) {
		return new UnknownType();
	}

	@Override
	public Type visit(final VariableDeclarationExpr n, final Map<String, Type> arg) {
		return n.getType().accept(this, arg);
	}

	@Override
	public Type visit(final VoidType n, final Map<String, Type> arg) {
		return new VoidType();
	}

	@Override
	public Type visit(final WildcardType n, final Map<String, Type> arg) {
		logger.warning("Not supported wildcard type");
		return null;
	}

	@Override
	public Type visit(LambdaExpr n, Map<String, Type> arg) {
		logger.warning("Not supported lambda expression yet");
		return null;
	}

	@Override
	public Type visit(MethodReferenceExpr n, Map<String, Type> arg){
		logger.severe("FIXME: MethodReferenceExpr type checking: " + n.toString());
		return null;
	}

	@Override
	public Type visit(TypeExpr n, Map<String, Type> arg){
		return n.getType();
	}
}
