package cz.zalusky.profiler;

import java.lang.instrument.Instrumentation;

public class Agent {

	public static void premain(String args, Instrumentation instrumentation) {
		System.out.println("premain");
		Transformer transformer = new Transformer();
		instrumentation.addTransformer(transformer);
	}
}
