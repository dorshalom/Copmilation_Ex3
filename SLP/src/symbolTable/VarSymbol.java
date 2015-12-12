package symbolTable;

import semanticTypes.*;

// variable symbol for symbol table (need to be initialized)
public class VarSymbol extends Symbol {
	public boolean isAssigned = false;
	
	public VarSymbol(String symName, SemanticType type){
		super(symName, type);
	}
	
	public VarSymbol(String symName, SemanticType type, boolean isAssigned){
		super(symName, type);
		this.isAssigned = isAssigned;
	}
}
