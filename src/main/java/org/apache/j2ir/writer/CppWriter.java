package org.apache.j2ir.writer;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.internal.Utils;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;
import org.apache.j2ir.model.ClassModel;
import org.apache.j2ir.model.FieldModel;
import org.apache.j2ir.utils.Util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
	CppWriter generates g++ compilable C++ code under C99 standard.
	The entry class will be flatten and put into the source file;
	while other classes will be put into the header file.
 */
public class CppWriter extends IRWriter {

	private final CodeWriter srcWriter = createCppCodeWriter();
	private final CodeWriter headWriter = createCppCodeWriter();
	private final Map<String, Map<String, String>> attr;

	private boolean writingKernelMethod = false;

	public CppWriter(Map<String, Map<String, String>> attr) {
		this.attr = attr;
		writer = srcWriter;
	}

	protected void writeInclude(CodeWriter w) {
		w.writeln("#include <math.h>");
		w.writeln("#include <string.h>");
	}

	protected CodeWriter createCppCodeWriter() {
		CodeWriter w = new CodeWriter("\t");
		writeInclude(w);
		return w;
	}

	@Override
	public String getCode() {
		return srcWriter.getCode();
	}

	public String getHeaderCode() {
		return headWriter.getCode();
	}

	@Override
	public void saveAsFile(String fileName) throws IOException {
		BufferedWriter headerFile = new BufferedWriter(new FileWriter(fileName + ".h"));
		headerFile.write(getHeaderCode());
		headerFile.close();

		BufferedWriter srcFile = new BufferedWriter(new FileWriter(fileName + ".cpp"));
		srcFile.write(getCode());
		srcFile.close();
	}

	public void writeToHead(final ClassOrInterfaceDeclaration n, ClassModel arg) {
		writer = headWriter;
		visit(n, arg);
	}

	public void writeToSource(final ClassOrInterfaceDeclaration n, ClassModel arg) {
		writer = srcWriter;

		// Only the entry class can be put in the source file.
		assert (arg.isEntryClass());

		// TODO: Add base class's fields and methods as well
		if (!Utils.isNullOrEmpty(n.getExtends())) {
			for (final ClassOrInterfaceType c : n.getExtends()) {
				//c.accept(this, arg);
			}
		}

		// TODO: Add base class's fields and methods as well
		if (!Utils.isNullOrEmpty(n.getImplements())) {
			for (final ClassOrInterfaceType c : n.getImplements()) {
				//c.accept(this, arg);
			}
		}

		// Write methods.
		for (final BodyDeclaration member : n.getMembers()) {
			member.accept(this, arg);
			writer.writeln();
		}

		writeOrphanCommentsEnding(n);
	}

	// Visit methods start here

	@Override
	public void visit(final CompilationUnit n, final ClassModel arg) {
		if (!Utils.isNullOrEmpty(n.getTypes())) {
			for (final Iterator<TypeDeclaration> i = n.getTypes().iterator(); i
					.hasNext(); ) {
				i.next().accept(this, arg);
				writer.writeln();
				if (i.hasNext())
					writer.writeln();
			}
		}
		writeOrphanCommentsEnding(n);
	}

	@Override
	public void visit(final PackageDeclaration n, final ClassModel arg) {
		// Do nothing
		writeOrphanCommentsEnding(n);
	}

	@Override
	public void visit(final NameExpr n, final ClassModel arg) {
		writer.write(n.getName());
		writeOrphanCommentsEnding(n);
	}

	@Override
	public void visit(final QualifiedNameExpr n, final ClassModel arg) {
		n.getQualifier().accept(this, arg);
		writer.write("_");
		writer.write(n.getName());

		writeOrphanCommentsEnding(n);
	}

	@Override
	public void visit(final ImportDeclaration n, final ClassModel arg) {
		// Do nothing
		writeOrphanCommentsEnding(n);
	}

