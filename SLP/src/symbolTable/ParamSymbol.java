package symbolTable;

import semanticTypes.SemanticType;

// method parameter symbol (no need to be initialized) for symbol table
public class ParamSymbol extends Symbol {
	
	public ParamSymbol(String symName, SemanticType type){
		super(symName, type);
	}
}
