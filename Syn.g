// COMS22201: Syntax analyser
//  mc13818   Michael Christensen

parser grammar Syn;

options {
  tokenVocab = Lex;
  output = AST;
}

@members
{
	private String cleanString(String s){
		String tmp;
		tmp = s.replaceAll("^'", "");
		s = tmp.replaceAll("'$", "");
		tmp = s.replaceAll("''", "'");
		return tmp;
	}
  
  //override any error messages
  @Override
  public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
      String hdr = getErrorHeader(e);
      String msg = getErrorMessage(e, tokenNames);
      //append text to error message to indicate error was syntactical
      System.out.println("Syntax Error: " + hdr + " " + msg);
      //abort compilation
      System.exit(1);
  }
}

program :
    statements
  ;
  
statements :
    statement ( SEMICOLON^ statement )*
  ;
  
statement :
    IDENTIFIER ASSIGN^ exp
  | SKIP
  | WRITE^ OPENPAREN! ( string | exp) CLOSEPAREN!
  | WRITELN
  | READ^ OPENPAREN! IDENTIFIER CLOSEPAREN!
  | READR^ OPENPAREN! IDENTIFIER CLOSEPAREN!
  | IF^ exp THEN! statement ELSE! statement
  | WHILE^ exp DO! statement
  | FOR^ IDENTIFIER SUCHTHAT! exp STEP! assignident DO! statement
  | OPENPAREN! statements CLOSEPAREN!
  | callfn
  | function
  ;

callfn :
  CALL^ OPENPAREN! IDENTIFIER OPENSQUARE! call_arglist CLOSESQUARE! CLOSEPAREN! 
  ;
  
function :
  FUNCTION^ arguments IDENTIFIER OPENPAREN! statements CLOSEPAREN!
  ;

arguments:
  ARGS^ OPENSQUARE! def_arglist? CLOSESQUARE!
  ;
  
def_arglist :
  IDENTIFIER (COMMA! IDENTIFIER)*
  ;

call_arglist :
  (IDENTIFIER | neg) (COMMA! (IDENTIFIER | neg))*
  ;
  
assignident :
  IDENTIFIER ASSIGN^ exp
  ;
  
/* Note that boolean and arithmetic expressions can be mixed (like in C).
 * (This means that e.g. 3*4-12=false => true)
 * This is illegal according to the language spec and as such these errors
 * are caught at the semantic level, by Irt.java
 */

exp:
	unary (( AND^ | OR^ | XOR^ ) unary)*
	;

unary:
	 (NOT^)? rel
    ;

rel:
	add ((EQUALS^ | NOTEQUALS^ | LESSTHANEQUALS^ | GREATERTHANEQUALS^ | LESSTHAN^ | GREATERTHAN^) add)*
	;
  
add :
   mult ( ( ADD^ | SUB^ ) mult )*
  ;

mult :
  atom ( ( MUL^ | DIV^ ) atom )*
  ;

atom:
	  IDENTIFIER
	| neg
	| TRUE
	| FALSE
	| OPENPAREN! exp CLOSEPAREN!
	;

//neg permits use of negative numbers
neg :
      (SUB^)? INTNUM
    | (SUB^)? REALNUM
    ;
  
string
    scope { String tmp; }
    :
    s=STRING { $string::tmp = cleanString($s.text); }-> STRING[$string::tmp]
;