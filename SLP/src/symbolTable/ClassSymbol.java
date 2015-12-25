package symbolTable;

import java.util.*;
import semanticTypes.*;

// class symbol for symbol table
 public class ClassSymbol extends Symbol {
	private Map<String, MethodSymbol> methods = new HashMap<String,MethodSymbol>();
	private Map<String, FieldSymbol> fields = new HashMap<String,FieldSymbol>();
	private SymbolTable symbolTable;
	public final String superName; 
	
	// for classes without super
	public ClassSymbol(String name, SymbolTable global)  throws SemanticError{
		super(name, new SClassType(name));
		superName = null;
		this.symbolTable = global;
	}
	
	// for classes with super
	public ClassSymbol(String name, String superName, TypeTable typeTable, SymbolTable global)  throws SemanticError{
		super(name, new SClassType(name, superName, typeTable));
		this.symbolTable = global;
		this.superName = superName;
	}

	// for classes without super with offset
	public ClassSymbol(String name, SymbolTable global,int offset)  throws SemanticError{
		super(name, new SClassType(name), offset);
		superName = null;
		this.symbolTable = global;
	}
	
	// for classes with super with offset
	public ClassSymbol(String name, String superName, TypeTable typeTable, SymbolTable global,int offset)  throws SemanticError{
		super(name, new SClassType(name, superName, typeTable), offset);
		this.symbolTable = global;
		this.superName = superName;
	}

	// try to find a member method by its name. Looks only in current class
	public MethodSymbol getMethodSymbol(String name) throws SemanticError{
		MethodSymbol ms = methods.get(name);
		if (ms == null) 
			throw new SemanticError("no such method defined in "+this.name, name);
		return ms; 
	}
	
	// try to find a member method by its name. Also searches recursively in parents
	public MethodSymbol getMethodSymbolRec(String name) throws SemanticError{
		MethodSymbol ms = methods.get(name);
		if (ms == null) {
			if (superName != null){
				ms = ((ClassSymbol) symbolTable.findEntryGlobal(superName)).getMethodSymbolRec(name);
			} else {
				throw new SemanticError("method does not exist",name);
			}
		}
		return ms;
	}

	// try to add a new member method. Fails if the name of the method is already used in current class,
	// or new method overloads a super method. Method overriding is allowed.
	public void addMethodSymbol(String name, SemanticType type, List<ParamSymbol> params, boolean isStatic,int offset) throws SemanticError{
		StringBuilder sb = new StringBuilder();
		
		try{
			getFieldSymbolRec(name);
			sb.append("method already defined as a field");
		} catch (SemanticError e){ 
			try{
				getMethodSymbol(name);
				sb.append("method already defined, method overloading not supported");
			} catch (SemanticError e2){
				try{
					MethodSymbol superMS = getMethodSymbolRec(name);
					// method defined in super -> check that both methods are virtual
					if ((isStatic == false) && (superMS.isStatic == isStatic)){
						if (offset > -1){
							methods.put(name, new MethodSymbol(name, type, params, isStatic, offset)); 		
						}
						else{
							methods.put(name, new MethodSymbol(name, type, params, isStatic)); 		
						}
						return;
					}
					else
						sb.append("method defined in super, overloading not allowed");
				}catch(SemanticError e3){
					// method is not previously defined
					if (offset > -1){
						methods.put(name, new MethodSymbol(name, type, params, isStatic, offset));
					}
					else{
						methods.put(name, new MethodSymbol(name, type, params, isStatic));
					}
					return;
				}
			}
		}
		throw new SemanticError(sb.toString(), name);
	}
	
	// try to find a member field by its name. Looks only in current class
	public FieldSymbol getFieldSymbol(String name) throws SemanticError{
		FieldSymbol fs = fields.get(name);
		if (fs == null) 
			throw new SemanticError("field does not exist in "+this.name, name);
		return fs;
	}
	
	// try to find a member method by its name. Also searches recursively in parents
	public FieldSymbol getFieldSymbolRec(String name) throws SemanticError{
		FieldSymbol fs = fields.get(name);
		if (fs == null) {
			if (superName != null){
				fs = ((ClassSymbol) symbolTable.findEntryGlobal(superName)).getFieldSymbolRec(name);
			} else {
				throw new SemanticError("name cannot be resolved",name);
			}
		}
		return fs;
	}

	// try to add a new member field. Fails if the name of the field is already used in current class,
	// or in super classes.
	public void addFieldSymbol(String name, SemanticType type, int o) throws SemanticError{
		try{
			getFieldSymbolRec(name);
		} catch (SemanticError e){
			try{
				getMethodSymbolRec(name);
			} catch (SemanticError e2){
				this.fields.put(name,new FieldSymbol(name,type,o));
				return;
			}
		}
		throw  new SemanticError("field name already in use", name);
	}
	
	public int bytesInMemory(){
		//return number of bytes this class needs for allocation = (number of fields+1)*4
		int size=0;
		size += this.getNumberOfFieldsRec() + 1;
		size *= 4;		
		return size;
	}
	
	
	// returns the number of fields in this class, including all of super's fields, recursively...
	public int getNumberOfFieldsRec(){
		int n=0;
		n+=fields.keySet().size();
		if (this.superName != null){
			n+=((ClassSymbol) symbolTable.findEntryGlobal(this.superName)).getNumberOfFieldsRec();
		}
		
		return n;
	}
}