	@Override
	public void visit(final ClassOrInterfaceDeclaration n, final ClassModel arg) {
		writer.write("class " + n.getName());

		writeTypeParameters(n.getTypeParameters(), arg);

		if (!Utils.isNullOrEmpty(n.getExtends())
				|| !Utils.isNullOrEmpty(n.getImplements()))
			writer.write(" : ");

		if (!Utils.isNullOrEmpty(n.getExtends())) {
			for (final Iterator<ClassOrInterfaceType> i = n.getExtends().iterator(); i
					.hasNext(); ) {
				final ClassOrInterfaceType c = i.next();
				writer.write("public ");
				c.accept(this, arg);
				if (i.hasNext()) {
					writer.write(", ");
				}
			}
		}

		if (!Utils.isNullOrEmpty(n.getImplements())) {
			for (final Iterator<ClassOrInterfaceType> i = n.getImplements()
					.iterator(); i.hasNext(); ) {
				final ClassOrInterfaceType c = i.next();
				writer.write("public ");
				c.accept(this, arg);
				if (i.hasNext()) {
					writer.write(", ");
				}
			}
		}

		writer.writeln(" {");
		writer.in();

		// Enforce all members to be public
		writer.writeln("public:");

		if (!Utils.isNullOrEmpty(n.getMembers())) {
			writeMembers(n.getMembers(), arg);
		}

		writeOrphanCommentsEnding(n);

		writer.out();
		writer.writeln("}");
	}

	@Override
	public void visit(final EmptyTypeDeclaration n, final ClassModel arg) {
		// Not supported so just ignore
		logger.warning("Ignore empty type declaration: " + n.toString());
		writeOrphanCommentsEnding(n);
	}

	@Override
	public void visit(final JavadocComment n, final ClassModel arg) {
		// Do nothing
	}

	@Override
	public void visit(final ClassOrInterfaceType n, final ClassModel arg) {

		if (n.getScope() != null)
			throw new RuntimeException("Not support nested classes");

		writer.write(n.getName());

		if (!n.isUsingDiamondOperator())
			writeTypeArgs(n.getTypeArgs(), arg);
	}

	@Override
	public void visit(final TypeParameter n, final ClassModel arg) {
		// TODO: What's this?
		writer.write(n.getName());
		if (!Utils.isNullOrEmpty(n.getTypeBound())) {
			writer.write(" extends ");
			for (final Iterator<ClassOrInterfaceType> i = n.getTypeBound().iterator(); i
					.hasNext(); ) {
				final ClassOrInterfaceType c = i.next();
				c.accept(this, arg);
				if (i.hasNext()) {
					writer.write(" & ");
				}
			}
		}
	}

	@Override
	public void visit(final PrimitiveType n, final ClassModel arg) {
		switch (n.getType()) {
			case Boolean:
				writer.write("char");
				break;
			case Byte:
				writer.write("byte");
				break;
			case Char:
				writer.write("char");
				break;
			case Double:
				writer.write("double");
				break;
			case Float:
				writer.write("float");
				break;
			case Int:
				writer.write("int");
				break;
			case Long:
				writer.write("long");
				break;
			case Short:
				writer.write("short");
				break;
		}
	}

	@Override
	public void visit(final ReferenceType n, final ClassModel arg) {
		n.getType().accept(this, arg);
		for (int i = 0; i < n.getArrayCount(); i++)
			writer.write("*");
	}

	@Override
	public void visit(final IntersectionType n, final ClassModel arg) {
		throw new RuntimeException("Not support intersection types");
	}

	@Override
	public void visit(final UnionType n, final ClassModel arg) {
		throw new RuntimeException("Not support union types");
	}

	@Override
	public void visit(final WildcardType n, final ClassModel arg) {
		throw new RuntimeException("Not support wildcard types");
	}

	@Override
	public void visit(final UnknownType n, final ClassModel arg) {
		// Nothing to dump
	}

	@Override
	public void visit(final FieldDeclaration n, final ClassModel arg) {
		// Fields become method arguments in a flatten class.
		if (arg.isEntryClass())
			return ;

		boolean ignore = true;
		for (final VariableDeclarator var : n.getVariables()) {
			if (arg.hasField(var.getId().getName())) {
				ignore = false;
				break;
			}
		}
		if (ignore)
			return;

		writeOrphanCommentsBeforeThisChildNode(n);

		n.getType().accept(this, arg);

		boolean isFirst = true;
		writer.write(" ");
		Iterator<VariableDeclarator> i;
		for (i = n.getVariables().iterator(); i
				.hasNext(); ) {
			final VariableDeclarator var = i.next();
			if (!arg.hasField(var.getId().getName()))
				continue;
			if (!isFirst)
				writer.write(", ");
			var.accept(this, arg);
			isFirst = false;
		}

		writer.write(";");
	}

