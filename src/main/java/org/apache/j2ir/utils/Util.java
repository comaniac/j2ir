package org.apache.j2ir.utils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.internal.Utils;
import com.github.javaparser.ast.type.Type;
import javafx.util.Pair;
import org.apache.j2ir.model.ClassModel;
import org.apache.j2ir.visitor.TypeVisitor;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public class Util {
	final private static Logger logger = (new J2IRLogger()).logger;
	final private static String WORKING_OS;
	final private static String ps;

	static {
		WORKING_OS = System.getProperty("os.name");
		if (WORKING_OS.contains("Windows"))
			ps = "\\";
		else
			ps = "/";
	}

	public static Map<String, String> buildClass2SrcMapFromJar(String pathString) throws Exception {
		Map<String, String> classMap = new HashMap<>();
		String[] jarPaths = pathString.split(";");

		for (String jar : jarPaths) {
			JarFile f = new JarFile(jar);
			Enumeration<JarEntry> entity = f.entries();
			while (entity.hasMoreElements()) {
				JarEntry je = entity.nextElement();
				if (!je.isDirectory() && je.getName().endsWith(".class")) {
					String clazzName = je.getName().substring(0, je.getName().length() - 6).replace('/', '.');
					classMap.put(clazzName, jar + ":" + je.getName());
					logger.info(clazzName + " -> " + jar + ":" + je.getName());
				}
			}
			f.close();
		}
		return classMap;
	}

	public static void dumpMap(Map<?, ?> m) {
		for (Object key : m.keySet()) {
			logger.severe(key + " -> " + m.get(key));
		}
	}

	public static String decompileClassToJava(String filePath) throws IOException, InterruptedException {
		// FIXME: Configurable temp directory path
		String tmpPath = "tmpOutput";

		String bcFilePath = filePath;
		if (filePath.contains(":")) { // Class file is put inside a jar file
			String jarFilePath = filePath.substring(0, filePath.lastIndexOf(":"));
			bcFilePath = filePath.substring(0, filePath.lastIndexOf(ps) + 1) +
					filePath.substring(filePath.lastIndexOf(":") + 1, filePath.length());
			try {
				Process unzipProc = Runtime.getRuntime().exec("jar xMf " + jarFilePath + " " + bcFilePath,
						null, new File(jarFilePath.substring(0, jarFilePath.lastIndexOf(ps))));
				unzipProc.waitFor();
			} catch (IOException e) {
				throw new RuntimeException("Cannot open jar file " + jarFilePath);
			}
		}

		File bcFile = new File(bcFilePath);
		if (!bcFile.exists())
			throw new RuntimeException("Cannot load class file " + bcFilePath);

		Process decompileProc = Runtime.getRuntime().exec("java -jar fernflower.jar " + bcFilePath + " " + tmpPath);
		decompileProc.waitFor();

//		Map<String, Object> mapOptions = new HashMap<String, Object>();
//		File dest = new File(tmpPath);
//		ConsoleDecompiler decompiler = new ConsoleDecompiler(dest, mapOptions);
//		decompiler.addSpace(bcFile, true);
//		decompiler.decompileContext();

		// FIXME: Windows/Linux support
		String JavaFilePath = tmpPath + ps;

		if (bcFilePath.contains(ps))
			JavaFilePath += bcFilePath.substring(bcFilePath.lastIndexOf(ps) + 1, bcFilePath.length());
		else
			JavaFilePath += bcFilePath;
		JavaFilePath = JavaFilePath.replace(".class", ".java");

		// Preprocess to bridge the gap between decompiler and java parser
		String tmpFilePath = JavaFilePath.replace(".java", "_processed.java");

		BufferedWriter bw = new BufferedWriter(new FileWriter(tmpFilePath));
		try (BufferedReader br = new BufferedReader(new FileReader(JavaFilePath))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("import ") && line.contains(".class"))
					continue;
				else if (line.contains(".;"))
					continue;
				else if (line.contains(".MODULE$"))
					continue;
				if (line.contains("class."))
					line = line.replace("class.", "super.");
				bw.write(line + "\n");
			}
		}
		bw.close();


		return tmpFilePath;
	}

	public static CompilationUnit parseJavaSource(String srcFilePath) throws ParseException, IOException {
		CompilationUnit cu;
		try (FileInputStream f = new FileInputStream(srcFilePath)) {
			cu = JavaParser.parse(f);
		}
		return cu;
	}

	public static Type getExpType(Expression exp, Map<String, Type> typeEnv, ClassModel model) {
		return new TypeVisitor(model).visit(exp, typeEnv);
	}

	public static MethodDeclaration getFirstMethodByName(CompilationUnit cu, String name) {
		List<TypeDeclaration> types = cu.getTypes();
		for (TypeDeclaration type : types) {
			List<BodyDeclaration> members = type.getMembers();
			for (BodyDeclaration member : members) {
				if (member instanceof MethodDeclaration) {
					MethodDeclaration method = (MethodDeclaration) member;
					if (name.equals(method.getName()))
						return method;
				}
			}
		}
		return null;
	}

	public static MethodDeclaration getFirstMethodWithAnnotation(CompilationUnit cu, String annotation) {
		List<TypeDeclaration> types = cu.getTypes();
		for (TypeDeclaration type : types) {
			List<BodyDeclaration> members = type.getMembers();
			for (BodyDeclaration member : members) {
				if (member instanceof MethodDeclaration) {
					MethodDeclaration method = (MethodDeclaration) member;
					if (method.getAnnotations() != null) {
						for (final AnnotationExpr e : method.getAnnotations()) {
							if (e.getName().toString().equals(annotation))
								return method;
						}
					}
				}
			}
		}
		return null;
	}

	public static boolean isSameMethodSig(String sig1, String sig2) {
		String [] str1 = sig1.split("-");
		String [] str2 = sig2.split("-");

		if (str1.length != str2.length)
			return false;

		for (int i = 0; i < str1.length; i += 1) {
			if (!str1[i].equals(str2[i])) {
				if (!str1[i].equals(("null")) && !str2[i].equals("null"))
					return false;
			}
		}
		return true;
	}

	public static String getMethodSig(BodyDeclaration n) {
		String sig;
		List<Parameter> params;

		if (n instanceof MethodDeclaration) {
			sig = ((MethodDeclaration) n).getName();
			params = ((MethodDeclaration) n).getParameters();
		}
		else {
			sig = ((ConstructorDeclaration) n).getName();
			params = ((ConstructorDeclaration) n).getParameters();
		}

		if (!Utils.isNullOrEmpty(params)) {
			for (final Parameter p : params)
				sig += "-" + p.getType().toString();
		}
		return sig;
	}

	public static String getMethodSig(Expression n, Map<String, Type> typeEnv, ClassModel model) {
		String sig;
		List<Expression> args;
		if (n instanceof MethodCallExpr) {
			MethodCallExpr m = (MethodCallExpr) n;
			sig = m.getName();
			args = m.getArgs();
		}
		else if (n instanceof ObjectCreationExpr) {
			ObjectCreationExpr m = (ObjectCreationExpr) n;
			sig = m.getType().getName();
			args = m.getArgs();
		}
		else
			throw new RuntimeException("Cannot fetch method signature from " + n.toString());

		if (args.size() != 0) {
			for (final Expression e : args) {
				Type type = getExpType(e, typeEnv, model);
				if (type == null)
					throw new RuntimeException("Cannot infer type for " + e.toString());
				sig += "-" + type.toString();
			}
		}
		return sig;
	}

	public static BodyDeclaration getMethodDeclarationBySig(ClassOrInterfaceDeclaration classDecl, String targetSig) {
		List<BodyDeclaration> members = classDecl.getMembers();
		for (BodyDeclaration member : members) {
			if (member instanceof MethodDeclaration) {
				MethodDeclaration method = (MethodDeclaration) member;
				String sig = method.getName();
				if (!Utils.isNullOrEmpty(method.getParameters())) {
					for (final Parameter p : method.getParameters())
						sig += "-" + p.getType().toString();
				}
				if (isSameMethodSig(sig, targetSig))
					return method;
			}
			else if (member instanceof ConstructorDeclaration) {
				ConstructorDeclaration method = (ConstructorDeclaration) member;
				String sig = method.getName();
				if (!Utils.isNullOrEmpty(method.getParameters())) {
					for (final Parameter p : method.getParameters())
						sig += "-" + p.getType().toString();
				}
				if (isSameMethodSig(sig, targetSig))
					return method;
			}
		}
		return null;
	}

	public static ClassOrInterfaceDeclaration getClassOrInterfaceDeclarationByName(CompilationUnit cu, String targetName) {
		List<TypeDeclaration> types = cu.getTypes();
		for (TypeDeclaration type : types) {
			if (type instanceof ClassOrInterfaceDeclaration) {
				ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) type;
				if (classDecl.getName().equals(targetName)) {
					return classDecl;
				}
			}
		}
		return null;
	}
}
