package slp;

/** An interface for AST visitors.
 */
public interface Visitor {
	public void visit(UnaryOpExpr expr);
	public void visit(BinaryOpExpr expr);
	public void visit(Program expr);
	public void visit(Class expr);
	public void visit(Field expr);
	public void visit(Formal expr);
	public void visit(Type expr);
	public void visit(Method expr);
	public void visit(AssignStmt stmt);
	public void visit(ReturnStmt stmt);
	public void visit(StaticCall expr);
	public void visit(VirtCall expr);
	public void visit(VarLocation expr);
	public void visit(ArrayLocation expr);
	public void visit(CallStmt stmt);
	public void visit(StmtList stmt);
	public void visit(IfStmt stmt);
	public void visit(WhileStmt stmt);
	public void visit(BreakStmt stmt);
	public void visit(ContinueStmt stmt); 
	public void visit(LocalVarStmt stmt);
	public void visit(ThisExpr expr);
	public void visit(NewClassExpr expr);
	public void visit(NewArrayExpr expr);
	public void visit(LengthExpr expr);
	public void visit(LiteralExpr expr);
}