	@Override
	public void visit(final VariableDeclarator n, final ClassModel arg) {
		n.getId().accept(this, arg);
		if (n.getInit() != null) {
			writer.write(" = ");
			n.getInit().accept(this, arg);
		}
	}

	@Override
	public void visit(final VariableDeclaratorId n, final ClassModel arg) {
		writer.write(n.getName());
		for (int i = 0; i < n.getArrayCount(); i++) {
			writer.write("*");
		}
	}

	@Override
	public void visit(final ArrayInitializerExpr n, final ClassModel arg) {
		writer.write("{");
		if (!Utils.isNullOrEmpty(n.getValues())) {
			writer.write(" ");
			for (final Iterator<Expression> i = n.getValues().iterator(); i.hasNext(); ) {
				final Expression expr = i.next();
				expr.accept(this, arg);
				if (i.hasNext()) {
					writer.write(", ");
				}
			}
			writer.write(" ");
		}
		writer.write("}");
	}

	@Override
	public void visit(final VoidType n, final ClassModel arg) {
		writer.write("void");
	}

	@Override
	public void visit(final ArrayAccessExpr n, final ClassModel arg) {
		n.getName().accept(this, arg);
		writer.write("[");
		n.getIndex().accept(this, arg);
		writer.write("]");
	}

	@Override
	public void visit(final ArrayCreationExpr n, final ClassModel arg) {
		// FIXME: Static declaration

		// Check if user has specified the max length for this array
		assert (n.getParentNode() instanceof VariableDeclarator);
		VariableDeclarator varDecl = (VariableDeclarator) n.getParentNode();
		String [] maxLength = new String[n.getDimensions().size()];
		if (attr.containsKey(varDecl.getId().getName())) {
			Map<String, String> varAttr = attr.get(varDecl.getId().getName());
			if (varAttr.containsKey("length")) {
				maxLength = varAttr.get("length").split(",");
				if (maxLength.length != n.getDimensions().size())
					throw new RuntimeException("Dimension mismatch for array variable " + varDecl.getId().getName());
			}
			else {
				for (int i = 0; i < n.getDimensions().size(); i += 1)
					maxLength[i] = null;
			}
		}

		writer.write("new ");
		n.getType().accept(this, arg);
		if (!Utils.isNullOrEmpty(n.getDimensions())) {
			for (int i = 0; i < n.getDimensions().size(); i += 1) {
				final Expression dim = n.getDimensions().get(i);
				writer.write("[");
				if (maxLength[i] != null)
					writer.write(maxLength[i]);
				else
					dim.accept(this, arg);
				writer.write("]");
			}
		}
		if (n.getInitializer() != null)
			n.getInitializer().accept(this, arg);
	}

	@Override
	public void visit(final AssignExpr n, final ClassModel arg) {
		n.getTarget().accept(this, arg);
		writer.write(" ");
		switch (n.getOperator()) {
			case assign:
				writer.write("=");
				break;
			case and:
				writer.write("&=");
				break;
			case or:
				writer.write("|=");
				break;
			case xor:
				writer.write("^=");
				break;
			case plus:
				writer.write("+=");
				break;
			case minus:
				writer.write("-=");
				break;
			case rem:
				writer.write("%=");
				break;
			case slash:
				writer.write("/=");
				break;
			case star:
				writer.write("*=");
				break;
			case lShift:
				writer.write("<<=");
				break;
			case rSignedShift:
				writer.write(">>=");
				break;
			case rUnsignedShift:
				writer.write(">>>=");
				break;
		}
		writer.write(" ");
		n.getValue().accept(this, arg);
	}

