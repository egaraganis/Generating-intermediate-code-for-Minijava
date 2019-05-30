package lowering;
import symboltable.*;
import java.util.*;

public class Lowering {
  String BUFFER = "";

  // Return llvm type based on minijava type
  public String LLVM_type(String Minijava_type){
    if(Minijava_type.equals("boolean"))
      return "i1";
    else if(Minijava_type.equals("int"))
      return "i32";
    else if(Minijava_type.equals("int array"))
      return "i32*";
    else
      return "i8*";
  }

  // Emit function
  public void Emit_HelperFunctions(){
    String CODE;

    CODE = "declare i8* @calloc(i32, i32)\ndeclare i32 @printf(i8*, ...)\ndeclare void @exit(i32)\n\n@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\ndefine void @print_int(i32 %i) {\n\t%_str = bitcast [4 x i8]* @_cint to i8*\n\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n\tret void\n}\n\ndefine void @throw_oob() {\n\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n\tcall i32 (i8*, ...) @printf(i8* %_str)\n\t\n\tcall void @exit(i32 1)\n\tret void\n}";

    // Append code to buffer
    BUFFER += CODE;
  }

  // Emit VTables for program classes
  public void Emit_VTables(LinkedHashMap <String,ClassInfo> classes_data){
    String CODE = "";

    Set<Map.Entry <String,ClassInfo>> st = classes_data.entrySet();
    boolean isMainClass = true;
    String MainClassName;
    // For every class
    for(Map.Entry <String,ClassInfo> cur:st){
      String currentClass = cur.getKey();
      String declareVTableSize = "";
      String fillVTable = "";
      if(isMainClass){ // Main Class has an empty vtable
        isMainClass = false;
        MainClassName = currentClass;
        declareVTableSize = "[0 x i8*] ";
        fillVTable = "[]";

        // Add VTable's code to our current buffer
        String currentVTable = String.format("@.%s_vtable = global %s %s \n", currentClass ,declareVTableSize, fillVTable);

        CODE += currentVTable;
      }
      else{
        // Declare VTable Size
        int noOfFunctions = classes_data.get(currentClass).methods_data.size();
        declareVTableSize = String.format("[%d x i8*]",noOfFunctions);
        // Fill VTable
        boolean flagForFirstArg = true;
        fillVTable = "[";
        // Iterate all class' methods
        Set<Map.Entry<String,MethodInfo>> st1 = classes_data.get(currentClass).methods_data.entrySet();
        for (Map.Entry<String,MethodInfo> cur1:st1){
          String methodName = cur1.getKey();
          String llvm_methodName = "@" + currentClass + "." + methodName;
          String returnType = cur1.getValue().type;
          String llvm_returnType = LLVM_type(returnType);
          // Iretate all method's arguments
          Set< Map.Entry <String,String> > st2 = cur1.getValue().arguments_data.entrySet();
          String llvm_argsType = "i8*";
          for (Map.Entry<String,String> cur2:st2){
             String argType = cur2.getValue();
             llvm_argsType += "," + LLVM_type(argType);
          }
          String ARG_CODE = String.format("i8* bitcast (%s(%s)* %s to i8*)",llvm_returnType,llvm_argsType,llvm_methodName);
          if(flagForFirstArg){
            fillVTable += ARG_CODE;
            flagForFirstArg = false;
          }
          else
            fillVTable += "," + ARG_CODE;
        }

        fillVTable += "]";
        // Add VTable's code to our current buffer
        String currentVTable = String.format("@.%s_vtable = global %s %s \n", currentClass ,declareVTableSize, fillVTable);

        CODE += currentVTable;
      }
    }

    // Append code to buffer
    CODE += "\n";
    BUFFER += CODE;
  }

  // Log BUFFER
  public void Log_Buffer(){
    System.out.println(BUFFER);
  }
}
