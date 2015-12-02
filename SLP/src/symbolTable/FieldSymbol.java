package symbolTable;

import semanticTypes.*;

// class field symbol for symbol table
public class FieldSymbol extends VarSymbol {
	
	public FieldSymbol(String symName, SemanticType type){
		super(symName, type);
	}
}
