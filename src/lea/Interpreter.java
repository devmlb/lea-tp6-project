package lea;

import java.util.*;

import lea.Node.*;
import lea.Reporter.Phase;

public class Interpreter {

	private static class PanicException extends Exception {
		private static final long serialVersionUID = 1L;
		public PanicException(String message) {super(message);}
	}
	private static class BreakException extends Exception {
		private static final long serialVersionUID = 1L;
		private final Break node;
		public BreakException(Break node) {this.node=node;}
	}

	private final Reporter reporter;
	private final Map<Identifier, Value> variables = new HashMap<>();

	public Interpreter(Reporter reporter) {
		this.reporter=reporter;
	}

	public void interpret(Program program) {
		try {
			try {
				interpret(program.body());
			} catch (BreakException e) {
				error(e.node, "Interrompre ne peut pas être en dehors d'une boucle");
			}
		} catch (PanicException e) {}
	}

	private void interpret(Instruction instruction) throws BreakException, PanicException {
		switch(instruction) {
		case Sequence s		-> interpret(s);
		case Assignment a	-> interpret(a);
		case Write w		-> interpret(w);
		case If i			-> interpret(i);
		case While w		-> interpret(w);
		case For f 			-> interpret(f);
		case Break b 		-> throw new BreakException(b);
		case ErrorNode e	-> throw error(e, "Le programme contient une erreur de syntaxe");
		}
	}

	private Value eval(Expression expression) throws PanicException {
		return switch(expression) {
		case Value l		-> l;
		case Identifier id 	-> eval(id);
		case Sum s			-> eval(s);
		case Difference d	-> new Int(evalAsInt(d.left()) - evalAsInt(d.right()));
		case Product p		-> new Int(evalAsInt(p.left()) * evalAsInt(p.right()));
		case Lower l		-> new Bool(evalAsInt(l.left()) < evalAsInt(l.right()));
		case Equal e 		-> new Bool(eval(e.left()).equals(eval(e.right())));
		case And a			-> new Bool(evalAsBool(a.left()) && evalAsBool(a.right()));
		case Or o 			-> new Bool(evalAsBool(o.left()) || evalAsBool(o.right()));
		case Inverse i		-> new Int(-evalAsInt(i.argument()));
		case Length l		-> eval(l);
		case Index i		-> eval(i);
		case InitArray i	-> eval(i);
		case InitList i		-> eval(i);
		case ErrorNode e	-> throw error(e, "Le programme contient une erreur de syntaxe");
		};
	}
	
	private void interpret(Assignment assignment) throws PanicException {
		switch(assignment.lhs()) {
		case Identifier id	-> variables.put(id, eval(assignment.rhs()));
		case Index index	-> {
			Array array = evalAsArray(index.left());
			int i = evalAsInt(index.right());
			if(i <= 0 || i > array.values().length) {
				throw error(assignment.lhs(), "Indice hors limites");
			}
			array.values()[i-1] = eval(assignment.rhs());
		}
		default	-> throw error(assignment.lhs(), "Affectation invalide");
		}
	}

	private void interpret(Write w) throws BreakException, PanicException {
		for(var argument : w.values()) 
			System.out.print(evalAsStr(argument));
		System.out.println();
	}

	private void interpret(Sequence sequence) throws BreakException, PanicException {
		for(var commande : sequence.commands()) 
			interpret(commande);
	}

	private void interpret(If i) throws BreakException, PanicException {
		if(evalAsBool(i.cond())) {
			interpret(i.bodyT());
		} else if(i.bodyF().isPresent()) {
			interpret(i.bodyF().get());
		}
	}

	private void interpret(While w) throws BreakException, PanicException {
		try {
			while(evalAsBool(w.cond())) {
				interpret(w.body());
			}
		}catch(BreakException e) {}
	}

