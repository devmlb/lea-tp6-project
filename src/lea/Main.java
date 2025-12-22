package lea;

import java.io.*;
import java.nio.charset.StandardCharsets;
import lea.Node.*;
import lea.Reporter.Phase;

public class Main {

	public static final String defaultProgram = """
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

	public static void main(String[] args) throws Exception {
		if(args.length >= 1) {
			executeFromFile(args[0]);
		} else {
			executeFromString(defaultProgram);
		}
	}

	public static void executeFromFile(String fileName) {
		try(Reader reader = new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8)) {
			execute(reader);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void executeFromString(String program) {
		try(Reader reader = new StringReader(program)) {
			execute(reader);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void execute(Reader reader) {

		Reporter reporter = new Reporter();
		boolean hasErrors = false;

		// Analyse lexicale
		Lexer lexer = new Lexer(reader, reporter);

		// Analyse syntaxique
		Parser parser = new Parser(lexer,reporter);
		Program program = parser.parseProgram();
		hasErrors |= reporter.reportErrors(Phase.LEXER);
		hasErrors |= reporter.reportErrors(Phase.PARSER);

		// Analyse statique
		var Analyser = new Analyser(reporter);
		Analyser.analyse(program);
		hasErrors |= reporter.reportErrors(Phase.STATIC);

		// Analyse de types
		var typeChecker = new TypeChecker(reporter);
		typeChecker.checkProgram(program);
		hasErrors |= reporter.reportErrors(Phase.TYPE);

		// Exécution
		if(hasErrors) return;
		Interpreter interpreter = new Interpreter(reporter);
		interpreter.interpret(program);
		reporter.reportErrors(Phase.RUNTIME);
	}

}
