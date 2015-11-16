// COMS22201: IR tree construction
//  mc13818   Michael Christensen

import java.util.*;
import java.io.*;
import java.lang.reflect.Array;
import antlr.collections.AST;
import org.antlr.runtime.*;
import org.antlr.runtime.tree.*;
import java.util.Hashtable;

public class Irt
{
// The code below is generated automatically from the ".tokens" file of the 
// ANTLR syntax analysis, using the TokenConv program.

// CAMLE TOKENS BEGIN
  public static final String[] tokenNames = new String[] {
"NONE", "NONE", "NONE", "NONE", "WRITE", "WRITELN", "READ", "READR", "SKIP", "IF", "THEN", "ELSE", "TRUE", "FALSE", "WHILE", "DO", "FOR", "SUCHTHAT", "STEP", "FUNCTION", "ARGS", "CALL", "SEMICOLON", "OPENPAREN", "CLOSEPAREN", "OPENSQUARE", "CLOSESQUARE", "ASSIGN", "EQUALS", "NOTEQUALS", "LESSTHANEQUALS", "GREATERTHANEQUALS", "LESSTHAN", "GREATERTHAN", "AND", "OR", "XOR", "NOT", "ADD", "SUB", "MUL", "DIV", "COMMA", "DIGIT", "LOWERCASE", "UPPERCASE", "IDENTIFIER", "INTNUM", "EXPONENT", "REALNUM", "STRING", "COMMENT", "WS"};
  public static final int FUNCTION=19;
  public static final int EXPONENT=48;
  public static final int WHILE=14;
  public static final int DO=15;
  public static final int FOR=16;
  public static final int STEP=18;
  public static final int SUB=39;
  public static final int EQUALS=28;
  public static final int NOT=37;
  public static final int AND=34;
  public static final int IF=9;
  public static final int INTNUM=47;
  public static final int UPPERCASE=45;
  public static final int THEN=10;
  public static final int COMMA=42;
  public static final int IDENTIFIER=46;
  public static final int GREATERTHANEQUALS=31;
  public static final int ARGS=20;
  public static final int CLOSESQUARE=26;
  public static final int DIGIT=43;
  public static final int COMMENT=51;
  public static final int OPENSQUARE=25;
  public static final int GREATERTHAN=33;
  public static final int ADD=38;
  public static final int CLOSEPAREN=24;
  public static final int LESSTHAN=32;
  public static final int XOR=36;
  public static final int LESSTHANEQUALS=30;
  public static final int ELSE=11;
  public static final int NOTEQUALS=29;
  public static final int SEMICOLON=22;
  public static final int TRUE=12;
  public static final int MUL=40;
  public static final int WRITE=4;
  public static final int SKIP=8;
  public static final int WS=52;
  public static final int LOWERCASE=44;
  public static final int WRITELN=5;
  public static final int READ=6;
  public static final int REALNUM=49;
  public static final int OR=35;
  public static final int READR=7;
  public static final int ASSIGN=27;
  public static final int CALL=21;
  public static final int DIV=41;
  public static final int OPENPAREN=23;
  public static final int FALSE=13;
  public static final int SUCHTHAT=17;
  public static final int STRING=50;
// CAMLE TOKENS END

  //Global vars
  public static int labelNum = 0;
  public static int callLabelNum = 0;
  
  public static final int TYPE_BOOLEAN = 0;
  public static final int TYPE_INTEGER = 1;
  public static final int TYPE_REAL = 2;
  public static final int TYPE_STRING = 3;
  
  //table to keep track of the type of each variable
  private static Hashtable<String, Integer> varTypes = new Hashtable<String, Integer>();
  private static Hashtable<String, ArrayList<String>> functionArgs = new Hashtable<String, ArrayList<String>>();
    
  public static IRTree convert(CommonTree ast)
  {
    IRTree irt = new IRTree();
    program(ast, irt);
    return irt;
  }

  public static void program(CommonTree ast, IRTree irt)
  {
    //strings for boolean output and newline need only
    //be declared once, at the start. Their addresses in memory
    //are 0, 6 and 11 respectively.
    Memory.allocateString("false");
    Memory.allocateString("true");
    Memory.allocateString("\n");
    statements(ast, irt);
  }

