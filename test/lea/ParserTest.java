package lea;

import static org.junit.jupiter.api.Assertions.*;

import java.io.Reader;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import lea.Reporter.Phase;

/**
 * JUnit tests for the Parser class.
 */
public final class ParserTest {

	private static Reporter analyse(String source) {
		Reporter reporter = new Reporter();
		try(Reader reader = new StringReader(source)) {
			var lexer = new Lexer(reader, reporter);
			var parser = new Parser(lexer,reporter);
			parser.parseProgram();
			assertTrue(reporter.getErrors(Phase.LEXER).isEmpty(), "Lexing errors");
		} catch (Exception e) {
			fail(e.getMessage());
		}
		return reporter;
	}

	private static void assertHasErrorContaining(String source, String fragment) {
		Reporter reporter = analyse(source);
		boolean matches = reporter.getErrors(Phase.PARSER)
				.stream()
				.anyMatch(m -> m.contains(fragment));
		assertTrue(matches,	() -> "Expected error containing: \"" + fragment);
	}

	private static void assertNoErrors(String source) {
		Reporter reporter = analyse(source);
		var runtimeErrors = reporter.getErrors(Phase.PARSER);
		assertTrue(runtimeErrors.isEmpty(), () -> runtimeErrors.stream().reduce("", (x,y)->x+y+"\n"));
	}

	/* =========================
	 * === PROGRAMMES VALIDES ==
	 * ========================= */

	@Test
	void minimalProgram_emptyBody() {
		String source = """
			algorithme
			début
			fin
			""";
		assertNoErrors(source);
	}

	@Test
	void declarations_types_and_arrayType() {
		String source = """
			algorithme
			variables
				x : entier;
				s : chaîne;
				a : tableau de entier;
			début
				écrire(x, s, a);
			fin
			""";
		assertNoErrors(source);
	}

	@Test
	void commands_and_expressions_smoke() {
		String source = """
			algorithme
			variables
				x : entier;
				a : tableau de entier;
			début
				x <- 1 + 2 * 3;
				a <- [1, 2, 3];
				a[2] <- -x;
				écrire(longueur("abc"), ":", "ab"[2]);
				écrire(tableau(3, 0));
				écrire(x = 7 ou faux et vrai);
			fin
			""";
		assertNoErrors(source);
	}

	@Test
	void structuredStatements_if_while_for() {
		String source = """
			algorithme
			variables
				x : entier;
				i : entier;
			début
				si x = 0 alors
					écrire("zero");
				sinon
					écrire("nonzero");
				fin si

				tant que x < 3 faire
					x <- x + 1;
				fin tant que

				pour i de 1 à 3 faire
					écrire(i);
				fin pour

				pour i de 5 à 1 pas -1 faire
					écrire(i);
				fin pour
			fin
			""";
		assertNoErrors(source);
	}

	/* =========================
	 * === ERREURS STRUCTURELLES
	 * ========================= */

	@Test
	void programStructureError_isReported() {
		String source = """
			algorithme
			variables
				x : entier;
			fin
			""";
		assertHasErrorContaining(source, "Erreur dans le programme");
	}

	/* =========================
	 * === catch_expr : MANQUANT
	 * ========================= */

	@Test
	void missingExpression_inIfCondition_isReported() {
		String source = """
			algorithme
			variables
			début
				si alors
					écrire(1);
				fin si
			fin
			""";
		assertHasErrorContaining(source, "Expression manquante");
	}

	@Test
	void missingExpression_inWhileCondition_isReported() {
		String source = """
			algorithme
			variables
			début
				tant que faire
					écrire(1);
				fin tant que
			fin
			""";
		assertHasErrorContaining(source, "Expression manquante");
	}

	@Test
	void missingExpression_inForStart_isReported() {
		String source = """
			algorithme
			variables
				i : entier;
			début
				pour i de à 5 faire
					écrire(i);
				fin pour
			fin
			""";
		assertHasErrorContaining(source, "Expression manquante");
	}

	@Test
	void missingExpression_inForEnd_isReported() {
		// "à faire" : end manquant
		String source = """
			algorithme
			variables
				i : entier;
			début
				pour i de 1 à faire
					écrire(i);
				fin pour
			fin
			""";
		assertHasErrorContaining(source, "Expression manquante");
	}

	@Test
	void missingExpression_inForStep_isReported() {
		String source = """
			algorithme
			variables
				i : entier;
			début
				pour i de 1 à 5 pas faire
					écrire(i);
				fin pour
			fin
			""";
		assertHasErrorContaining(source, "Expression manquante");
	}

	/* =========================
	 * === catch_expr : CASSEE
	 * ========================= */

	@Test
	void invalidExpression_inIfCondition_isReported() {
		String source = """
			algorithme
			variables
				x : entier;
			début
				si x < alors
					écrire(1);
				fin si
			fin
			""";
		assertHasErrorContaining(source, "Erreur dans l'expression");
	}

	@Test
	void invalidExpression_inForEnd_isReported() {
		String source = """
			algorithme
			variables
				i : entier;
			début
				pour i de 1 à 1 + faire
					écrire(i);
				fin pour
			fin
			""";
		assertHasErrorContaining(source, "Erreur dans l'expression");
	}

	/* =========================
	 * === RECOVERY : CONTINUER
	 * ========================= */

	@Test
	void recovery_afterBadCommand_continuesUntilEnd() {
		String source = """
			algorithme
			variables
				x : entier;
			début
				écrire(1;
				x <- 2;
				écrire(x);
			fin
			""";
		assertHasErrorContaining(source, "Erreur dans la commande");
	}
	
}
