package lowering;
import symboltable.*;
import java.util.*;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileNotFoundException;

public class Lowering {
  String currentClass = "";
  String currentMethod = "";
  String BUFFER = "";
  SymbolTable ST;
  int REG_NUM = 0;

  // Constructor
  public Lowering(SymbolTable symbolTable){
    ST = symbolTable;
  }

  // Setter methods
  public void SetCurrentMethod(String curMethod){
    currentMethod = curMethod;
  }
  public void SetCurrentClass(String curClass){
    currentClass = curClass;
  }

  // Getters
  public String GetCurrentClass(){
    return currentClass;
  }
  public String GetCurrentMethod(){
    return currentMethod;
  }
  public SymbolTable GetSymbolTable(){
    return ST;
  }

  // Get a new temp register
  public String new_temp(){
    String new_reg = "%_" + REG_NUM;
    REG_NUM++;
    return new_reg;
  }

  // Get a new label
  public String new_label(String typeOfCond){
    String new_label = typeOfCond + REG_NUM;
    REG_NUM++;
    return new_label;
  }

  // Print current class and current method
  public void WhereAreWe(){
    System.out.println("We are in: " + currentClass + "," + currentMethod);
  }

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
  public void Emit_VTables(){
    String CODE = "";

    Set<Map.Entry <String,ClassInfo>> st = ST.classes_data.entrySet();
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
        int noOfFunctions = ST.classes_data.get(currentClass).methods_data.size();
        declareVTableSize = String.format("[%d x i8*]",noOfFunctions);
        // Fill VTable
        boolean flagForFirstArg = true;
        fillVTable = "[";
        // Iterate all class' methods
        Set<Map.Entry<String,MethodInfo>> st1 = ST.classes_data.get(currentClass).methods_data.entrySet();
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

  // Emit Main Method LLVM code
  public void Emit_MainMethodDefinition(){
    String CODE = "\n\ndefine i32 @main() {\n";

    BUFFER += CODE;
  }

  // Emit Main return llvm code
  public void Emit_MainReturn(){
    String CODE = "\n\tret i32 0\n";
    // Append to buffer
    BUFFER += CODE;
  }

  // Emit closing bracket
  public void Emit_RBRACK(){
    BUFFER += "\n}";
  }

  // Emit variable declaration in llvm
  public void Emit_VarDeclaration(String vtype,String vname){
    if( currentMethod != ""){
      String CODE = "\t%" + vname + " = alloca " + LLVM_type(vtype) + "\n";
      BUFFER += CODE;
    }
  }

  // Emit Method Definition LLVM code
  public void Emit_MethodDefinition(String methodName){
    String CODE = "";
    // Reset reg
    REG_NUM = 0;
    MethodInfo method_data = ST.classes_data.get(currentClass).methods_data.get(methodName);
    // Get method type in llvm
    String llvm_method_type = LLVM_type(method_data.type);
    String llvm_method_name = "@" + currentClass + "." + currentMethod;
    String llvm_method_args = "i8* %this";
    String llvm_args_alloc = "";
    // Iretate all method's arguments
    Set< Map.Entry <String,String> > st = method_data.arguments_data.entrySet();
    for (Map.Entry<String,String> cur:st){
      String argName = cur.getKey();
      String argType = cur.getValue();
      String llvm_arg_type = LLVM_type(argType);
      llvm_method_args += ", " + llvm_arg_type+ " %." + argName;
      llvm_args_alloc += "\t%" + argName + " = alloca " + llvm_arg_type + "\n";
      llvm_args_alloc += "\tstore " + llvm_arg_type + " " + "%." + argName;
      llvm_args_alloc += ", " + llvm_arg_type + "* " + "%" + argName + "\n";
    }
    CODE = String.format("\n\ndefine %s %s (%s) {\n %s",llvm_method_type,llvm_method_name,llvm_method_args,llvm_args_alloc);
    // Append code to buffer
    BUFFER += CODE;
  }

  // Emit a load llvm instruction that is going to be used in a primary expression
  public String Emit_LoadIdentifierForAPrimaryExpr(String identifier){
    String CODE;
    String identifierType = ST.GetVarType(identifier,currentClass,currentMethod);
    String new_reg = new_temp();
    String llvm_type = LLVM_type(identifierType);
    if(!(identifier.startsWith("%")))
      identifier = "%" + identifier;
    CODE = "\t" + new_reg + " = load " + llvm_type + ", " + llvm_type + "* " + identifier + "\n";
    // Append to buffer
    BUFFER += CODE;
    return new_reg;
  }

  // Emit calloc and llvm instructions so that a new object cant be created
  public String Emit_AllocationExpressionForAPrimaryExpr(String object){
    String CODE = "";
    String vtable_pointer = new_temp();
    int objectSizeInt = ST.classes_data.get(object).ClassSize + 8;
    String objectSize = Integer.toString(objectSizeInt);
    String calloc = "\t" + vtable_pointer + " = call i8* @calloc(i32 1, i32 " + objectSize +
     ")\n";
    String casted_pointer = new_temp();
    String bitcast = "\t" + casted_pointer + " = bitcast i8* " + vtable_pointer + " to i8***\n";
    int vtable_sz = ST.classes_data.get(object).methods_data.size();
    String elementptr = new_temp();
    String getelementptr = "\t" + elementptr + " = getelementptr [" + vtable_sz + " x i8*], [" + vtable_sz + " x i8*]* " + "@." + object + "_vtable, i32 0,i32 0\n";
    String store = "\tstore i8** " + elementptr + ", i8*** " + casted_pointer + "\n";
    CODE += calloc + bitcast + getelementptr + store;
    //Append to buffer
    BUFFER += CODE;
    return vtable_pointer;
  }

  // Emit llvm code that allocates space for an array
  public String Emit_ArrayAllocationExpression(String arrSize_reg){
    String CODE = "";
    // Check if array size is negative
    String icmp_Reg = new_temp();
    String icmp = "\t" + icmp_Reg + " = icmp slt i32 " + arrSize_reg + ", 0\n";
    // branch code
    String arr_err = new_label("arr_alloc");
    String arr_cont = new_label("arr_alloc");
    String br = "\tbr i1 " + icmp_Reg + ", label %" + arr_err + ", label %" + arr_cont + "\n";
    // emit code for the bad array alloc
    String bad_alloc = "\n" + arr_err + ":\n\tcall void @throw_oob()\n\tbr label %" + arr_cont + "\n";
    // emit code for the good array alloc
    String add_Reg = new_temp();
    String add = "\n" + arr_cont + ":\n\t" + add_Reg + " = add i32 " + arrSize_reg + ", 1\n";
    String calloc_reg = new_temp();
    String calloc = "\t" + calloc_reg + " = call i8* @calloc(i32 4, i32 " + add_Reg + ")\n";
    String casted_Reg = new_temp();
    String bitcast = "\t" + casted_Reg + " = bitcast i8* " + calloc_reg + " to i32*\n";
    String store = "\tstore i32 " + arrSize_reg + ", i32* " + casted_Reg + "\n";
    String good_alloc = add + calloc + bitcast + store;
    // Compine code
    CODE += icmp + br + bad_alloc + good_alloc;
    // Append code to buffer
    BUFFER += CODE;
    return casted_Reg;
  }

  // Emit function call before arguments
  public String Emit_FunctionCall(String callFrom,String method,String addressIndex_inVT){
    String CODE = "";
    String method_type = ST.classes_data.get(callFrom).methods_data.get(method).type;
    // Object position in V-table
    int positionInt = 0;
    // find elements position in vtable
    Set< Map.Entry <String,MethodInfo> > st_pos = ST.classes_data.get(callFrom).methods_data.entrySet();
    for (Map.Entry<String,MethodInfo> cur:st_pos){
      if(cur.getKey().equals(method))
        break;
      positionInt++;
    }
    String position = Integer.toString(positionInt);
    String funcPos_InVT = "\t; " + callFrom + "." + method + " : " + position + "\n";
    // Bitcast allocated pointer
    String castAlloced_Reg = new_temp();
    if(addressIndex_inVT.equals("this"))
      addressIndex_inVT = "%this";
    String bitcast = "\t" + castAlloced_Reg + " = bitcast i8* " + addressIndex_inVT + " to i8***\n";
    // Load casted register
    String loadCast_Reg = new_temp();
    String loadcastedRegister = "\t" + loadCast_Reg + " = load i8**, i8*** " + castAlloced_Reg + "\n";
    // Get element pointer
    String getElementPtr_Reg = new_temp();
    String getelementptr = "\t" + getElementPtr_Reg + " = getelementptr i8*, i8** " + loadCast_Reg + ",i32 " + position + "\n";
    // Load element
    String loadElement_Reg = new_temp();
    String loadelementRegister = "\t" + loadElement_Reg + " = load i8*, i8** " + getElementPtr_Reg + "\n";
    // Bitcast element so that we can call
    String castToCall_Reg = new_temp();
    String bitcastToCall = "\t" + castToCall_Reg + " = bitcast i8* " + loadElement_Reg + " to " + LLVM_type(method_type) + "(";
    // Iterate all arguments and append the type of each argument
    Set< Map.Entry <String,String> > st = ST.classes_data.get(callFrom).methods_data.get(method).arguments_data.entrySet();
    String llvm_argsType = "i8*";
    for (Map.Entry<String,String> cur:st){
       String argType = cur.getValue();
       llvm_argsType += "," + LLVM_type(argType);
    }
    bitcastToCall += llvm_argsType + ")*\n";
    // Append to CODE
    CODE += funcPos_InVT + bitcast + loadcastedRegister + getelementptr + loadelementRegister + bitcastToCall;
    // Append to BUFFER
    BUFFER += CODE;
    return castToCall_Reg;
  }

  // Emit the code that actually calls the method and return result
  public String Emit_ResultingCall(String callFrom,String method,String castToCall_Reg,String addressIndex_inVT,ArrayList arguments){
    String CODE = "";
    String method_type = ST.classes_data.get(callFrom).methods_data.get(method).type;
    // Call function and store result
   String result_Reg = new_temp();
   String callFunction = "\t" + result_Reg + " = call " + LLVM_type(method_type) + " " + castToCall_Reg + "(";
   // Append llvm arguments code
   if(!(addressIndex_inVT.startsWith("%")))
    addressIndex_inVT = "%" + addressIndex_inVT;
   String args_llvm = "i8* " + addressIndex_inVT;
   // Iterate all arguments and append the type of each argument
   int i=0;
   Set<Map.Entry <String,String>> st = ST.classes_data.get(callFrom).methods_data.get(method).arguments_data.entrySet();
   for (Map.Entry<String,String> cur:st){
      String argType = cur.getValue();
      args_llvm += "," + LLVM_type(argType) + " " + arguments.get(i);
      i++;
   }
   args_llvm += ")\n";
   CODE += callFunction + args_llvm;
   // Append code to buffer
   BUFFER += CODE;
   return result_Reg;
  }

  // Emit return llvm code
  public void Emit_MethodReturn(String type,String toReturn){
    String CODE = "\n\tret " + type + " " + toReturn + "\n";
    // Append to BUFFER
    BUFFER += CODE;
  }

  // Emit minus operation llvm code
  public String Emit_PlusOperation(String expr1,String expr2){
    String CODE = "";
    String r = new_temp();
    CODE += "\t" + r + " = add i32 " + expr1 + "," + expr2 + "\n";
    // Append code to buffer
    BUFFER += CODE;
    return r;
  }

  // Emit minus operation llvm code
  public String Emit_MinusOperation(String expr1,String expr2){
    String CODE = "";
    String r = new_temp();
    CODE += "\t" + r + " = sub i32 " + expr1 + "," + expr2 + "\n";
    // Append code to buffer
    BUFFER += CODE;
    return r;
  }

  // Emit times operation llvm code
  public String Emit_TimesOperation(String expr1,String expr2){
    String CODE = "";
    String r = new_temp();
    CODE += "\t" + r + " = mul i32 " + expr1 + "," + expr2 + "\n";
    // Append code to buffer
    BUFFER += CODE;
    return r;
  }

  // Emit compare operation llvm
  public String Emit_CompareOperation(String expr1,String expr2){
    String CODE = "";
    String r = new_temp();
    CODE += "\t" + r + " = icmp slt i32 " + expr1 + "," + expr2 + "\n";
    // Append code to buffer
    BUFFER += CODE;
    return r;
  }

  // Emit print statement in llvm
  public void Emit_PrintOperation(String toPrint){
    String CODE = "\tcall void (i32) @print_int(i32 " + toPrint + ")\n";
    // Append to buffer
    BUFFER += CODE;
  }

  // Emit if statement llvm code , 1rst part
  public void Emit_IfStatement(String iflbl,String elselbl,String reg){
    String CODE = "";
    // Emit br code
    CODE += "\n\tbr i1 " + reg + ", label %" + iflbl + ", label %" + elselbl + "\n";
    // Emit the label
    CODE += iflbl + ": \n";
    // Append code to buffer
    BUFFER += CODE;
  }

  // Emit br to end of code, after if statement
  public void Emit_NextStatement(String elselbl,String endlbl){
    String CODE = "";
    // Emit branching to end after statement
    CODE +=  "\n\tbr label %" + endlbl + "\n";
    // Emit code of next's statement
    CODE += elselbl + ":\n";
    // Append code to buffer
    BUFFER += CODE;
  }

  // Emit llvm code to load a class' field
  public String Emit_LoadClassField(String fieldName,String fieldType,String className){
    String CODE = "";
    int positionInVT_int = 8 + ST.classes_data.get(className).fields_offsets.get(fieldName);
    String positionInVT = Integer.toString(positionInVT_int);
    // getelementptr code
    String fieldPointerInVt_reg = new_temp();
    String getelementptr = "\t" + fieldPointerInVt_reg + " = getelementptr i8,i8* %this, " + LLVM_type(fieldType) + " " + positionInVT + "\n";
    // bitcast code
    String casted_Reg = new_temp();
    String bitcast = "\t" + casted_Reg + " = bitcast i8* " + fieldPointerInVt_reg + " to " + LLVM_type(fieldType) + "*\n";
    // Create the llvm code
    CODE += getelementptr + bitcast;
    // Append code to buffer
    BUFFER += CODE ;
    return casted_Reg;
  }

  // Emit assign operation in llvm to a class field
  public void Emit_AssignmentStatement_ToClassField(String expr,String fieldType,String dest){
    String CODE = "";
    CODE = "\tstore " + LLVM_type(fieldType) + " " + expr + ", " + LLVM_type(fieldType) + "* " + dest + "\n";
    // Append code to buffer
    BUFFER += CODE;
  }

  // Emit assign operation in llvm to a local method var
  public void Emit_AssignmentStatement_ToLocalVar(String des,String expr){
    String CODE = "";
    String statements_type_llvm = LLVM_type(ST.GetVarType(des,currentClass,currentMethod));
    CODE = "\tstore " + statements_type_llvm + " " + expr + "," + statements_type_llvm + "* %" + des + "\n";
    // Append code to buffer
    BUFFER += CODE;
  }

  // Emit array assignment operation in llvm for a class field
  public void Emit_ArrayAssignmentStatement_ToClassField(String index_reg,String index,String toBeAssigned){
    String CODE = "";
    String oob_good_lbl = new_label("oob");
    String oob_bad_lbl = new_label("oob");
    String oob_cont_lbl = new_label("oob");
    String load_index_reg = new_temp();
    String load_index = "\t" + load_index_reg + " = load i32*, i32** " + index_reg + "\n";
    String load_loaded_reg = new_temp();
    String load_again = "\t" + load_loaded_reg + " = load i32,i32* " + load_index_reg + "\n";
    String icmp_Reg = new_temp();
    String icmp = "\t" + icmp_Reg + " = icmp ult i32 0, " + load_loaded_reg + "\n";
    String br = "\tbr i1 " + icmp_Reg + ", label " + oob_good_lbl + ", label " + oob_bad_lbl + "\n";
    String oob_good = "\n" + oob_good_lbl + ":\n";
    String add_Reg = new_temp();
    String add = "\t" + add_Reg + " = add i32 " + index + ",1\n";
    String getElementPtr_Reg = new_temp();
    String getelementptr = "\t" + getElementPtr_Reg + " = getelementptr i32, i32* " + load_index_reg + ", i32 " + add_Reg + "\n";
    String store = "\tstore i32 " + toBeAssigned + ", i32* " + getElementPtr_Reg + "\n";
    String br2 = "\tbr label %" + oob_cont_lbl + "\n";
    String oob_bad = "\n" + oob_bad_lbl + ":\n\tcall void @throw_oob()\n\tbr label %" + oob_cont_lbl + "\n";
    String oob_cont = "\n" + oob_cont_lbl + ":\n\n";
    // Combine parts
    CODE += load_index + load_again + icmp + br + oob_good + add + getelementptr + store + br2 + oob_bad + oob_cont;
    // Append to buffer
    BUFFER += CODE;
  }

  // Log BUFFER
  public void Log_Buffer(){
    System.out.println(BUFFER);
  }

  // Write BUFFER's code to file
  public void Write_To_File(String fileName) throws FileNotFoundException {
    try{
      System.out.println("\n\nWriting code to " + fileName);
      PrintWriter out = new PrintWriter(fileName);
      out.println(BUFFER);
      out.close();
    }
    catch (FileNotFoundException ex){
      System.out.println(ex);
    }
  }
}
