package com.st.plugin.fingerprint;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class FingerprintMojo extends AbstractMojo {

	/*
	 *  All resources should have absolute paths:
     *  Valid: <img src="/img/test.png"> .
     *  Invalid: <img src="test.png">
     *	All resources should point to existing files without any pre-processing:
     *  Valid: <img src="/img/test.png"> .
     *  Invalid: <img src="<c:if test="${var}">/img/test.png</c:if>"
	 */
	public Pattern LINK_PATTERN = Pattern.compile("(<link.*?href=\")(.*?)(\".*?>)");
	public Pattern SCRIPT_PATTERN = Pattern.compile("(\")([^\\s]*?\\.js)(\")");
	public Pattern IMG_PATTERN = Pattern.compile("(<img.*?src=\")(.*?)(\".*?>)");
	public Pattern CSS_IMG_PATTERN = Pattern.compile("(url\\([\",'])(.*?)([\",']\\))");
	public Pattern JSTL_URL_PATTERN = Pattern.compile("(<c:url.*?value=\")(/{1}.*?)(\".*?>)");
	public String DYNAMIC_SCRIPT_PATTERN_STRING = Pattern.quote("${")+"[\\S]+"+Pattern.quote("}");
	public Pattern DYNAMIC_SCRIPT_PATTERN = Pattern.compile(DYNAMIC_SCRIPT_PATTERN_STRING);	
	
	//([^<%\w%>])\S*.js
	
	/**
	 * Output directory
	 */
	@Parameter(defaultValue = "${project.build.directory}/fingered-web", required = true)
	private File outputDirectory;

	/**
	 * Webapp directory
	 */
	@Parameter(defaultValue="${basedir}/src/main/webapp", required = true)
	private File sourceDirectory;

	/**
	 * Exclude resources
	 */
	@Parameter
	private List<String> excludeResources;

	@Parameter
	private List<String> extensionsToFilter;

	@Parameter
	private Set<String> trimTagExtensions;

	/**
	 * CDN url
	 */
	@Parameter
	private String cdn;
	
	public static final String FINGERPRINT_SEPERATOR = "-";

	private final Map<String, String> processedFiles = new HashMap<String, String>();

	//@Override
	public void execute() throws MojoExecutionException {
		if (!sourceDirectory.isDirectory()) {
			throw new MojoExecutionException("source directory is not a directory: " + sourceDirectory.getAbsolutePath());
		}
		if (!outputDirectory.exists()) {
			if (!outputDirectory.mkdirs()) {
				throw new MojoExecutionException("unable to create outputdirectory: " + outputDirectory.getAbsolutePath());
			}
		}
		if (!outputDirectory.isDirectory()) {
			throw new MojoExecutionException("output directory is not a directory: " + outputDirectory.getAbsolutePath());
		}
		List<File> pagesToFilter = new ArrayList<File>();
		findPagesToFilter(pagesToFilter, sourceDirectory);
		if (pagesToFilter.isEmpty()) {
			return;
		}

		copyDirectories(sourceDirectory, outputDirectory);

		for (File curHTml : pagesToFilter) {
			processPage(curHTml);
		}

		
		try {
			copyDeepFiles(sourceDirectory, outputDirectory);
		} catch (IOException e) {
			throw new MojoExecutionException("unable to deep copy files", e);
		}
		
		processJavascriptApp(sourceDirectory, outputDirectory);
	}

	private void processPage(File file) throws MojoExecutionException {
		File fileToProcess;
		if (processedFiles.containsKey(file.getAbsolutePath())) {
			fileToProcess = new File(processedFiles.get(file.getAbsolutePath()));
		} else {
			fileToProcess = file;
		}
		getLog().info("processing file: " + fileToProcess.getAbsolutePath());
		String data = readFile(fileToProcess);
		StringBuffer outputFileData = new StringBuffer(data);
		outputFileData = processPattern(LINK_PATTERN, outputFileData.toString());
		outputFileData = processPattern(SCRIPT_PATTERN, outputFileData.toString());
		outputFileData = processPattern(IMG_PATTERN, outputFileData.toString());
		outputFileData = processPattern(CSS_IMG_PATTERN, outputFileData.toString());
		outputFileData = processPattern(JSTL_URL_PATTERN, outputFileData.toString());
		
		String processedData = null;
		if (trimTagExtensions != null && !trimTagExtensions.isEmpty()) {
			String extension = getExtension(fileToProcess.getName());
			if (extension != null && trimTagExtensions.contains(extension)) {
				processedData = TextShrinker.shrink(outputFileData.toString());
			}
		}

		if (processedData == null) {
			processedData = outputFileData.toString();
		}

		FileWriter w = null;
		File targetFile;
		if (!processedFiles.containsKey(file.getAbsolutePath())) {
			String targetHtmlFilename = generateTargetFilename(sourceDirectory, fileToProcess);
			targetFile = new File(outputDirectory, targetHtmlFilename);
		} else {
			targetFile = fileToProcess;
		}
		try {
			w = new FileWriter(targetFile);
			w.append(processedData);
			w.flush();
		} catch (IOException e) {
			throw new MojoExecutionException("unable to write html file: " + targetFile.getAbsolutePath(), e);
		} finally {
			if (w != null) {
				try {
					w.close();
				} catch (IOException e) {
					getLog().warn("unable to close file cursor", e);
				}
			}
		}
		if (!processedFiles.containsKey(file.getAbsolutePath())) {
			processedFiles.put(file.getAbsolutePath(), targetFile.getAbsolutePath());
		}
	}

	private StringBuffer processPattern(Pattern p, String data) throws MojoExecutionException {
		StringBuffer outputFileData = new StringBuffer();
		Matcher m = p.matcher(data);
		while (m.find()) {
			String curLink = m.group(2);
			
			String dynamicPath = null;
			Matcher dm = DYNAMIC_SCRIPT_PATTERN.matcher(curLink);
			if(dm.find()) {
				dynamicPath = dm.group();
				curLink = curLink.replaceAll(DYNAMIC_SCRIPT_PATTERN_STRING, "");
				getLog().info("Replaced link: "+curLink);
			}
			
			for (int i = 0; i < m.groupCount(); ++i) {
				getLog().debug("group " + i + ": " + m.group(i));
			}
			if (isExcluded(curLink)) {
				getLog().info("resource excluded: " + curLink);
				m.appendReplacement(outputFileData, "$1" + curLink + "$3");
				continue;
			}
			int queryIndex = curLink.indexOf("?");
			String query = "";
			if (queryIndex != -1) {
				query = curLink.substring(queryIndex);
				curLink = curLink.substring(0, queryIndex);
			} else {
				queryIndex = curLink.indexOf("#");
				if (queryIndex != -1) {
					query = curLink.substring(queryIndex);
					curLink = curLink.substring(0, queryIndex);
				}
			}

			File linkFile = new File(sourceDirectory, curLink);
			if (!linkFile.exists()) {
				getLog().warn("resource file doesnt exist: " + curLink + " file: " + linkFile.getName());
				curLink = curLink.replaceAll("\\$", "\\\\\\$");
				m.appendReplacement(outputFileData, "$1" + curLink + "$3");
				continue;
			}
			if (curLink.length() > 0 && curLink.charAt(0) != '/') {
				getLog().warn("resource has relative path: " + curLink);
			}
			String fingerprint = generateFingerprint(readBinaryFile(linkFile));
			String targetPath = generateTargetResourceFilename(fingerprint, curLink);
			if (targetPath.length() != 0 && targetPath.charAt(0) != '/') {
				getLog().warn("relative path detected: " + curLink);
			}
			

			String targetURL;
			if (cdn == null) {
				String path = targetPath + query;
				if(dynamicPath!=null) {
					path =  Matcher.quoteReplacement(dynamicPath+path);
				}
				targetURL = "$1" + path + "$3";
			} else {
				targetURL = "$1" + cdn + targetPath + query + "$3";
			}

			m.appendReplacement(outputFileData, targetURL);
			File targetFilename = new File(outputDirectory, targetPath);
			if (targetFilename.exists()) {
				getLog().info("processing link: " + linkFile.getAbsolutePath());
				continue;
			}
			targetFilename.getParentFile().mkdirs();
			getLog().info("processing link: " + linkFile.getAbsolutePath() + " copy to: " + targetFilename.getAbsolutePath());
			if (processedFiles.containsKey(linkFile.getAbsolutePath())) {
				String pathWithinSource = linkFile.getAbsolutePath();
				linkFile = new File(processedFiles.get(pathWithinSource));
				processedFiles.put(pathWithinSource, targetFilename.getAbsolutePath());
				try {
					copy(new FileInputStream(linkFile), new FileOutputStream(targetFilename), 2048);
				} catch (Exception e) {
					throw new MojoExecutionException("unable to copy resource file: " + linkFile + " to: " + targetFilename, e);
				}
				if (!linkFile.delete()) {
					getLog().warn("unable to move " + linkFile.getAbsolutePath());
				}
			} else {
				processedFiles.put(linkFile.getAbsolutePath(), targetFilename.getAbsolutePath());
				try {
					copy(new FileInputStream(linkFile), new FileOutputStream(targetFilename), 2048);
				} catch (Exception e) {
					throw new MojoExecutionException("unable to copy resource file: " + linkFile + " to: " + targetFilename, e);
				}
			}
		}
		m.appendTail(outputFileData);
		return outputFileData;
	}

	private boolean isExcluded(String path) {
		if (excludeResources == null) {
			return false;
		}
		for (String curExclude : excludeResources) {
			if (path.contains(curExclude)) {
				return true;
			}
		}
		return false;
	}

	private static String generateFingerprint(byte[] data) throws MojoExecutionException {
		MessageDigest md5Alg;
		try {
			md5Alg = MessageDigest.getInstance("MD5");
			md5Alg.reset();
			byte[] digest = md5Alg.digest(data);
			BigInteger result = new BigInteger(digest);
			String resultStr = null;
			if (result.signum() < 0) {
				resultStr = result.negate().toString(16);
			} else {
				resultStr = result.toString(16);
			}
			return resultStr;
		} catch (NoSuchAlgorithmException e) {
			throw new MojoExecutionException("unable to generate fingerprint", e);
		}
	}

	static String generateTargetResourceFilename(String fingerprint, String sourceFilename) {
		int index = sourceFilename.lastIndexOf("/");
		if (index == -1) {
			return fingerprint + sourceFilename;
		}
		String filename = sourceFilename.substring(index + 1);
		return sourceFilename.substring(0, index) + "/" + fingerprint + FINGERPRINT_SEPERATOR + filename;
	}

	static String generateTargetFilename(File sourceDirectory, File file) {
		return file.getAbsolutePath().substring(sourceDirectory.getAbsolutePath().length());
	}

	private void copyDirectories(File curSrcDirectory, File curDestDirectory) {
		if (!curSrcDirectory.isDirectory()) {
			return;
		}
		File[] subFiles = curSrcDirectory.listFiles();
		for (File curFile : subFiles) {
			if (!curFile.isDirectory()) {
				continue;
			}
			File newDir = new File(curDestDirectory, curFile.getName());
			if (!newDir.exists()) {
				if (!newDir.mkdirs()) {
					getLog().warn("unable to create directory in outputDirectory: " + newDir);
					continue;
				}
			}
			copyDirectories(curFile, newDir);
		}
	}

	private void copyDeepFiles(File srcDir, File dstDir) throws IOException {
		File[] srcFiles = srcDir.listFiles();
		for (File curFile : srcFiles) {
			if (curFile.isDirectory()) {
				copyDeepFiles(curFile, new File(dstDir, curFile.getName()));
				continue;
			}

			if (processedFiles.containsKey(curFile.getAbsolutePath())) {
				continue;
			}

			File targetFilename = new File(dstDir, curFile.getName());
			targetFilename.getParentFile().mkdirs();
			copy(new FileInputStream(curFile), new FileOutputStream(targetFilename), 2048);
		}
	}

	private String readFile(File file) throws MojoExecutionException {
		BufferedReader r = null;
		String curLine = null;
		StringBuilder builder = new StringBuilder();
		try {
			r = new BufferedReader(new FileReader(file));
			while ((curLine = r.readLine()) != null) {
				builder.append(curLine);
				builder.append("\n");
			}
		} catch (Exception e) {
			throw new MojoExecutionException("unable to read file: " + file, e);
		} finally {
			if (r != null) {
				try {
					r.close();
				} catch (IOException e) {
					getLog().warn("unable to close file cursor: " + file, e);
				}
			}
		}
		return builder.toString();
	}

	private byte[] readBinaryFile(File f) throws MojoExecutionException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(f);
			byte[] buf = new byte[2048];
			int bytesRead = -1;
			while ((bytesRead = fis.read(buf)) != -1) {
				baos.write(buf, 0, bytesRead);
			}
			return baos.toByteArray();
		} catch (Exception e) {
			throw new MojoExecutionException("unable to read file: " + f, e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					getLog().warn("unable to close file cursor: " + f, e);
				}
			}
		}
	}

	private static void copy(InputStream inputStream, OutputStream outputStream, int bufferSize) throws IOException {
		byte[] buffer = new byte[bufferSize];
		int n;
		while (-1 != (n = inputStream.read(buffer))) {
			outputStream.write(buffer, 0, n);
		}
		inputStream.close();
		outputStream.close();
	}

	public void findPagesToFilter(List<File> output, File source) {
		if (!source.isDirectory()) {
			return;
		}
		File[] subFiles = source.listFiles();
		for (File curFile : subFiles) {
			if (curFile.isDirectory()) {
				findPagesToFilter(output, curFile);
				continue;
			}

			if (!curFile.isFile()) {
				continue;
			}

			String extension = getExtension(curFile.getName());
			if (extension == null) {
				continue;
			}

			if (extensionsToFilter.contains(extension)) {
				output.add(curFile);
				continue;
			}
		}
	}

	public static String getExtension(String filename) {
		int extensionIndex = filename.lastIndexOf(".");
		if (extensionIndex == -1) {
			return null;
		}
		String extension = filename.substring(extensionIndex + 1);
		return extension;
	}

	private void processJavascriptApp(File sourceDirectory, File outputDirectory) throws MojoExecutionException {
		File jsBaseDir = new File(sourceDirectory, "js");
		File jsApps = new File(jsBaseDir, "app");
		File jsLibs = new File(jsBaseDir, "lib");
		
		File jsOutputDir = new File(outputDirectory, "js");
		
		File appJs = new File(jsBaseDir, "app/app.js");
		String appJsContent = readFile(appJs);		
		
		File mainJs = new File(jsBaseDir, "app/main.js");
		String mainJsContent = readFile(mainJs);		
		
		List<File> jsFilesToFilter = new ArrayList<File>();
		findAssetsToFilter(jsFilesToFilter, jsApps);
		findAssetsToFilter(jsFilesToFilter, jsLibs);
		
		for (File jsFile : jsFilesToFilter) {
			String fingerprint = generateFingerprint(readBinaryFile(jsFile));
			String fileName = jsFile.getName();
			//getLog().info("JS app name: "+fileName);
			String fileNameKey = generateTargetFilename(jsBaseDir, jsFile).replaceAll(Pattern.quote("\\"), "/");
			//getLog().info("JS app file path: "+fileNameKey);
			
			File fingeredBasePath = new File(fileNameKey);
			String fbase = fingeredBasePath.getParent().replaceAll(Pattern.quote("\\"), "/");
			String fingeredPath = fbase + "/" + fingerprint + FINGERPRINT_SEPERATOR + fileName;
						
			mainJsContent = mainJsContent.replace(fileNameKey.replace(".js", "")+"'", fingeredPath.replace(".js", "")+"'");			
			
			if(fileNameKey.startsWith("/app")) {
				appJsContent = appJsContent.replaceFirst("'"+fileNameKey.substring(5).replace(".js", "")+"'", 
												"'"+fingeredPath.substring(5).replace(".js", "")+"'");
				
				mainJsContent = mainJsContent.replace("'"+fileNameKey.substring(5).replace(".js", "")+"'", 
													"'"+fingeredPath.substring(5).replace(".js", "")+"'");
			}
			
			getLog().debug("Original path: "+ fileNameKey +" Fingered path: "+fingeredPath);
			File targetFilename = new File(jsOutputDir, fingeredPath);
			File orgFile = new File(jsBaseDir, fileNameKey);
			targetFilename.getParentFile().mkdirs();
			try {
				copy(new FileInputStream(orgFile), new FileOutputStream(targetFilename), 2048);
			} catch (FileNotFoundException e) {
				throw new MojoExecutionException("Unable to copy file from: "+orgFile.getAbsolutePath()+" to: "+targetFilename.getAbsolutePath(), e);
			} catch (IOException e) {
				throw new MojoExecutionException("Unable to copy file from: "+orgFile.getAbsolutePath()+" to: "+targetFilename.getAbsolutePath(), e);
			}		
			
		}
		
		writeFingerprintedFile(jsOutputDir, mainJs, mainJsContent);
		writeFingerprintedFile(jsOutputDir, appJs, appJsContent);
		
		generateFingerprintForHtmlTemplates();
	}
	
	private void generateFingerprintForHtmlTemplates() throws MojoExecutionException {
		
		File templateDir = new File(sourceDirectory, "templates");
		File templateOutputDir = new File(outputDirectory, "templates");
		
		File templateJsFile = new File(sourceDirectory, "js/app/templates.js");
		String templateJsContent = readFile(templateJsFile);
		
		List<File> htmlFilesToFilter = new ArrayList<File>();
		findAssetsToFilter(htmlFilesToFilter, templateDir);
		
		for (File htmlFile : htmlFilesToFilter) {
			String fingerprint = generateFingerprint(readBinaryFile(htmlFile));
			String fileName = htmlFile.getName();
			
			
			String fileNameKey = generateTargetFilename(templateDir, htmlFile).replaceAll(Pattern.quote("\\"), "/");
			File fingeredBasePath = new File(fileNameKey);
			String fbase = fingeredBasePath.getParent().replaceAll(Pattern.quote("\\"), "/");
			if(!"/".equals(fbase)) {
				fbase = fbase + "/";
			}
			String fingeredPath = fbase + fingerprint + FINGERPRINT_SEPERATOR + fileName;
		
			templateJsContent = templateJsContent.replace("'/templates"+fileNameKey+"'", "'/templates"+fingeredPath+"'");
			
			getLog().debug("Original path: "+ fileNameKey +" Fingered path: "+fingeredPath);
			File targetFilename = new File(templateOutputDir, fingeredPath);
			File orgFile = new File(templateDir, fileNameKey);
			targetFilename.getParentFile().mkdirs();
			try {
				copy(new FileInputStream(orgFile), new FileOutputStream(targetFilename), 2048);
			} catch (FileNotFoundException e) {
				throw new MojoExecutionException("Unable to copy file from: "+orgFile.getAbsolutePath()+" to: "+targetFilename.getAbsolutePath(), e);
			} catch (IOException e) {
				throw new MojoExecutionException("Unable to copy file from: "+orgFile.getAbsolutePath()+" to: "+targetFilename.getAbsolutePath(), e);
			}					
		}
		
		File jsOutputDir = new File(outputDirectory, "js");
		writeFingerprintedFile(jsOutputDir, templateJsFile, templateJsContent);
	}
	
	private void writeFingerprintedFile(File jsOutputDir, File originalJsFile, String modifiedFileContent) throws MojoExecutionException {
		//getLog().info("Modified File content\n"+modifiedFileContent);
		String mainJsfingerprint = generateFingerprint(readBinaryFile(originalJsFile));
		File mainOutputDir = new File(jsOutputDir, "app/");
		mainOutputDir.mkdirs();
		File mainJsModified = new File(mainOutputDir, mainJsfingerprint+FINGERPRINT_SEPERATOR+originalJsFile.getName());
		try (FileWriter w = new FileWriter(mainJsModified)){
			w.append(modifiedFileContent);
			w.flush();							
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to write file to: "+originalJsFile.getAbsolutePath(), e);
		}		
	}
	
	public void findAssetsToFilter(List<File> output, File source) {
		if (!source.isDirectory()) {
			return;
		}
		File[] subFiles = source.listFiles();
		for (File curFile : subFiles) {
			if (curFile.isDirectory()) {
				findAssetsToFilter(output, curFile);
				continue;
			}

			if (!curFile.isFile()) {
				continue;
			}

			//getLog().info("Adding js "+curFile.getPath());
			output.add(curFile);
		}
	}	
	
}
