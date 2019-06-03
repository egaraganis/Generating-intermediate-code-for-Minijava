import syntaxtree.*;
import visitor.GJDepthFirst;
import symboltable.*;
import java.util.*;
import symboltable.*;
import lowering.*;

public class LWRVisitor extends GJDepthFirst <String,String> {
  String Output;
  Lowering L;
  Stack <ArrayList<String>> stacked_args; // stack method calling arguments

  // Helper function to get register from emit method
  private String GetReg(String expression_result){
    if(expression_result.contains(",")){
      String[] result = expression_result.split(",");
      return result[0];
    }
    else
      return expression_result;
  }

  // Helper function to get type from emit method
  private String GetType(String expression_result){
    if(expression_result.contains(",")){
      String[] result = expression_result.split(",");
      return result[1];
    }
    else
      return expression_result;
  }

  // Lowering visitor's constructor
  LWRVisitor(String Input,SymbolTable symbolTable){
    Output = Input.replaceAll("java","ll");
    System.out.println("\n\nGenerated Code Will reside " + Output + "\n");
    // Create the stack of arguments
    stacked_args = new Stack <ArrayList<String>>() ;
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

  // get output file name
  public String getOutput(){
    return Output;
  }

  // Visit main method declaration
  public String visit(MainClass n,String argu){
    //System.out.println("We are in Main Class Declaration");
    String MainClassName = n.f1.accept(this,null);
    String MainName = "main";
    L.SetCurrentMethod(MainName);
    L.SetCurrentClass(MainClassName);
    L.Emit_MainMethodDefinition(); // Emit code for the main method declaratio
    n.f14.accept(this,argu); // Visit VarDeclaration
    n.f15.accept(this,null); // Visit Statement
    L.Emit_MainReturn(); // Emit code for main return value
    L.Emit_RBRACK(); // Emit a closing bracket
    return "generated MainClass";
  }

  // Visit class declaration
  public String visit(ClassDeclaration n,String argu){
    //System.out.println("We are in Class Declaration");
    String className = n.f1.accept(this,null);
    L.SetCurrentMethod("");
    L.SetCurrentClass(className);
    n.f3.accept(this,null); // Visit VarDeclaration
    n.f4.accept(this,null); // Visit MethodDeclaration
    return "generated ClassDeclaration";
  }

  // Visit class extends declaration
  public String visit(ClassExtendsDeclaration n,String argu){
    //System.out.println("We are in ClassExtends Declaration");
    String className = n.f1.accept(this,null);
    L.SetCurrentMethod("");
    L.SetCurrentClass(className);
    n.f5.accept(this,null); // Visit VarDeclaration
    n.f6.accept(this,null); // Visit MethodDeclaration
    return "generated ClassExtendsDeclaration";
  }

  // Visit var declaration
  public String visit(VarDeclaration n,String argu){
    //System.out.println("We are in VarDeclaration");
    String vartype = n.f0.accept(this,null); // get var type
    String varname = n.f1.accept(this,null); // get var name
    L.Emit_VarDeclaration(vartype,varname);
    return "generated VarDeclaration";
  }

  // Visit method declaration
  public String visit(MethodDeclaration n, String argu){
    //System.out.println("We are in Method Declaration");
    String MethodName = n.f2.accept(this,null); // Visit identifier for method's name
    L.SetCurrentMethod(MethodName);
    L.Emit_MethodDefinition(MethodName); // Add LLVM method definition
    n.f7.accept(this,null); // Visit VarDeclaration
    n.f8.accept(this,null); // Visit Statement
    String returnType_Expr = n.f10.accept(this,null); // Visit Return Expression
    String retType = GetType(returnType_Expr); // get the return type
    if(returnType_Expr.matches("-?\\d+"))
      retType = "i32"; // if return is an integer then set up llvm type for ints
    L.Emit_MethodReturn(retType,GetReg(returnType_Expr)); // emit llvm code for method return operation
    L.Emit_RBRACK(); // Emit closing bracket
    return "generated MethodDeclaration";
  }

  // Visit message send
  public String visit(MessageSend n,String argu){
    //System.out.println("We are in MessageSend");
    stacked_args.push(new ArrayList<String>());
    String callFrom_primaryExpr = n.f0.accept(this,null); // Get primary expression of the caller
    String callFrom = GetType(callFrom_primaryExpr); // Get the object that calls the method
    String callerReg = GetReg(callFrom_primaryExpr); // Get the register that holds object's position
    if(callFrom.equals("this"))
      callFrom = L.GetCurrentClass();
    String method = n.f2.accept(this,null); // Get the identifier aka the method
    String bitcastedReg = L.Emit_FunctionCall(callFrom,method,callerReg); // Emit the function call
    String arguments_llvm = n.f4.accept(this,null); // Visit expression list aka the arguments of calling fun
    String result = L.Emit_ResultingCall(callFrom,method,bitcastedReg,callerReg,stacked_args.pop());
    return result;
  }

  // Visit expression list
  public String visit(ExpressionList n, String argu){
    //System.out.println("We are in ExpressionList");
    String firstParameter_PrimaryExpr = n.f0.accept(this,argu); // Visit expression
    stacked_args.peek().add(GetReg(firstParameter_PrimaryExpr));
    n.f1.accept(this,null); // Visit ExpressionTail
    return "generated expressionlistvisited";
  }

  // Visit expression term
  public String visit(ExpressionTerm n, String argu){
    //System.out.println("We are in ExpressionTerm");
    String anotherParameter_PrimaryExpr = n.f1.accept(this,null); // Visit expression
    stacked_args.peek().add(GetReg(anotherParameter_PrimaryExpr));
    return "ExpressionTermvisited";
  }

  // Visit assignment statement
  public String visit(AssignmentStatement n,String argu){
    //System.out.println("We are in AssignmentStatement");
    String Dest = n.f0.accept(this,null); // Visit Identifier
    String toAssign_Expression = n.f2.accept(this,null); // Visit Expression
    L.Emit_AssignmentStatement(Dest,GetReg(toAssign_Expression)); // Emit code for the assignment operation
    return "generated AssignmentStatementVisited";
  }

  // Visit if statement
  public String visit(IfStatement n,String argu){
    //System.out.println("We are in IfStatement");
    String ifLabel = L.new_label("if");
    String elseLabel = L.new_label("if");
    String endStatement = L.new_label("if");
    String conditionExpr = n.f2.accept(this,null); // Visit expression
    String condResult = GetReg(conditionExpr);
    L.Emit_IfStatement(ifLabel,elseLabel,condResult);
    n.f4.accept(this,null); // Visit statement of if
    L.Emit_NextStatement(elseLabel,endStatement);
    n.f6.accept(this,null); // Visit statement of else
    L.Emit_NextStatement(endStatement,endStatement);
    return "generated IfVisited";
  }

  // Visit print statement
  public String visit(PrintStatement n, String argu){
    //System.out.println("We are in PrintStatement");
    String toPrint_PrimaryExpr = n.f2.accept(this, argu); // visit experssion
    L.Emit_PrintOperation(GetReg(toPrint_PrimaryExpr)); // emit llvm code for the print statement
    return "generated PrintStatementVisited";
  }

  // Visit expression
  public String visit(Expression n,String argu){
    return n.f0.accept(this,null);
  }

  // Visit plus expression
  public String visit(PlusExpression n,String argu){
    //System.out.println("We are in plus expression");
    String left_PrimaryExpr,right_PrimaryExpr;
    left_PrimaryExpr = n.f0.accept(this,null);
    right_PrimaryExpr = n.f2.accept(this,null);
    String result = L.Emit_PlusOperation(GetReg(left_PrimaryExpr),GetReg(right_PrimaryExpr)); // Emit addition
    return result;
  }

  // Visit sub expression
  public String visit(MinusExpression n,String argu){
    //System.out.println("We are in min expression");
    String left_PrimaryExpr,right_PrimaryExpr;
    left_PrimaryExpr = n.f0.accept(this,null);
    right_PrimaryExpr = n.f2.accept(this,null);
    // Emit substraction
    String result = L.Emit_MinusOperation(GetReg(left_PrimaryExpr),GetReg(right_PrimaryExpr));
    return result;
  }

  // Visit times expression
  public String visit(TimesExpression n, String argu){
    //System.out.println("We are in TimesExpression");
    String left_PrimaryExpr,right_PrimaryExpr;
    left_PrimaryExpr = n.f0.accept(this,null);
    right_PrimaryExpr = n.f2.accept(this,null);
    // Emit multiplication
    String result = L.Emit_TimesOperation(GetReg(left_PrimaryExpr),GetReg(right_PrimaryExpr));
    return result;
  }

  // Visit compare expression
  public String visit(CompareExpression n,String argu){
    //System.out.println("We are in CompareExpression");
    String left_PrimaryExpr,right_PrimaryExpr;
    left_PrimaryExpr = n.f0.accept(this,null);
    right_PrimaryExpr = n.f2.accept(this,null);
    String result = L.Emit_CompareOperation(GetReg(left_PrimaryExpr),GetReg(right_PrimaryExpr));
    return result;
  }

  // Visit primary expression
  public String visit(PrimaryExpression n,String argu){
    //System.out.println("We are in PrimaryExpression");
    String primary_expr = n.f0.accept(this,null);
    if(primary_expr.equals("true"))
      return "true";
    else if(primary_expr.equals("false"))
      return "false";
    else if(primary_expr.matches("-?\\d+"))
      return primary_expr;
    else if(primary_expr.startsWith("/")){
      String obj = primary_expr.substring(1);
      String result = L.Emit_AllocationExpressionForAPrimaryExpr(obj);
      return result + "," + obj;
    }
    else if(primary_expr.equals("this")){
      return "this";
    }
    else if(primary_expr.startsWith("%"))
      return primary_expr;
    else{
      String register = L.Emit_LoadIdentifierForAPrimaryExpr(primary_expr);
      String ident_Type = L.GetSymbolTable().GetVarType(primary_expr,L.GetCurrentClass(),L.GetCurrentMethod());
      String ident_Type_LLVM = L.LLVM_type(ident_Type);
      return register + "," + ident_Type_LLVM;
    }
  }

  // Visit integer literal
  public String visit(IntegerLiteral n, String argu){
     return n.f0.toString();
  }

  // Visit true literal
  public String visit(TrueLiteral n, String argu){
     return "true";
  }

  // Visit false literal
  public String visit(FalseLiteral n, String argu){
     return "false";
  }

  // Visit identifier
  public String visit(Identifier n, String argu){
     return n.f0.toString();
  }

  // Visit this expression
  public String visit(ThisExpression n, String argu){
   return "this";
  }

  // Visit array allocation expression
  public String visit(ArrayAllocationExpression n, String argu){
    String typeOfArraySize = n.f3.accept(this,null);
    return "int array";
  }

  // Visit allocation expression
  public String visit(AllocationExpression n, String argu){
    //System.out.println("We are in AllocationExpression");
    String classObj = n.f1.accept(this,null);
    // add special characters, so we can now that a primary expression if an allocation expression
    return "/" + classObj;
  }

  // Visit integer type
  public String visit(IntegerType n, String argu){
     return n.f0.toString();
  }

  // Visit int array
  public String visit(ArrayType n, String argu){
    return "int array";
  }

  // Visit boolean type
  public String visit(BooleanType n, String argu){
    return n.f0.toString();
  }

  // Visit bracket expression
  public String visit(BracketExpression n, String argu){
    return n.f1.accept(this,null);
  }
}
