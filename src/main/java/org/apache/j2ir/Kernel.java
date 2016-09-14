package org.apache.j2ir;

import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.apache.j2ir.model.ClassModel;
import org.apache.j2ir.model.MethodModel;
import org.apache.j2ir.utils.J2IRLogger;
import org.apache.j2ir.utils.Util;
import org.apache.j2ir.visitor.MethodVisitor;
import org.apache.j2ir.writer.CppWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class Kernel {
	final private static Logger logger = (new J2IRLogger()).logger;
	private final ClassModel entryClass;
	private final Map<String, Map<String, String>> attr;
	private Map<String, ClassModel> usedClasses = new HashMap<>();

	Kernel(MethodDeclaration kernelMethod, Map<String, String> classSrcMap, Map<String, Map<String, String>> attr)
			throws ParseException, IOException, InterruptedException {
		this.attr = attr;

		// Initial main class
		ClassModel tmpModel = null;
		Node node = kernelMethod.getParentNode();
		while (node != null) {
			if (node instanceof ClassOrInterfaceDeclaration) {
				tmpModel = new ClassModel((ClassOrInterfaceDeclaration) node);
				break;
			}
			node = node.getParentNode();
		}
		if (tmpModel == null)
			throw new RuntimeException(
					"Cannot find the class declaration for the input method.");
		entryClass = tmpModel;

		// Setup kernel method
		entryClass.addMethod(kernelMethod, true);

		// Build type environment for class and kernel method
		entryClass.buildOrUpdateTypeEnv();
		entryClass.getKernelMethod().buildOrUpdateTypeEnv();

		// Traverse kernel method
		MethodVisitor methodVisitor = new MethodVisitor(usedClasses);
		methodVisitor.visit(kernelMethod, entryClass.getKernelMethod());
		logger.info(entryClass.toString());

		// Build models for reference classes/methods
		boolean done = false;
		int iter = 1;
		while (!done) {
			logger.info("Processing reference class iteration #" + iter);

			// Temporary list for reference classes
			Map<String, ClassModel> tmpUsedClasses = new HashMap<>();

			for (String cls : usedClasses.keySet()) {
				ClassModel classModel = usedClasses.get(cls);
				if (classModel.isBuilt())
					continue;

				logger.info("\tProcessing class " + cls);

				buildClassModelFromSource(classModel, classSrcMap);

				// Check base classes
				for (ClassOrInterfaceType base : classModel.getDecl().getExtends()) {
					ClassModel baseModel = new ClassModel(base.getName());
					tmpUsedClasses.put(base.getName(), baseModel);
					classModel.addBaseClass(baseModel);
					buildClassModelFromSource(baseModel, classSrcMap);
				}

				// Setup method declaration
				boolean methodDone = false;
				while (!methodDone) {
					methodDone = true;
					Set<String> sigs = new HashSet<>(classModel.getMethods().keySet());
					for (String sig : sigs) {
						MethodModel methodModel = classModel.getMethods().get(sig);
						if (methodModel.isBuilt())
							continue;

						methodDone = false;

						logger.info("\t\tProcessing method " + sig);

						BodyDeclaration decl = Util.getMethodDeclarationBySig(classModel.getDecl(), sig);
						if (decl == null)
							throw new RuntimeException("Cannot find method " + sig + " from the class");
						methodModel.setDecl(decl);

						// Update sig if necessary since the method sig may be
						// inferred from a method call expr which contains "null"
						if (!sig.equals(Util.getMethodSig(decl))) {
							String newSig = Util.getMethodSig(decl);
							classModel.getMethods().remove(sig);
							classModel.getMethods().put(newSig, methodModel);
							logger.info("\t\t-> Update sig to " + newSig);
						}

						// Build type environment for the method
						methodModel.buildOrUpdateTypeEnv();
						MethodVisitor tmpMethodVisitor = new MethodVisitor(tmpUsedClasses);
						if (methodModel.isConstructor())
							tmpMethodVisitor.visit((ConstructorDeclaration) decl, methodModel);
						else
							tmpMethodVisitor.visit((MethodDeclaration) decl, methodModel);
					}
				}
			}
			if (tmpUsedClasses.size() == 0)
				done = true;
			else {
				for (String clsName : tmpUsedClasses.keySet())
					usedClasses.put(clsName, tmpUsedClasses.get(clsName));
			}
			iter += 1;
		}
		logger.info("Finish collecting all necessary classes and methods using " + (iter - 1) + " iterations");
	}

	public ClassModel getEntryClass() {
		return entryClass;
	}

	public CppWriter writeCpp() {
		CppWriter cppWriter = new CppWriter(this.attr);
		for (String cls : usedClasses.keySet()) {
			ClassModel classModel = usedClasses.get(cls);
			cppWriter.writeToHead(classModel.getDecl(), classModel);
		}
		cppWriter.writeToSource(entryClass.getDecl(), entryClass);
		return cppWriter;
	}

	private void buildClassModelFromSource(ClassModel classModel, Map<String, String> classSrcMap)
			throws IOException, InterruptedException, ParseException {

		String classFilePath = classSrcMap.get(classModel.getName());
		if (classFilePath == null) {
			throw new RuntimeException("Cannot find class " + classModel.getName() +
			" in the provided jar files");
		}

		// FIXME: Decompile class file
		//String JavaFilePath = classFilePath.replace(".class", ".java");
		String JavaFilePath = Util.decompileClassToJava(classFilePath);

		CompilationUnit cu = Util.parseJavaSource(JavaFilePath);

		// Setup class declaration
		ClassOrInterfaceDeclaration classDecl = Util.getClassOrInterfaceDeclarationByName(cu, classModel.getName());
		assert (classDecl != null);
		classModel.setDecl(classDecl);

		// Build type environment for the class
		classModel.buildOrUpdateTypeEnv();
	}
}
