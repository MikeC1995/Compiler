// COMS22201: Optimiser
//  mc13818   Michael Christensen

import java.util.*;
import java.io.*;
import antlr.collections.AST;
import org.antlr.runtime.*;
import org.antlr.runtime.tree.*;

public class IRTOptimiser 
{

  public static IRTree optimise(IRTree irt) {
    fold_constants(irt);
    fold_equivalents(irt);
    fold_ifs(irt);
    return irt;
  }
  
  private static void fold_constants(IRTree irt) {
    int i = 0;
    if(irt.getOp().equals("A_BINOP")) {
      fold_constants(irt.getSub(1));
      fold_constants(irt.getSub(2));
      i = 3;
      if(irt.getSub(1).getOp().equals("CONST") &&
         irt.getSub(2).getOp().equals("CONST")) {
        int a = Integer.parseInt(irt.getSub(1).getSub(0).getOp());
        int b = Integer.parseInt(irt.getSub(2).getSub(0).getOp());
        int c = 0;
        if(irt.getSub(0).getOp().equals("ADD")) {
          c = a + b;
        } else if(irt.getSub(0).getOp().equals("SUB")) {
          c = a - b;
        } else if(irt.getSub(0).getOp().equals("MUL")) {
          c = a * b;
        } else if(irt.getSub(0).getOp().equals("DIV")) {
          c = a / b;
        }
        
        irt.setOp("CONST");
        irt.removeSubs();
        irt.addSub(new IRTree(String.valueOf(c)));
      } else if(irt.getSub(1).getOp().equals("CONSTR") &&
         irt.getSub(2).getOp().equals("CONSTR")) {
        float a = Float.parseFloat(irt.getSub(1).getSub(0).getOp());
        float b = Float.parseFloat(irt.getSub(2).getSub(0).getOp());
        float c = 0;
        if(irt.getSub(0).getOp().equals("ADDR")) {
          c = a + b;
        } else if(irt.getSub(0).getOp().equals("SUBR")) {
          c = a - b;
        } else if(irt.getSub(0).getOp().equals("MULR")) {
          c = a * b;
        } else if(irt.getSub(0).getOp().equals("DIVR")) {
          c = a / b;
        }
        
        irt.setOp("CONSTR");
        irt.removeSubs();
        irt.addSub(new IRTree(String.valueOf(c)));
      }
    }      
    while(irt.getSub(i) != null) {
      fold_constants(irt.getSub(i));
      i++;
    }
  }
  
  private static void fold_equivalents(IRTree irt) {
    int i = 0;
    if(irt.getOp().equals("BINOP")) {
      fold_equivalents(irt.getSub(1));
      fold_equivalents(irt.getSub(2));
      i = 3;
      if(irt.getSub(1).getOp().equals("CONST") &&
         irt.getSub(2).getOp().equals("CONST")) {
        int a = Integer.parseInt(irt.getSub(1).getSub(0).getOp());
        int b = Integer.parseInt(irt.getSub(2).getSub(0).getOp());
        String c = "false";
        if(irt.getSub(0).getOp().equals("EQUALS")) {
          if(a == b) {
            c = "true";
          }
        } else if(irt.getSub(0).getOp().equals("NOTEQUALS")) {
          if(a != b) {
            c = "true";
          }
        } else if(irt.getSub(0).getOp().equals("LEQUALS")) {
          if(a <= b) {
            c = "true";
          }
        } else if(irt.getSub(0).getOp().equals("GEQUALS")) {
          if(a >= b) {
            c = "true";
          }
        } else if(irt.getSub(0).getOp().equals("LESSTHAN")) {
          if(a < b) {
            c = "true";
          }
        } else if(irt.getSub(0).getOp().equals("GREATERTHAN")) {
          if(a > b) {
            c = "true";
          }
        }
        
        irt.setOp("BOOL");
        irt.removeSubs();
        irt.addSub(new IRTree(c));
      } else if(irt.getSub(1).getOp().equals("CONSTR") &&
         irt.getSub(2).getOp().equals("CONSTR")) {
        float a = Float.parseFloat(irt.getSub(1).getSub(0).getOp());
        float b = Float.parseFloat(irt.getSub(2).getSub(0).getOp());
        String c = "false";
        if(irt.getSub(0).getOp().equals("EQUALSR")) {
          if(a == b) {
            c = "true";
          }
        } else if(irt.getSub(0).getOp().equals("NOTEQUALSR")) {
          if(a != b) {
            c = "true";
          }
        } else if(irt.getSub(0).getOp().equals("LEQUALSR")) {
          if(a <= b) {
            c = "true";
          }
        } else if(irt.getSub(0).getOp().equals("GEQUALSR")) {
          if(a >= b) {
            c = "true";
          }
        } else if(irt.getSub(0).getOp().equals("LESSTHANR")) {
          if(a < b) {
            c = "true";
          }
        } else if(irt.getSub(0).getOp().equals("GREATERTHANR")) {
          if(a > b) {
            c = "true";
          }
        }
        
        irt.setOp("BOOL");
        irt.removeSubs();
        irt.addSub(new IRTree(c));
      }
    }
    while(irt.getSub(i) != null) {
      fold_equivalents(irt.getSub(i));
      i++;
    }
  }
  
  //TODO: could fold further? dead code is generated.. maybe a dead code removal?
  private static void fold_ifs(IRTree irt) {
    int i = 0;
    if(irt.getOp().equals("CJUMP") && irt.getSub(0).getOp().equals("BOOL")) {
      String jumpTo;
      if(irt.getSub(0).getSub(0).getOp().equals("true")) {
        jumpTo = irt.getSub(1).getOp();
      } else {
        jumpTo = irt.getSub(2).getOp();
      }
      irt.setOp("JUMP");
      irt.removeSubs();
      irt.addSub(new IRTree(jumpTo));
    }
    
    while(irt.getSub(i) != null) {
      fold_ifs(irt.getSub(i));
      i++;
    }
  }  
}