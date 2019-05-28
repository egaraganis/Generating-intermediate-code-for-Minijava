package typecheck;
import symboltable.*;
import staticheckingexception.*;
import java.util.*;

public class TypeCheck{
  public SymbolTable ST;
  String currentClass="";
  String currentMethod="";

  // Constructor
  public TypeCheck(SymbolTable symbolTable){
    ST = symbolTable;
  }

  public void SetCurrentMethod(String curMethod){
    currentMethod = curMethod;
  }

  public void SetCurrentClass(String curClass){
    currentClass = curClass;
  }

  public String GetCurrentMethod(){
    return this.currentMethod;
  }

  public String GetCurrentClass(){
    return this.currentClass;
  }

  // Check that variable has been declared
  public boolean IsVarDeclared(String var) throws StatiCheckingException
  {
    boolean declaredAsField = ST.classes_data.get(currentClass).class_variables_data.containsKey(var);
    boolean declaredAsMethodVar=false;
    boolean declaredAsArgument=false;
    if(currentMethod != ""){
      declaredAsMethodVar = ST.classes_data.get(currentClass).methods_data.get(currentMethod).method_variables_data.containsKey(var);
      declaredAsArgument = ST.classes_data.get(currentClass).methods_data.get(currentMethod).arguments_data.containsKey(var);
    }
    boolean declaredAsFieldInSuperclass = false;
    if(SearchAncestors(currentClass,var) != "")
      declaredAsFieldInSuperclass = true;
    if( !(declaredAsField || declaredAsArgument || declaredAsMethodVar || declaredAsFieldInSuperclass) )
      throw new StatiCheckingException("\n     ✗ Var " + var + " in method " + this.currentMethod + " of class " + this.currentClass + " has not been declared");
    return true;
  }

  // Check if class has been declared
  public boolean IsClassDeclared(String className) throws StatiCheckingException
  {
    if( !(ST.classes_data.containsKey(className)) )
      throw new StatiCheckingException("\n     ✗ There is no class object " + className + " in method " + this.currentMethod + " of class " + this.currentClass);
    return true;
  }

  // Return variable's type that have been declared to an ancestor
  public String SearchAncestors(String className,String var){
    String superclass = ST.classes_data.get(className).extendsFrom;
    while(superclass != ""){
      String type = ST.classes_data.get(superclass).class_variables_data.get(var);
      if(type != null)
        return type;
      superclass = ST.classes_data.get(superclass).extendsFrom;
    }
    return "";
  }

  // Return given variable's type
  public String GetVarType(String var){
    IsVarDeclared(var);
    String typeGotFromField,typeGotFromMethodVar,typeGotFromMethodArg,typeGotFromSuperField=null;
    if(currentMethod != ""){
      typeGotFromMethodVar = ST.classes_data.get(currentClass).methods_data.get(currentMethod).method_variables_data.get(var);
      if(typeGotFromMethodVar != null)
      return typeGotFromMethodVar;

      typeGotFromMethodArg = ST.classes_data.get(currentClass).methods_data.get(currentMethod).arguments_data.get(var);
      if(typeGotFromMethodArg != null)
      return typeGotFromMethodArg;
    }
    typeGotFromField = ST.classes_data.get(currentClass).class_variables_data.get(var);
    if(typeGotFromField != null)
      return typeGotFromField;

    typeGotFromSuperField = SearchAncestors(currentClass,var);
    if(typeGotFromSuperField != "")
      return typeGotFromSuperField;
    return "error";
  }

  // Check if variable is an existing class
  public String IsVarDeclaredClass(String var){
    String type = GetVarType(var);
    if( type == "int" || type == "int array" || type == "boolean" )
      throw new StatiCheckingException("\n     ✗ Var " + var + " in method " + this.currentMethod + " of class " + this.currentClass + " must be a declared class");
    return type;
  }

  // Check if variable is an array
  public void IsVarArray(String var) throws StatiCheckingException
  {
    // get the type of the var
    String type = GetVarType(var);
    // check if type is int array
    if(type != "int array")
      throw new StatiCheckingException("\n     ✗ Var " + var + " in method " + this.currentMethod + " of class " + this.currentClass + " isn't an int array");
  }