  /*
   * Recurses through the ast and builds the irt.
   * When the first statement (non-semicolon node) is reached,
   * it processes that statement; then then the recursion unravels and
   * each statement is processed in turn.
   * Note the structure of the ast:
   * 							;
   * 						;	
   * 				...		stmt
   * 			;
   * 		;		stmt
   * 	:=	write
   * x	5
	 */
  public static void statements(CommonTree ast, IRTree irt)
  {
    int i;
    Token t = ast.getToken();
    int tt = t.getType();
    if (tt == SEMICOLON) {
      IRTree irt1 = new IRTree();
      IRTree irt2 = new IRTree();
      CommonTree ast1 = (CommonTree)ast.getChild(0);
      CommonTree ast2 = (CommonTree)ast.getChild(1);
      statements(ast1, irt1);
      statements(ast2, irt2);
      irt.setOp("SEQ");
      irt.addSub(irt1);
      irt.addSub(irt2);
    }
    else {
      statement(ast, irt);
    }
  }

	/*	Builds the irt subtree to be appended to the
   *  tree (by 'statements') for an individual statement.
	 */
  public static void statement(CommonTree ast, IRTree irt)
  {
    CommonTree ast1, ast2, ast3;
    IRTree irt1 = new IRTree(), irt2 = new IRTree(), irt3 = new IRTree();
    Token t = ast.getToken();
    int tt = t.getType();
    if (tt == WRITE) {
      ast1 = (CommonTree)ast.getChild(0);
      //parseWriteArgument works out if arg is an int, string or boolean,
      //and builds the appropriate subtree into irt1
      int type = parseWriteArgument(ast1, irt1);
      if (type == TYPE_INTEGER) {
          irt.setOp("WR");	//WR for writing ints
          irt.addSub(irt1);
      } else if(type == TYPE_REAL) {
          irt.setOp("WRR"); //WRR for writing ints
          irt.addSub(irt1);
      } else {
          irt.setOp("WRS");	//WRS for writing strings
          irt.addSub(irt1);
      }
    } else if (tt == WRITELN) {
        //writeln is treated as writing the pre-stored string '\n'
        irt.setOp("WRS");
        irt.addSub(new IRTree("NEWLINE"));
    } else if(tt == READ) { //read int
        irt.setOp("READ");
        //allocate space for the var/get its address if it exists
        IRTree memAddr = new IRTree(String.valueOf(Memory.allocateInteger(ast.getChild(0).getText())));
        irt.addSub(new IRTree("MEM", new IRTree("CONST", memAddr)));
        //save the variables type  
        varTypes.put(memAddr.getOp(), TYPE_INTEGER);
    } else if(tt == READR) {   //read real
        irt.setOp("READR");
        //allocate space for the var/get its address if it exists
        IRTree memAddr = new IRTree(String.valueOf(Memory.allocateInteger(ast.getChild(0).getText())));
        irt.addSub(new IRTree("MEM", new IRTree("CONST", memAddr)));
        //save the variables type  
        varTypes.put(memAddr.getOp(), TYPE_REAL);
    } else if (tt == ASSIGN) {
        /*
         *					 		MOVE
         *				 MEM				expr
         *	CONST
         *	 int	<-(var addr)
         */
        irt.setOp("MOVE");
        IRTree varTree = new IRTree();	//tree to contain MEM->CONST->addr of var
        expression((CommonTree)ast.getChild(0), varTree);
        expression((CommonTree)ast.getChild(1), irt1);	//irt1 will hold val to be assigned
        
        //set/update the type of this variable
        int type = checkType(irt1);
        varTypes.put(varTree.getSub(0).getSub(0).getOp(), type);

        irt.addSub(varTree);
        irt.addSub(irt1);
    } else if(tt == IF) {
        irt.setOp("SEQ");
        if_stmt((CommonTree)ast, irt);
    } else if(tt == WHILE) {
        irt.setOp("SEQ");
        while_loop((CommonTree)ast, irt);
    } else if(tt == FOR) {
        irt.setOp("SEQ");
        for_loop((CommonTree)ast, irt);
    } else if(tt == SKIP) {
        irt.setOp("SKIP");
    } else if(tt == FUNCTION) {
        irt.setOp("SEQ");
        function((CommonTree)ast, irt);
    }  else if(tt == CALL) {
        irt.setOp("SEQ");
        call_function((CommonTree)ast, irt);
    } else {
        error(tt);
    }
  }

