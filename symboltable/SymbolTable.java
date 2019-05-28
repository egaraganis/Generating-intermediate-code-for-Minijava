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