	@Override
	public void visit(final BinaryExpr n, final ClassModel arg) {
		n.getLeft().accept(this, arg);
		writer.write(" ");
		switch (n.getOperator()) {
			case or:
				writer.write("||");
				break;
			case and:
				writer.write("&&");
				break;
			case binOr:
				writer.write("|");
				break;
			case binAnd:
				writer.write("&");
				break;
			case xor:
				writer.write("^");
				break;
			case equals:
				writer.write("==");
				break;
			case notEquals:
				writer.write("!=");
				break;
			case less:
				writer.write("<");
				break;
			case greater:
				writer.write(">");
				break;
			case lessEquals:
				writer.write("<=");
				break;
			case greaterEquals:
				writer.write(">=");
				break;
			case lShift:
				writer.write("<<");
				break;
			case rSignedShift:
				writer.write(">>");
				break;
			case rUnsignedShift:
				writer.write(">>>");
				break;
			case plus:
				writer.write("+");
				break;
			case minus:
				writer.write("-");
				break;
			case times:
				writer.write("*");
				break;
			case divide:
				writer.write("/");
				break;
			case remainder:
				writer.write("%");
				break;
		}
		writer.write(" ");
		n.getRight().accept(this, arg);
	}

	@Override
	public void visit(final CastExpr n, final ClassModel arg) {
		writer.write("(");
		n.getType().accept(this, arg);
		writer.write(") ");
		n.getExpr().accept(this, arg);
	}

	@Override
	public void visit(final ClassExpr n, final ClassModel arg) {
		throw new RuntimeException("Not support class expression");
	}

	@Override
	public void visit(final ConditionalExpr n, final ClassModel arg) {
		n.getCondition().accept(this, arg);
		writer.write(" ? ");
		n.getThenExpr().accept(this, arg);
		writer.write(" : ");
		n.getElseExpr().accept(this, arg);
	}

	@Override
	public void visit(final EnclosedExpr n, final ClassModel arg) {
		writer.write("(");
		if (n.getInner() != null)
			n.getInner().accept(this, arg);
		writer.write(")");
	}

	@Override
	public void visit(final FieldAccessExpr n, final ClassModel arg) {
		n.getScope().accept(this, arg);
		writer.write(".");
		writer.write(n.getField());
	}

	@Override
	public void visit(final InstanceOfExpr n, final ClassModel arg) {
		// TODO: Dynamic type checking
		n.getExpr().accept(this, arg);
		writer.write(" instanceof ");
		n.getType().accept(this, arg);
	}

	@Override
	public void visit(final CharLiteralExpr n, final ClassModel arg) {
		writer.write("'");
		writer.write(n.getValue());
		writer.write("'");
	}

	@Override
	public void visit(final DoubleLiteralExpr n, final ClassModel arg) {
		writer.write(n.getValue());
	}

	@Override
	public void visit(final IntegerLiteralExpr n, final ClassModel arg) {
		writer.write(n.getValue());
	}

	@Override
	public void visit(final LongLiteralExpr n, final ClassModel arg) {
		writer.write(n.getValue());
	}

	@Override
	public void visit(final IntegerLiteralMinValueExpr n, final ClassModel arg) {
		writer.write(n.getValue());
	}

	@Override
	public void visit(final LongLiteralMinValueExpr n, final ClassModel arg) {
		writer.write(n.getValue());
	}

	@Override
	public void visit(final StringLiteralExpr n, final ClassModel arg) {
		writer.write("\"");
		writer.write(n.getValue());
		writer.write("\"");
	}

	@Override
	public void visit(final BooleanLiteralExpr n, final ClassModel arg) {
		writer.write(String.valueOf(n.getValue()));
	}

	@Override
	public void visit(final NullLiteralExpr n, final ClassModel arg) {
		writer.write("NULL");
	}

	@Override
	public void visit(final ThisExpr n, final ClassModel arg) {
		// TODO
		if (n.getClassExpr() != null) {
			n.getClassExpr().accept(this, arg);
			writer.write(".");
		}
		writer.write("this");
	}

	@Override
	public void visit(final SuperExpr n, final ClassModel arg) {
		// TODO
		if (n.getClassExpr() != null) {
			n.getClassExpr().accept(this, arg);
			writer.write(".");
		}
		writer.write("super");
	}

	@Override
	public void visit(final MethodCallExpr n, final ClassModel arg) {
		if (n.getScope() != null) {
			n.getScope().accept(this, arg);
			writer.write(".");
		}
		writeTypeArgs(n.getTypeArgs(), arg);
		writer.write(n.getName());
		writeArguments(n.getArgs(), arg);
	}

