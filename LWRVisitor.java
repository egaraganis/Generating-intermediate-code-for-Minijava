import syntaxtree.*;
import visitor.GJDepthFirst;
import symboltable.*;
import java.util.*;
import symboltable.*;
import lowering.*;

public class LWRVisitor extends GJDepthFirst <String,String> {
  String Output;
  Lowering L;

  // Lowering visitor's constructor
  LWRVisitor(String Input,SymbolTable symbolTable){
    Output = Input.replaceAll("java","ll");
    System.out.println("\n\nGenerated Code Will reside " + Output + "\n");
    // Create the lowering class
    L = new Lowering(symbolTable);
    // Create the V-Tables for each class
    L.Emit_VTables();
    // Add Helper Functions
    L.Emit_HelperFunctions();
  }

  // get Lowering class
  public Lowering getL(){
    return L;
  }

  // Visit main method
  public String visit(MainClass n,String argu) {
    //System.out.println("We are in Main Class Declaration");
    String MainClassName = n.f1.accept(this,null);
    String MainName = "main";
    L.SetCurrentMethod(MainName);
    L.SetCurrentClass(MainClassName);
    L.Emit_MainMethodDefinition();
    // Visit VarDeclaration
    n.f14.accept(this,argu);
    // Visit Statement
    n.f15.accept(this,null);
    L.Emit_RBRACK();
    return "generated MainClass";
  }

  // Visit class declaration
  public String visit(ClassDeclaration n,String argu) {
    String className = n.f1.accept(this,null);
    L.SetCurrentMethod("");
    L.SetCurrentClass(className);
    // Visit VarDeclaration
    n.f3.accept(this,null);
    // Visit MethodDeclaration
    n.f4.accept(this,null);
    return "generated ClassDeclaration";
  }

  // Visit class extends declaration
  public String visit(ClassExtendsDeclaration n,String argu) {
    //System.out.println("We are in ClassExtends Declaration");
    String className = n.f1.accept(this,null);
    L.SetCurrentMethod("");
    L.SetCurrentClass(className);
    // Visit VarDeclaration
    n.f5.accept(this,null);
    // Visit MethodDeclaration
    n.f6.accept(this,null);
    return "generated ClassExtendsDeclaration";
  }

  // Visit var declaration
  public String visit(VarDeclaration n,String argu){
    System.out.println("We are in VarDeclaration");
    String vartype = n.f0.accept(this,null); // get var type
    String varname = n.f1.accept(this,null); // get var name
    L.Emit_VarDeclaration(vartype,varname);

    return "generated VarDeclaration";
  }

  // Visit method declaration
  public String visit(MethodDeclaration n, String argu){
    //System.out.println("We are in Method Declaration");
    String MethodName = n.f2.accept(this,null);
    L.SetCurrentMethod(MethodName);

    // Add LLVM method definition
    L.Emit_MethodDefinition(MethodName);

    // Visit VarDeclaration
    n.f7.accept(this,null);
    // Visit Statement
    n.f8.accept(this,null);
    // Visit Return Expression
    String returnType = n.f10.accept(this,null);

    L.Emit_RBRACK();
    return "generated MethodDeclaration";
  }

  // Visit integer type
  public String visit(IntegerType n, String argu) {
     return n.f0.toString();
  }

  // Visit int array type
  public String visit(ArrayType n, String argu) {
    return "int array";
  }

  // Visit boolean type
  public String visit(BooleanType n, String argu){
    return n.f0.toString();
  }

  // Visit identifier
  public String visit(Identifier n, String argu) {
     return n.f0.toString();
  }
}