  // Check if variable is int
  public void IsVarInt(String var) throws StatiCheckingException
  {
    String type = GetVarType(var);
    // check if type is int
    if(type != "int" )
      throw new StatiCheckingException("\n     ✗ Var " + var + " in method " + this.currentMethod + " of class " + this.currentClass + " isn't an int");
  }

  // Check if variable is a boolean
  public void IsVarBoolean(String var) throws StatiCheckingException
  {
    String type = GetVarType(var);
    // check if type is boolean
    if(type != "boolean")
      throw new StatiCheckingException("\n     ✗ Var " + var + " in method " + this.currentMethod + " of class " + this.currentClass + " isn't a boolean");
  }

  // Check that type is allowed
  public void IsTypeAllowed(String type) throws StatiCheckingException
  {
    // check if type is one of the classes declared
    if( !(ST.classes_data.containsKey(type)) ){
      // check if type is one of the basic types
      if(type != "int" && type != "boolean" && type != "int array"){
        if(this.currentMethod == "")
          throw new StatiCheckingException("\n     ✗ Illegal type " + type + " in class " + this.currentClass);
        else
          throw new StatiCheckingException("\n     ✗ Illegal type " + type + " in method " + this.currentMethod + " of class " + this.currentClass);
      }
    }
  }

  // Check if NOT operation is allowed for the given clause
  public void CheckNotOperation(String clause) throws StatiCheckingException
  {
    if(clause == "int" || clause == "this" || clause == "int array" || clause.startsWith("/") || ST.classes_data.containsKey(clause) )
      throw new StatiCheckingException("\n     ✗ Illegal NOT operation in class " + this.currentClass + " of method " + this.currentMethod + ", clause must be of type boolean");
    if(clause != "boolean"){
      this.IsVarBoolean(clause);
    }
  }

  // Check if AND operation is allowed for the given clause
  public void CheckAndOperation(String lClause,String rClause) throws StatiCheckingException
  {
    // check left clause
    if(lClause == "int" || lClause == "this" || lClause == "int array" || lClause.startsWith("/") || ST.classes_data.containsKey(lClause) )
      throw new StatiCheckingException("\n     ✗ Illegal AND operation in class " + this.currentClass + " of method " + this.currentMethod + ", clause must be of type boolean");
    if(lClause != "boolean"){
      this.IsVarBoolean(lClause);
    }
    // check right clause
    if(rClause == "int" || rClause == "this" || rClause == "int array" || rClause.startsWith("/") || ST.classes_data.containsKey(rClause))
      throw new StatiCheckingException("\n     ✗ Illegal AND operation in class " + this.currentClass + " of method " + this.currentMethod + ", clause must be of type boolean");
    if(rClause != "boolean"){
      this.IsVarBoolean(rClause);
    }
  }

  // Check if Compare Expression is allowed for the given primary expressions
  public void CheckArithmeticExpression(String lPrimaryExpr,String rPrimaryExpr) throws StatiCheckingException
  {
    // check left primary expression
    if(lPrimaryExpr == "boolean" || lPrimaryExpr == "this" || lPrimaryExpr == "int array" || lPrimaryExpr.startsWith("/") || ST.classes_data.containsKey(lPrimaryExpr) )
      throw new StatiCheckingException("\n     ✗ Illegal COMPARE operation in class " + this.currentClass + " of method " + this.currentMethod + ", expression must be of type int");
    if(lPrimaryExpr != "int"){
      this.IsVarInt(lPrimaryExpr);
    }

    // check right primary expression
    if(rPrimaryExpr == "boolean" || rPrimaryExpr == "this" || rPrimaryExpr == "int array" || rPrimaryExpr.startsWith("/") || ST.classes_data.containsKey(rPrimaryExpr) )
      throw new StatiCheckingException("\n     ✗ Illegal COMPARE operation in class " + this.currentClass + " of method " + this.currentMethod + ", expression must be of type int");
    if(rPrimaryExpr != "int"){
      this.IsVarInt(rPrimaryExpr);
    }
  }

