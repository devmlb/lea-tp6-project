package lea;

import static org.junit.jupiter.api.Assertions.*;

import java.io.Reader;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import lea.Node.Program;
import lea.Reporter.Phase;

/**
 * JUnit tests for the TypeChecker class.
 */
public final class TypeCheckerTest {

	private static Reporter analyse(String source) {
		Reporter reporter = new Reporter();
		try (Reader reader = new StringReader(source)) {
			var lexer = new Lexer(reader, reporter);
			var parser = new Parser(lexer, reporter);
			var analyser = new Analyser(reporter);
			var typeChecker = new TypeChecker(reporter);

			Program program = parser.parseProgram();
			analyser.analyse(program);

			assertTrue(reporter.getErrors(Phase.LEXER).isEmpty(), "Lexing errors");
			assertTrue(reporter.getErrors(Phase.PARSER).isEmpty(), "Parsing errors");
			assertTrue(reporter.getErrors(Phase.STATIC).isEmpty(), "Static errors");

			typeChecker.checkProgram(program);
		} catch (Exception e) {
			fail(e);
		}
		return reporter;
	}

	private static void assertHasErrorContaining(String source, String fragment) {
		Reporter reporter = analyse(source);
		boolean matches = reporter.getErrors(Phase.TYPE)
				.stream()
				.anyMatch(m -> m.contains(fragment));
		assertTrue(matches, () -> "Expected error containing: \"" + fragment + "\"");
	}

	private static void assertNoErrors(String source) {
		Reporter reporter = analyse(source);
		var errors = reporter.getErrors(Phase.TYPE);
		assertTrue(errors.isEmpty(), () -> errors.stream().reduce("", (x, y) -> x + y + "\n"));
	}

	/* =========================
	 * === PROGRAMMES VALIDES ==
	 * ========================= */

	@Test
	void ok_basic_arith_and_write() {
		String source = """
			algorithme
			variables
				x : entier;
				y : entier;
			début
				x <- 1;
				y <- x * 3 - 2;
				écrire("y=", y);
			fin
			""";
		assertNoErrors(source);
	}

	@Test
	void ok_plus_is_coercive() {
		// Selon ta sémantique : si pas int+int, alors string (concat)
		String source = """
			algorithme
			variables
				s : chaîne;
			début
				s <- 1 + vrai;
				écrire(s);
			fin
			""";
		assertNoErrors(source);
	}

	@Test
	void ok_equal_is_total() {
		// Pas d'erreur de type attendue : mismatch -> false au runtime, mais typage OK
		String source = """
			algorithme
			variables
				b : booléen;
			début
				b <- (1 = vrai);
				écrire(b);
			fin
			""";
		assertNoErrors(source);
	}

	@Test
	void ok_string_length_and_index() {
		String source = """
			algorithme
			variables
				n : entier;
				c : caractère;
			début
				n <- longueur("abc");
				c <- "abc"[2];
				écrire(n, c);
			fin
			""";
		assertNoErrors(source);
	}

	@Test
	void ok_arrays_init_and_index() {
		String source = """
			algorithme
			variables
				a : tableau de entier;
				x : entier;
			début
				a <- [1, 2, 3];
				x <- a[1] + a[3];
				écrire(x);
			fin
			""";
		assertNoErrors(source);
	}

	@Test
	void ok_tableau_constructor() {
		String source = """
			algorithme
			variables
				a : tableau de booléen;
				b : booléen;
			début
				a <- tableau(3, vrai);
				b <- a[2] et faux;
				écrire(b);
			fin
			""";
		assertNoErrors(source);
	}

	@Test
	void ok_if_while_for_types() {
		String source = """
			algorithme
			variables
				i : entier;
				x : entier;
			début
				x <- 0;
				si x < 1 alors
					x <- 1;
				fin si

				tant que x < 3 faire
					x <- x + 1;
				fin tant que

				pour i de 1 à 3 pas 1 faire
					écrire(i);
				fin pour
			fin
			""";
		assertNoErrors(source);
	}

	/* =========================
	 * === ERREURS : CONDITIONS
	 * ========================= */

	@Test
	void error_if_condition_must_be_bool() {
		String source = """
			algorithme
			variables
				x : entier;
			début
				x <- 1;
				si x alors
					écrire(1);
				fin si
			fin
			""";
		assertHasErrorContaining(source, "Type incompatible");
	}

	@Test
	void error_while_condition_must_be_bool() {
		String source = """
			algorithme
			début
				tant que 1 faire
					écrire(0);
				fin tant que
			fin
			""";
		assertHasErrorContaining(source, "Type incompatible");
	}

