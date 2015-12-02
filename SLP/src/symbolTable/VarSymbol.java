package symbolTable;

import semanticTypes.*;

// variable symbol for symbol table (need to be initialized)
public class VarSymbol extends Symbol {
	public boolean initialized = false;
	
	public VarSymbol(String symName, SemanticType type){
		super(symName, type);
	}
}
