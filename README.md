## Generating intermediate code ( From Minijava To LLVM )

Using the visitor pattern to typecheck minijava code and produce linear level IR in LLVM  language. Code produces LLVM intermediate representation for a subset of instructions and produces code that can be compiled with C-Lang.

### Execution of the code generator

First, compile java code with 

    make
Then, execute with

   `java [MainClassName] [file1.java] [file2.java] ... [fileN.java]`

The above command will produce *file1.ll , file2,ll , ... , fileN.ll* files respectively that will contain the LLVM code, producing the same output, as minijava files will.

#### Execution of LLVM files

You can either compile and run a file with :

    clang-4.0 -o out1 file1.ll // to compile 
    ./out1                     // to run

or using the script in **inputs** folder

    ./RunGeneratedFiles.sh
That will compile and execute all  **.ll** files.

##### More info
[BNF Grammar for minijava language](http://cgi.di.uoa.gr/~thp06/project_files/minijava-new/minijava.html)
[LLVM Language reference manual](https://llvm.org/docs/LangRef.html#instruction-reference)
