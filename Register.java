import java.util.ArrayList;
import java.io.*;

public class Register {
	private static ArrayList<Integer> registers = new ArrayList<Integer>();
	private static ArrayList<Integer> free = new ArrayList<Integer>();

  private static ArrayList<Integer> stack = new ArrayList<Integer>();

  //RESERVED REGISTERS:
  //R0 is always = 0
  //R1 is the call-stack pointer
  
	public static String getReg() {
		if(free.size() > 0) {
			int id = free.get(0);
			free.remove(0);
			return "R"+id;
		} else {
			registers.add(registers.size() + 1);
			return "R"+Integer.toString(registers.size() + 1);
		}
	}
	public static void disposeReg(String reg) {
		try {
			int id = Integer.parseInt(reg.substring(1));
			free.add(id);
		} catch (Exception e) {
			System.out.println("Failed register disposal. Incorrect argument. Message: " + e.getMessage());
			System.exit(1);
		}
	}
}