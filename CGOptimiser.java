import java.util.*;
import java.io.*;
import java.util.ArrayList;
import antlr.collections.AST;
import org.antlr.runtime.*;
import org.antlr.runtime.tree.*;

public class CGOptimiser 
{
  public static void optimise(String file) {
    cleanJumps(file);
    removeDuplicateLabels(file);
  }
  
  /* Removes redundant jumps like:
   *  JMP L3:
   *  L3:
   */
  public static void cleanJumps(String file) {
    String tempFile = "tmp_"+file;
    BufferedReader br = null;
    BufferedWriter bw = null;
    try {
     br = new BufferedReader(new FileReader(file));
     bw = new BufferedWriter(new FileWriter(tempFile));
     String line = br.readLine();
     while (line != null) {
        if (line.contains("JMP L")) {
          String jump = line;
          int code = Integer.parseInt(line.substring(line.indexOf('L')+1));
          line = br.readLine();
          if(line.charAt(0) == 'L' && line.charAt(line.length()-1) == ':') {
            if(Integer.parseInt(line.substring(1, line.length() - 1)) != code) {
              bw.write(jump+"\n");
              bw.write(line+"\n");
            }
          }
        } else {
          bw.write(line+"\n");
        }
        line = br.readLine();
     }
    } catch (Exception e) {
     return;
    } finally {
     try {
      if(br != null)
           br.close();
      } catch (IOException e) {
        //
      }
      try {
        if(bw != null)
           bw.close();
      } catch (IOException e) {
        //
      }
    }
    // Once everything is complete, delete old file..
    File oldFile = new File(file);
    oldFile.delete();

    // And rename tmp file's name to old file name
    File newFile = new File(tempFile);
    newFile.renameTo(oldFile);
  }
  
  public static void removeDuplicateLabels(String file) {
    String tempFile = "tmp_"+file;
    BufferedReader br = null;
    BufferedWriter bw = null;
    ArrayList<ArrayList<String>> removeList = new ArrayList<ArrayList<String>>();
    try {
      br = new BufferedReader(new FileReader(file));
      String line = br.readLine();
      while (line != null) {
        if(line.charAt(0) == 'L' && line.charAt(line.length()-1) == ':') {
          ArrayList<String> group = new ArrayList<String>();
          String l1 = line.substring(0, line.length()-1);
          group.add(l1);
          line = br.readLine();
          while(line.charAt(0) == 'L' && line.charAt(line.length()-1) == ':') {
            group.add(line.substring(0, line.length()-1));
            line = br.readLine();
          }
          if(group.size() > 1) {
            removeList.add(group);
          }
        } else {
          line = br.readLine();
        }
      }
      
      br = new BufferedReader(new FileReader(file));
      bw = new BufferedWriter(new FileWriter(tempFile));
      line = br.readLine();
      boolean write = true;
      while (line != null) {
        write = true;
        for(int i = 0; i < removeList.size(); i++) {
          for(String l : removeList.get(i)) {
            if(line.endsWith(l) || line.contains(l+":")) {
              if(line.charAt(0) == 'L' && line.charAt(line.length()-1) == ':') {
                if(!line.substring(0, line.length()-1).equals(removeList.get(i).get(0))) {
                  //skip this line
                  
                  //System.out.println("skip " + line + "  " + line.substring(0, line.length()-1) + 
                  //"   " + removeList.get(i).get(0));
                  write = false;
                }
              } else {
                //System.out.println("replace " + l + " in line " + line + " with " + removeList.get(i).get(0));
                line = line.replace(l, removeList.get(i).get(0));
                //System.out.println("new line: " + line);
              }
            }
          }
        }
        if(write) {
          bw.write(line+"\n");
        }
        line = br.readLine();
      }
      
    } catch (Exception e) {
     return;
    } finally {
     try {
      if(br != null)
           br.close();
      } catch (IOException e) {
        //
      }
      try {
        if(bw != null)
           bw.close();
      } catch (IOException e) {
        //
      }
    }
    // Once everything is complete, delete old file..
    File oldFile = new File(file);
    oldFile.delete();

    // And rename tmp file's name to old file name
    File newFile = new File(tempFile);
    newFile.renameTo(oldFile);
  }
}