  //parses and returns an argument's type,
  //(used for WRITE), and builds the appropriate subtree,
  public static int parseWriteArgument(CommonTree ast, IRTree irt)
  {
    Token t = ast.getToken();
    int tt = t.getType();
    if(tt == STRING) {
      String tx = t.getText();
      int addr = Memory.allocateString(tx);
      String st = String.valueOf(addr);
      irt.setOp("MEM");
      irt.addSub(new IRTree("CONST", new IRTree(st)));
      return TYPE_STRING;
    } else {
      IRTree tr = new IRTree();
      expression(ast, tr);
      int type = checkType(tr);
      if(type == TYPE_INTEGER) {
          irt.setOp("ARITHEXP");
          irt.addSub(tr);
          return TYPE_INTEGER;
      } else if(type == TYPE_REAL) { 
          irt.setOp("ARITHEXP");
          irt.addSub(tr);
          return TYPE_REAL;
      } else {
          irt.setOp("BOOLEXP");
          irt.addSub(tr);
          return TYPE_BOOLEAN;
      }
    }
  }

	/* Parses expressions, including ints hard coded into program, as well as vars.
	 * and builds the appropriate subtree
	 */
  public static void expression(CommonTree ast, IRTree irt)
  {
    CommonTree ast1;
    IRTree irt1 = new IRTree();
    Token t = ast.getToken();
    int tt = t.getType();
    if (tt == INTNUM) {	//directly available: sets up CONST->int
      constant(ast, irt1);
      irt.setOp("CONST");
      irt.addSub(irt1);
    } else if(tt == REALNUM) { //directly available: sets up CONSTR->real
      constant(ast, irt1);
      irt.setOp("CONSTR");
      irt.addSub(irt1);
    } else if(tt == IDENTIFIER) {	//needs mem access: sets up MEM->CONST->int
			irt.setOp("MEM");
			IRTree memAddr = new IRTree(String.valueOf(Memory.allocateInteger(ast.getText())));
			irt.addSub(new IRTree("CONST", memAddr));
		} else if(tt == ADD) {	//create addition tree
			//			 BINOP
			//	ADD	  arg1	arg2
			irt.setOp("A_BINOP");
            
			//the arguments need to be parsed too (could be expressions themselves)
			IRTree arg1Tree = new IRTree();
			IRTree arg2Tree = new IRTree();
			expression((CommonTree)ast.getChild(0), arg1Tree);
			expression((CommonTree)ast.getChild(1), arg2Tree);
      
      IRTree opTree = new IRTree();

      //perform type error checks
      checkArithmeticTypes(arg1Tree, arg2Tree);
      checkConsistentTypes(arg1Tree, arg2Tree);   //required to ensure both real or both ints
      
      //choose which type of addition is required
      if(checkType(arg1Tree) == TYPE_REAL || checkType(arg2Tree) == TYPE_REAL) {
          opTree.setOp("ADDR");
      } else {
          opTree.setOp("ADD");
      }
            
			irt.addSub(opTree);
			irt.addSub(arg1Tree);
			irt.addSub(arg2Tree);
		} else if(tt == SUB) {	//create subtraction tree
			//			 BINOP
			//	SUB	  arg1	arg2
			irt.setOp("A_BINOP");
			
			//the arguments need to be parsed too (could be expressions themselves)
			IRTree arg1Tree = new IRTree();
			IRTree arg2Tree = new IRTree();
            
      //check if SUB is acting as a unary or a binary operand!
      if((CommonTree)ast.getChild(1) != null) {      //it is binary.. proceed as usual
          expression((CommonTree)ast.getChild(0), arg1Tree);
          expression((CommonTree)ast.getChild(1), arg2Tree);
      } else {    //it is unary..
          //make first argument 0 and the second the expression,
          //i.e. -5 becomes 0-5, and -(2+3) becomes 0 - (2+3)
          expression((CommonTree)ast.getChild(0), arg2Tree);
          if(checkType(arg2Tree) == TYPE_INTEGER) {
              arg1Tree = new IRTree("CONST", new IRTree("0"));
          } else {
              arg1Tree = new IRTree("CONSTR", new IRTree("0.0"));
          }
      }
      IRTree opTree = new IRTree();
      
      //type error checks
      checkArithmeticTypes(arg1Tree, arg2Tree);
      checkConsistentTypes(arg1Tree, arg2Tree);   //required to ensure both real or both ints
      
      //choose which type of subtraction is required
      if(checkType(arg1Tree) == TYPE_REAL || checkType(arg2Tree) == TYPE_REAL) {
          opTree.setOp("SUBR");
      } else {
          opTree.setOp("SUB");
      }
      
      irt.addSub(opTree);
			irt.addSub(arg1Tree);
			irt.addSub(arg2Tree);
		} else if(tt == MUL) {	//create multiplication tree
			//			 BINOP
			//	MUL	  arg1	arg2
			irt.setOp("A_BINOP");

			//the arguments need to be parsed too (could be expressions themselves)
			IRTree arg1Tree = new IRTree();
			IRTree arg2Tree = new IRTree();
			expression((CommonTree)ast.getChild(0), arg1Tree);
			expression((CommonTree)ast.getChild(1), arg2Tree);
            
      IRTree opTree = new IRTree();

      //type error checks
      checkArithmeticTypes(arg1Tree, arg2Tree);
      checkConsistentTypes(arg1Tree, arg2Tree); //required to ensure both real or both ints
      
      //choose which type of multiplication is required
      if(checkType(arg1Tree) == TYPE_REAL || checkType(arg2Tree) == TYPE_REAL) {
          opTree.setOp("MULR");
      } else {
          opTree.setOp("MUL");
      }
      
			irt.addSub(opTree);
			irt.addSub(arg1Tree);
			irt.addSub(arg2Tree);
		} else if(tt == DIV) {	//create division tree
			//			 BINOP
			//	DIV	  arg1	arg2
			irt.setOp("A_BINOP");

			//the arguments need to be parsed too (could be expressions themselves)
			IRTree arg1Tree = new IRTree();
			IRTree arg2Tree = new IRTree();
			expression((CommonTree)ast.getChild(0), arg1Tree);
			expression((CommonTree)ast.getChild(1), arg2Tree);
            
      IRTree opTree = new IRTree();

      //type error checks
      checkArithmeticTypes(arg1Tree, arg2Tree);
      checkConsistentTypes(arg1Tree, arg2Tree);   //required to ensure both real or both ints
      
      //choose which type of division is required
      if(checkType(arg1Tree) == TYPE_REAL || checkType(arg2Tree) == TYPE_REAL) {
          opTree.setOp("DIVR");
      } else {
          opTree.setOp("DIV");
      }
      
			irt.addSub(opTree);
			irt.addSub(arg1Tree);
			irt.addSub(arg2Tree);
		} else if(tt == AND) {
			irt.setOp("B_BINOP");
			IRTree opTree = new IRTree("AND");
			irt.addSub(opTree);

			IRTree arg1Tree = new IRTree();
			IRTree arg2Tree = new IRTree();
			expression((CommonTree)ast.getChild(0), arg1Tree);
			expression((CommonTree)ast.getChild(1), arg2Tree);
      
      //type error check
      checkBooleanTypes(arg1Tree, arg2Tree);
            
			irt.addSub(arg1Tree);
			irt.addSub(arg2Tree);
		} else if(tt == OR) {
			irt.setOp("B_BINOP");
			IRTree opTree = new IRTree("OR");
			irt.addSub(opTree);

			IRTree arg1Tree = new IRTree();
			IRTree arg2Tree = new IRTree();
			expression((CommonTree)ast.getChild(0), arg1Tree);
			expression((CommonTree)ast.getChild(1), arg2Tree);
      
      //type error check
      checkBooleanTypes(arg1Tree, arg2Tree);
            
			irt.addSub(arg1Tree);
			irt.addSub(arg2Tree);
		} else if(tt == XOR) {
			irt.setOp("B_BINOP");
			IRTree opTree = new IRTree("XOR");
			irt.addSub(opTree);

			IRTree arg1Tree = new IRTree();
			IRTree arg2Tree = new IRTree();
			expression((CommonTree)ast.getChild(0), arg1Tree);
			expression((CommonTree)ast.getChild(1), arg2Tree);
      
      //type error check
      checkBooleanTypes(arg1Tree, arg2Tree);
            
			irt.addSub(arg1Tree);
			irt.addSub(arg2Tree);
		} else if(tt == NOT) {
			irt.setOp("UNOP");
			IRTree opTree = new IRTree("NOT");
			irt.addSub(opTree);

			IRTree argTree = new IRTree();
			expression((CommonTree)ast.getChild(0), argTree);
      
      //type error checks
      checkBoolean(argTree);
            
			opTree.addSub(argTree);
		} else if(tt == EQUALS || tt == NOTEQUALS || tt == LESSTHANEQUALS || tt == GREATERTHANEQUALS || tt == LESSTHAN || tt == GREATERTHAN) {
			irt.setOp("BINOP");
      
			IRTree arg1Tree = new IRTree();
			IRTree arg2Tree = new IRTree();
			expression((CommonTree)ast.getChild(0), arg1Tree);
			expression((CommonTree)ast.getChild(1), arg2Tree);
      
      //type error checks
      checkConsistentTypes(arg1Tree, arg2Tree);
      int type = checkType(arg1Tree);
      
      String opCode = "";
      switch(tt) {
          case EQUALS:
              if(type == TYPE_REAL) {
                opCode = "EQUALSR";
              } else {
                opCode = "EQUALS";
              }
              break;
          case NOTEQUALS:
              if(type == TYPE_REAL) {
                opCode = "NOTEQUALSR";
              } else {
                opCode = "NOTEQUALS";
              }
              break;
          case LESSTHANEQUALS:
              if(type == TYPE_REAL) {
                opCode = "LEQUALSR";
              } else {
                opCode = "LEQUALS";
              }
              break;
          case GREATERTHANEQUALS:
              if(type == TYPE_REAL) {
                opCode = "GEQUALSR";
              } else {
                opCode = "GEQUALS";
              }
              break;
          case LESSTHAN:
              if(type == TYPE_REAL) {
                opCode = "LESSTHANR";
              } else {
                opCode = "LESSTHAN";
              }
              break;
          case GREATERTHAN:
              if(type == TYPE_REAL) {
                opCode = "GREATERTHANR";
              } else {
                opCode = "GREATERTHAN";
              }
              break;
      }
            
			IRTree opTree = new IRTree(opCode);
			irt.addSub(opTree);
			irt.addSub(arg1Tree);
			irt.addSub(arg2Tree);
		} else if(tt == TRUE) {
			irt.setOp("BOOL");
			irt.addSub(new IRTree("true"));
		} else if(tt == FALSE) {
			irt.setOp("BOOL");
			irt.addSub(new IRTree("false"));
		} else {
      //if none of the above were satisfied we have an
      //unrecognised token!
      error(tt);
    }
  }