	/* =========================
	 * === ERREURS : OPERATEURS
	 * ========================= */

	@Test
	void error_difference_requires_int() {
		String source = """
			algorithme
			variables
				x : entier;
			début
				x <- vrai - 1;
			fin
			""";
		assertHasErrorContaining(source, "Type incompatible");
	}

	@Test
	void error_product_requires_int() {
		String source = """
			algorithme
			variables
				x : entier;
			début
				x <- "a" * 3;
			fin
			""";
		assertHasErrorContaining(source, "Type incompatible");
	}

	@Test
	void error_lower_requires_ints() {
		String source = """
			algorithme
			variables
				b : booléen;
			début
				b <- "a" < "b";
			fin
			""";
		assertHasErrorContaining(source, "Type incompatible");
	}

	@Test
	void error_and_requires_bool() {
		String source = """
			algorithme
			variables
				b : booléen;
			début
				b <- vrai et 1;
			fin
			""";
		assertHasErrorContaining(source, "Type incompatible");
	}

	@Test
	void error_or_requires_bool() {
		String source = """
			algorithme
			variables
				b : booléen;
			début
				b <- 0 ou faux;
			fin
			""";
		assertHasErrorContaining(source, "Type incompatible");
	}

	@Test
	void error_unary_minus_requires_int() {
		String source = """
			algorithme
			variables
				x : entier;
			début
				x <- -"abc";
			fin
			""";
		assertHasErrorContaining(source, "Type incompatible");
	}

	/* =========================
	 * === ERREURS : LONGUEUR / INDEX
	 * ========================= */

	@Test
	void error_length_requires_string_or_array() {
		String source = """
			algorithme
			variables
				n : entier;
			début
				n <- longueur(1);
			fin
			""";
		assertHasErrorContaining(source, "Chaîne ou tableau attendu");
	}

	@Test
	void error_index_requires_string_or_array() {
		String source = """
			algorithme
			variables
				x : entier;
			début
				x <- 1[1];
			fin
			""";
		assertHasErrorContaining(source, "Chaîne ou tableau attendu");
	}

	@Test
	void error_index_position_requires_int() {
		String source = """
			algorithme
			variables
				c : caractère;
			début
				c <- "abc"[vrai];
			fin
			""";
		assertHasErrorContaining(source, "Type incompatible");
	}

	/* =========================
	 * === ERREURS : TABLEAUX
	 * ========================= */

	@Test
	void error_tableau_length_requires_int() {
		String source = """
			algorithme
			variables
				a : tableau de entier;
			début
				a <- tableau(vrai, 0);
			fin
			""";
		assertHasErrorContaining(source, "Type incompatible");
	}

	@Test
	void error_list_must_be_homogeneous() {
		String source = """
			algorithme
			variables
				a : tableau de entier;
			début
				a <- [1, vrai, 3];
			fin
			""";
		assertHasErrorContaining(source, "Type incompatible");
	}

	@Test
	void error_assign_incompatible_rhs() {
		String source = """
			algorithme
			variables
				x : entier;
			début
				x <- vrai;
			fin
			""";
		assertHasErrorContaining(source, "Type incompatible");
	}

	@Test
	void error_array_index_result_type_must_match_assignment() {
		String source = """
			algorithme
			variables
				a : tableau de booléen;
				x : entier;
			début
				a <- tableau(3, vrai);
				x <- a[1];
			fin
			""";
		assertHasErrorContaining(source, "Type incompatible");
	}

	/* =========================
	 * === ERREURS : POUR
	 * ========================= */

	@Test
	void error_for_bounds_must_be_int() {
		String source = """
			algorithme
			variables
				i : entier;
			début
				pour i de vrai à 3 faire
					écrire(i);
				fin pour
			fin
			""";
		assertHasErrorContaining(source, "Type incompatible");
	}

	@Test
	void error_for_step_must_be_int() {
		String source = """
			algorithme
			variables
				i : entier;
			début
				pour i de 1 à 3 pas faux faire
					écrire(i);
				fin pour
			fin
			""";
		assertHasErrorContaining(source, "Type incompatible");
	}

	@Test
	void error_for_id_must_be_int() {
		String source = """
			algorithme
			variables
				i : booléen;
			début
				pour i de 1 à 3 faire
					écrire(i);
				fin pour
			fin
			""";
		assertHasErrorContaining(source, "Type incompatible");
	}
}
