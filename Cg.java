// COMS22201: Code generation
//  mc13818   Michael Christensen

import java.util.*;
import java.io.*;
import java.lang.reflect.Array;
import antlr.collections.AST;
import org.antlr.runtime.*;
import org.antlr.runtime.tree.*;

public class Cg
{
  //used to create unique labels
	public static int labelNum = 0;
  
  //addresses of pre-stored strings
  public static final int addrFALSE = 0;
  public static final int addrTRUE = 6;
  public static final int addrNEWLINE = 11;
    
  public static void program(IRTree irt, PrintStream o)
  {
    emit(o, "; *** PROGRAM ***");
    emit(o, "XOR R0,R0,R0");    //R0 always stores 0
    emit(o, "ADDI R1,R0,"+(Memory.getStackStartAddress()-4));    //R1 points to stack
    statement(irt, o);
    emit(o, "HALT");           // Program must end with HALT
    emit(o, "; *** INITIAL MEMORY CONTENTS ***");
    emit(o, "; *** (TRUE, FALSE and NEWLINE ALWAYS ADDED) ***");
    Memory.dumpData(o);        // Dump DATA lines: initial memory contents
  }

  private static void statement(IRTree irt, PrintStream o)
  {
    //if sequential instructions, execute left branch first, then right branch
    if (irt.getOp().equals("SEQ")) {
        statement(irt.getSub(0), o);
        statement(irt.getSub(1), o);
    } else if (irt.getOp().equals("WRS")) {
      //if WRS->MEM->CONST (i.e. writing an arith var)
      if(irt.getSub(0).getOp().equals("MEM") && irt.getSub(0).getSub(0).getOp().equals("CONST")) {
          String addr = irt.getSub(0).getSub(0).getSub(0).getOp();
          emit(o, "WRS "+addr);
      }
      //if WRS->BOOLEXP (i.e. writing a boolean)
      else if(irt.getSub(0).getOp().equals("BOOLEXP")) {
          String result = expression(irt.getSub(0).getSub(0), o);
          String labelNum = createLabelCode();	//creates unique label code
          String l1 = "WRSFALSE"+labelNum;
          String l2 = "WRSTRUE"+labelNum;
          String l3 = "WRSDONE"+labelNum;
          emit(o, l1+":");
          emit(o, "BEQZ "+result+","+l2);
          emit(o, "WRS "+addrTRUE);
          emit(o, "JMP "+l3);
          emit(o, l2+":");
          emit(o, "WRS "+addrFALSE);
          emit(o, l3+":");
          Register.disposeReg(result);
      } else if(irt.getSub(0).getOp().equals("NEWLINE")) {
          emit(o, "WRS "+addrNEWLINE);
      }
    }
    //simple write to stdout
    else if (irt.getOp().equals("WR") && irt.getSub(0).getOp().equals("ARITHEXP")) {
      String e = expression(irt.getSub(0).getSub(0), o);
      emit(o, "WR "+e);
      Register.disposeReg(e);
    } else if (irt.getOp().equals("WRR") && irt.getSub(0).getOp().equals("ARITHEXP")) {
      String e = expression(irt.getSub(0).getSub(0), o);
      emit(o, "WRR "+e);
      Register.disposeReg(e);
    } else if(irt.getOp().equals("READ")) { //read int
      String r = Register.getReg();
      String destAddr = irt.getSub(0).getSub(0).getSub(0).getOp();
      emit(o, "RD "+r);
      emit(o, "STORE "+r+","+"R0,"+destAddr);
      Register.disposeReg(r);
    } else if(irt.getOp().equals("READR")) {  //read real
      String r = Register.getReg();
      String destAddr = irt.getSub(0).getSub(0).getSub(0).getOp();
      emit(o, "RDR "+r);
      emit(o, "STORE "+r+","+"R0,"+destAddr);
      Register.disposeReg(r);
    } else if (irt.getOp().equals("MOVE")) {	//var assignment
      //get addr of var to assign: MOVE->MEM->CONST->addr
      String storeAddr = irt.getSub(0).getSub(0).getSub(0).getOp();
      //get the register the value to be assigned has been loaded into
      String valReg = expression(irt.getSub(1), o);
      //perform assignment (store the value into the var's mem addr)
      emit(o, "STORE "+valReg+",R0,"+storeAddr);
      Register.disposeReg(valReg);
    } else if(irt.getOp().equals("CJUMP")) {	//generate if/while/for stmts
      //get the register storing the evaluated expression
      String eReg = expression(irt.getSub(0), o);
      //get where to jump if the stmt is true or false
      String l1 = irt.getSub(1).getOp();
      String l2 = irt.getSub(2).getOp();
      emit(o, "BEQZ "+eReg+","+l2);	//if the result of the exp is 0, it's false so jump to false part
      emit(o, "JMP "+l1);				//otherwise jump to true part
      Register.disposeReg(eReg);
    } else if(irt.getOp().equals("JUMP")) {	//unconditional jump
      if(irt.getSub(0).getOp().equals("STACK")) {
        String e = expression(irt.getSub(0), o);
        emit(o, "JUMP " + e);
      } else {
        emit(o, "JMP "+ irt.getSub(0).getOp());
      }
    } else if(irt.getOp().equals("SKIP")) {	//do nothing
      //emit(o, "NOP");
    } else if(irt.getOp().equals("LABEL")) {	//write a label
      emit(o, irt.getSub(0).getOp() + ":");
    } else if(irt.getOp().equals("LOAD")) {	//write a label
      //skip
    } else if(irt.getOp().equals("STACK") && irt.getSub(0).getOp().equals("PUSH")) {
      String r = Register.getReg();
      emit(o, "ADDI R1,R1,4");  //inc stack pointer
      //push to stack:
      emit(o, "IADDR "+r+","+irt.getSub(0).getSub(0).getOp());  //put label addr in reg
      emit(o, "STORE "+r+",R1,0");  //store reg contents in stack at address given by the pointer
      Register.disposeReg(r);
    } else {
      //unrecognised token
      error(irt.getOp());
    }
  }
  //evaluates an expression returning the register where the result is stored
  private static String expression(IRTree irt, PrintStream o)
  {
    String result = Register.getReg();
    if (irt.getOp().equals("CONST")) {	//expression is a int constant
      String t = irt.getSub(0).getOp();
      emit(o, "ADDI "+result+",R0,"+t);	//put the const in the reg
    } else if (irt.getOp().equals("CONSTR")) {	//expression is a simple lone constant
      String t = irt.getSub(0).getOp();
      emit(o, "MOVIR "+result+","+t);
    } else if(irt.getOp().equals("MEM")) {	//need to load from mem
      //get the mem addr. (MEM->CONST->addr)
      String addr = irt.getSub(0).getSub(0).getOp();
      emit(o, "LOAD "+result+",R0,"+addr);	//Load the val from mem into the reg
    } else if(irt.getOp().equals("BOOL")) {	//need to load from mem
      //get the bool val (BOOL->val)
      String addr = irt.getSub(0).getOp();
      String val;
      if(addr.equals("true")) {
          val = "1";
      } else {
          val = "0";
      }
      emit(o, "ADDI "+result+",R0,"+val);
    } else if(irt.getOp().equals("A_BINOP")) {	//arithmetic binary operators
      String arg1Reg = expression(irt.getSub(1), o);
      String arg2Reg = expression(irt.getSub(2), o);
      if(irt.getSub(0).getOp() == "ADD") {  //int addition
        emit(o, "ADD "+result+","+arg1Reg+","+arg2Reg);
      } else if(irt.getSub(0).getOp() == "ADDR") {  //real addition
        emit(o, "ADDR "+result+","+arg1Reg+","+arg2Reg);
      } else if(irt.getSub(0).getOp() == "SUB") { //int subtraction
        emit(o, "SUB "+result+","+arg1Reg+","+arg2Reg);
      } else if(irt.getSub(0).getOp() == "SUBR") {  //real subtraction
        emit(o, "SUBR "+result+","+arg1Reg+","+arg2Reg);
      } else if(irt.getSub(0).getOp() == "MUL") {   //int multiplication
        emit(o, "MUL "+result+","+arg1Reg+","+arg2Reg);
      } else if(irt.getSub(0).getOp() == "MULR") {  //real multiplication
        emit(o, "MULR "+result+","+arg1Reg+","+arg2Reg);
      } else if(irt.getSub(0).getOp() == "DIV") {   //int division
        emit(o, "DIV "+result+","+arg1Reg+","+arg2Reg);
      } else if(irt.getSub(0).getOp() == "DIVR") {  //real division
        emit(o, "DIVR "+result+","+arg1Reg+","+arg2Reg);
      } else {  //unrecognised token
        error(irt.getSub(0).getOp());
      }
      //free up intermediate result registers
      Register.disposeReg(arg1Reg);
      Register.disposeReg(arg2Reg);
    } else if(irt.getOp().equals("B_BINOP")) {	//boolean binary operators
      String arg1Reg = expression(irt.getSub(1), o);
      String arg2Reg = expression(irt.getSub(2), o);
      if(irt.getSub(0).getOp() == "AND") {
        //AND is equivalent to multiplication when using true=1 and false=0
        emit(o, "MUL "+result+","+arg1Reg+","+arg2Reg);
      } else if(irt.getSub(0).getOp() == "OR") {
        //OR is equivalent to addition when using true=1 and false=0
        emit(o, "ADD "+result+","+arg1Reg+","+arg2Reg);
      } else if(irt.getSub(0).getOp() == "XOR") {
        emit(o, "XOR "+result+","+arg1Reg+","+arg2Reg);
      }
      Register.disposeReg(arg1Reg);
      Register.disposeReg(arg2Reg);
    } else if(irt.getOp().equals("BINOP")) {	//relational binary operators
      String tempReg = Register.getReg();
      String arg1Reg = expression(irt.getSub(1), o);
      String arg2Reg = expression(irt.getSub(2), o);      
      if(irt.getSub(0).getOp().equals("EQUALS") || irt.getSub(0).getOp().equals("EQUALSR")) {
        //get where to jump if the stmt is true or false
        //(in order to set result reg to 1 or 0 accordingly)
        String labelNum = createLabelCode();	//creates unique label code (stops ambiguity)
        String l = "ISTRUE"+labelNum;
        
        emit(o, "ADDI "+result+",R0,1");
        if(irt.getSub(0).getOp().equals("EQUALS")) {
          emit(o, "SUB "+tempReg+","+arg1Reg+","+arg2Reg);
        } else {
          emit(o, "SUBR "+tempReg+","+arg1Reg+","+arg2Reg);
        }
        emit(o, "BEQZ "+tempReg+","+l);
        emit(o, "XOR "+result+","+result+","+result);
        emit(o, l+":");
      } else if(irt.getSub(0).getOp().equals("NOTEQUALS") || irt.getSub(0).getOp().equals("NOTEQUALSR")) {
        //get where to jump if the stmt is true or false
        //(in order to set result reg to 1 or 0 accordingly)
        String labelNum = createLabelCode();	//creates unique label code (stops ambiguity)
        String l = "ISTRUE"+labelNum;
       
        emit(o, "ADDI "+result+",R0,1");
        if(irt.getSub(0).getOp().equals("NOTEQUALS")) {
          emit(o, "SUB "+tempReg+","+arg1Reg+","+arg2Reg);
        } else {
          emit(o, "SUBR "+tempReg+","+arg1Reg+","+arg2Reg);
        }
        emit(o, "BNEZ "+tempReg+","+l);
        emit(o, "XOR "+result+","+result+","+result);
        emit(o, l+":");
      } else if(irt.getSub(0).getOp().equals("LEQUALS") || irt.getSub(0).getOp().equals("LEQUALSR")) {
        //get where to jump if the stmt is true or false
        //(in order to set result reg to 1 or 0 accordingly)
        String labelNum = createLabelCode();	//creates unique label code (stops ambiguity)
        String l = "ISTRUE"+labelNum;
        emit(o, "ADDI "+result+",R0,1");
        if(irt.getSub(0).getOp().equals("LEQUALS")) {
          emit(o, "SUB "+tempReg+","+arg1Reg+","+arg2Reg);
        } else {
          emit(o, "SUBR "+tempReg+","+arg1Reg+","+arg2Reg);
        }
        emit(o, "BEQZ "+tempReg+","+l);
        emit(o, "BLTZ "+tempReg+","+l);
        emit(o, "XOR "+result+","+result+","+result);
        emit(o, l+":");
      } else if(irt.getSub(0).getOp().equals("GEQUALS") || irt.getSub(0).getOp().equals("GEQUALSR")) {
        //get where to jump if the stmt is true or false
        //(in order to set result reg to 1 or 0 accordingly)
        String labelNum = createLabelCode();	//creates unique label code (stops ambiguity)
        String l = "ISTRUE"+labelNum;
        emit(o, "ADDI "+result+",R0,1");                
        if(irt.getSub(0).getOp().equals("GEQUALS")) {
          emit(o, "SUB "+tempReg+","+arg1Reg+","+arg2Reg);
        } else {
          emit(o, "SUBR "+tempReg+","+arg1Reg+","+arg2Reg);
        }
        emit(o, "BGEZ "+tempReg+","+l);
        emit(o, "XOR "+result+","+result+","+result);
        emit(o, l+":");
      }  else if(irt.getSub(0).getOp().equals("LESSTHAN") || irt.getSub(0).getOp().equals("LESSTHANR")) {
        //get where to jump if the stmt is true or false
        //(in order to set result reg to 1 or 0 accordingly)
        String labelNum = createLabelCode();	//creates unique label code (stops ambiguity)
        String l = "ISTRUE"+labelNum;
        
        emit(o, "ADDI "+result+",R0,1");                
        if(irt.getSub(0).getOp().equals("LESSTHAN")) {
          emit(o, "SUB "+tempReg+","+arg1Reg+","+arg2Reg);
        } else {
          emit(o, "SUBR "+tempReg+","+arg1Reg+","+arg2Reg);
        }
        emit(o, "BLTZ "+tempReg+","+l);
        emit(o, "XOR "+result+","+result+","+result);
        emit(o, l+":");
      } else if(irt.getSub(0).getOp().equals("GREATERTHAN") || irt.getSub(0).getOp().equals("GREATERTHANR")) {
        //get where to jump if the stmt is true or false
        //(in order to set result reg to 1 or 0 accordingly)
        String labelNum = createLabelCode();	//creates unique label code (stops ambiguity)
        String l1 = "ISFALSE"+labelNum;
        String l2 = "ISTRUE"+labelNum;
        emit(o, "ADDI "+result+",R0,1");
        if(irt.getSub(0).getOp().equals("GREATERTHAN")) {
          emit(o, "SUB "+tempReg+","+arg1Reg+","+arg2Reg);
        } else {
          emit(o, "SUBR "+tempReg+","+arg1Reg+","+arg2Reg);
        }
        emit(o, "BEQZ "+tempReg+","+l1);
        emit(o, "BGEZ "+tempReg+","+l2);
        emit(o, l1+":");
        emit(o, "XOR "+result+","+result+","+result);
        emit(o, l2+":");
      } else {
        error(irt.getOp());
      }  
      Register.disposeReg(tempReg);
      Register.disposeReg(arg1Reg);
      Register.disposeReg(arg2Reg);
    } else if(irt.getOp().equals("UNOP")) {	//unary operators
      if(irt.getSub(0).getOp() == "NOT") {
        //evaluate the argument and get the register of the result
        String argReg = expression(irt.getSub(0).getSub(0), o);
        //NOT is equivalent to 1-x where x=true=1 or x=false=0
        emit(o, "XOR "+result+","+result+","+result);	//initialise the reg
        emit(o, "ADDI "+result+","+result+",1");	//put 1 in it
        emit(o, "SUB "+result+","+result+","+argReg);	//take the val away from 1
        Register.disposeReg(argReg);
      } else {
        //unrecognised token
        error(irt.getOp());
      }
    } else if(irt.getOp().equals("STACK") && irt.getSub(0).getOp().equals("POP")) {
      emit(o, "LOAD "+result+",R1,0");  //load the address to jump to which is stored in the stack
      emit(o, "SUBI R1,R1,4");  //decrement stack pointer
    } else {
      //unrecognised token
      error(irt.getOp());
    }
    return result;
  }  

  private static String createLabelCode() {
    labelNum++;
    return "" + labelNum;
  }

  private static void emit(PrintStream o, String s)
  {
    o.println(s);
  }

  private static void error(String op)
  {
    System.out.println("Code Generation Error: unrecognised internal token "+op);
    //abort compilation
    System.exit(1);
  }
}
