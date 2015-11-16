import java.util.ArrayList;
import java.util.Hashtable;
import java.io.*;

public class Memory {

  static ArrayList<Byte> memory = new ArrayList<Byte>();
  static Hashtable<String, Integer> varMemLocs = new Hashtable<String, Integer>();
  static Hashtable<String, Integer> strMemLocs = new Hashtable<String, Integer>();
  static final int STACK_SIZE = 20;
  static int PAD = 0;

	//allocate the string to memory
  static public int allocateString(String text)
  {
    if(strMemLocs.containsKey(text)) {
        return strMemLocs.get(text);
    } else {
        int addr = memory.size();
        strMemLocs.put(text, addr);
        int size = text.length();
        for (int i=0; i<size; i++) {
          memory.add(new Byte("", text.charAt(i)));
        }
        memory.add(new Byte("", 0));
        return addr;
    }
  }
	
	//allocate blank space for vars to reside
	static public int allocateInteger(String varname) {
		if(varMemLocs.containsKey(varname)) {	//if the var already exists, return its location
			return varMemLocs.get(varname);
		} else {	//otherwise need to allocate space for it
			int size = 4;	//32bit int has 4 bytes of memory
			while(memory.size()%4 != 0) {	//var addr must be a multiple of 4 so fill with blank space as required
				memory.add(new Byte("", 0));
			}
			int addr = memory.size();	//get the starting location of the var
			varMemLocs.put(varname, addr);	//store the var so it can be looked up
			//allocate space for the var:
            memory.add(new Byte(varname, 0));
            memory.add(new Byte("", 0));
            memory.add(new Byte("", 0));
            memory.add(new Byte("", 0));		
			return addr;
		}
	}

  //CG calls this to dump the memory into the prog
  static public void dumpData(PrintStream o)
  {
    Byte b;
    String s;
    int c;
    int size = memory.size();
    for (int i=0; i<size; i++) {
      b = memory.get(i);

      c = b.getContents();
      if (c >= 32) {
        s = String.valueOf((char)c);
      }
      else {
        s = "";
      }
      o.println("DATA "+c+" ; "+s+" "+b.getName()+"  Mem. addr: "+i);
    }
    int sz = size;
    while(sz%4 != 0) {	//var addr must be a multiple of 4 so fill with blank space as required
      o.println("DATA 0 ;  PAD  Mem. addr: "+ sz);
      sz++;
      PAD++;
    }
    for(int i = 0; i < STACK_SIZE; i++) {
      o.println("DATA 0 ;  CALL_STACK  Mem. addr: "+ (i + size + PAD));
    }
  }
  
  static public int getStackStartAddress() {
    return memory.size() + PAD;
  }
  
}

class Byte {
  String varname;
  int contents;

  Byte(String n, int c)
  {
    varname = n;
    contents = c;
  }

  String getName()
  {
    return varname;
  }

  int getContents()
  {
    return contents;
  }
}
