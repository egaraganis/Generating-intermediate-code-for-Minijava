import java.io.*;
import syntaxtree.*;
import typecheck.*;
import visitor.*;
import staticheckingexception.*;

class Main {
  public static void main(String[] args){
    if (args.length < 1){
      System.err.println("Please add input files");
      System.exit(1);
    }
    FileInputStream fis = null;
    // Static Checking for every file given as input
    for(int i=0; i<args.length; i++){
      try {
        System.out.println("\n   ▸ Generating LLVM Code for: " + args[i]);
        fis = new FileInputStream(args[i]);
        MiniJavaParser parser = new MiniJavaParser(fis);
        Goal root = parser.Goal(); // get the root of the tree
        // Populate the symbol table
        STPVisitor SymbolTablePopulator = new STPVisitor();
        root.accept(SymbolTablePopulator, null);
        // Type check the program
        TCVisitor TypeChecker = new TCVisitor(SymbolTablePopulator.getSymbolTable());
        root.accept(TypeChecker,null);
        // Calculate offsets
        TypeChecker.getTypeCheck().StartCalculation();
        // Generate LLVM code - Lowering visitor
        LWRVisitor LoweringVisitor = new LWRVisitor(args[i],SymbolTablePopulator.getSymbolTable());
        root.accept(LoweringVisitor,null);
        LoweringVisitor.getL().Log_Buffer();
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
