package cz.zalusky.profiler;

import java.io.IOException;
import java.lang.instrument.Instrumentation;

public class Agent {

	public static void premain(String args, Instrumentation instrumentation) throws IOException {
		System.out.println("premain [" + args + "]");
		Config config = new Config(args);
		Transformer transformer = new Transformer(config);
		instrumentation.addTransformer(transformer);
	}
}