  //parses constants hard coded into program
  public static void constant(CommonTree ast, IRTree irt)
  {
    Token t = ast.getToken();
    int tt = t.getType();
    if (tt == INTNUM || tt == REALNUM) {
      String tx = t.getText();
      irt.setOp(tx);
    } else {
      error(tt);
    }
  }

	public static void if_stmt(CommonTree ast, IRTree irt) {
			//Modified version of slide "Translating conditionals"
			//https://www.cs.bris.ac.uk/Teaching/Resources/COMS22201/coms22201-lec11.pdf
			
			/*	***condTree***
			 *					CJUMP
			 *	optype		l1			l2
			 *		op
			 * e1		e2
			 *
			 */
			IRTree condTree = new IRTree("CJUMP");
			IRTree expTree = new IRTree();
			expression((CommonTree)ast.getChild(0), expTree);
      checkBoolean(expTree);  //ensure the if-expression is actually a boolean condition!

			String label1 = createLabel();	//label true statements
			String label2 = createLabel();	//label false statements
			String label3 = createLabel();	//label post-if

			condTree.addSub(expTree);
			condTree.addSub(new IRTree(label1));
			condTree.addSub(new IRTree(label2));
			irt.addSub(condTree);

			/*	***execTree***
			 *			SEQ
			 *	LABEL			SEQ
			 *l1			s1				SEQ
			 *						JUMP				SEQ
			 *					NAME		LABEL				SEQ
			 *				l3			l2				s2			LABEL
			 *																l3
			 */

			IRTree execTree = new IRTree("SEQ");
			IRTree l1Tree = new IRTree("LABEL", new IRTree(label1));
			execTree.addSub(l1Tree);
			IRTree s1Tree = new IRTree("SEQ");
			IRTree s1 = new IRTree();
			statements((CommonTree)ast.getChild(1), s1);
			s1Tree.addSub(s1);
			execTree.addSub(s1Tree);
			IRTree jumpTree = new IRTree("SEQ", new IRTree("JUMP", new IRTree(label3)));
			s1Tree.addSub(jumpTree);
			IRTree l2Tree = new IRTree("SEQ", new IRTree("LABEL", new IRTree(label2)));
			jumpTree.addSub(l2Tree);
			IRTree s2Tree = new IRTree("SEQ");
			IRTree s2 = new IRTree();
			statements((CommonTree)ast.getChild(2), s2);
			s2Tree.addSub(s2);
			l2Tree.addSub(s2Tree);
			s2Tree.addSub(new IRTree("LABEL", new IRTree(label3)));			

			irt.addSub(execTree);
	}
	public static void while_loop(CommonTree ast, IRTree irt) {
		//See slide "Translating while loops"
		//https://www.cs.bris.ac.uk/Teaching/Resources/COMS22201/coms22201-lec11.pdf

		String label1 = createLabel();	//label pre-loop
		String label2 = createLabel();	//label pre-loop statements
		String label3 = createLabel();	//label post loop
		
		IRTree l1Tree = new IRTree("LABEL", new IRTree(label1));
		irt.addSub(l1Tree);

		/*	***condTree***
		 *					CJUMP
		 *	optype		l2			l3
		 *		op
		 * e1		e2
		 *
		 */
		IRTree condTree = new IRTree("CJUMP");
		IRTree expTree = new IRTree();
		expression((CommonTree)ast.getChild(0), expTree);
    checkBoolean(expTree);  //ensure the expression is actually a boolean condition!

		condTree.addSub(expTree);
		condTree.addSub(new IRTree(label2));
		condTree.addSub(new IRTree(label3));
		IRTree loopTree = new IRTree("SEQ", condTree);	//loop tree is the parent tree describing the loop

		IRTree execTree = new IRTree("SEQ");	//execTree describes the body of the loop
		IRTree l2Tree = new IRTree("LABEL", new IRTree(label2));
		execTree.addSub(l2Tree);
		IRTree sTree = new IRTree("SEQ");	//sTree holds the statements executed in the loop
		IRTree s = new IRTree();
		statements((CommonTree)ast.getChild(1), s);
		sTree.addSub(s);
		IRTree jumpTree = new IRTree("JUMP", new IRTree(label1));	//jumpTree describes the jump back to the start of the loop
		IRTree l3Tree = new IRTree("LABEL", new IRTree(label3));
		
		/* join the trees together:
		 *					irt
		 *	l1Tree			loopTree
		 *					condTree		execTree
		 *										l2Tree		sTree
		 *														s			(tree)
		 *															jumpTree	l3Tree
		 */
		sTree.addSub(new IRTree("SEQ", jumpTree, l3Tree));
		execTree.addSub(sTree);
		loopTree.addSub(execTree);
		irt.addSub(loopTree);
	}
  
