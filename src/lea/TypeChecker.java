package lea;

import lea.Node.*;
import lea.Reporter.Phase;

public class TypeChecker {

	private static final Type Bool	= new TBool();
	private static final Type Int	= new TInt();
	private static final Type Char	= new TChar();
	private static final Type Str	= new TStr();

	private Program program;
	private final Reporter reporter;

	public TypeChecker(Reporter reporter) {
		this.reporter = reporter;
	}

	public void checkProgram(Program program) {
		this.program = program;
		check(program.body());
	}

	public void check(Instruction instruction)  {
		switch(instruction) {
		case Sequence s		-> {for(var commande : s.commands()) check(commande);}
		case Assignment a	-> checkCompatibility(a.rhs(), type(a.lhs()));
		case Write w		-> {for(var argument : w.values()) type(argument);}
		case If i			-> check(i);
		case While w		-> check(w);
		case For f			-> check(f);
		case Break b		-> {}
		case ErrorNode e	-> error(e, "Parser error");
		}
	}

	public Type type(Expression expression) {
		return switch(expression) {
		case Value v		-> type(v);
		case Equal e		-> type(e);
		case Identifier i	-> program.declared().get(i);
		case Sum s			-> type(s);
		case Difference d	-> checkBinaryOperator(d.left(), d.right(), Int, Int, Int);
		case Product p		-> checkBinaryOperator(p.left(), p.right(), Int, Int, Int);
		case Lower l		-> checkBinaryOperator(l.left(), l.right(), Int, Int, Bool);
		case And a			-> checkBinaryOperator(a.left(), a.right(), Bool, Bool, Bool);
		case Or o			-> checkBinaryOperator(o.left(), o.right(), Bool, Bool, Bool);
		case Inverse i		-> checkUnaryOperator(i.argument(), Int, Int);
		case Length l		-> type(l);
		case Index i		-> type(i);
		case InitArray i	-> type(i);
		case InitList i		-> type(i);
		case ErrorNode e	-> error(e, "Parser error");
		};
	}
	
	public static Type type(Value v) {
		return switch(v) {
		case Int i			-> Int;
		case Bool b			-> Bool;
		case Char c			-> Char;
		case Str s			-> Str;
		case Array s		-> s.type();
		};
	}


	private Type type(InitArray array) {
		checkCompatibility(array.length(), Int);
		Type type = type(array.byDefault());
		return new TArray(type);
	}

	private Type type(InitList list) {
		Type type = type(list.values().get(0));
		for(int i = 1; i<list.values().size(); i++) {
			checkCompatibility(list.values().get(i), type);
		}
		return new TArray(type);
	}

	private Type type(Index index) {
		checkCompatibility(index.right(), Int);
		Type lType = type(index.left());
		if(Str.equals(lType)) return Char;
		if(lType instanceof TArray arrayType) return arrayType.values();
		error(index, "Chaîne ou tableau attendu");
		return null;
	}

	private Type type(Length length) {
		Type type = type(length.argument());
		if(!Str.equals(type) && !(type instanceof TArray)) 
			error(length.argument(), "Chaîne ou tableau attendu");
		return Int;
	}

	private Type type(Equal equal) {
		type(equal.left());
		type(equal.right());
		return Bool;
	}

	private Type type(Sum sum) {
		Type lType = type(sum.left());
		Type rType = type(sum.right());
		if(Int.equals(lType) && Int.equals(rType)) return Int;
		return Str;
	}

	private Type checkUnaryOperator(Expression input, Type iType, Type oType) {
		checkCompatibility(input, iType);
		return oType;
	}

	private Type checkBinaryOperator(Expression left, Expression right, Type lType, Type rType, Type oType) {
		checkCompatibility(left, lType);
		checkCompatibility(right, rType);
		return oType;
	}

	private void check(If i) {
		checkCompatibility(i.cond(), Bool);
		check(i.bodyT());
		if(i.bodyF().isPresent()) check(i.bodyF().get());
	}

	private void check(While w) {
		checkCompatibility(w.cond(), Bool);
		check(w.body());
	}

	private void check(For f) {
		checkCompatibility(f.id(), Int);
		checkCompatibility(f.start(), Int);
		checkCompatibility(f.end(), Int);
		if(f.step().isPresent())
			checkCompatibility(f.step().get(), Int);
		check(f.body());
	}

	private void checkCompatibility(Expression expression, Type expectedType) {
		Type type = type(expression);
		if (type!=null && !type.equals(expectedType))
			error(expression, "Type incompatible avec " + expectedType);
	}

	private Type error(Node n, String message) {
		reporter.error(Phase.TYPE, n, message);
		return null;
	}

}
