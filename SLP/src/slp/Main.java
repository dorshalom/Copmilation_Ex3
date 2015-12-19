package slp;

import java.io.*;

import LIR.LIRTranslator;
import semanticTypes.TypeTable;
import symbolTable.SemanticChecker;
import symbolTable.SymbolTable;
import java_cup.runtime.*;

// just checking how commit works

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
			String outputsFileName = args[0].substring(0,args[0].length()-2)+"lir";
			try {
				BufferedWriter buff = new BufferedWriter(new FileWriter(outputsFileName));
				buff.write(translation);
				buff.flush();
				buff.close();
			} catch (IOException e) {
				System.err.println("Failed writing to file: "+outputsFileName);
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