  public static void for_loop(CommonTree ast, IRTree irt) {
    String label1 = createLabel();	//label pre-loop
		String label2 = createLabel();	//label pre-loop statements
		String label3 = createLabel();	//label post loop
		
		IRTree l1Tree = new IRTree("LABEL", new IRTree(label1));
		irt.addSub(l1Tree);
        
        /*	***condTree***
		 *					CJUMP
		 *	optype		l2			l3
		 *		op
		 * e1		e2
		 *
		 */
		IRTree condTree = new IRTree("CJUMP");
		IRTree expTree = new IRTree();
		expression((CommonTree)ast.getChild(1), expTree);
    checkBoolean(expTree);  //ensure the expression is actually a boolean condition!
        
    condTree.addSub(expTree);
		condTree.addSub(new IRTree(label2));
		condTree.addSub(new IRTree(label3));
		IRTree loopTree = new IRTree("SEQ", condTree);	//loop tree is the parent tree describing the loop

    IRTree execTree = new IRTree("SEQ");	//execTree describes the body of the loop
		IRTree l2Tree = new IRTree("LABEL", new IRTree(label2));
		execTree.addSub(l2Tree);
		IRTree sTree = new IRTree("SEQ");	//sTree holds the statements executed in the loop
		IRTree s = new IRTree();
		statements((CommonTree)ast.getChild(3), s);
		sTree.addSub(s);
    
    //stepTree describes the step operation of the for loop
    IRTree stepTree = new IRTree();
    statement((CommonTree)ast.getChild(2), stepTree);
    
		IRTree jumpTree = new IRTree("JUMP", new IRTree(label1));	//jumpTree describes the jump back to the start of the loop
		IRTree l3Tree = new IRTree("LABEL", new IRTree(label3));
    IRTree lastTree = new IRTree("SEQ");
    lastTree.addSub(stepTree);
    lastTree.addSub(new IRTree("SEQ", jumpTree, l3Tree));
    sTree.addSub(lastTree);
		execTree.addSub(sTree);
		loopTree.addSub(execTree);
		irt.addSub(loopTree);
  }
  