  // Check if the array lookup operation is legal
  public void CheckArrayLookUp(String Arr,String Index) throws StatiCheckingException
  {
    // check if the first expr is an array
    if(Arr == "boolean" || Arr == "this" || Arr.startsWith("/") || Arr == "int" || ST.classes_data.containsKey(Arr) )
      throw new StatiCheckingException("\n     ✗ Illegal array look up in class " + this.currentClass + " of method " + this.currentMethod + ", expression must be of type array");
    if(Arr != "int array"){
      this.IsVarArray(Arr);
    }
    // check if the  index is int
    if(Index == "boolean" || Index == "this" || Index == "int array" || Index.startsWith("/") )
      throw new StatiCheckingException("\n     ✗ Illegal array look up in class " + this.currentClass + " of method " + this.currentMethod + ", expression must be of type int");
    if(Index != "int"){
      this.IsVarInt(Index);
    }
  }

  // Check if the array length operation is legal
  public void CheckArrayLength(String Arr) throws StatiCheckingException
  {
    // check if the first expr is an array
    if(Arr == "boolean" || Arr == "this" || Arr.startsWith("/") || Arr == "int" || ST.classes_data.containsKey(Arr) )
      throw new StatiCheckingException("\n     ✗ Illegal array length operation in class " + this.currentClass + " of method " + this.currentMethod + ", expression must be of type array");
    if(Arr != "int array"){
      this.IsVarArray(Arr);
    }
  }

  // Check if a method can be called from primary expression specified
  public void CanBeCalled(String expr) throws StatiCheckingException
  {
    if(expr == "this" || expr.startsWith("/"))
      return;
    if(ST.classes_data.containsKey(expr))
      return;
    if( expr == "boolean" || expr == "int" || expr == "int array" )
      throw new StatiCheckingException("\n     ✗ Function call in class " + this.currentClass + " of method " + this.currentMethod + ", cannot be called by a non class object");
    IsVarDeclared(expr);
    IsVarDeclaredClass(expr);
  }

  // Check if class given contains the method given
  public String CheckMessageSend(String callFrom,String MethodName,ArrayList<String> params_given) throws StatiCheckingException
  {
    // Firstly check if we can call from
    CanBeCalled(callFrom);
    // get the class which method is called from
    String whichClass;
    if(callFrom == "this")
      whichClass = currentClass;
    else if(callFrom.startsWith("/"))
      whichClass = callFrom.substring(1);
    else if( ST.classes_data.containsKey(callFrom) )
      whichClass = callFrom;
    else
      whichClass = this.IsVarDeclaredClass(callFrom);
    // check if method exists in method, check arguments and get type
    String type = null;
    String superclass = ST.classes_data.get(whichClass).extendsFrom;
    ArrayList<String> method_args_types = null;
    // check if method exists in current class
    if( ST.classes_data.get(whichClass).methods_data.containsKey(MethodName) ){
      type = ST.classes_data.get(whichClass).methods_data.get(MethodName).type;
      method_args_types = new ArrayList<>(ST.classes_data.get(whichClass).methods_data.get(MethodName).arguments_data.values());
    }
    // check if methods exists in ancestor classes
    else if(superclass != ""){
      boolean found = false;
      while(superclass != ""){
        if( ST.classes_data.get(superclass).methods_data.containsKey(MethodName) ){
          type = ST.classes_data.get(superclass).methods_data.get(MethodName).type;
          method_args_types = new ArrayList<>(ST.classes_data.get(superclass).methods_data.get(MethodName).arguments_data.values());
          found = true;
          break;
        }
        superclass = ST.classes_data.get(superclass).extendsFrom;
      }
      if(!found)
        throw new StatiCheckingException("\n     ✗ There is no method " + MethodName + " in class " + whichClass + " to call from. ( tried to call from method " + this.currentMethod + " of class " + this.currentClass + ")");
    }
    else
      throw new StatiCheckingException("\n     ✗ There is no method " + MethodName + " in class " + whichClass + " to call from. ( tried to call from method " + this.currentMethod + " of class " + this.currentClass + ")");
    // Check if the number of parameters given is the same with method's declared number of parameters
    if( method_args_types.size() != params_given.size() )
      throw new StatiCheckingException("\n     ✗ Parameters aren't the same type, as declared, in method " + MethodName + " of class " + whichClass + " to call from. ( tried to call from method " + this.currentMethod + " of class " + this.currentClass + ")");
    // Check for type equality
    for(int i=0; i <method_args_types.size(); i++){
      if( !(method_args_types.get(i).equals(params_given.get(i))) ){
        // if type of expr is a class
        if( ST.classes_data.containsKey(params_given.get(i)) ){
          // check if that class derives from a class with same type of destination's type
          boolean found = false;
          superclass = ST.classes_data.get(params_given.get(i)).extendsFrom;
          while(superclass != ""){
            if( method_args_types.get(i).equals(superclass) )
              found = true;
            superclass = ST.classes_data.get(superclass).extendsFrom;
          }
          if(!found)
            throw new StatiCheckingException("\n     ✗ Parameters aren't the same type, as declared, in method " + MethodName + " of class " + whichClass + " to call from. ( tried to call from method " + this.currentMethod + " of class " + this.currentClass + ")");
        }
      }
    }

    return type;
  }

