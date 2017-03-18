package cz.zalusky.profiler;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class Config {

	private static final Pattern SIGNATURE_CONFIG = Pattern.compile("(.+)\\.([^\\.\\(]+)\\(([^\\)]*)\\)(.+)");
	                                                              // ^class ^method        ^args       ^returnType
	
	private Map<String,List<Map.Entry<String,String>>> config = new LinkedHashMap<>();
	
	private String initialClassName;

	private String initialMethodName;
	
	private String initialMethodSignature;
	
	public Config(String configFilePath) throws IOException {
		List<String> configLines = Files.readAllLines(new File(configFilePath).toPath());
		for (String configLine : configLines) {
			if (configLine.startsWith("#") || configLine.trim().length() == 0) {
				continue;
			}
			Matcher matcher = SIGNATURE_CONFIG.matcher(configLine);
			if (matcher.matches()) {
				String clazz = matcher.group(1);
				String jvmClass = clazz.replace('.','/');
				String method = matcher.group(2);
				String args = matcher.group(3);
				String jvmArgs = (args.isEmpty() ? Stream.<String>empty() : Arrays.stream(args.split(",")))
						.map(arg -> toJvmForm(arg))
						.collect(joining());
				String returnType = matcher.group(4);
				String jvmReturnType = toJvmForm(returnType);
				String jvmSignature = String.format("(%s)%s",jvmArgs,jvmReturnType);
				this.config.computeIfAbsent(jvmClass, key -> new ArrayList<>())
						.add(new AbstractMap.SimpleEntry<>(method, jvmSignature));
			} else {
				System.err.println("Unrecognized signature at line " + configLine);
			}
		}
		Map.Entry<String,List<Entry<String,String>>> initialEntry = config.entrySet().iterator().next();
		this.initialClassName       = initialEntry.getKey();
		this.initialMethodName      = initialEntry.getValue().get(0).getKey();
		this.initialMethodSignature = initialEntry.getValue().get(0).getValue();
	}

	private static String toJvmForm(String type) {
		type = type.trim();
		switch (type) {
			case "byte"    : return "B";
			case "char"    : return "C";
			case "double"  : return "D";
			case "float"   : return "F";
			case "int"     : return "I";
			case "long"    : return "J";
			case "short"   : return "S";
			case "boolean" : return "Z";
			case "void"    : return "V";
			default :
				if (type.endsWith("[]")) {
					return "[" + toJvmForm(type.substring(0, type.length() - 2));
				} else {
					return "L" + type.replace('.', '/') + ";";
				}
		}
	}

	public byte[] instrument(String className, ClassPool cp, byte[] classfileBuffer) {
		List<Map.Entry<String,String>> rules = config.get(className);
		if (rules == null) {
			return classfileBuffer;
		}
		System.err.println("instrumenting " + className + " really");
		String methodName = null;
		String methodSignature = null;
		try {
			CtClass ct = cp.makeClass(new ByteArrayInputStream(classfileBuffer));
			for (Map.Entry<String,String> rule : rules) {
				methodName = rule.getKey();
				methodSignature = rule.getValue();
				CtMethod method = ct.getMethod(methodName,methodSignature);
				boolean isInitial = Objects.equals(className,initialClassName) && Objects.equals(methodName,initialMethodName) && Objects.equals(methodSignature,initialMethodSignature);
				String reportedName = className.substring(className.lastIndexOf('/') + 1, className.length()) + "." + methodName;
				method.insertBefore("{" + (isInitial ? "Profiler.reset(); " : "") + "Profiler.start(\"" + reportedName + "\");}");
				method.insertAfter("{Profiler.stop();" + (isInitial ? " Profiler.print();" : "") + "}",true);
			}
			return ct.toBytecode();
		} catch (Throwable e) {
			System.err.printf("Error instrumenting method %s with signature %s%n", methodName, methodSignature);
			e.printStackTrace(System.err);
		}
		return classfileBuffer;
	}

}