  public static void function(CommonTree ast, IRTree irt) {
    CommonTree ctr = (CommonTree)ast.getChild(1);
    Token t = ctr.getToken();
    String name = t.getText();
    
    String label1 = name + "1";	//label pre-fn
		String label2 = name + "2";	//label pre-fn statements
		
    IRTree jumpPast = new IRTree("JUMP", new IRTree(label2));
    irt.addSub(jumpPast);
    
    IRTree l1Tree = new IRTree("SEQ", new IRTree("LABEL", new IRTree(label1)));
		irt.addSub(l1Tree);
    
    int i = 0;
    ArrayList<String> argAddresses = new ArrayList<String>();
    while(ast.getChild(0).getChild(i) != null) {
      ctr = (CommonTree)ast.getChild(0).getChild(i);
      t = ctr.getToken();
      String addr = String.valueOf(Memory.allocateInteger(t.getText()));
      argAddresses.add(addr);
      i++;
    }
    functionArgs.put(name, argAddresses);
    
    IRTree sTree = new IRTree("SEQ");
    IRTree s = new IRTree();
		statements((CommonTree)ast.getChild(2), s);
    sTree.addSub(s);
    l1Tree.addSub(sTree);
    
    IRTree jumpBack = new IRTree("JUMP", new IRTree("STACK", new IRTree("POP")));
    IRTree l2Tree = new IRTree("LABEL", new IRTree(label2));
    IRTree tr = new IRTree("SEQ");
    tr.addSub(jumpBack);
    tr.addSub(l2Tree);
    
    sTree.addSub(tr);
  }
  
