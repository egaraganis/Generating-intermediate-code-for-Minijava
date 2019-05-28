import syntaxtree.*;
import visitor.GJDepthFirst;
import symboltable.*;
import typecheck.*;
import staticheckingexception.*;
import java.util.*;

public class TCVisitor extends GJDepthFirst <String,String> {
  TypeCheck TC; // our type checker
  Stack <ArrayList<String>> stacked_args; // stack method calling arguments

  TCVisitor(SymbolTable symbolTable){
    TC = new TypeCheck(symbolTable);
    stacked_args = new Stack <ArrayList<String>>() ;
  }

  public TypeCheck getTypeCheck(){
    return this.TC;
  }

  public String visit(MainClass n,String argu) {
    //System.out.println("We are in Main Class Declaration");
    String MainClassName = n.f1.accept(this,null);
    String MainName = "main";
    TC.SetCurrentMethod(MainName);
    TC.SetCurrentClass(MainClassName);
    // Visit VarDeclaration
    n.f14.accept(this,argu);
    // Visit Statement
    n.f15.accept(this,null);
    return "MainClassVisited";
  }

  public String visit(ClassDeclaration n,String argu) {
    //System.out.println("We are in Class Declaration");
    String className = n.f1.accept(this,null);
    TC.SetCurrentMethod("");
    TC.SetCurrentClass(className);
    // Visit VarDeclaration
    n.f3.accept(this,null);
    // Visit MethodDeclaration
    n.f4.accept(this,null);
    return "ClassVisited";
  }

  public String visit(ClassExtendsDeclaration n,String argu) {
    //System.out.println("We are in ClassExtends Declaration");
    String className = n.f1.accept(this,null);
    TC.SetCurrentMethod("");
    TC.SetCurrentClass(className);
    // Visit VarDeclaration
    n.f5.accept(this,null);
    // Visit MethodDeclaration
    n.f6.accept(this,null);
    return "ClassExtendsVisited";
  }

  public String visit(MethodDeclaration n,String argu) throws StatiCheckingException
  {
    //System.out.println("We are in Method Declaration");
    String MethodName = n.f2.accept(this,null);
    TC.SetCurrentMethod(MethodName);
    // Visit Type
    n.f1.accept(this,null);
    // Visit FormalParameterList
    n.f4.accept(this,null);
    // Visit VarDeclaration
    n.f7.accept(this,null);
    // Visit Statement
    n.f8.accept(this,null);
    // Visit Return Expression
    String returnType = n.f10.accept(this,null);
    TC.CheckReturnType(returnType);
    return "MethodDeclarationVisited";
  }

  public String visit(AssignmentStatement n,String argu) throws StatiCheckingException
  {
    //System.out.println("We are in AssignmentStatement");
    String Dest = n.f0.accept(this,null);
    // Visit Expression
    String typeOfExpr = n.f2.accept(this,null);
    TC.CheckTypeCompatibility(Dest,typeOfExpr);
    return "AssignmentStatementVisited";
  }

  public String visit(ArrayAssignmentStatement n,String argu) throws StatiCheckingException
  {
    //System.out.println("We are in ArrayAssignmentStatement");
    String ArrDest = n.f0.accept(this,null);
    String typeOfIndex = n.f2.accept(this,null);
    String typeOfExpr = n.f5.accept(this,null);
    TC.CheckArrayAssignment(ArrDest,typeOfIndex,typeOfExpr);
    return "ArrayAssignmentStatementVisited";
  }

  public String visit(IfStatement n,String argu) throws StatiCheckingException
  {
    //System.out.println("We are in IfStatement");
    String conditionType = n.f2.accept(this,null);
    TC.CheckConditionType(conditionType);
    // Visit statement of if
    n.f4.accept(this,null);
    // Visit statement of else
    n.f6.accept(this,null);
    return "IfVisited";
  }

  public String visit(WhileStatement n, String argu) throws StatiCheckingException
  {
    //System.out.println("We are in WhileStatement");
    String conditionType = n.f2.accept(this,null);
    TC.CheckConditionType(conditionType);
    // Visit statement of if
    n.f4.accept(this,null);
    return "WhileStatementVisited";
  }

  public String visit(PrintStatement n, String argu) throws StatiCheckingException
  {
    //System.out.println("We are in PrintStatement");
    String toPrintType = n.f2.accept(this, argu);
    TC.CheckPrintStatement(toPrintType);
    return "PrintStatementVisited";
  }

  public String visit(Type n, String argu) throws StatiCheckingException
  {
    //System.out.println("We are in Type");
    String type = n.f0.accept(this,null);
    TC.IsTypeAllowed(type);
    return "TypeVisited";
  }

  public String visit(Expression n, String argu){
    String gotFromExpression =  n.f0.accept(this, argu);
    String type = gotFromExpression;
    // if expression is this then it refers to current class
    if(gotFromExpression == "this")
      type = TC.GetCurrentClass();
    // if expression returns a string starting with "/", then a new allocation expression occured of type /<type>
    else if(gotFromExpression.startsWith("/"))
      type = gotFromExpression.substring(1);
    // if expression returns an intetifier
    else if( !gotFromExpression.startsWith("/") && gotFromExpression != "int" && gotFromExpression != "boolean" && gotFromExpression != "int array" && !(TC.ST.classes_data.containsKey(gotFromExpression)) ){
      type = TC.GetVarType(gotFromExpression);
    }
    //System.out.println(type);
    return type;
  }

