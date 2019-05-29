import syntaxtree.*;
import visitor.GJDepthFirst;
import symboltable.*;
import java.util.*;

public class LWRVisitor extends GJDepthFirst <String,String> {
  String BUFFER = "";
  String Output;

  LWRVisitor(String Input){
    Output = Input.replaceAll("java","ll");
    System.out.println("\n\nGenerated Code Will reside " + Output + "\n");

    // Create the vtable

    BUFFER +=  "declare i8* @calloc(i32, i32)\ndeclare i32 @printf(i8*, ...)\ndeclare void @exit(i32)\n\n@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\ndefine void @print_int(i32 %i) {\n\t%_str = bitcast [4 x i8]* @_cint to i8*\n\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n\tret void\n}\n\ndefine void @throw_oob() {\n\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n\tcall i32 (i8*, ...) @printf(i8* %_str)\n\t\n\tcall void @exit(i32 1)\n\tret void\n}";

    System.out.println(BUFFER);
  }
}
