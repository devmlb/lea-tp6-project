package lea;

import java.util.*;

import lea.Reporter.Phase;
import lea.Node.*;

public final class Analyser {

	private final Reporter reporter;

	public Analyser(Reporter reporter) {
		this.reporter = reporter;
	}

	public void analyse(Program program) {
		analyse(program, new Context(program));
	}

	private Context analyse(Node node, Context context) {
		return switch(node) {
		case Program p			-> analyse(p.body(), context);

		case Sequence s			-> analyse(s, context);
		case Assignment a		-> analyse(a, context);
		case Write w			-> analyse(w.values(), context);
		case If i				-> analyse(i, context); 
		case While w			-> analyse(w, context); 
		case For f				-> analyse(f, context); 
		case Break b 			-> context.withAccessible(false);

		case Value v			-> context;
		case Identifier id		-> analyse(id, context);
		case Sum s				-> analyse(s.right(), analyse(s.left(), context));
		case Difference d		-> analyse(d.right(), analyse(d.left(), context));
		case Product p			-> analyse(p.right(), analyse(p.left(), context));
		case And a				-> analyse(a.right(), analyse(a.left(), context));
		case Or o				-> analyse(o.right(), analyse(o.left(), context));
		case Equal e			-> analyse(e.right(), analyse(e.left(), context));
		case Lower l			-> analyse(l.right(), analyse(l.left(), context));
		case Index i			-> analyse(i.right(), analyse(i.left(), context));
		case Inverse i			-> analyse(i.argument(), context);
		case Length l			-> analyse(l.argument(), context);
		case InitArray i		-> analyse(i.byDefault(), analyse(i.length(), context));
		case InitList i			-> analyse(i.values(), context);

		case Type t				-> context;
		case ErrorNode e		-> context;
		};
	}

	private Context analyse(Identifier id, Context context) {
		if (!context.declared.containsKey(id))	error(id, "Variable non déclarée", context);
		else if (!context.written.contains(id))	error(id, "Variable non initialisée", context);
		return context;
	}

	private Context analyse(Sequence sequence, Context context) {
		for(var commande : sequence.commands()) {
			if(!context.accessible) error(commande, "Code mort", context);
			context = analyse(commande, context);
		}
		return context;
	}

	private Context analyse(Assignment assignment, Context context) {
		Context cRhs = analyse(assignment.rhs(), context);
		return switch(assignment.lhs()) {
		case Identifier id	-> cRhs.withWritten(id);
		case Index index	-> analyse(index.right(), analyse(index.left(), cRhs));
		default				-> error(assignment.lhs(), "Affectation invalide", cRhs);
		};
	}

	private Context analyse(If i, Context context) {
		Context cCond  = analyse(i.cond(), context);
		Context cTrue  = analyse(i.bodyT(), cCond);
		if(i.bodyF().isEmpty())
			return cCond.merge(cTrue);
		Context cFalse = analyse(i.bodyF().get(), cCond);
		return cTrue.merge(cFalse);
	}

	private Context analyse(While w, Context context) {
		Context cCond = analyse(w.cond(), context);
		Context cBody = analyse(w.body(), cCond);
		return cCond.merge(cBody);
	}

	private Context analyse(For f, Context context) {
		context = analyse(f.start(), context);
		context = analyse(f.end(), context);
		if(f.step().isPresent()) context = analyse(f.step().get(), context);
		Context cBefore = context.withWritten(f.id());
		Context cBody = analyse(f.body(), cBefore);
		return cBefore.merge(cBody);
	}

	private <N extends Expression> Context analyse(List<N> nodes, Context context) {
		for(var node : nodes) 
			context = analyse(node, context);
		return context;
	}

	private Context error(Node n, String message, Context context) {
		reporter.error(Phase.STATIC, n, message);
		return context;
	}

	static final class Context {

		final Map<Identifier, Type> declared;
		final Set<Identifier> written;
		final Map<Identifier, Map<Identifier, Type>> enrgs;
		final boolean accessible;

		public Context(Program program) {
			declared = Map.copyOf(program.declared());
			written = Set.of();
			enrgs = Map.copyOf(program.enrs());
			accessible=true;
		}

		private Context(Map<Identifier, Type> declared, Set<Identifier> written, Map<Identifier, Map<Identifier, Type>> enrgs, boolean accessible) {
			this.declared = Map.copyOf(declared);
			this.written = Set.copyOf(written);
			this.enrgs = Map.copyOf(enrgs);
			this.accessible = accessible;
		}

		public Context withWritten(Identifier id) { 
			var writ = new HashSet<>(written); 
			writ.add(id);
			return new Context(declared, writ, enrgs, accessible); 
		}

		public Context withAccessible(boolean accessible) { 
			return new Context(declared, written, enrgs, accessible); 
		}

		public Context merge(Context other) {
			var writ = new HashSet<>(written);
			writ.retainAll(other.written);
			return new Context(declared, writ, enrgs, accessible || other.accessible);
		}

	}

}
