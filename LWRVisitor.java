import syntaxtree.*;
import visitor.GJDepthFirst;
import symboltable.*;
import java.util.*;
import lowering.*;

public class LWRVisitor extends GJDepthFirst <String,String> {
  String Output;
  Lowering L;

  LWRVisitor(String Input){
    Output = Input.replaceAll("java","ll");
    System.out.println("\n\nGenerated Code Will reside " + Output + "\n");

    // Create the lowering class
    L = new Lowering();

    // Create the V-Table

    // Add Helper Functions
    L.Emit_HelperFunctions();

    L.Log_Buffer();
  }
}
