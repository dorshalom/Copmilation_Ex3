package symbolTable;

import semanticTypes.*;

// class field symbol for symbol table
public class FieldSymbol extends VarSymbol {
	
	public FieldSymbol(String symName, SemanticType type, int offset){
		super(symName, type, offset);
	}
}