	private void interpret(For f) throws BreakException, PanicException {
		try {
			int start = evalAsInt(f.start());
			int end = evalAsInt(f.end());
			if(start < end) {
				int step = f.step().isPresent() ? evalAsInt(f.step().get()) : 1;
				if(step <= 0) throw error(f.step().get(), "Boucle pour infinie");
				for(int i = start; i < end; i+=step) {
					variables.put(f.id(), new Int(i));
					interpret(f.body());
				}
			} else {
				int step = f.step().isPresent() ? evalAsInt(f.step().get()) : -1;
				if(step >= 0) throw error(f.step().get(), "Boucle pour infinie");
				for(int i = start; i > end; i+=step) {
					variables.put(f.id(), new Int(i));
					interpret(f.body());
				}
			}
		}catch(BreakException e) {}
	}

	private Value eval(Identifier id) throws PanicException {
		Value v = variables.get(id);
		if (v != null) return v;
		throw error(id, "Utilisation d'une variable pas initialisée");
	}

	private Value eval(InitList list) throws PanicException {
		Value array[] = new Value[list.values().size()];
		for(int i = 0; i < array.length; i++) {
			array[i] = eval(list.values().get(i));
		}
		return new Array(new TArray(TypeChecker.type(array[0])), array);
	}
	
	private Value eval(InitArray initArray) throws PanicException {
		int length = evalAsInt(initArray.length());
		if(length < 0) throw error(initArray.length(), "Taille invalide");
		Value array[] = new Value[length];
		Value byDefault = eval(initArray.byDefault());
		for(int i = 0; i < length; i++) {
			array[i] = byDefault;
		}
		return new Array(new TArray(TypeChecker.type(array[0])), array);
	}

	private Value eval(Sum s) throws PanicException {
		Value left = eval(s.left());
		Value right = eval(s.right());
		if(left instanceof Int(int l) && right instanceof Int(int r))
			return new Int(l + r);
		return new Str(evalAsStr(left) + evalAsStr(right));
	}

	private Value eval(Length length) throws PanicException {
		Value argument = eval(length.argument());
		if(argument instanceof Str s)
			return new Int(s.value().length());
		if(argument instanceof Array array)
			return new Int(array.values().length);
		throw error(length.argument(), "Erreur de type (chaîne ou tableau)");
	}

	private Value eval(Index index) throws PanicException {
		Value left = eval(index.left());
		int i = evalAsInt(index.right());
		if(left instanceof Array array) {
			if(i <= 0 || i > array.values().length) {
				throw error(index.right(), "Indice hors limites");
			}
			return array.values()[i-1];
		}
		if(left instanceof Str s) {
			if(i <= 0 || i > s.value().length()) {
				throw error(index.right(), "iIndice hors limites");
			}
			return new Char(s.value().charAt(i-1));
		}		
		throw error(index.left(), "Type (chaîne ou tableau)");
	}

	private boolean evalAsBool(Expression expression) throws PanicException {
		return switch(eval(expression)) {
		case Bool b -> b.value();
		default -> throw error(expression, "Type (booléen)");
		};
	}

	private int evalAsInt(Expression expression) throws PanicException {
		return switch(eval(expression)) {
		case Int i -> i.value();
		default -> throw error(expression, "Type (entier)");
		};
	}

	private String evalAsStr(Expression expression) throws PanicException {
		return switch(eval(expression)) {
		case Bool b		-> Boolean.toString(b.value());
		case Int i		-> Integer.toString(i.value());
		case Char c		-> Character.toString(c.value());
		case Str i		-> i.value();
		case Array a	-> {
			String s = "[";
			for(int i = 0; i < a.values().length; i++) {
				if(i>0) s+=", ";
				s+= evalAsStr(a.values()[i]);
			}
			yield s + "]";
		}
		};
	}

	private Array evalAsArray(Expression expression) throws PanicException {
		return switch(eval(expression)) {
		case Array a -> a;
		default -> throw error(expression, "Type (tableau)");
		};
	}

	private PanicException error(Node n, String message) {
		reporter.error(Phase.RUNTIME, n, message);
		return new PanicException(message);
	}

}
