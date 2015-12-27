package symbolTable;

import semanticTypes.*;

// abstract symbol for symbol table
public abstract class Symbol {
	public final String name;
	public final SemanticType type;
	private int offset;
	public SemanticType runtimeType;
	
	public Symbol(String name, SemanticType type){
		this.name = name;
		this.type = type;
	}
	
	//with offset
	public Symbol(String name, SemanticType type, int o){
		this.name = name;
		this.type = type;
		this.offset = o;
	}
	
	public int getOffset(){
		return this.offset;
	}
	
	public void setOffset(int o){
		this.offset = o;
	}
}
