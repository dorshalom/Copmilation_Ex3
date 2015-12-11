package semanticTypes;

import java.util.*;

// holds all primitive, array and user defined types we have seen 
public class TypeTable {
	// unique objects for primitive types
	public final SIntType intType = new SIntType();
	public final SBooleanType booleanType = new SBooleanType();
	public final SStringType stringType = new SStringType();
	public final SVoidType voidType = new SVoidType();
	public final SNullType nullType = new SNullType();
	
	// array-type objects
	private Map<String, SArrayType> arrays = new HashMap<String,SArrayType>();
	
	// user defined types
	private Map<String, SClassType> classes = new HashMap<String,SClassType>();
     
	// returns a unique semantic type object, representing the type defined in the parameter
	public SemanticType resolveType(String name) throws SemanticError{
		switch(name){
		case "int":		return intType;
		case "boolean":	return booleanType;
		case "string":	return stringType;
		case "void":	return voidType;
		case "null":	return nullType;
		}
		if(name.contains("[]"))
			return resolveArrayType(name);
		else
			return resolveClassType(name);
    }
	  
	// returns a unique semantic type object, representing the array type defined in the parameter
    public SArrayType resolveArrayType(String name) throws SemanticError{
    	// array hashmap is populated on-the-go. When we see a new array type, its unique object
    	// is created and added to hashmap.
    	
    	String typeT = name.replace("[]", "");	// get the T from T[]
		resolveType(typeT);
		// if we still here, T (of T[]) is already defined, so its OK to add T[]
		if (!arrays.containsKey(name)) {
    		SArrayType array = new SArrayType(name);
    		arrays.put(name, array);
    		return array;
    	}
    	return arrays.get(name);
    }
    
    // returns a unique semantic type object, representing the class type defined in the parameter.
    public SemanticType resolveClassType(String name) throws SemanticError{
    	SClassType classType = classes.get(name);
    	if (classType == null) 
    		throw new SemanticError("undefined class", name);
    	return classType;
    }
    
    // tries to add a new class type to hashmap. Doesn't allow to define a class twice
    // or to define a class that extends a previously undefined superclass.
    public void addClassType(String name, String superName) throws SemanticError{
    	if (classes.containsKey(name))
    		throw new SemanticError("class already defined", name);
    	if (superName != null && !classes.containsKey(superName)) 
    		throw new SemanticError("super class is undefined", superName);
    	SClassType classType;
    	if (superName != null)
    		classType = new SClassType(name, superName, this);
    	else
    		classType = new SClassType(name);
    	classes.put(name, classType);
    }
    
    // returns true iff the parameter is a previously-seen array type
    public boolean isArrayType(SemanticType type){
    	return arrays.containsKey(type.name) ? true : false;
    }
}
