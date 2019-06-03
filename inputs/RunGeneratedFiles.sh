#!/bin/bash
for filename in *.ll; do
    echo Compiling "$filename" ...
    clang-4.0 -Wno-override-module -o "$filename".out "$filename"
    echo Executing "$filename".out ...
    ./"$filename".out
    echo
done
