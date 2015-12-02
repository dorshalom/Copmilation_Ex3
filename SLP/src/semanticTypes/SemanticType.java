package semanticTypes;

public abstract class SemanticType {
	public final String name;
	
	protected SemanticType(String name){
		this.name = name;
	}
	
	// for all SemanticTypes other than SClassType and SNullType, type equality is resolved
	// by equality of their unique SemanticTypes.
	public boolean isLike(SemanticType other){
		return this == other;
	}

}