  // Check if expression's return type is the same as method's declared one
  public void CheckReturnType(String returnType) throws StatiCheckingException
  {
    String declaredType = ST.classes_data.get(this.currentClass).methods_data.get(this.currentMethod).type;
    if( !(declaredType.equals(returnType)) ){
      if( !(ST.classes_data.containsKey(returnType)) )
        throw new StatiCheckingException("\n     ✗ Method " + this.currentMethod + " in class " + this.currentClass + " is trying to return a type " + returnType + " , while it expects a type " + declaredType);
      String superclass = ST.classes_data.get(returnType).extendsFrom;
      while(superclass != ""){
        if(superclass.equals(declaredType))
          return;
        superclass = ST.classes_data.get(superclass).extendsFrom;
      }
      throw new StatiCheckingException("\n     ✗ Method " + this.currentMethod + " in class " + this.currentClass + " is trying to return a type " + returnType + " , while it expects a type " + declaredType);
    }
  }

  // Check type compatibility
  public void CheckTypeCompatibility(String Dest,String typeOfExpr) throws StatiCheckingException
  {
    String DestType = GetVarType(Dest);
    if( !(DestType.equals(typeOfExpr)) ){
      if( !(ST.classes_data.containsKey(typeOfExpr)) )
        throw new StatiCheckingException("\n     ✗ Illegal ASSIGN operation, trying to assign type " + typeOfExpr + " to variable of type " + DestType + " in method " + this.currentMethod + " of class " + this.currentClass);

      String superclass = ST.classes_data.get(typeOfExpr).extendsFrom;
      while(superclass != ""){
        if(superclass.equals(DestType))
          return;
        superclass = ST.classes_data.get(superclass).extendsFrom;
      }
      throw new StatiCheckingException("\n     ✗ Illegal ASSIGN operation, trying to assign type " + typeOfExpr + " to variable of type " + DestType + " in method " + this.currentMethod + " of class " + this.currentClass);
    }
  }

  // Check array assignment operation
  public void CheckArrayAssignment(String ArrDest,String typeOfIndex,String typeOfExpr) throws StatiCheckingException
  {
    IsVarDeclared(ArrDest);
    IsVarArray(ArrDest);
    if(typeOfIndex != "int")
      throw new StatiCheckingException("\n     ✗ Illegal ARRAY ASSIGN operation, index must be of type int, not " + typeOfIndex + ", in method " + this.currentMethod + " of class " + this.currentClass);
    if(typeOfExpr != "int")
      throw new StatiCheckingException("\n     ✗ Illegal ARRAY ASSIGN operation, trying to assign a type of " + typeOfExpr + " to an integer array, in method " + this.currentMethod + " of class " + this.currentClass);
  }

