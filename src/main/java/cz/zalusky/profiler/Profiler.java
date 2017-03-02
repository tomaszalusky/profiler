package cz.zalusky.profiler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongFunction;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

/**
 * @author Tomas Zalusky
 */
public class Profiler {

    static {
        reset();
    }
    
    private static class Node {
        private final String name;
        private final Node parent;
        private Map<String,Node> children = new LinkedHashMap<>();
        private List<Long> elapsedParticularTimes = new ArrayList<>();
        private long elapsed = 0;
        private long started;
        private int count = 0;
        Node(String name, Node parent) {
            this.name = name;
            this.parent = parent;
        }
        private void print(int indentation) {
            if (this != root) {
                long elapsedInParentContext = parent == root ? elapsed : parent.elapsed;
				System.out.printf("%s- %s %s %sms #=%s %s%% (avg %s %s%%)%n",
                		Stream.generate(() -> "    ").limit(indentation).collect(joining()),
                		name,
                        formatTime(elapsed),
                        "[...]", //elapsedParticularTimes.stream().map(t -> t/1000000).collect(toList()),
                        count,
                        safeDiv(elapsed * 100, elapsedInParentContext),
                        safeDiv(elapsed, count, this::formatTime),
                        safeDiv(elapsed * 100, elapsedInParentContext * count)
                );
            }
            children.values().forEach(n -> n.print(indentation + 1));
        }
        private String formatTime(long timeInNanos) {
            double timeInSec = timeInNanos / 1000000000d;
            return String.format("$=%.3fs",timeInSec);
        }
        public String safeDiv(long dividend, long divisor) {
        	return safeDiv(dividend, divisor, String::valueOf);
        }
        public String safeDiv(long dividend, long divisor, LongFunction<String> formatter) {
        	String result = divisor == 0 ? "/byzero" : formatter.apply(dividend / divisor);
			return result;
    	}
    }
    
    private static Node root;
    
    private static Node last;
    
    public static void reset() {
        root = new Node("",null);
        last = root;
    }

	public static void start(String name) {
        Node node = last.children.computeIfAbsent(name, n -> new Node(n,last));
        last = node;
        last.count++;
        last.started = System.nanoTime();
    }
    
    public static void stop() {
        long elapsedNanos = System.nanoTime() - last.started;
        last.elapsedParticularTimes.add(elapsedNanos);
        last.elapsed += elapsedNanos;
        last = last.parent;
    }
    
    public static void print() {
        root.print(-1);
    }
    
    public static void main(String[] args) {
        Profiler.start("a");
        Profiler.start("b");
        Profiler.stop();
        Profiler.start("b");
        Profiler.stop();
        Profiler.start("b");
        Profiler.stop();
        Profiler.start("b");
        Profiler.stop();
        Profiler.start("c");
        Profiler.stop();
        Profiler.stop();
        Profiler.print();
    }

}
