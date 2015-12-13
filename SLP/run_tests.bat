del /Q results.txt
forfiles /p .\test\ /m *.ic /c "cmd /c echo @file>>..\results.txt & java -jar ..\slp.jar @file>>..\results.txt"