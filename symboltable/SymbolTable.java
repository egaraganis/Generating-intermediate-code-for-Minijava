package symboltable;
import java.util.*;
import staticheckingexception.*;

// Symbol table for our semantic analysis - type checking
public class SymbolTable {
  public LinkedHashMap <String,ClassInfo> classes_data; // [ class name , class info ]

  // Constructor
  public SymbolTable(){
    classes_data = new LinkedHashMap<String,ClassInfo>();
  }

  // Insert a class to symbol table
  public void InsertClassToSymbolTable(String className, ClassInfo classInfo) throws StatiCheckingException
  {
    // check if class is already declared in program
    if(classes_data.containsKey(className))
      throw new StatiCheckingException("\n     ✗ Class with name " + className + " has already been declared");

    // if class extends from superclass
    if(classInfo.extendsFrom != ""){
      // make sure that superclass has been declared
      if(classes_data.containsKey(classInfo.extendsFrom) == false)
        throw new StatiCheckingException("\n     ✗ Class with name " + classInfo.extendsFrom + " has not been declared, to extend from");

      // a class cannot inherit from itself
      if(classInfo.extendsFrom == className)
        throw new StatiCheckingException("\n     ✗ Class with name " + classInfo.extendsFrom + " cannot inherit from itself");

      // check for circural inheritance
      String grandfather = classes_data.get(classInfo.extendsFrom).extendsFrom;
      if(grandfather == className)
        throw new StatiCheckingException("\n     ✗ Class with name " + className + " can't have circural inheritance");

    }
    classes_data.put(className,classInfo);
  }

  // Return variable's type that have been declared to an ancestor
  public String SearchAncestors(String className,String var){
    String superclass = classes_data.get(className).extendsFrom;
    while(superclass != ""){
      String type = classes_data.get(superclass).class_variables_data.get(var);
      if(type != null)
        return type;
      superclass = classes_data.get(superclass).extendsFrom;
    }
    return "";
  }

  // Return given variable's type
  public String GetVarType(String var,String currentClass,String currentMethod){
    String typeGotFromField,typeGotFromMethodVar,typeGotFromMethodArg,typeGotFromSuperField=null;
    if(currentMethod != ""){
      typeGotFromMethodVar = classes_data.get(currentClass).methods_data.get(currentMethod).method_variables_data.get(var);
      if(typeGotFromMethodVar != null)
      return typeGotFromMethodVar;

      typeGotFromMethodArg = classes_data.get(currentClass).methods_data.get(currentMethod).arguments_data.get(var);
      if(typeGotFromMethodArg != null)
      return typeGotFromMethodArg;
    }
    typeGotFromField = classes_data.get(currentClass).class_variables_data.get(var);
    if(typeGotFromField != null)
      return typeGotFromField;

    typeGotFromSuperField = SearchAncestors(currentClass,var);
    if(typeGotFromSuperField != "")
      return typeGotFromSuperField;
    return "error";
  }

  // Check if identifier is a field in class specified or to it's ancestors
  public String IsClassField(String var,String currentClass,String currentMethod){
    String typeGotFromField,typeGotFromSuperField;
    typeGotFromField = classes_data.get(currentClass).class_variables_data.get(var);
    if(typeGotFromField != null)
      return typeGotFromField;

    typeGotFromSuperField = SearchAncestors(currentClass,var);
    if(typeGotFromSuperField != "")
      return typeGotFromSuperField;
    return null;
  }

  // List all classes in symbol table
  public void ListClasses(){
    System.out.println("Symbol Table contains the following classes:");
    Set< Map.Entry< String,ClassInfo> > st = classes_data.entrySet();
     for (Map.Entry< String,ClassInfo> cur:st){
         System.out.print(cur.getKey()+", ");
     }
     System.out.println("");
  }

  // List everything in symbol table
  public void ListEverything(){
    System.out.println(" Symbol Table contains the following classes:");
    Set< Map.Entry <String,ClassInfo> > st = classes_data.entrySet();
     for (Map.Entry <String,ClassInfo> cur:st){
         System.out.println(" • " + cur.getKey() + " and extends from " + cur.getValue().extendsFrom);
         cur.getValue().ListFields();
         cur.getValue().ListMethodsDetailed();
     }
     System.out.println("");
  }
}
