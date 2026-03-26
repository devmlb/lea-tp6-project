package lea;

import lea.Node.ErrorNode;

public class TupleWithError<T1, T2> {
	T1 a;
	T2 b;
	ErrorNode e;

	TupleWithError(T1 a, T2 b, ErrorNode e) {
		this.a = a;
		this.b = b;
		this.e = e;
	}

	T1 getA() {
		return this.a;
	}

	T2 getB() {
		return this.b;
	}

	ErrorNode getError() {
		return this.e;
	}
}