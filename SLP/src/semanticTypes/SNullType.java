package semanticTypes;

// Semantic NULL type
public class SNullType extends SemanticType {
	public SNullType(){
		super("null");
	}
	
	// NULL type can be assigned to any class type. So NULL is like any semantic class type.
	@Override
	public boolean isLike(SemanticType other){
		return (other instanceof SClassType);
	}
}
