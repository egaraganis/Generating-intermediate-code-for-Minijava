package symboltable;
import staticheckingexception.*;
import java.util.*;

// Class containing data for a Class
public class ClassInfo {
  public String className="";
  public String extendsFrom=""; // inheritance relationship
  public String extendsTo=""; // inheritance relationship
  public LinkedHashMap <String,String> class_variables_data; // [ class variable name , type  ]
  public LinkedHashMap <String,MethodInfo> methods_data; // [ class methods name , methods info ]

  // Constructor
  public ClassInfo(){
    class_variables_data = new LinkedHashMap<String,String>();
    methods_data = new LinkedHashMap<String,MethodInfo>();
  }

  // Insert a method to a class
  public void InsertMethodToClass(String MethodsName,MethodInfo methodsInf) throws StatiCheckingException
  {
    // Check if method has already been declared in class
    if(methods_data.containsKey(MethodsName))
      throw new StatiCheckingException("\n     ✗ Multiple declaration of method " + MethodsName + " in class " + this.className);

    methods_data.put(MethodsName,methodsInf);
  }

  // Only Inheritant Polymorphism is allowed
  public void InheritantPolymorphismCheck(String MethodsName,ClassInfo superclass){
    if(superclass != null){
      if(superclass.methods_data.containsKey(MethodsName)){
        // check for the return type of polymorphed function
        if(superclass.methods_data.get(MethodsName).type != methods_data.get(MethodsName).type)
          throw new StatiCheckingException("\n     ✗ Methods " + MethodsName + " return type in class " + className + " must be the same with superclass' " + superclass.className + " inheritant function ");

        // check for the arguments of the polymorphed function
        ArrayList<String> superclass_args_types = new ArrayList<>(superclass.methods_data.get(MethodsName).arguments_data.values());
        ArrayList<String> derived_args_types =    new ArrayList<>(methods_data.get(MethodsName).arguments_data.values());
        if( !(superclass_args_types.equals(derived_args_types)) )
          throw new StatiCheckingException("\n     ✗ Methods " + MethodsName + " arguments in class " + className + " must be the same with superclass' " + superclass.className + " inheritant function ");
      }
    }
  }

  // Insert a field to a class
  public void InsertFieldToClass(String fieldName, String fieldType) throws StatiCheckingException
  {
    // check if field has already been declared in class
    if(class_variables_data.containsKey(fieldName))
      throw new StatiCheckingException("\n     ✗ Multiple declaration of field " + fieldName + " in class " + this.className);
    class_variables_data.put(fieldName,fieldType);
  }

  // List all methods in a class
  public void ListMethods(){
    System.out.println("    Class contains the following methods:");
    Set< Map.Entry <String,MethodInfo> > st = methods_data.entrySet();
     for (Map.Entry<String,MethodInfo> cur:st){
         System.out.print(cur.getKey()+", ");
     }
     System.out.println("");
  }

  // List all methods in detailed form
  public void ListMethodsDetailed(){
    System.out.println("    Class contains the following methods:");
    Set< Map.Entry <String,MethodInfo> > st = methods_data.entrySet();
    for (Map.Entry<String,MethodInfo> cur:st){
      System.out.println("    • " + cur.getKey());
      cur.getValue().ListArguments();
      cur.getValue().ListVariables();
      System.out.println("     and it's return type is " + cur.getValue().type);
    }
    System.out.println("");
  }

  // List all fields of the class
  public void ListFields(){
    System.out.println("    Class contains the following fields:");
    Set< Map.Entry <String,String> > st = class_variables_data.entrySet();
    for (Map.Entry<String,String> cur:st){
      System.out.print("      " + cur.getKey()+":");
      System.out.println(cur.getValue());    }
    System.out.println("");
  }
}
