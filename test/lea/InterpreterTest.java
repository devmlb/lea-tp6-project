package lea;

import static org.junit.jupiter.api.Assertions.*;

import java.io.Reader;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import lea.Node.Program;
import lea.Reporter.Phase;

/**
 * JUnit tests for the Interpreter class.
 */
public final class InterpreterTest {

	private static Reporter analyse(String source) {
		Reporter reporter = new Reporter();
		try(Reader reader = new StringReader(source)) {
			var lexer = new Lexer(reader, reporter);
			var parser = new Parser(lexer,reporter);
			var analyser = new Analyser(reporter);
			var typeChecker = new TypeChecker(reporter);
			var interpreter = new Interpreter(reporter);
			Program program = parser.parseProgram();
			analyser.analyse(program);
			typeChecker.checkProgram(program);
			assertTrue(reporter.getErrors(Phase.LEXER).isEmpty(), "Lexing errors");
			assertTrue(reporter.getErrors(Phase.PARSER).isEmpty(), "Parsing errors");
			assertTrue(reporter.getErrors(Phase.STATIC).isEmpty(), "Static errors");
			assertTrue(reporter.getErrors(Phase.TYPE).isEmpty(), "Type errors");
			interpreter.interpret(program);
		} catch (Exception e) {
			fail(e);
		}
		return reporter;
	}

	private static void assertHasErrorContaining(String source, String fragment) {
		Reporter reporter = analyse(source);
		boolean matches = reporter.getErrors(Phase.RUNTIME)
				.stream()
				.anyMatch(m -> m.contains(fragment));
		assertTrue(matches,	() -> "Expected error containing: \"" + fragment + "\"");
	}

	private static void assertNoErrors(String source) {
		Reporter reporter = analyse(source);
		var runtimeErrors = reporter.getErrors(Phase.RUNTIME);
		assertTrue(runtimeErrors.isEmpty(), () -> runtimeErrors.stream().reduce("", (x,y)->x+y+"\n"));
	}
	

	/* =========================
	 * === NORMAL CASES ========
	 * ========================= */

	@Test
	void simpleAssignmentsAndExpressions() {
		String source = """
			algorithme
			variables
				x : entier;
				y : entier;
			début
				x <- 1;
				y <- x + 1;
				écrire("Résultat=", y * 3);
			fin
			""";
		assertNoErrors(source);
	}

	@Test
	void ifThenElseExecution() {
		String source = """
			algorithme
			variables
				x : entier;
			début
				x <- 0;
				si x = 0 alors
					x <- 1;
				sinon
					x <- 2;
				fin si
				écrire(x);
			fin
			""";
		assertNoErrors(source);
	}

	@Test
	void whileLoopExecution() {
		String source = """
			algorithme
			variables
				x : entier;
			début
				x <- 0;
				tant que x < 3 faire
					x <- x + 1;
				fin tant que
				écrire(x);
			fin
			""";
		assertNoErrors(source);
	}

	@Test
	void forLoopIncreasing() {
		String source = """
			algorithme
			variables
				i : entier;
				s : entier;
			début
				s <- 0;
				pour i de 1 à 5 faire
					s <- s + i;
				fin pour
				écrire(s);
			fin
			""";
		assertNoErrors(source);
	}

	@Test
	void forLoopDecreasing() {
		String source = """
			algorithme
			variables
				i : entier;
				s : entier;
			début
				s <- 0;
				pour i de 5 à 1 pas -1 faire
					s <- s + i;
				fin pour
				écrire(s);
			fin
			""";
		assertNoErrors(source);
	}

	@Test
	void arraysAndIndexing() {
		String source = """
			algorithme
			variables
				a : tableau de entier;
			début
				a <- [1, 2, 3];
				a[2] <- 5;
				écrire("arraysAndIndexing: ", a[2]);
			fin
			""";
		assertNoErrors(source);
	}

	@Test
	void stringsAndLength() {
		String source = """
			algorithme
			variables
				s : chaîne;
			début
				s <- "abc";
				écrire(longueur(s));
				écrire(s[2]);
			fin
			""";
		assertNoErrors(source);
	}

	/* =========================
	 * ==== RUNTIME ERRORS =====
	 * ========================= */
	
	@Test
	void breakOutsideLoop_isReported() {
		String source = """
			algorithme
			variables
			début
				interrompre;
			fin
			""";
		assertHasErrorContaining(source, "Interrompre ne peut pas être en dehors d'une boucle");
	}

	@Test
	void arrayIndexOutOfBounds_isReported() {
		String source = """
			algorithme
			variables
				a : tableau de entier;
			début
				a <- [1, 2];
				écrire(a[3]);
			fin
			""";
		assertHasErrorContaining(source, "Indice hors limites");
	}

	@Test
	void stringIndexOutOfBounds_isReported() {
		String source = """
			algorithme
			variables
				s : chaîne;
			début
				s <- "ab";
				écrire(s[3]);
			fin
			""";
		assertHasErrorContaining(source, "Indice hors limites");
	}
	
	@Test
	void invalidArraySize_isReported() {
		String source = """
			algorithme
			variables
				a : tableau de entier;
			début
				a <- tableau(-1, 0);
			fin
			""";
		assertHasErrorContaining(source, "Taille invalide");
	}

	@Test
	void infiniteForLoop_isReported() {
		String source = """
			algorithme
			variables
				i : entier;
			début
				pour i de 1 à 5 pas 0 faire
					écrire(i);
				fin pour
			fin
			""";
		assertHasErrorContaining(source, "Boucle pour infinie");
	}

	@Test
	void infiniteForLoop_decreasingStepIsNonNegative_isReported() {
		String source = """
			algorithme
			variables
				i : entier;
			début
				pour i de 5 à 1 pas 1 faire
					écrire(i);
				fin pour
			fin
			""";
		assertHasErrorContaining(source, "Boucle pour infinie");
	}
	
	@Test
	void infiniteForLoop_decreasingStepIsZero_isReported() {
		String source = """
			algorithme
			variables
				i : entier;
			début
				pour i de 5 à 1 pas 0 faire
					écrire(i);
				fin pour
			fin
			""";
		assertHasErrorContaining(source, "Boucle pour infinie");
	}
	
}