	@Override
	public void visit(final ObjectCreationExpr n, final ClassModel arg) {
		// TODO
		if (n.getScope() != null) {
			n.getScope().accept(this, arg);
			writer.write(".");
		}

		writer.write("new ");

		writeTypeArgs(n.getTypeArgs(), arg);
		if (!Utils.isNullOrEmpty(n.getTypeArgs())) {
			writer.write(" ");
		}

		n.getType().accept(this, arg);

		writeArguments(n.getArgs(), arg);

		if (n.getAnonymousClassBody() != null) {
			writer.writeln(" {");
			writer.in();
			writeMembers(n.getAnonymousClassBody(), arg);
			writer.out();
			writer.write("}");
		}
	}

	@Override
	public void visit(final UnaryExpr n, final ClassModel arg) {
		switch (n.getOperator()) {
			case positive:
				writer.write("+");
				break;
			case negative:
				writer.write("-");
				break;
			case inverse:
				writer.write("~");
				break;
			case not:
				writer.write("!");
				break;
			case preIncrement:
				writer.write("++");
				break;
			case preDecrement:
				writer.write("--");
				break;
			default:
		}

		n.getExpr().accept(this, arg);

		switch (n.getOperator()) {
			case posIncrement:
				writer.write("++");
				break;
			case posDecrement:
				writer.write("--");
				break;
			default:
		}
	}

	@Override
	public void visit(final ConstructorDeclaration n, final ClassModel arg) {
		if (!arg.hasMethod(Util.getMethodSig(n)))
			return ;

		writeTypeParameters(n.getTypeParameters(), arg);
		if (!n.getTypeParameters().isEmpty()) {
			writer.write(" ");
		}
		writer.write(n.getName());

		writer.write("(");
		if (!n.getParameters().isEmpty()) {
			for (final Iterator<Parameter> i = n.getParameters().iterator(); i
					.hasNext(); ) {
				final Parameter p = i.next();
				p.accept(this, arg);
				if (i.hasNext()) {
					writer.write(", ");
				}
			}
		}
		writer.write(")");

		if (!Utils.isNullOrEmpty(n.getThrows()))
			logger.warning("Ignore throws in the constructor");
		writer.write(" ");
		n.getBlock().accept(this, arg);
	}

	@Override
	public void visit(final MethodDeclaration n, final ClassModel arg) {
		if (!arg.hasMethod(Util.getMethodSig(n)))
			return;

		if (arg.isKernelMethod(n))
			writingKernelMethod = true;

		writeOrphanCommentsBeforeThisChildNode(n);

		writeTypeParameters(n.getTypeParameters(), arg);
		if (!Utils.isNullOrEmpty(n.getTypeParameters()))
			writer.write(" ");

		if (!writingKernelMethod)
			n.getType().accept(this, arg);
		else
			writer.write("void");
		writer.write(" ");
		writer.write(n.getName());

		writer.write("(");
		if (!Utils.isNullOrEmpty(n.getParameters())) {
			for (final Iterator<Parameter> i = n.getParameters().iterator(); i
					.hasNext(); ) {
				final Parameter p = i.next();
				p.accept(this, arg);
				if (i.hasNext()) {
					writer.write(", ");
				}
			}
		}

		// Transform return value to an argument for the kernel method
		if (arg.isKernelMethod(n)) {
			if (!Utils.isNullOrEmpty(n.getParameters()))
				writer.write(", ");
			n.getType().accept(this, arg);
			writer.write(" " + n.getName() + "_ret");
		}

		// Write field arguments for methods in the entry class
		if (arg.isEntryClass()) {
			Map<String, FieldModel> fields = arg.getFields();
			for (String argName : fields.keySet()) {
				if (!Utils.isNullOrEmpty(n.getParameters()) || writingKernelMethod)
					writer.write(", ");
				writer.write(fields.get(argName).toString());
			}
		}

		writer.write(")");

		if (n.getArrayCount() != 0)
			throw new RuntimeException(
					"Not support method declaration with array count");

		if (!Utils.isNullOrEmpty(n.getThrows()))
			logger.warning("Ignore throws in the method declaration");

		if (n.getBody() == null) {
			writer.write(";");
		} else {
			writer.write(" ");
			n.getBody().accept(this, arg);
		}
		writingKernelMethod = false;
	}

