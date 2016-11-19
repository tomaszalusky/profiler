package cz.zalusky.profiler;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class Transformer implements ClassFileTransformer {
	
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		System.out.println("instrumenting " + className);
		ClassPool cp = ClassPool.getDefault();
		cp.importPackage("cz.zalusky.profiler");
		if (className.startsWith("cz/zalusky/profiler")) { // we do not want to profile ourselves
			return classfileBuffer;
		}
		if (!className.startsWith("sendmail")) { // filter rules
			return classfileBuffer;
		}
		System.out.println("really");
		try {
			CtClass ct = cp.makeClass(new ByteArrayInputStream(classfileBuffer));
			CtMethod[] declaredMethods = ct.getDeclaredMethods();
			for (CtMethod method : declaredMethods) {
				method.insertBefore(" { " + "Stack.push();" + "Stack.log(\"" + className + "." + method.getName() + "\"); " + "}");
				method.insertAfter("{ Stack.pop(); }", true);
			}
			return ct.toBytecode();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return classfileBuffer;
	}
}
