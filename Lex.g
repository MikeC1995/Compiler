// COMS22201: Lexical analyser
//  mc13818   Michael Christensen

lexer grammar Lex;

@members {
  //override any error messages
  @Override
  public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
      String hdr = getErrorHeader(e);
      String msg = getErrorMessage(e, tokenNames);
      //append text to error message to indicate error was lexical
      System.out.println("Lexical Error: " + hdr + " " + msg);
      //abort compilation
      System.exit(1);
  }
}

//---------------------------------------------------------------------------
// KEYWORDS
//---------------------------------------------------------------------------
WRITE      : 'write' ;
WRITELN    : 'writeln' ;
READ       : 'read' ;
READR      : 'readr' ;
SKIP       : 'skip' ;
IF         : 'if' ;
THEN       : 'then' ;
ELSE       : 'else' ;
TRUE       : 'true' ;
FALSE      : 'false' ;
WHILE      : 'while' ;
DO         : 'do' ;
FOR        : 'for' ;
SUCHTHAT   : 'st' ;
STEP       : 'step' ;
FUNCTION   : 'function' ;
ARGS       : 'args' ;
CALL       : 'call' ;

//---------------------------------------------------------------------------
// OPERATORS
//---------------------------------------------------------------------------
SEMICOLON           : ';' ;
OPENPAREN           : '(' ;
CLOSEPAREN          : ')' ;
OPENSQUARE          : '[' ;
CLOSESQUARE         : ']' ;
ASSIGN              : ':=' ;
EQUALS              : '=' ;
NOTEQUALS           : '!=' ;
LESSTHANEQUALS      : '<=' ;
GREATERTHANEQUALS   : '>=' ;
LESSTHAN            : '<' ;
GREATERTHAN         : '>' ;
AND                 : '&' ;
OR                  : '|' ;
XOR                 : '^' ;
NOT                 : '!' ;
ADD                 : '+' ;
SUB                 : '-' ;
MUL                 : '*' ;
DIV                 : '/' ;
COMMA               : ',' ;

//fragments won't be counted as tokens and only serve to simplify a grammar:
fragment DIGIT : '0'..'9';
fragment LOWERCASE : 'a' .. 'z';
fragment UPPERCASE : 'A' .. 'Z';

//This rule throws an exception for identifiers that are too long
IDENTIFIER   : (LOWERCASE | UPPERCASE)(LOWERCASE | UPPERCASE | DIGIT)*
	{
		final String id = getText();
        if((id.length() > 8)) throw new IllegalArgumentException("Identifier '" + id + "' is too long! (Max 8 chars)");
	};

INTNUM       : DIGIT+ ;

REALNUM      : INTNUM '.' INTNUM (EXPONENT)? ;

EXPONENT     : 'e' ('-')? INTNUM ;

STRING       : '\'' ('\'' '\'' | ~'\'')* '\'';

COMMENT      : '{' (~'}')* '}' {skip();} ;

WS           : (' ' | '\t' | '\r' | '\n' )+ {skip();} ;