	@Override
	public void visit(final Parameter n, final ClassModel arg) {
		if (n.getType() != null) {
			n.getType().accept(this, arg);
		}

		if (n.isVarArgs())
			throw new RuntimeException("Not support vary arguments");

		writer.write(" ");
		n.getId().accept(this, arg);
	}

	@Override
	public void visit(MultiTypeParameter n, ClassModel arg) {
		Type type = n.getType();
		if (type != null) {
			type.accept(this, arg);
		}

		writer.write(" ");
		n.getId().accept(this, arg);
	}

	@Override
	public void visit(final ExplicitConstructorInvocationStmt n, final ClassModel arg) {
		// TODO
		if (n.isThis()) {
			writeTypeArgs(n.getTypeArgs(), arg);
			writer.write("this");
		} else {
			if (n.getExpr() != null) {
				n.getExpr().accept(this, arg);
				writer.write(".");
			}
			writeTypeArgs(n.getTypeArgs(), arg);
			writer.write("super");
		}
		writeArguments(n.getArgs(), arg);
		writer.write(";");
	}

	@Override
	public void visit(final VariableDeclarationExpr n, final ClassModel arg) {
		n.getType().accept(this, arg);
		writer.write(" ");

		for (final Iterator<VariableDeclarator> i = n.getVars().iterator(); i
				.hasNext(); ) {
			final VariableDeclarator v = i.next();
			v.accept(this, arg);
			if (i.hasNext()) {
				writer.write(", ");
			}
		}
	}

	@Override
	public void visit(final TypeDeclarationStmt n, final ClassModel arg) {
		n.getTypeDeclaration().accept(this, arg);
	}

	@Override
	public void visit(final AssertStmt n, final ClassModel arg) {
		writer.write("assert ");
		n.getCheck().accept(this, arg);
		if (n.getMessage() != null) {
			writer.write(" : ");
			n.getMessage().accept(this, arg);
		}
		writer.write(";");
	}

	@Override
	public void visit(final BlockStmt n, final ClassModel arg) {
		writeOrphanCommentsBeforeThisChildNode(n);
		writer.writeln("{");
		if (n.getStmts() != null) {
			writer.in();
			for (final Statement s : n.getStmts()) {
				s.accept(this, arg);
				writer.writeln();
			}
			writer.out();
		}
		writeOrphanCommentsEnding(n);
		writer.write("}");
	}

	@Override
	public void visit(final LabeledStmt n, final ClassModel arg) {
		writer.write(n.getLabel());
		writer.write(": ");
		n.getStmt().accept(this, arg);
	}

	@Override
	public void visit(final EmptyStmt n, final ClassModel arg) {
		writer.write(";");
	}

	@Override
	public void visit(final ExpressionStmt n, final ClassModel arg) {
		writeOrphanCommentsBeforeThisChildNode(n);
		n.getExpression().accept(this, arg);
		writer.write(";");
	}

	@Override
	public void visit(final SwitchStmt n, final ClassModel arg) {
		writer.write("switch(");
		n.getSelector().accept(this, arg);
		writer.writeln(") {");
		if (n.getEntries() != null) {
			writer.in();
			for (final SwitchEntryStmt e : n.getEntries()) {
				e.accept(this, arg);
			}
			writer.out();
		}
		writer.write("}");
	}

	@Override
	public void visit(final SwitchEntryStmt n, final ClassModel arg) {
		if (n.getLabel() != null) {
			writer.write("case ");
			n.getLabel().accept(this, arg);
			writer.write(":");
		} else {
			writer.write("default:");
		}
		writer.writeln();
		writer.in();
		if (n.getStmts() != null) {
			for (final Statement s : n.getStmts()) {
				s.accept(this, arg);
				writer.writeln();
			}
		}
		writer.out();
	}

	@Override
	public void visit(final BreakStmt n, final ClassModel arg) {
		writer.write("break");
		if (n.getId() != null) {
			writer.write(" ");
			writer.write(n.getId());
		}
		writer.write(";");
	}

