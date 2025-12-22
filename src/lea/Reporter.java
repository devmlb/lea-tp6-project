package lea;

import java.util.*;

public final class Reporter {

	public enum Phase { LEXER, PARSER, STATIC, TYPE, RUNTIME }

	public record Diag(Phase phase, Span span, String message) {}

	private final List<Diag> diags = new ArrayList<>();
	private final IdentityHashMap<Node, Span> spans = new IdentityHashMap<>();

	public void error(Phase phase, Object position, String message) {
		diags.add(new Diag(phase, span(position), message));
	}

	public boolean reportErrors(Phase phase) {
		String errs = "";
		for (var d : diags) {
			if(d.phase() == phase) {
				String pos = "";
				if(!d.span().isEmpty()) {
					pos = ("[" + d.span().startLine() + ":" + d.span().startColumn()
							+ "->" + d.span().endLine() + ":" + d.span().endColumn()
							+ "]\t");
				}
				errs += "\t" + pos + d.message() + "\n";
			}
		}
		if(errs != "") {
			System.out.println("La phase " + phase + " s'est terminée avec des erreurs");
			System.out.println(errs);
			return true;
		} else {
			System.out.println("La phase " + phase + " s'est terminée avec succès");
			return false;
		}
	}

	public List<String> getErrors(Phase phase) {
		return diags.stream().filter(d -> d.phase() == phase).map(d->d.message).toList();
	}


//	public boolean hasErrors(Phase phase) {
//		return diags.stream().anyMatch(d -> d.phase() == phase);
//	}

	//	public List<Diag> all(Phase phase) {
	//		return diags.stream().filter(d -> d.phase() == phase).toList();
	//	}

//	public void printAll() {
//		for (var d : diags) {
//			String pos = "";
//			if(!d.span().isEmpty()) {
//				pos = ("(from l." + d.span().startLine() + ", c." + d.span().startColumn()
//						+ " to l." + d.span().endLine() + ", c." + d.span().endColumn()
//						+ ") ");
//			}
//			System.out.println(d.phase() + " " + pos + d.message());
//		}
//	}

	public static record Span(int startLine, int startColumn, int endLine, int endColumn) {
		public Span(int startLine, int startColumn, int length) { this(startLine, startColumn, startLine, startColumn + length - 1);}
		private Span() { this(-1,-1,-1,-1);}
		public Span union(Span other) {
			if (isEmpty()) return other;
			if (other.isEmpty()) return this;
			int sl = startLine, sc = startColumn, el=endLine, ec = endColumn;
			if(other.startLine < startLine || (other.startLine == startLine && other.startColumn < startColumn)) {
				sl = other.startLine; sc = other.startColumn;
			}
			if(endLine < other.endLine || (endLine == other.endLine && endColumn < other.endColumn)) {
				el = other.endLine; ec = other.endColumn;
			}
			return new Span (sl, sc, el, ec);
		}
		public final static Span EMPTY = new Span();
		public boolean isEmpty() { return startLine < 0; }
	}

	public Node attach(Node n, Object anchorStart, Object anchorEnd) {
		return attach(n, span(anchorStart).union(span(anchorEnd)));
	}

	public Node attach(Node n, Span span) {
		spans.put(n, span);
		return n;
	}

	public Span span(Object o) {
		return switch(o) {
		case null -> Span.EMPTY;
		case Span s -> s;
		case Node n -> spans.getOrDefault(n, Span.EMPTY);
		default -> Span.EMPTY;
		};
	}

}
