package symbolTable;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class SymbolTable {
	private Map<String, List<SymbolEntry>> symbolMap = new HashMap<String, List<SymbolEntry>>();
	private List<List<SymbolEntry>> scope = new ArrayList<List<SymbolEntry>>();
	private int scopeLevel = 0;
	
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
}
