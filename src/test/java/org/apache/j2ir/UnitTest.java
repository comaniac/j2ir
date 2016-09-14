package org.apache.j2ir;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

class UnitTest {
	final private static String WORKING_OS;
	final private static String ps;

	static {
		WORKING_OS = System.getProperty("os.name");
		if (WORKING_OS.contains("Windows"))
			ps = "\\";
		else
			ps = "/";
	}

	private final String tmpOutputDir = "tmpOutput";

	void doTest(String testName) {
		String kernelName = testName;
		if (kernelName.contains("/"))
			kernelName = kernelName.substring(kernelName.lastIndexOf('/') + 1, kernelName.length());

		// Setup working directory
		String prjOutputDir = tmpOutputDir + ps + kernelName;
		new File(prjOutputDir).delete();
		new File(prjOutputDir).mkdir();

		String[] args = new String[3];
		try {
			// Setup config file
			args[1] = getResourceFileFullPath(testName + "/" + kernelName + ".xml");
			assert (args[1] != null);

			// Setup resource path
			String testPath = args[1].substring(0, args[1].lastIndexOf("/"));

			// Setup jar file
			args[0] = getResourceFileFullPath(testPath + ".jar");
			if (args[0] == null) { // Make a jar file
				Process compileProc = Runtime.getRuntime().exec("javac -g -d " + prjOutputDir + " " + testPath + "/*.java");
				compileProc.waitFor();
				compileProc = Runtime.getRuntime().exec("cmd /c start /b scalac.bat -d " + prjOutputDir + " " + testPath + "/*.scala");
				compileProc.waitFor();
				Process jarProc = Runtime.getRuntime().exec("jar cf " + kernelName + ".jar *.class",
						null, new File(prjOutputDir));
				System.err.println("jar cf " + kernelName + ".jar *.class");
				jarProc.waitFor();
				args[0] = prjOutputDir + ps + kernelName + ".jar";
			}

			// Setup output file
			args[2] = prjOutputDir + ps + kernelName;

			// Setup golden file
			String goldenSrcFile = testPath + ps + kernelName + "_expected.cpp";
			String goldenHeadFile = testPath + ps + kernelName + "_expected.h";

			System.out.println("Testing " + testName);
			J2IR.main(args);
			int srcResCode = compareResult(goldenSrcFile, args[2] + ".cpp");
			int headResCode = compareResult(goldenHeadFile, args[2] + ".h");
			if (srcResCode == 0 && headResCode == 0)
				System.out.println(testName + " passed");
			else if (srcResCode == -1 || headResCode == -1)
				throw new RuntimeException(testName + " failed: cannot find output file");
			else
				throw new RuntimeException(testName + " failed: result mismatch");
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(testName + " failed: ");
		}
	}

	private String getResourceFileFullPath(String fileName) {
		final URL res = getClass().getClassLoader().getResource(fileName);
		if (res == null)
			return null;
		String path = res.getFile();
		if (path.startsWith("/"))
			path = path.substring(1, path.length());
		return path;
	}

	private int compareResult(String expected, String generate) throws IOException {
		Path ePath = Paths.get(expected);
		Path gPath = Paths.get(generate);
		if (!Files.exists(ePath) || !Files.exists(gPath))
			return -1;

		byte[] e = Files.readAllBytes(ePath);
		byte[] g = Files.readAllBytes(gPath);
		if (Arrays.equals(e, g))
			return 0;
		return 1;
	}
}
