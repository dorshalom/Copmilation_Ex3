package symbolTable;


import java.util.*;

import semanticTypes.*;

// class method symbol for symbol table
public class MethodSymbol extends Symbol {
	public boolean isStatic;
	public List<ParamSymbol> params;
	 
	// try to create a method symbol from given method signature. Fails if there are parameters with the same name
	public MethodSymbol(String name, SemanticType type, List<ParamSymbol> params, boolean isStatic) throws SemanticError{
		super(name, type);
		this.isStatic = isStatic;
		this.params = new ArrayList<ParamSymbol>();
		for(ParamSymbol p: params){
			addParamSymbol(p);
		}
	}
	
	// checks if this method's params are type compatible with other method's params
	public boolean checkParamTypes(List<SemanticType> params){
		if(this.params.size() != params.size())
			return false;
		for(int i=0; i<params.size(); i++){
			if(!params.get(i).isLike(this.params.get(i).type))
				return false;
		}
		return true;
	}
	
	// try to add a new parameter to parameter list. Fails is there are parameters with the same name
	protected void addParamSymbol(ParamSymbol param) throws SemanticError{
		for(ParamSymbol p: params){
			if(p.name.equals(param.name))
				throw new SemanticError("parameter with this name already exists", param.name);
		}
		params.add(param);
	}
	
	
}
