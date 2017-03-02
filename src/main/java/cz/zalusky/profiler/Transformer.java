package cz.zalusky.profiler;

import javassist.ClassPool;
import javassist.LoaderClassPath;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class Transformer implements ClassFileTransformer {
	
	private Config config;

	public Transformer(Config config) {
		this.config = config;
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		if (className.startsWith("cz/zalusky/profiler")) { // we do not want to profile ourselves
			return classfileBuffer;
		}
		ClassPool cp = ClassPool.getDefault();
		cp.insertClassPath(new LoaderClassPath(loader));
		cp.importPackage("cz.zalusky.profiler");
		byte[] result = config.instrument(className,cp,classfileBuffer);
		return result;
	}

}
