package slp;

import java.io.*;

import semanticTypes.*;
import symbolTable.*;
//import java_cup.runtime.*;
import java_cup.runtime.Symbol;
import LIR.LIRTranslator;

public class Main {
	/** Reads an IC and pretty-prints it.
	 * 
	 * @param args Should be the name of the file containing an IC.
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Error: Missing input file argument!");
			System.out.println("Usage: slp <filename>");
			System.exit(-1);
		}
		try {
			// Parse the input file
			FileReader txtFile = new FileReader(args[0]);
			Lexer scanner = new Lexer(txtFile);
			Parser parser = new Parser(scanner);

			Symbol parseSymbol = parser.parse();
			Program root = (Program) parseSymbol.value;
			
			// Pretty-print the program to System.out
			//PrettyPrinter printer = new PrettyPrinter(root);
			//printer.print();
			
			SymbolTable symTab = new SymbolTable();
			TypeTable typTab = new TypeTable();
			  
			
			SemanticChecker checker = new SemanticChecker(root, symTab, typTab);
			checker.start();
			System.out.println("Passed semantic checks successfully!\n");
			
			LIRTranslator translator = new LIRTranslator(root, symTab, typTab);
			String translation = root.accept(translator, 0).lirCode;
			
			// print output to file
			String resultFile = "output.lir";
			try {
				BufferedWriter buff = new BufferedWriter(new FileWriter(resultFile));
				buff.write(translation);
				buff.flush();
				buff.close();
			} catch (IOException e) {
				System.out.println("Failed writing to file: "+resultFile);
				e.printStackTrace();
			}
			System.out.println("LIR translation");
			System.out.println("===============");
			System.out.println(translation);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}