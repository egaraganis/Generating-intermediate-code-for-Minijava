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
  String CallFrom_MessageSend = null; // holds the caller

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

  // Helper function to get type of class field
  private String GetFieldType(String IsClassFieldResult){
    String[] result = IsClassFieldResult.split("/");
    return result[0];
  }

  // Helper function to get the class, field's belongs to
  private String GetFieldClass(String IsClassFieldResult){
    String[] result = IsClassFieldResult.split("/");
    return result[1];
  }

  // Lowering visitor's constructor
  LWRVisitor(String Input,SymbolTable symbolTable){
    Output = Input.replaceAll("java","ll");
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
    //System.out.println("we are in Main Class Declaration");
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
    //System.out.println("we are in Class Declaration");
    String className = n.f1.accept(this,null);
    L.SetCurrentMethod("");
    L.SetCurrentClass(className);
    n.f3.accept(this,null); // Visit VarDeclaration
    n.f4.accept(this,null); // Visit MethodDeclaration
    return "generated ClassDeclaration";
  }

  // Visit class extends declaration
  public String visit(ClassExtendsDeclaration n,String argu){
    //System.out.println("we are in ClassExtends Declaration");
    String className = n.f1.accept(this,null);
    L.SetCurrentMethod("");
    L.SetCurrentClass(className);
    n.f5.accept(this,null); // Visit VarDeclaration
    n.f6.accept(this,null); // Visit MethodDeclaration
    return "generated ClassExtendsDeclaration";
  }

  // Visit var declaration
  public String visit(VarDeclaration n,String argu){
    //System.out.println("we are in VarDeclaration");
    String vartype = n.f0.accept(this,null); // get var type
    String varname = n.f1.accept(this,null); // get var name
    L.Emit_VarDeclaration(vartype,varname);
    return "generated VarDeclaration";
  }

  // Visit method declaration
  public String visit(MethodDeclaration n, String argu){
    //System.out.println("we are in Method Declaration");
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
    //System.out.println("we are in MessageSend");
    stacked_args.push(new ArrayList<String>());
    String callFrom_primaryExpr = n.f0.accept(this,"getobj"); // Get primary expression of the caller
    String callFrom = this.CallFrom_MessageSend; // Get the object that calls the method
    String callerReg = GetReg(callFrom_primaryExpr); // Get the register that holds object's position
    if(callFrom.equals("this"))
      callFrom = L.GetCurrentClass();
    String method = n.f2.accept(this,null); // Get the identifier aka the method
    String bitcastedReg = L.Emit_FunctionCall(callFrom,method,callerReg); // Emit the function call
    String arguments_llvm = n.f4.accept(this,null); // Visit expression list aka the arguments of calling fun
    String result = L.Emit_ResultingCall(callFrom,method,bitcastedReg,callerReg,stacked_args.pop());
    String method_type;
    MethodInfo method_inf = L.GetSymbolTable().classes_data.get(callFrom).methods_data.get(method);
    if(method_inf == null){
      String searchResult = L.GetSymbolTable().SearchAncestors_ForMethod(callFrom,method);
      String[] resultTokenized = searchResult.split("/");
      method_type = resultTokenized[0];
      callFrom = resultTokenized[1];
    }
    else
      method_type = method_inf.type;
    return result + "," + L.LLVM_type(method_type);
  }

  // Visit expression list
  public String visit(ExpressionList n, String argu){
    //System.out.println("we are in ExpressionList");
    String firstParameter_PrimaryExpr = n.f0.accept(this,argu); // Visit expression
    stacked_args.peek().add(GetReg(firstParameter_PrimaryExpr));
    n.f1.accept(this,null); // Visit ExpressionTail
    return "generated expressionlistvisited";
  }

  // Visit expression term
  public String visit(ExpressionTerm n, String argu){
    //System.out.println("we are in ExpressionTerm");
    String anotherParameter_PrimaryExpr = n.f1.accept(this,null); // Visit expression
    stacked_args.peek().add(GetReg(anotherParameter_PrimaryExpr));
    return "ExpressionTermvisited";
  }

  // Visit assignment statement
  public String visit(AssignmentStatement n,String argu){
    //System.out.println("we are in AssignmentStatement");
    String Dest = n.f0.accept(this,null); // Visit Identifier
    // Check if destination is class field that we need to load
    String typeOfField = L.GetSymbolTable().IsClassField(Dest,L.GetCurrentClass(),L.GetCurrentMethod());
    if(typeOfField != null){
      //System.out.println("assigning to a class field");
      String toAssign_Expression = n.f2.accept(this,null); // Visit Expression
      String casted_Reg = L.Emit_LoadClassField(Dest,GetFieldType(typeOfField),GetFieldClass(typeOfField)); // Emit llvm code to load field
      L.Emit_AssignmentStatement_ToClassField(GetReg(toAssign_Expression),GetFieldType(typeOfField),casted_Reg);
    }
    else{
      //System.out.println("assigning to a local var");
      String toAssign_Expression = n.f2.accept(this,null); // Visit Expression
      L.Emit_AssignmentStatement_ToLocalVar(Dest,GetReg(toAssign_Expression)); // Emit code for the assignment operation
    }
    return "generated AssignmentStatementVisited";
  }

  // Visit array assignment statement
  public String visit(ArrayAssignmentStatement n,String argu){
    //System.out.println("we are in ArrayAssignmentStatement");
    String ArrDest_Ident = n.f0.accept(this,null); // visit identifier
    // Check if destination is class field that we need to load
    String classField = L.GetSymbolTable().IsClassField(ArrDest_Ident,L.GetCurrentClass(),L.GetCurrentMethod());
    if(classField != null){
      //System.out.println("assigning to a field array");
      String casted_Reg = L.Emit_LoadClassField(ArrDest_Ident,"int array",GetFieldClass(classField)); // Emit llvm code to load field
      String typeOfIndex_Expr = n.f2.accept(this,null); // visit index expression
      String typeOfExpr_Expr = n.f5.accept(this,null); // visit the value expression to be
      L.Emit_ArrayAssignmentStatement_ToClassField(casted_Reg,GetReg(typeOfIndex_Expr),GetReg(typeOfExpr_Expr));
    }
    else{
      //System.out.println("assigning to a local array");
      String typeOfIndex_Expr = n.f2.accept(this,null); // visit index expression
      String typeOfExpr_Expr = n.f5.accept(this,null); // visit the value expression to be
      //L.Emit_ArrayAssignmentStatement_ToLocalVar(Dest,GetReg(toAssign_Expression)); // Emit code for the assignment operation
    }
    String typeOfIndex_Expr = n.f2.accept(this,null); // visit index expression
    String typeOfExpr_Expr = n.f5.accept(this,null); // visit the value expression to be assigned
    return "generatd ArrayAssignmentStatementVisited";
  }

  // Visit if statement
  public String visit(IfStatement n,String argu){
    //System.out.println("we are in IfStatement");
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

  // Visit while statement
  public String visit(WhileStatement n, String argu){
    //System.out.println("we are in WhileStatement");
    String start_label = L.Emit_WhileStatement_StartOfLoop();
    String condition_Expr = n.f2.accept(this,null); // Visit expression
    String end_label = L.Emit_WhileStatement_AfterCondition(GetReg(condition_Expr));
    n.f4.accept(this,null); // Visit statement of while
    L.Emit_WhileStatement_EndOfLoop(start_label,end_label);
    return "generated WhileStatementVisited";
  }

  // Visit print statement
  public String visit(PrintStatement n, String argu){
    //System.out.println("we are in PrintStatement");
    String toPrint_PrimaryExpr = n.f2.accept(this, argu); // visit experssion
    L.Emit_PrintOperation(GetReg(toPrint_PrimaryExpr),GetType(toPrint_PrimaryExpr)); // emit llvm code for the print statement
    return "generated PrintStatementVisited";
  }

  // Visit expression
  public String visit(Expression n,String argu){
    return n.f0.accept(this,null);
  }

  // Visit and expression
  public String visit(AndExpression n,String argu){
    //System.out.println("we are in AndExpression");
    String andlbl1 = L.new_label("andclause");
    String andlbl2 = L.new_label("andclause");
    String andlbl3 = L.new_label("andclause");
    String andlbl4 = L.new_label("andclause");
    String leftClause_expr = n.f0.accept(this,null);
    L.Emit_AndOperation_Start(GetReg(leftClause_expr),andlbl1,andlbl2,andlbl4);
    String rightClause_expr = n.f2.accept(this,null);
    String result = L.Emit_AndOperation_End(GetReg(rightClause_expr),andlbl1,andlbl3,andlbl4);
    return result + "," + "i32";
  }

  // Visit plus expression
  public String visit(PlusExpression n,String argu){
    //System.out.println("we are in plus expression");
    String left_PrimaryExpr,right_PrimaryExpr;
    left_PrimaryExpr = n.f0.accept(this,null);
    right_PrimaryExpr = n.f2.accept(this,null);
    String result = L.Emit_PlusOperation(GetReg(left_PrimaryExpr),GetReg(right_PrimaryExpr)); // Emit addition
    return result + "," + "i32";
  }

  // Visit sub expression
  public String visit(MinusExpression n,String argu){
    //System.out.println("we are in min expression");
    String left_PrimaryExpr,right_PrimaryExpr;
    left_PrimaryExpr = n.f0.accept(this,null);
    right_PrimaryExpr = n.f2.accept(this,null);
    // Emit substraction
    String result = L.Emit_MinusOperation(GetReg(left_PrimaryExpr),GetReg(right_PrimaryExpr));
    return result + "," + "i32";
  }

  // Visit times expression
  public String visit(TimesExpression n, String argu){
    //System.out.println("we are in TimesExpression");
    String left_PrimaryExpr,right_PrimaryExpr;
    left_PrimaryExpr = n.f0.accept(this,null);
    right_PrimaryExpr = n.f2.accept(this,null);
    // Emit multiplication
    String result = L.Emit_TimesOperation(GetReg(left_PrimaryExpr),GetReg(right_PrimaryExpr));
    return result + "," + "i32";
  }

  // Visit compare expression
  public String visit(CompareExpression n,String argu){
    //System.out.println("we are in CompareExpression");
    String left_PrimaryExpr,right_PrimaryExpr;
    left_PrimaryExpr = n.f0.accept(this,null);
    right_PrimaryExpr = n.f2.accept(this,null);
    String result = L.Emit_CompareOperation(GetReg(left_PrimaryExpr),GetReg(right_PrimaryExpr));
    return result + "," + "i32";
  }

  // Visit array look up
  public String visit(ArrayLookup n,String argu){
    //System.out.println("we are in ArrayLookup");
    String theArray_expr = n.f0.accept(this,null); // Visit primary_expr
    String theIndex_expr = n.f2.accept(this,null); // Visit primary_expr
    String result = L.Emit_ArrayLookUpOperation(GetReg(theArray_expr),GetReg(theIndex_expr));
    return result + "," + "i32";
  }

  // Visit array length
  public String visit(ArrayLength n,String argu){
    ////System.out.println("we are in ArrayLength");
    String theArray_expr = n.f0.accept(this,null);
    String result = L.Emit_LengthOperation(GetReg(theArray_expr));
    return result + "," + "i32";
  }

  // Visit clause
  public String visit(Clause n,String argu){
    return n.f0.accept(this,null);
  }

  // Visit primary expression
  public String visit(PrimaryExpression n,String argu){
    //System.out.println("we are in PrimaryExpression");
    String primary_expr = n.f0.accept(this,argu);
    // If TrueLiteral return true
    if(primary_expr.equals("1,i1"))
      return primary_expr;
    // If FalseLiteral return false
    else if(primary_expr.equals("0,i1"))
      return primary_expr;
    // If integer return the interger
    else if(primary_expr.matches("-?\\d+"))
      return primary_expr;
    // If new allocation expression, create object
    else if(primary_expr.startsWith("/")){
      String obj = primary_expr.substring(1);
      String result = L.Emit_AllocationExpressionForAPrimaryExpr(obj);
      if(argu != null)
        this.CallFrom_MessageSend = obj;
      return result + "," + L.LLVM_type(obj);
    }
    // If new array allocation expression, create array
    else if(primary_expr.startsWith("[")){
      String arrSize_reg = primary_expr.substring(1);
      String casted_Reg  = L.Emit_ArrayAllocationExpression(GetReg(arrSize_reg));
      return casted_Reg + "," + "i32*";
    }
    // If "this" expression return "this"
    else if(primary_expr.equals("this")){
      return "this";
    }
    // If we already got the register then proceed to return to upper level
    else if(primary_expr.startsWith("%"))
      return primary_expr;
    // If we have an identifier, load it
    else{
      // Check if destination is class field that we need to load
      String classField = L.GetSymbolTable().IsClassField(primary_expr,L.GetCurrentClass(),L.GetCurrentMethod());
      if(classField != null){
        //System.out.println("assigning to a field array");
        String casted_Reg = L.Emit_LoadClassField(primary_expr,GetFieldType(classField),GetFieldClass(classField)); // Emit llvm code to load field
        String register = L.Emit_LoadIdentifierForAPrimaryExpr(casted_Reg,GetFieldType(classField));
        if(argu != null)
          this.CallFrom_MessageSend = GetFieldType(classField);
        return register + "," + L.LLVM_type(GetFieldType(classField));
      }
      else{
        String register = L.Emit_LoadIdentifierForAPrimaryExpr(primary_expr);
        String ident_Type = L.GetSymbolTable().GetVarType(primary_expr,L.GetCurrentClass(),L.GetCurrentMethod());
        String ident_Type_LLVM = L.LLVM_type(ident_Type);
        if(argu != null)
          this.CallFrom_MessageSend = ident_Type;
        return register + "," + ident_Type_LLVM;
      }
    }
  }

  // Visit not expression
  public String visit(NotExpression n, String argu){
    //System.out.println("we are in NotExpression");
    String clause_expr = n.f1.accept(this,null);
    String result = L.Emit_NotOperation(GetReg(clause_expr));
    return result;
  }

  // Visit integer literal
  public String visit(IntegerLiteral n, String argu){
     return n.f0.toString();
  }

  // Visit true literal
  public String visit(TrueLiteral n, String argu){
     return "1,i1";
  }

  // Visit false literal
  public String visit(FalseLiteral n, String argu){
     return "0,i1";
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
    // System.out.println("we are in ArrayAllocationExpression");
    String typeOfArraySize = n.f3.accept(this,null); // Visit Expression
    return "[" + typeOfArraySize; // "[" indicates that we have an array allocation expresssion
  }

  // Visit allocation expression
  public String visit(AllocationExpression n, String argu){
    //System.out.println("we are in AllocationExpression");
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