  // Check if condition is boolean
  public void CheckConditionType(String conditionType) throws StatiCheckingException
  {
    if(conditionType != "boolean")
      throw new StatiCheckingException("\n     ✗ Condition must be of type boolean, in method " + this.currentMethod + " of class " + this.currentClass);

  }

  // Check if type is something printable
  public void CheckPrintStatement(String toPrintType) throws StatiCheckingException
  {
    if( toPrintType != "int" && toPrintType != "boolean")
      throw new StatiCheckingException("\n     ✗ Only ints and booleans can be printed with println , in method " + this.currentMethod + " of class " + this.currentClass);

  }

  // Check if the type of array size is int
  public void CheckTypeOfArraySize(String typeOfArraySize){
    if(typeOfArraySize != "int")
      throw new StatiCheckingException("\n     ✗ Illegal NEW ARRAY ALLOCATION operation, type of array size must be integer, method " + this.currentMethod + " of class " + this.currentClass);
  }

  // Start offset calculation
  public void StartCalculation(){
    Map<String,Integer> trackClassesFields = new HashMap<String,Integer>();
    Map<String,Integer> trackClassesMethods = new HashMap<String,Integer>();
    // For every class
    Set< Map.Entry <String,ClassInfo> > st = ST.classes_data.entrySet();
    boolean isMainClass = true;
    String MainClassName="";
    for(Map.Entry <String,ClassInfo> cur:st){
      int VarCounter,MethodCounter;
      String thisClass = cur.getKey();
      // configure currentClass and currentMethod, so that GetVarType can search properly
      this.currentClass = thisClass;
      this.currentMethod = "";
      if(isMainClass){
        isMainClass = false;
        MainClassName = thisClass;
        continue;
      }
      // If class inherits from another,start from where it left
      System.out.println("\n\t\t\t--------------Class " + thisClass + "--------------");
      String superclass = ST.classes_data.get(thisClass).extendsFrom;
      if(superclass != "" && !(superclass.equals(MainClassName))){
        VarCounter = trackClassesFields.get(superclass);
        MethodCounter = trackClassesMethods.get(superclass);
        trackClassesFields.put(thisClass,VarCounter);
        trackClassesMethods.put(thisClass,MethodCounter);
      }
      else{
        VarCounter = 0;
        MethodCounter = 0;
        trackClassesFields.put(thisClass,0);
        trackClassesMethods.put(thisClass,0);
      }
      // for every field in class, calculate the offset
      System.out.println("\n\t\t\t     --------Variables--------");
      Set< Map.Entry <String,String> > st_f = ST.classes_data.get(thisClass).class_variables_data.entrySet();
      for (Map.Entry<String,String> cur_f:st_f){
        String thisField = cur_f.getKey();
        String thisFieldType = GetVarType(thisField);
        int size;
        if(thisFieldType == "int")
          size = 4;
        else if(thisFieldType == "boolean")
          size = 1;
        else
          size = 8;
        System.out.println("\t\t\t     " + thisClass + "." + thisField + " : " + VarCounter);
        VarCounter += size;
      }
      // for every method in class, calculate the offset
      System.out.println("\n\t\t\t     ---------Methods---------");
      Set< Map.Entry <String,MethodInfo> > st_m = ST.classes_data.get(thisClass).methods_data.entrySet();
      for (Map.Entry<String,MethodInfo> cur_m:st_m){
        // if method exists in class' ancestors, skip
        boolean existsInSuperclass = false;
        String thisMethod = cur_m.getKey();
        while(superclass != ""){
          if( ST.classes_data.get(superclass).methods_data.containsKey(thisMethod) ){
            existsInSuperclass = true;
            break;
          }
          superclass = ST.classes_data.get(superclass).extendsFrom;
        }
        if(existsInSuperclass)
          continue;
        System.out.println("\t\t\t     " + thisClass + "." + thisMethod + " : " + MethodCounter);
        MethodCounter += 8;
      }
      trackClassesFields.put(thisClass,VarCounter);
      trackClassesMethods.put(thisClass,MethodCounter);
    }
  }


}
