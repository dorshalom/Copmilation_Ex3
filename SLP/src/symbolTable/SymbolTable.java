package symbolTable;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class SymbolTable {
	private Map<String, List<SymbolEntry>> symbolMap = new HashMap<String, List<SymbolEntry>>();
	private List<List<SymbolEntry>> scope = new ArrayList<List<SymbolEntry>>();
	public int scopeLevel = 0;
	
	// binds scope level to a symbol
	private class SymbolEntry{
		private Symbol symbol;
		private int level;
		
		private SymbolEntry(Symbol symbol, int level){
			this.symbol = symbol;
			this.level = level;
		}
	}
	
	public SymbolTable(){
		enterScope();
	}
	
	// create a new list to hold references to symbols of a new scope
	public void enterScope(){
		scope.add(0, new ArrayList<SymbolEntry>());
		++scopeLevel;
	}
	
	// delete all symbols defined in current scope from the table 
	public void exitScope(){
		for (SymbolEntry se: scope.get(0)){
			symbolMap.get(se.symbol.name).remove(0);
			if(symbolMap.get(se.symbol.name).isEmpty())
				symbolMap.remove(se.symbol.name);
		}
		scope.remove(0);
		--scopeLevel;
	}
	
	// add a new symbol to the table and save a reference to it in the scope list
	public void addEntry(Symbol sym){
		if(sym == null)
			return;
		
		SymbolEntry se = new SymbolEntry(sym, scopeLevel);
		if(findEntryGlobal(sym.name) == null){
			List<SymbolEntry> l = new ArrayList<SymbolEntry>();
			l.add(0, se);
			symbolMap.put(sym.name, l);
		}
		else
			symbolMap.get(sym.name).add(0, se);
		scope.get(0).add(se);
	}

	// searches for a symbol with given name, returns null if not found
	public Symbol findEntryGlobal(String name){
		List<SymbolEntry> l = symbolMap.get(name);
		if(l == null)
			return null;
		return l.get(0).symbol;
	}
	
	// searches for a symbol with given name, returns null if not found in CURRENT scope
	public Symbol findEntryLocal(String name){
		List<SymbolEntry> l = symbolMap.get(name);
		if(l == null || (l.get(0).level < scopeLevel))
			return null;
		return l.get(0).symbol;
	}
	
	// returns the scope level of the last definition of <name>
	public int findScopeLevel(String name){
		List<SymbolEntry> l = symbolMap.get(name);
		if(l == null)
			return -1;
		return l.get(0).level;
	}
}

/*---------------------------------------------------------------IN SEMANTIC CHECKER----------------------------------------------------------------------------
* 
*
*	scopeLevel 1 (always exists)				    scopeLevel 2 (THIS class scope)		     	  scopeLevel 3 (Method scope)	 scopeLevel 4...N (Block scopes)
*---------------------------------------------------------------------------------------------------------------------------------------------------------------
*	Class A { Field_A ... Field_Z;				    CurrentClass_Field_A						  CurrentMethod_Param_A			 CurrentBlock_LocalVar_A
*			  Method A(Param_A...Param_Z)		    ...											  ...							 ...
*			  ...								    CurrentClass_Field_Z						  CurrentMethod_Param_Z		     CurrentBlock_LocalVar_Z
*			  Method Z(Param_A...Param_Z)		
*	}												CurrentClass_Method_A(Param_A...Param_Z)	  CurrentMethod_LocalVar_A
*													...											  ...
*	...												CurrentClass_Method_Z(Param_A...Param_Z)	  CurrentMethod_LocalVar_Z
*
*	Class Z { Field_A ... Field_Z;
*			  Method A(Param_A...Param_Z)
*			  ...
*			  Method Z(Param_A...Param_Z)
*	}
*
*	Class Library{library function definitions}
*
*/

/*---------------------------------------------------------------IN LIRTRANSLATOR----------------------------------------------------------------------------
* 
*
*	scopeLevel 1 (always exists)				    scopeLevel 2 (THIS class scope)		     	  scopeLevel 3 (Method scope)	 scopeLevel 4...N (Block scopes)
*---------------------------------------------------------------------------------------------------------------------------------------------------------------
*	Class A { Field_A ... Field_Z;				    CurrentClass_Field_A						  CurrentMethod_LocalVar_A		 CurrentBlock_LocalVar_A
*			  Method A(Param_A...Param_Z)		    ...											  ...								 ...
*			  ...								    CurrentClass_Field_Z						  CurrentMethod_LocalVar_Z	     CurrentBlock_LocalVar_Z
*			  Method Z(Param_A...Param_Z)		
*	}													  										  
*																								  
*	...													  										  
*
*	Class Z { Field_A ... Field_Z;
*			  Method A(Param_A...Param_Z)
*			  ...
*			  Method Z(Param_A...Param_Z)
*	}
*
*	Class Library{library function definitions}
*/