  public String visit(AndExpression n,String argu) throws StatiCheckingException
  {
    //System.out.println("We are in AndExpression");
    String leftClause = n.f0.accept(this,null);
    String rightClause = n.f2.accept(this,null);
    TC.CheckAndOperation(leftClause,rightClause);
    return "boolean";
  }

  public String visit(CompareExpression n,String argu) throws StatiCheckingException
  {
    //System.out.println("We are in CompareExpression");
    String leftPrimaryExpr,rightPrimaryExpr;
    leftPrimaryExpr = n.f0.accept(this,null);
    rightPrimaryExpr = n.f2.accept(this,null);
    TC.CheckArithmeticExpression(leftPrimaryExpr,rightPrimaryExpr);
    return "boolean";
  }

  public String visit(PlusExpression n, String argu) throws StatiCheckingException
  {
    //System.out.println("We are in PlusExpression");
    String leftPrimaryExpr,rightPrimaryExpr;
    leftPrimaryExpr = n.f0.accept(this,null);
    rightPrimaryExpr = n.f2.accept(this,null);
    TC.CheckArithmeticExpression(leftPrimaryExpr,rightPrimaryExpr);
    return "int";
  }

  public String visit(MinusExpression n, String argu) throws StatiCheckingException
  {
    //System.out.println("We are in MinusExpression");
    String leftPrimaryExpr,rightPrimaryExpr;
    leftPrimaryExpr = n.f0.accept(this,null);
    rightPrimaryExpr = n.f2.accept(this,null);
    TC.CheckArithmeticExpression(leftPrimaryExpr,rightPrimaryExpr);
    return "int";
  }

  public String visit(TimesExpression n, String argu) throws StatiCheckingException
  {
    //System.out.println("We are in TimesExpression");
    String leftPrimaryExpr,rightPrimaryExpr;
    leftPrimaryExpr = n.f0.accept(this,null);
    rightPrimaryExpr = n.f2.accept(this,null);
    TC.CheckArithmeticExpression(leftPrimaryExpr,rightPrimaryExpr);
    return "int";
  }

  public String visit(ArrayLookup n,String argu) throws StatiCheckingException
  {
    //System.out.println("We are in ArrayLookup");
    String theArray = n.f0.accept(this,null);
    String theIndex = n.f2.accept(this,null);
    TC.CheckArrayLookUp(theArray,theIndex);
    return "int";
  }

  public String visit(ArrayLength n,String argu) throws StatiCheckingException
  {
    //System.out.println("We are in ArrayLookup");
    String theArray = n.f0.accept(this,null);
    TC.CheckArrayLength(theArray);
    return "int";
  }

  public String visit(MessageSend n,String argu) throws StatiCheckingException
  {
    //System.out.println("We are in MessageSend");
    stacked_args.push(new ArrayList<String>());
    String callFrom = n.f0.accept(this,null);
    String method = n.f2.accept(this,null);
    // Visit EpxressionList
    n.f4.accept(this,null);
    String methodType = TC.CheckMessageSend(callFrom,method,stacked_args.pop());
    return methodType;
  }

  public String visit(ExpressionList n, String argu) {
    //System.out.println("We are in ExpressionList");
    String firstParameter = n.f0.accept(this,argu);
    stacked_args.peek().add(firstParameter);
    // Visit ExpressionTail
    n.f1.accept(this,null);
    return "expressionlistvisited";
  }

  public String visit(ExpressionTerm n, String argu) {
    //System.out.println("We are in ExpressionTerm");
    String anotherParameter = n.f1.accept(this,null);
    stacked_args.peek().add(anotherParameter);
    return "ExpressionTermvisited";
  }

  public String visit(IntegerLiteral n, String argu) {
     return "int";
  }

  public String visit(TrueLiteral n, String argu) {
     return "boolean";
  }

  public String visit(FalseLiteral n, String argu) {
     return "boolean";
  }

  public String visit(Identifier n, String argu) {
     return n.f0.toString();
  }

  public String visit(ThisExpression n, String argu) {
   return "this";
  }

  public String visit(ArrayAllocationExpression n, String argu) {
    String typeOfArraySize = n.f3.accept(this,null);
    TC.CheckTypeOfArraySize(typeOfArraySize);
    return "int array";
  }

  public String visit(AllocationExpression n, String argu) throws StatiCheckingException
  {
    //System.out.println("We are in AllocationExpression");
    String classObj = n.f1.accept(this,null);
    TC.IsClassDeclared(classObj);
    // add special characters, so we can now that a primary expression if an allocation expression
    return "/" + classObj;
  }

  public String visit(NotExpression n, String argu) throws StatiCheckingException
  {
    //System.out.println("We are in AllocationExpression");
    String clause = n.f1.accept(this,null);
    TC.CheckNotOperation(clause);
    return "boolean";
  }

  public String visit(IntegerType n, String argu) {
     return n.f0.toString();
  }

  public String visit(ArrayType n, String argu) {
    return "int array";
  }

  public String visit(BracketExpression n, String argu) {
    return n.f1.accept(this,null);
  }

  public String visit(BooleanType n, String argu){
    return n.f0.toString();
  }
}