  public static void call_function(CommonTree ast, IRTree irt) {
    CommonTree ctr = (CommonTree)ast.getChild(0);
    Token t = ctr.getToken();
    String name = t.getText();
    
    int i = 1;
    ArrayList<IRTree> args = new ArrayList<IRTree>();
    ArrayList<String> argAddrs = functionArgs.get(name);
    try {
      while(ast.getChild(i) != null) {
        IRTree a = new IRTree("MOVE");
        IRTree val = new IRTree();
        expression((CommonTree)ast.getChild(i), val);
        a.addSub(new IRTree("MEM", new IRTree("CONST", new IRTree(argAddrs.get(i-1)))));
        a.addSub(val);
        args.add(a);
        i++;
      }
      for(i = 0; i < args.size(); i++) {
        if(i == 0) {
          irt.addSub(args.get(i));
        } else if(i == 1) {
          args.set(i, new IRTree("SEQ", args.get(i)));
          irt.addSub(args.get(i));
        } else {
          args.set(i, new IRTree("SEQ", args.get(i)));
          args.get(i-1).addSub(args.get(i));
        }
      }
    } catch(Exception e) {
      error("Function argument mismatch.");
    }
    
    IRTree tr = new IRTree("SEQ");
    String l = createCallLabel();
    IRTree stackTree = new IRTree("STACK", new IRTree("PUSH", new IRTree(l)));
    tr.addSub(stackTree);
    
    IRTree lastTree = new IRTree("SEQ");
    IRTree jumpTree = new IRTree("JUMP");
    jumpTree.addSub(new IRTree(name+"1"));
    lastTree.addSub(jumpTree);
    lastTree.addSub(new IRTree("LABEL", new IRTree(l)));
    tr.addSub(lastTree);
    
    if(args.size() == 1 || args.size() == 0) {
      irt.addSub(tr);
    } else {
      args.get(args.size() - 1).addSub(tr);
    }
  }
  
