import java.io.*;
import syntaxtree.*;
import typecheck.*;
import visitor.*;
import staticheckingexception.*;

class Main {
  public static void main(String[] args){
    int InputsPassed = 0;
    if (args.length < 1){
      System.err.println("Please add input files");
      System.exit(1);
    }
    FileInputStream fis = null;
    // Static Checking for every file given as input
    for(int i=0; i<args.length; i++){
      try {
        System.out.print("\n   ▸ Generating LLVM Code for: " + args[i]);
        fis = new FileInputStream(args[i]);
        MiniJavaParser parser = new MiniJavaParser(fis);
        Goal root = parser.Goal(); // get the root of the tree
        // Populate the symbol table
        STPVisitor SymbolTablePopulator = new STPVisitor();
        root.accept(SymbolTablePopulator, null);
        //SymbolTablePopulator.getSymbolTable().ListEverything();
        //System.out.println("     • Populating the Symbol Table - Uniqueness Checks finished successfully...");
        // Type check the program
        TCVisitor TypeChecker = new TCVisitor(SymbolTablePopulator.getSymbolTable());
        root.accept(TypeChecker,null);
        //System.out.println("     • Type Cheking the input finished successfully...");
        System.out.print(" ✓ \n\n\n");
        // Produce offset results
        TypeChecker.getTypeCheck().StartCalculation();
        InputsPassed++;
      }
      catch (ParseException ex) {
        System.out.println(ex.getMessage());
      }
      catch (StatiCheckingException ex){
        System.err.println("\n\n     " + ex);
      }
      catch (FileNotFoundException ex) {
        System.err.println(ex.getMessage());
      }
      finally {
        try {
          if (fis != null) fis.close();
        }
        catch (IOException ex) {
          System.err.println(ex.getMessage());
        }
      }
    }
    // Number of files passed
  }
}