	@Override
	public void visit(final ReturnStmt n, final ClassModel arg) {
		// TODO: Transform object return to argument passing.

		if (writingKernelMethod)
			writer.write(arg.getKernelMethod().getName() + "_ret = ");
		else
			writer.write("return");

		if (n.getExpr() != null) {
			writer.write(" ");
			n.getExpr().accept(this, arg);
		}
		writer.write(";");
	}

	@Override
	public void visit(final EnumDeclaration n, final ClassModel arg) {
		// TODO
		writer.write("enum ");
		writer.write(n.getName());

		if (!n.getImplements().isEmpty()) {
			writer.write(" implements ");
			for (final Iterator<ClassOrInterfaceType> i = n.getImplements()
					.iterator(); i.hasNext(); ) {
				final ClassOrInterfaceType c = i.next();
				c.accept(this, arg);
				if (i.hasNext()) {
					writer.write(", ");
				}
			}
		}

		writer.writeln(" {");
		writer.in();
		if (n.getEntries() != null) {
			writer.writeln();
			for (final Iterator<EnumConstantDeclaration> i = n.getEntries()
					.iterator(); i.hasNext(); ) {
				final EnumConstantDeclaration e = i.next();
				e.accept(this, arg);
				if (i.hasNext()) {
					writer.write(", ");
				}
			}
		}
		if (!n.getMembers().isEmpty()) {
			writer.writeln(";");
			writeMembers(n.getMembers(), arg);
		} else {
			if (!n.getEntries().isEmpty()) {
				writer.writeln();
			}
		}
		writer.out();
		writer.write("}");
	}

	@Override
	public void visit(final EnumConstantDeclaration n, final ClassModel arg) {
		// TODO
		writer.write(n.getName());

		if (!n.getArgs().isEmpty()) {
			writeArguments(n.getArgs(), arg);
		}

		if (!n.getClassBody().isEmpty()) {
			writer.writeln(" {");
			writer.in();
			writeMembers(n.getClassBody(), arg);
			writer.out();
			writer.writeln("}");
		}
	}

	@Override
	public void visit(final EmptyMemberDeclaration n, final ClassModel arg) {
		writer.write(";");
	}

	@Override
	public void visit(final InitializerDeclaration n, final ClassModel arg) {
		n.getBlock().accept(this, arg);
	}

	@Override
	public void visit(final IfStmt n, final ClassModel arg) {
		writer.write("if (");
		n.getCondition().accept(this, arg);
		final boolean thenBlock = n.getThenStmt() instanceof BlockStmt;
		if (thenBlock) // block statement should start on the same line
			writer.write(") ");
		else {
			writer.writeln(")");
			writer.in();
		}
		n.getThenStmt().accept(this, arg);
		if (!thenBlock)
			writer.out();
		if (n.getElseStmt() != null) {
			if (thenBlock)
				writer.write(" ");
			else
				writer.writeln();
			final boolean elseIf = n.getElseStmt() instanceof IfStmt;
			final boolean elseBlock = n.getElseStmt() instanceof BlockStmt;
			if (elseIf || elseBlock) // put chained if and start of block statement on
				// a same level
				writer.write("else ");
			else {
				writer.writeln("else");
				writer.in();
			}
			n.getElseStmt().accept(this, arg);
			if (!(elseIf || elseBlock))
				writer.out();
		}
	}

	@Override
	public void visit(final WhileStmt n, final ClassModel arg) {
		writer.write("while (");
		n.getCondition().accept(this, arg);
		writer.write(") ");
		n.getBody().accept(this, arg);
	}

	@Override
	public void visit(final ContinueStmt n, final ClassModel arg) {
		writer.write("continue");
		if (n.getId() != null) {
			writer.write(" ");
			writer.write(n.getId());
		}
		writer.write(";");
	}

	@Override
	public void visit(final DoStmt n, final ClassModel arg) {
		writer.write("do ");
		n.getBody().accept(this, arg);
		writer.write(" while (");
		n.getCondition().accept(this, arg);
		writer.write(");");
	}

	@Override
	public void visit(final ForeachStmt n, final ClassModel arg) {
		writer.write("for (");
		n.getVariable().accept(this, arg);
		writer.write(" : ");
		n.getIterable().accept(this, arg);
		writer.write(") ");
		n.getBody().accept(this, arg);
	}

