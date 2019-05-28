package symboltable;
import staticheckingexception.*;
import java.util.*;

// Class containing data about a method
public class MethodInfo {
  public String methodName="";
  public String type; // method's type
  public LinkedHashMap <String,String> arguments_data; // [ argument name , type ]
  public LinkedHashMap <String,String> method_variables_data; // [ methods variable name , type ]

  // Constructor
  public MethodInfo(){
    arguments_data = new LinkedHashMap<String,String>();
    method_variables_data = new LinkedHashMap<String,String>();
  }

  // Insert a method argument
  public void InsertArgumentToMethod(String argName,String type) throws StatiCheckingException
  {
    // check if argument has already been in decalred in method
    if(arguments_data.containsKey(argName))
      throw new StatiCheckingException("\n     ✗ Multiple declaration of argument " + argName + " in method " + this.methodName);
    arguments_data.put(argName,type);
  }

  // Insert a method variable
  public void InsertVarToMethod(String varName, String varType) throws StatiCheckingException
  {
    // check if variable has already been in declared in method
    if(method_variables_data.containsKey(varName))
      throw new StatiCheckingException("\n     ✗ Multiple declaration of variable " + varName + " in method " + this.methodName);

    // check if variable has already been in decalred in method as argument
    if(arguments_data.containsKey(varName))
      throw new StatiCheckingException("\n     ✗ Variable " + varName + " has already been declared as argument in method " + this.methodName);
    method_variables_data.put(varName,varType);
  }

  // List all variables the method has
  public void ListVariables(){
    System.out.println("        Method contains the following variables:");
    Set< Map.Entry <String,String> > st = method_variables_data.entrySet();
     for (Map.Entry<String,String> cur:st){
         System.out.print("         " + cur.getKey()+":");
         System.out.println(cur.getValue());
     }
     System.out.println("");
  }

  // List all arguments the method has
  public void ListArguments(){
    System.out.println("        Method contains the following arguments:");
    Set< Map.Entry <String,String> > st = arguments_data.entrySet();
     for (Map.Entry<String,String> cur:st){
         System.out.print("         " + cur.getKey()+":");
         System.out.println(cur.getValue());
     }
     System.out.println("");
  }
}
