package symbolTable;

import semanticTypes.*;

// abstract symbol for symbol table
public abstract class Symbol {
	public final String name;
	public final SemanticType type;

	public Symbol(String name, SemanticType type){
		this.name = name;
		this.type = type;
	}
}
