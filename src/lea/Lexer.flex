package lea;

import java_cup.runtime.Symbol;

import lea.Node.*;

%%

%public
%class Lexer
%cupsym Terminal
%cup
%unicode
%line
%column

%{
  private Reporter reporter;

  public Lexer(java.io.Reader reader, Reporter reporter) {
    this(reader);
    this.reporter = reporter;
  }

  private String parseString(String s) {
    return s.subSequence(1, s.length()-1).toString()
      .replace("\\b", "\b")
      .replace("\\r", "\r")
      .replace("\\f", "\f")
      .replace("\\n", "\n")
      .replace("\\t", "\t")
      .replace("\\'", "'")
      .replace("\\\"", "\"")
      .replace("\\\\", "\\");
  }
  
  private Symbol mark(int terminal) {
    Reporter.Span span = new Reporter.Span(yyline+1, yycolumn+1, yylength());
  	return new Symbol(terminal, yyline+1, yycolumn+1, span);
  }

  private Symbol mark(int terminal, Node node) {
    Reporter.Span span = new Reporter.Span(yyline+1, yycolumn+1, yylength());
    reporter.attach(node, span);
  	return new Symbol(terminal, yyline+1, yycolumn+1, node);
  }

  private void error(String message) {
    reporter.error(Reporter.Phase.LEXER, new Reporter.Span(yyline+1, yycolumn+1, yylength()), message);
  }
  
%}

%%

 /* Keywords */
"algorithme"   				{ return mark(Terminal.ALGORITHME); }
"variables"    				{ return mark(Terminal.VARIABLES); }
"début"        				{ return mark(Terminal.DEBUT); }
"fin"        				{ return mark(Terminal.FIN); }

"si"        				{ return mark(Terminal.SI); }
"alors"        				{ return mark(Terminal.ALORS); }
"sinon"        				{ return mark(Terminal.SINON); }

"tant"        				{ return mark(Terminal.TANT); }
"que"        				{ return mark(Terminal.QUE); }
"faire"        				{ return mark(Terminal.FAIRE); }
"pour"		 				{ return mark(Terminal.POUR); }
"de"  						{ return mark(Terminal.DE); }
"à"  						{ return mark(Terminal.A); }
"pas"  						{ return mark(Terminal.PAS); }
"interrompre"  				{ return mark(Terminal.INTERROMPRE); }

"écrire"    				{ return mark(Terminal.ECRIRE); }
"longueur"    				{ return mark(Terminal.LONGUEUR); }
"tableau"    				{ return mark(Terminal.TABLEAU); }

 /* Symbols */
"<-"        				{ return mark(Terminal.AFFECTATION); }
"("         				{ return mark(Terminal.PAR_G); }
")"         				{ return mark(Terminal.PAR_D); }
"["         				{ return mark(Terminal.CROCHET_G); }
"]"         				{ return mark(Terminal.CROCHET_D); }
":"         				{ return mark(Terminal.DEUX_PT); }
";"         				{ return mark(Terminal.PT_VIRG); }
","         				{ return mark(Terminal.VIRG); }

"+"         				{ return mark(Terminal.PLUS); }
"-"         				{ return mark(Terminal.MOINS); }
"*"         				{ return mark(Terminal.MULTIPLIE); }
"="         				{ return mark(Terminal.EGAL); }
"<"         				{ return mark(Terminal.INFERIEUR); }
"et"         				{ return mark(Terminal.ET); }
"ou"         				{ return mark(Terminal.OU); }

 /* Types */
"booléen"					{ return mark(Terminal.TYPEBASE, new TBool()); }
"entier"					{ return mark(Terminal.TYPEBASE, new TInt()); }
"caractère"					{ return mark(Terminal.TYPEBASE, new TChar()); }
"chaîne"					{ return mark(Terminal.TYPEBASE, new TStr()); }

 /* Booleans */
"vrai"	                	{ return mark(Terminal.LITERAL, new Bool(true)); }
"faux"			 			{ return mark(Terminal.LITERAL, new Bool(false)); }

 /* Integers */
0|[1-9][0-9]*		 		{ return mark(Terminal.LITERAL, new Int(Integer.parseInt(yytext()))); }

 /* Characters */
\'([^\'\\]|\\.)\'	 		{ return mark(Terminal.LITERAL, new Char(parseString(yytext()).charAt(0))); }

 /* Strings */
\"([^\"\\]|\\.)*\"	 		{ return mark(Terminal.LITERAL, new Str(parseString(yytext()))); }

 /* Identifiers */
[A-Za-z][A-Za-z_0-9]*		{ return mark(Terminal.ID, new Identifier(yytext())); }

 /* Removal of characters that should not be parsed */
[ \t\n\r\f]					{ /* ignore white space. */ }
\/\/.*						{ /* One-line comments */ }
\/\*([^\*]|\*[^\/])*\*\/	{ /* Multi-line comments */ }

 /* Unexpected characters */
<<EOF>>						{ return new Symbol(Terminal.EOF); }
[^]           				{ error("Illegal character: " + yytext()); }
