package org.apache.j2ir;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.apache.j2ir.utils.J2IRLogger;
import org.apache.j2ir.utils.Util;
import org.apache.j2ir.writer.CppWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class J2IR {
	final private static Logger logger = (new J2IRLogger()).logger;

	private static String entryClassName;
	private static String kernelName;
	private static Map<String, Map<String, String>> kernelAttr = new HashMap<>();

	public static void main(String[] args) throws Exception {
		if (args.length != 3) {
			System.err.println("Usage: java -jar j2ir.jar <Jar files> <Config file> <Output file>");
			return;
		}
		logger.info("Jar files: " + args[0]);
		logger.info("Config file: " + args[1]);
		logger.info("Output file: " + args[2]);

		final Map<String, String> classSrcMap = Util.buildClass2SrcMapFromJar(args[0]);

		// TEST
//		classSrcMap.put("Vector", "..\\test\\Vector.class");
//		classSrcMap.put("BlazeBroadcast", "..\\test\\BlazeBroadcast.class");
		// TEST

		parseConfig(args[1]);
		String entryClassFilePath = classSrcMap.get(entryClassName);
		if (entryClassFilePath == null) {
			logger.severe("Available class map:");
			Util.dumpMap(classSrcMap);
			throw new RuntimeException("Cannot find the path for entry class " + entryClassName);
		}

		String entryJavaFilePath = Util.decompileClassToJava(entryClassFilePath);

		CompilationUnit cu = Util.parseJavaSource(entryJavaFilePath);
		MethodDeclaration kernelMethod = Util.getFirstMethodByName(cu, kernelName);
		if (kernelMethod == null)
			throw new RuntimeException("Cannot find the kernel method");

		Kernel kernel = new Kernel(kernelMethod, classSrcMap, kernelAttr);
		logger.info("Target: " + kernel.getEntryClass().getName() + "::" + kernelMethod.getName());

		logger.info("Generating output in CPP form");
		CppWriter cppWriter = kernel.writeCpp();
		cppWriter.saveAsFile(args[2]);
	}

	private static void parseConfig(String filePath) throws ParserConfigurationException, IOException, SAXException {
		File inputFile = new File(filePath);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(inputFile);
		doc.getDocumentElement().normalize();
		String fullKernelName = doc.getDocumentElement().getAttribute("name");
		entryClassName = fullKernelName.substring(0, fullKernelName.indexOf('.'));
		kernelName = fullKernelName.substring(fullKernelName.indexOf('.') + 1, fullKernelName.length());

		NodeList nList = doc.getElementsByTagName("variable");
		for (int i = 0; i < nList.getLength(); i += 1) {
			Node node = nList.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE)
				continue;
			Element elt = (Element) node;
			String varName = elt.getElementsByTagName("name").item(0).getTextContent();
			Map<String, String> attrMap = new HashMap<>();
			for(String attr : Config.kernelAttrList) {
				String value = elt.getElementsByTagName(attr).item(0).getTextContent();
				attrMap.put(attr, value);
			}
			kernelAttr.put(varName, attrMap);
		}
	}
}