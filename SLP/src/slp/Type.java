package slp;

public abstract class Type extends ASTNode {
	public int arrayLvl = 0;

	protected Type(int line){
		super(line);
	}
	
	public String getName(){
		if(arrayLvl != 0){
			StringBuilder fullName = new StringBuilder(getTypeName());
			for (int i=0; i<arrayLvl; i++)
				fullName.append("[]");
			return fullName.toString();
		}
		else
			return getTypeName();
	}
	
	protected abstract String getTypeName();
	
	/** Accepts a visitor object as part of the visitor pattern.
	 * @param visitor A visitor.
	 */
	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}
	
	/** Accepts a propagating visitor parameterized by two types.
	 * 
	 * @param <DownType> The type of the object holding the context.
	 * @param <UpType> The type of the result object.
	 * @param visitor A propagating visitor.
	 * @param context An object holding context information.
	 * @return The result of visiting this node.
	 */
	@Override
	public <DownType, UpType> UpType accept(
			PropagatingVisitor<DownType, UpType> visitor, DownType context) {
		return visitor.visit(this, context);
	}
}
