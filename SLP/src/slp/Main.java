package slp;

import java.io.*;

import symbolTable.SemanticChecker;
import java_cup.runtime.*;

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
			//System.out.println("Parsed " + args[0] + " successfully!\n");
			Program root = (Program) parseSymbol.value;
			
			// Pretty-print the program to System.out
			//PrettyPrinter printer = new PrettyPrinter(root);
			//printer.print();
			
			SemanticChecker checker = new SemanticChecker(root);
			checker.start();
			System.out.println("\nPassed semantic checks successfully!\n");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}