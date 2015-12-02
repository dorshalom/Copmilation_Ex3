package slp;

/** An interface for a propagating AST visitor.
 * The visitor passes down objects of type <code>DownType</code>
 * and propagates up objects of type <code>UpType</code>.
 */
public interface PropagatingVisitor<DownType,UpType> {
	public UpType visit(UnaryOpExpr expr, DownType d);
	public UpType visit(BinaryOpExpr expr, DownType d);
	public UpType visit(Program expr, DownType d);
	public UpType visit(Class expr, DownType d);
	public UpType visit(Field expr, DownType d);
	public UpType visit(Formal expr, DownType d);
	public UpType visit(Type expr, DownType d);
	public UpType visit(Method expr, DownType d);
	public UpType visit(AssignStmt stmt, DownType d);
	public UpType visit(ReturnStmt stmt, DownType d);
	public UpType visit(StaticCall expr, DownType d);
	public UpType visit(VirtCall expr, DownType d);
	public UpType visit(VarLocation expr, DownType d);
	public UpType visit(ArrayLocation expr, DownType d);
	public UpType visit(CallStmt stmt, DownType d);
	public UpType visit(StmtList stmt, DownType d);
	public UpType visit(IfStmt stmt, DownType d);
	public UpType visit(WhileStmt stmt, DownType d);
	public UpType visit(BreakStmt stmt, DownType d);
	public UpType visit(ContinueStmt stmt, DownType d); 
	public UpType visit(LocalVarStmt stmt, DownType d);
	public UpType visit(ThisExpr expr, DownType d);
	public UpType visit(NewClassExpr expr, DownType d);
	public UpType visit(NewArrayExpr expr, DownType d);
	public UpType visit(LengthExpr expr, DownType d);
	public UpType visit(LiteralExpr expr, DownType d);
}
/*
public interface PropagatingVisitor{
	public Object visit(UnaryOpExpr expr);
	public Object visit(BinaryOpExpr expr);
	public Object visit(Program expr);
	public Object visit(Class expr);
	public Object visit(Field expr);
	public Object visit(Formal expr);
	public Object visit(Type expr);
	public Object visit(Method expr);
	public Object visit(AssignStmt stmt);
	public Object visit(ReturnStmt stmt);
	public Object visit(StaticCall expr);
	public Object visit(VirtCall expr);
	public Object visit(VarLocation expr);
	public Object visit(ArrayLocation expr);
	public Object visit(CallStmt stmt);
	public Object visit(StmtList stmt);
	public Object visit(IfStmt stmt);
	public Object visit(WhileStmt stmt);
	public Object visit(BreakStmt stmt);
	public Object visit(ContinueStmt stmt); 
	public Object visit(LocalVarStmt stmt);
	public Object visit(ThisExpr expr);
	public Object visit(NewClassExpr expr);
	public Object visit(NewArrayExpr expr);
	public Object visit(LengthExpr expr);
	public Object visit(LiteralExpr expr);
}*/