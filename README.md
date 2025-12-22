# Languages and Automata – TP 6: Project

This repository contains the **starter code** for the final practical project of the *Languages and Automata* course at Nantes University.

It provides a complete compilation pipeline for a small imperative language, including:
- lexical analysis (JFlex),
- syntactic analysis (CUP),
- static analysis,
- type checking,
- interpretation,
- and systematic unit testing (JUnit).

See the [main organization](https://github.com/LangagesEtAutomates/) for more information on the course
and related teaching materials.

---

## Structure

```
├── LICENSE.txt           # MIT license (see organization-wide license file)
├── README.md             # This file
├── build.xml             # Ant build file
├── lib/                  # External libraries (JFlex, CUP, JUnit, etc.)
├── src/                  # Source files
│   └── lea/              # Main Java package
│       ├── Lexer.flex    # JFlex lexer specification
│       ├── Parser.cup    # CUP parser specification
│       ├── *.java        # AST, analyser, type checker, interpreter, main class
├── gen/                  # Generated sources (lexer and parser)
├── build/                # Compiled main classes
├── test/                 # JUnit test sources
│   └── lea/              # Tests for each compilation phase
├── build-test/           # Compiled test classes
```

Generated directories (`gen/`, `build/`, `build-test/`) are produced automatically and should not be edited manually.

---

## Build and Execution

The project uses **Apache Ant**.

- Generate lexer and parser:  

```bash
ant generate
```

- Compile all sources (including generated code):

```bash
ant compile
```

- Run the full JUnit test suite:

```bash
ant test
```

- Run the interpreter (`lea.Main`):

```bash
ant run
```

- Compile and immediately run:

```bash
ant build
```

- Remove all generated and compiled files:

```bash
ant clean
```

The project targets **Java 21**.

---

## Dependencies

All dependencies are provided in the `lib/` directory:

- **JFlex** — lexer generation  
- **Java CUP** — parser generation  
- **JUnit 5** — unit testing  

No external installation is required beyond a JDK and Ant.

---

## License

All **source code** in this repository is distributed under the **MIT License**.

- The full legal text is available in [`LICENSE.txt`](LICENSE.txt).
- Organization-wide licensing details and attributions are documented in  
  https://github.com/LangagesEtAutomates/.github/blob/main/LICENSE.md

This license applies to all Java sources, grammar files (`.flex`, `.cup`),
and test code in this repository.

---

## Contributing

Contributions are welcome, in particular:
- improvements to diagnostics or test coverage,
- bug reports and fixes.

Please use pull requests to propose changes.
For significant modifications, consider opening an issue first to discuss the design.