	@Override
	public void visit(final ForStmt n, final ClassModel arg) {
		writer.write("for (");
		if (n.getInit() != null) {
			for (final Iterator<Expression> i = n.getInit().iterator(); i.hasNext(); ) {
				final Expression e = i.next();
				e.accept(this, arg);
				if (i.hasNext()) {
					writer.write(", ");
				}
			}
		}
		writer.write("; ");
		if (n.getCompare() != null) {
			n.getCompare().accept(this, arg);
		}
		writer.write("; ");
		if (n.getUpdate() != null) {
			for (final Iterator<Expression> i = n.getUpdate().iterator(); i.hasNext(); ) {
				final Expression e = i.next();
				e.accept(this, arg);
				if (i.hasNext()) {
					writer.write(", ");
				}
			}
		}
		writer.write(") ");
		n.getBody().accept(this, arg);
	}

	@Override
	public void visit(final ThrowStmt n, final ClassModel arg) {
		logger.warning("Ignore throw statement: " + n.toString());
		writer.write(";");
	}

	@Override
	public void visit(final SynchronizedStmt n, final ClassModel arg) {
		throw new RuntimeException("Not support synchronized statements");
	}

	@Override
	public void visit(final TryStmt n, final ClassModel arg) {
		// TODO
		if (!n.getResources().isEmpty())
			throw new RuntimeException("Not support try-block with resources");

		n.getTryBlock().accept(this, arg);
		if (n.getCatchs() != null) {
			logger.warning("Ignore catch statement: " + n.getCatchs().toString());
		}
		if (n.getFinallyBlock() != null)
			n.getFinallyBlock().accept(this, arg);
	}

	@Override
	public void visit(final CatchClause n, final ClassModel arg) {
		n.getCatchBlock().accept(this, arg);
	}

	@Override
	public void visit(final AnnotationDeclaration n, final ClassModel arg) {
		throw new RuntimeException("Not support user-defined annotations");
	}

	@Override
	public void visit(final AnnotationMemberDeclaration n, final ClassModel arg) {
		throw new RuntimeException("Not support user-defined annotations");
	}

	@Override
	public void visit(final MarkerAnnotationExpr n, final ClassModel arg) {
		; // Do nothing
	}

	@Override
	public void visit(final SingleMemberAnnotationExpr n, final ClassModel arg) {
		; // Do nothing
	}

	@Override
	public void visit(final NormalAnnotationExpr n, final ClassModel arg) {
		; // Do nothing
	}

	@Override
	public void visit(final MemberValuePair n, final ClassModel arg) {
		// TODO: What's this?
		writer.write(n.getName());
		writer.write(" = ");
		n.getValue().accept(this, arg);
	}

	@Override
	public void visit(final LineComment n, final ClassModel arg) {
		; // Do nothing
	}

	@Override
	public void visit(final BlockComment n, final ClassModel arg) {
		; // Do nothing
	}

	@Override
	public void visit(LambdaExpr n, ClassModel arg) {
		// TODO
		final List<Parameter> parameters = n.getParameters();
		final boolean writePar = n.isParametersEnclosed();

		if (writePar) {
			writer.write("(");
		}
		if (parameters != null) {
			for (Iterator<Parameter> i = parameters.iterator(); i.hasNext(); ) {
				Parameter p = i.next();
				p.accept(this, arg);
				if (i.hasNext()) {
					writer.write(", ");
				}
			}
		}
		if (writePar) {
			writer.write(")");
		}

		writer.write(" -> ");
		final Statement body = n.getBody();
		if (body instanceof ExpressionStmt) {
			// Print the expression directly
			((ExpressionStmt) body).getExpression().accept(this, arg);
		} else {
			body.accept(this, arg);
		}
	}

	@Override
	public void visit(MethodReferenceExpr n, ClassModel arg) {
		Expression scope = n.getScope();
		String identifier = n.getIdentifier();
		if (scope != null) {
			n.getScope().accept(this, arg);
		}

		writer.write("::");
		writeTypeArgs(n.getTypeArguments().getTypeArguments(), arg);
		if (identifier != null) {
			writer.write(identifier);
		}
	}

	@Override
	public void visit(TypeExpr n, ClassModel arg) {
		if (n.getType() != null) {
			n.getType().accept(this, arg);
		}
	}
}
