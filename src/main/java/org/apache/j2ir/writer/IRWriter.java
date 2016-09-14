package org.apache.j2ir.writer;

import com.github.javaparser.PositionUtils;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.internal.Utils;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitor;
import org.apache.j2ir.model.ClassModel;
import org.apache.j2ir.model.FieldModel;
import org.apache.j2ir.utils.J2IRLogger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public abstract class IRWriter implements VoidVisitor<ClassModel> {
	final protected static Logger logger = (new J2IRLogger()).logger;
	protected CodeWriter writer;

	public static class CodeWriter {
		final static private String lineSeparator;

		static {
			//lineSeparator = System.getProperty("line.separator");
			lineSeparator = "\n";
		}

		private final String indentation;
		private int level = 0;
		private boolean ined = false;
		private final StringBuilder buf = new StringBuilder();

		public CodeWriter(final String indentation) {
			this.indentation = indentation;
		}

		public void in() {
			level++;
		}

		public void out() {
			level--;
		}

		private void makeIndent() {
			for (int i = 0; i < level; i++) {
				buf.append(indentation);
			}
		}

		public void write(final String arg) {
			if (!ined) {
				makeIndent();
				ined = true;
			}
			buf.append(arg);
		}

		public void writeln(final String arg) {
			write(arg);
			writeln();
		}

		public void writeln() {
			buf.append(lineSeparator);
			ined = false;
		}

		public String getCode() {
			return buf.toString();
		}

		@Override
		public String toString() {
			return getCode();
		}
	}

	public String getCode() {
		return writer.getCode();
	}

	public void saveAsFile(String fileName) throws IOException {
		BufferedWriter f = new BufferedWriter(new FileWriter(fileName));
		f.write(writer.getCode());
		f.close();
	}

	protected void writeMembers(final List<BodyDeclaration> members,
														final ClassModel arg) {
		for (final BodyDeclaration member : members) {
			writer.writeln();
			member.accept(this, arg);
			writer.writeln();
		}
	}

	protected void writeTypeArgs(final List<Type> args, final ClassModel arg) {
		if (!Utils.isNullOrEmpty(args)) {
			writer.write("_");
			for (final Iterator<Type> i = args.iterator(); i.hasNext(); ) {
				final Type t = i.next();
				t.accept(this, arg);
				if (i.hasNext()) {
					writer.write("_");
				}
			}
		}
	}

	protected void writeTypeParameters(final List<TypeParameter> args,
																	 final ClassModel arg) {
		if (!Utils.isNullOrEmpty(args)) {
			writer.write("_");
			for (final Iterator<TypeParameter> i = args.iterator(); i.hasNext(); ) {
				final TypeParameter t = i.next();
				t.accept(this, arg);
				if (i.hasNext()) {
					writer.write("_");
				}
			}
		}
	}

	protected void writeArguments(final List<Expression> args, final ClassModel arg) {
		// FIXME: Here we assume the method caller must be the method within the same (entry) class.

		writer.write("(");
		if (!Utils.isNullOrEmpty(args)) {
			for (final Iterator<Expression> i = args.iterator(); i.hasNext(); ) {
				final Expression e = i.next();
				e.accept(this, arg);
				if (i.hasNext()) {
					writer.write(", ");
				}
			}
		}
		if (arg.isEntryClass()) {
			Map<String, FieldModel> fields = arg.getFields();
			for (String argName : fields.keySet()) {
				if (!Utils.isNullOrEmpty(args))
					writer.write(", ");
				writer.write(argName);
			}
		}
		writer.write(")");
	}

	protected void writeOrphanCommentsBeforeThisChildNode(final Node node) {
		if (node instanceof Comment)
			return;

		Node parent = node.getParentNode();
		if (parent == null)
			return;

		List<Node> everything = new LinkedList<>();
		everything.addAll(parent.getChildrenNodes());
		PositionUtils.sortByBeginPosition(everything);
		int positionOfTheChild = -1;
		for (int i = 0; i < everything.size(); i++) {
			if (everything.get(i) == node)
				positionOfTheChild = i;
		}
		if (positionOfTheChild == -1)
			throw new RuntimeException("My index not found!!! " + node);

		int positionOfPreviousChild = -1;
		for (int i = positionOfTheChild - 1; i >= 0
				&& positionOfPreviousChild == -1; i--) {
			if (!(everything.get(i) instanceof Comment))
				positionOfPreviousChild = i;
		}
		for (int i = positionOfPreviousChild + 1; i < positionOfTheChild; i++) {
			Node nodeToPrint = everything.get(i);
			if (!(nodeToPrint instanceof Comment))
				throw new RuntimeException("Expected comment, instead "
						+ nodeToPrint.getClass() + ". Position of previous child: "
						+ positionOfPreviousChild + ", position of child "
						+ positionOfTheChild);
			nodeToPrint.accept(this, null);
		}
	}

	protected void writeOrphanCommentsEnding(final Node node) {
		List<Node> everything = new LinkedList<>();
		everything.addAll(node.getChildrenNodes());
		PositionUtils.sortByBeginPosition(everything);
		if (everything.isEmpty()) {
			return;
		}

		int commentsAtEnd = 0;
		boolean findingComments = true;
		while (findingComments && commentsAtEnd < everything.size()) {
			Node last = everything.get(everything.size() - 1 - commentsAtEnd);
			findingComments = (last instanceof Comment);
			if (findingComments) {
				commentsAtEnd++;
			}
		}
		for (int i = 0; i < commentsAtEnd; i++) {
			everything.get(everything.size() - commentsAtEnd + i).accept(this, null);
		}
	}
}
