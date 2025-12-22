package lea;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public sealed interface Node {

	public record Program(Map<Identifier, Type> declared, Instruction body)	implements	Node {}

	public sealed interface Instruction 									extends 	Node {}
	public record Sequence(List<Instruction> commands)						implements	Instruction {}
	public record Assignment(Expression lhs, Expression rhs)				implements	Instruction {}
	public record Write(List<Expression> values)							implements	Instruction {}
	public record If(Expression cond, 
			Instruction bodyT, Optional<Instruction> bodyF)					implements	Instruction {}
	public record While(Expression cond, Instruction body)					implements	Instruction {}
	public record Break()													implements	Instruction {}
	public record For(Identifier id, Expression start, Expression end,
			Optional<Expression> step, Instruction body)					implements	Instruction {}

	public sealed interface Expression										extends 	Node {}
	public record Identifier(String text)									implements	Expression {}
	public record Sum(Expression left, Expression right)					implements	Expression {}
	public record Difference(Expression left, Expression right)				implements	Expression {}
	public record Product(Expression left, Expression right)				implements	Expression {}
	public record And(Expression left, Expression right)					implements	Expression {}
	public record Or(Expression left, Expression right)						implements	Expression {}
	public record Equal(Expression left, Expression right)					implements	Expression {}
	public record Lower(Expression left, Expression right)					implements	Expression {}
	public record Index(Expression left, Expression right)					implements	Expression {}
	public record Inverse(Expression argument)								implements	Expression {}
	public record Length(Expression argument)								implements	Expression {}
	public record InitArray(Expression length, Expression byDefault)		implements	Expression {}
	public record InitList(List<Expression> values)							implements	Expression {}

	public sealed interface Value											extends		Expression {}
	public record Bool(boolean value)										implements	Value {}
	public record Int(int value)											implements	Value {}
	public record Char(char value)											implements	Value {}
	public record Str(String value)											implements	Value {}
	public record Array(TArray type, Value[] values)						implements	Value {}

	public sealed interface Type 											extends 	Node {}
	public record TBool()													implements	Type {}
	public record TInt()													implements	Type {}
	public record TChar()													implements	Type {}
	public record TStr()													implements	Type {}
	public record TArray(Type values)										implements	Type {}

	public record ErrorNode()												implements	Instruction, Expression{}

}
