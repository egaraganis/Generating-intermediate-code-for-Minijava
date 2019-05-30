import syntaxtree.*;
import visitor.GJDepthFirst;
import symboltable.*;
import java.util.*;
import symboltable.*;
import lowering.*;

public class LWRVisitor extends GJDepthFirst <String,String> {
  String Output;
  SymbolTable ST;
  Lowering L;

  LWRVisitor(String Input,SymbolTable symbolTable){
    Output = Input.replaceAll("java","ll");
    ST = symbolTable;
    System.out.println("\n\nGenerated Code Will reside " + Output + "\n");

    // Create the lowering class
    L = new Lowering();

    // Create the V-Tables for each class
    L.Emit_VTables(ST.classes_data);

    // Add Helper Functions
    L.Emit_HelperFunctions();

    L.Log_Buffer();
  }


}