	/*
   *  Type checking methods
   */
     
  //Checks both elements are of the same type
	private static void checkConsistentTypes(IRTree element1, IRTree element2) {
    int t1 = checkType(element1);
    int t2 = checkType(element2);
    if(t1 != t2) {
      error("Binary operator arguments have inconsistent types.");
    }
	}
  
  //checks both elements are boolean types
  private static void checkBooleanTypes(IRTree element1, IRTree element2) {
    int t1 = checkType(element1);
    int t2 = checkType(element2);
    if(t1 != TYPE_BOOLEAN || t2 != TYPE_BOOLEAN) {
      error("Boolean operator has non-boolean variable argument(s).");
    }
}
  
  //checks both elemets are arithmetic types
  private static void checkArithmeticTypes(IRTree element1, IRTree element2) {
    int t1 = checkType(element1);
    int t2 = checkType(element2);
    if((t1 != TYPE_INTEGER && t1 != TYPE_REAL) || (t2 != TYPE_INTEGER && t2 != TYPE_REAL)) {
      error("Arithmetic operator has non-arithmetic argument(s)");
    }
}
     
  //checks element is of boolean type
  private static void checkBoolean(IRTree element) {
    int t = checkType(element);
    if(t != TYPE_BOOLEAN) {
      error("Unexpected non-boolean argument encountered.");
    }
  }
  
  //checks element is of boolean type
  private static void checkArithmetic(IRTree element) {
    int t = checkType(element);
    System.out.println("type = "+ t);
    if(t != TYPE_INTEGER && t != TYPE_REAL) {
      error("Unexpected non-arithmetic argument encountered.");
    }
  }
  
  private static int checkType(IRTree exp) {
    if(exp.getOp().equals("CONST")) {
      return TYPE_INTEGER;
    } else if(exp.getOp().equals("CONSTR")) {
      return TYPE_REAL;
    } else if(exp.getOp().equals("A_BINOP")) {
      if(exp.getSubCount() == 1) {
        return checkType(exp.getSub(0));
      } else {
        return checkType(exp.getSub(1));
      }
    } else if(exp.getOp().equals("BOOL") || exp.getOp().equals("AND") || exp.getOp().equals("B_BINOP") || exp.getOp().equals("BINOP")) {
      return TYPE_BOOLEAN;
    } else if(exp.getOp().equals("UNOP") || exp.getOp().equals("NOT")) {
      return checkType(exp.getSub(0));
    } else if(exp.getOp().equals("MEM")) {
      try {
        return varTypes.get(exp.getSub(0).getSub(0).getOp());
      } catch(NullPointerException e) {
        error("Use of unassigned variable!");
      }
    }
    error("Type check error at " + exp.getOp());
    return -1;
  }

  private static void error(int tt)
  {
    System.out.println("Semantic Error: unrecognised internal token "+tokenNames[tt]);
    //abort compilation
    System.exit(1);
  }

  private static void error(String message)
  {
    System.out.println("Semantic Error: "+message);
    //abort compilation
    System.exit(1);
  }
  
  private static String createLabel() {
		labelNum++;
		return "L"+labelNum;
	}
  
  private static String createCallLabel() {
		callLabelNum++;
		return "C"+callLabelNum;
	}
}
