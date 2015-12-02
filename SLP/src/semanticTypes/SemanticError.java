package semanticTypes;

public class SemanticError extends Exception {
	private String value;
	
	public SemanticError(String message, String value){
		super(message);
		this.value = value;
	}

	public String toString(){
		return "Semantic error: "+this.getMessage()+": "+value;
	}
}
