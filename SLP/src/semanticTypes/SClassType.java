package semanticTypes;

// Semantic class type
public class SClassType extends SemanticType {
	public final String superName;
	private TypeTable typeTable;

	// for a class without a superclass
	public SClassType(String name) throws SemanticError{
		this(name, null, null);
	}
	
	// for a class with a superclass
	public SClassType(String name, String superName, TypeTable typeTable) throws SemanticError{
		super(name);
		this.superName = superName;
		this.typeTable = typeTable;
	}
	
	// returns TRUE if the semantic type of THIS is the same as OTHER
	// or semantic type of THIS is derived from OTHER
	@Override
	public boolean isLike(SemanticType other){
		if (this == other) return true;
		
		if (other instanceof SClassType){
			if(typeTable != null){
				try{
					return typeTable.resolveClassType(superName).isLike(other);
				}
				catch (SemanticError se){
					return false;
				}
			}
		}
		return false;
	}
}