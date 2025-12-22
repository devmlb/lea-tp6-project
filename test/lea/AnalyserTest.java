package lea;

import static org.junit.jupiter.api.Assertions.*;
import java.io.Reader;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import lea.Node.Program;
import lea.Reporter.Phase;

/**
 * JUnit tests for the Analyser class.
 */
public final class AnalyserTest {

	private static Reporter analyse(String source) {
		var reporter = new Reporter();
		try(Reader reader = new StringReader(source)) {
			var lexer = new Lexer(reader, reporter);
			var parser = new Parser(lexer,reporter);
			var analyser = new Analyser(reporter);
			Program program = parser.parseProgram();
			assertTrue(reporter.getErrors(Phase.LEXER).isEmpty(), "Lexing errors");
			assertTrue(reporter.getErrors(Phase.PARSER).isEmpty(), "Parsing errors");
			analyser.analyse(program);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		return reporter;
	}

	private static void assertHasErrorContaining(String source, String fragment) {
		Reporter reporter = analyse(source);
		boolean matches = reporter.getErrors(Phase.STATIC)
				.stream()
				.anyMatch(m -> m.contains(fragment));
		assertTrue(matches,	() -> "Expected error containing: \"" + fragment);
	}

	private static void assertNoErrors(String source) {
		Reporter reporter = analyse(source);
		var runtimeErrors = reporter.getErrors(Phase.STATIC);
		assertTrue(runtimeErrors.isEmpty(), () -> runtimeErrors.stream().reduce("", (x,y)->x+y+"\n"));
	}

	/* =========================
	 * === INITIALISATION ======
	 * ========================= */

	@Test
	void initializedVariable_isOk() {
		String source = """
			algorithme
			variables
				x : entier;
			début
				x <- 0;
				écrire(x);
			fin
			""";
		assertNoErrors(source);
	}

	@Test
	void useBeforeInitialization_isReported_straightline() {
		String source = """
			algorithme
			variables
				x : entier;
			début
				x <- x + 1;
			fin
			""";
		assertHasErrorContaining(source, "Variable non initialisée");
	}

	/* =========================
	 * === IF / ELSE ===========
	 * ========================= */

	@Test
	void ifWithoutElse_thenAssign_doesNotGuaranteeInitialization() {
		String source = """
			algorithme
			variables
				x : entier;
			début
				si vrai alors
					x <- 0;
				fin si
				écrire(x);
			fin
			""";
		assertHasErrorContaining(source, "Variable non initialisée");
	}

	@Test
	void ifElse_bothBranchesAssign_thenOk() {
		String source = """
			algorithme
			variables
				x : entier;
			début
				si vrai alors
					x <- 0;
				sinon
					x <- 1;
				fin si
				écrire(x);
			fin
			""";
		assertNoErrors(source);
	}

	@Test
	void ifElse_assignDifferentVars_thenNeitherIsCertainlyInitialized() {
		String source = """
			algorithme
			variables
				x : entier;
				y : entier;
			début
				si vrai alors
					x <- 0;
				sinon
					y <- 1;
				fin si
				écrire(x);
				écrire(y);
			fin
			""";
		assertHasErrorContaining(source, "Variable non initialisée");
	}

	/* =========================
	 * === WHILE ===============
	 * ========================= */

	@Test
	void whileMayNotIterate_assignmentInsideDoesNotGuaranteeInitialization() {
		String source = """
			algorithme
			variables
				x : entier;
			début
				tant que faux faire
					x <- 1;
				fin tant que
				écrire(x);
			fin
			""";
		assertHasErrorContaining(source, "Variable non initialisée");
	}

	@Test
	void while_assignmentAfterInitialization_isOk() {
		String source = """
			algorithme
			variables
				x : entier;
			début
				x <- 0;
				tant que faux faire
					x <- x + 1;
				fin tant que
				écrire(x);
			fin
			""";
		assertNoErrors(source);
	}

	/* =========================
	 * === FOR =================
	 * ========================= */

	@Test
	void forLoop_variableIsConsideredInitializedInBody() {
		String source = """
			algorithme
			variables
				i : entier;
			début
				pour i de 1 à 3 faire
					écrire(i);
				fin pour
			fin
			""";
		assertNoErrors(source);
	}

	@Test
	void forLoop_usesUndeclaredIterator_isReported() {
		String source = """
			algorithme
			variables
			début
				pour i de 1 à 3 faire
					écrire(i);
				fin pour
			fin
			""";
		assertHasErrorContaining(source, "Variable non déclarée");
	}

	/* =========================
	 * === CODE MORT ===========
	 * ========================= */

	@Test
	void deadCode_afterBreakInSequence_isReported() {
		String source = """
			algorithme
			variables
				x : entier;
			début
				x <- 0;
				interrompre;
				écrire(x);
			fin
			""";
		assertHasErrorContaining(source, "Code mort");
	}

	@Test
	void deadCode_afterBreakInIfBothBranches_isReported() {
		String source = """
			algorithme
			variables
				x : entier;
			début
				x <- 0;
				si vrai alors
					interrompre;
				sinon
					interrompre;
				fin si
				écrire(x);
			fin
			""";
		assertHasErrorContaining(source, "Code mort");
	}

	@Test
	void noDeadCode_ifBreakOnlyInOneBranch() {
		String source = """
			algorithme
			variables
				x : entier;
			début
				x <- 0;
				si vrai alors
					interrompre;
				sinon
					écrire(x);
				fin si
				écrire(x);
			fin
			""";
		assertNoErrors(source);
	}
	
}
