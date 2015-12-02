package slp;

/** Pretty-prints an SLP AST.
 */
public class PrettyPrinter implements Visitor {
	protected final ASTNode root;
	protected int indentLvl = -1;
	protected int indentStep = 2;

	protected void printIndented(ASTNode node){
		StringBuilder output = new StringBuilder();
		
		indentLvl += indentStep;
		for(int i = 0; i < indentLvl; i++)
			output.append(i%2 == 1 ? "|" : " ");
		System.out.print(output);
		node.accept(this);
		indentLvl -= indentStep;
	}
	
	/** Constructs a printin visitor from an AST.
	 * 
	 * @param root The root of the AST.
	 */
	public PrettyPrinter(ASTNode root) {
		this.root = root;
	}

	/** Prints the AST with the given root.
	 */
	public void print() {
		root.accept(this);
	}
	
	@Override
	public void visit(StmtList stmts) {
		System.out.println(stmts.line + ": Block of statements");
		for (Stmt s : stmts.statements)
			printIndented(s);
	}
	
	@Override
	public void visit(AssignStmt stmt) {
		System.out.println(stmt.line + ": Assignment statement");
		printIndented(stmt.lhs);
		printIndented(stmt.rhs);
	}
	
	@Override
	public void visit(UnaryOpExpr expr) {
		System.out.println(expr.line + ": " + expr.operator.type + " unary operation: " + expr.operator.name);
		printIndented(expr.rightOp);
	}
	
	@Override
	public void visit(BinaryOpExpr expr) {
		System.out.println(expr.line + ": " + expr.operator.type + " binary operation: " + expr.operator.name);
		printIndented(expr.leftOp);
		printIndented(expr.rightOp);
	}

	@Override
	public void visit(Program expr) {
		for (Class c : expr.classes)
			printIndented(c);
	}

	@Override
	public void visit(Class expr) {
		System.out.println(expr.line + ": Declaration of class: " + expr);
		
		for (Field f : expr.fields)
			printIndented(f);
		for (Method m : expr.methods)
			printIndented(m);
	}

	@Override
	public void visit(Field expr) {
		System.out.println(expr.line + ": Declaration of field: " + expr.name);
		printIndented(expr.type);
	}

	@Override
	public void visit(Formal expr) {
		System.out.println(expr.line + ": Parameter: " + expr.name);
		printIndented(expr.type);
	}

	@Override
	public void visit(Type expr) {
		System.out.print(expr.line + ": ");
		if (expr instanceof PrimitiveType)
			System.out.print("Primitive ");
		else
			System.out.print("User-defined ");
				
		System.out.println("data type: " + expr.getName());
	}

	@Override
	public void visit(Method expr) {
		System.out.print(expr.line + ": Declaration of ");
		System.out.println((expr.isStatic ? "static " : "virtual ") + "method: " + expr.name);

		printIndented(expr.type);

		for (Formal f : expr.formalList)
			printIndented(f);
		for (Stmt s : expr.statementList)
			printIndented(s);
	}

	@Override
	public void visit(ReturnStmt stmt) {
		System.out.print(stmt.line + ": Return statement");
		if (stmt.expr != null){
			System.out.println(", with return value");
			printIndented(stmt.expr);
		}
		else 
			System.out.println();
	}

	@Override
	public void visit(StaticCall expr) {
		System.out.println(expr.line + ": Call to static method: " + expr.funcName + ", in class "+expr.className);
		for (Expr arg : expr.args)
			printIndented(arg);
	}

	@Override
	public void visit(VirtCall expr) {
		System.out.print(expr.line + ": Call to virtual method: " + expr.funcName);
		if(expr.location != null){
			System.out.println(", in the context of");
			printIndented(expr.location);
		}
		else
			System.out.println();

		for (Expr arg : expr.args)
			printIndented(arg);
	}

	@Override
	public void visit(VarLocation expr) {
		System.out.print(expr.line + ": Reference to variable: " + expr.name);
		if(expr.location != null){
			System.out.println(", in the context of");
			printIndented(expr.location);
		}
		else
			System.out.println();
	}

	@Override
	public void visit(ArrayLocation expr) {
		System.out.println(expr.line + ": Reference to array");
		printIndented(expr.array);
		printIndented(expr.index);
	}

	@Override
	public void visit(IfStmt stmt) {
		System.out.println(stmt.line + ": If statement");
		printIndented(stmt.condition);
		printIndented(stmt.thenStmt);

		if(stmt.elseStmt != null)
			printIndented(stmt.elseStmt);
	}

	@Override
	public void visit(WhileStmt stmt) {
		System.out.println(stmt.line + ": While statement");
		printIndented(stmt.condition);
		printIndented(stmt.thenStmt);
	}
	
	@Override
	public void visit(BreakStmt stmt) {
		System.out.println(stmt.line + ": Break statement");
	}
	
	@Override
	public void visit(ContinueStmt stmt) {
		System.out.println(stmt.line + ": Continue statement");
	}
	
	@Override
	public void visit(LocalVarStmt stmt) {
		System.out.print(stmt.line + ": Declaration of local variable: " + stmt.name);
		System.out.println(stmt.init != null ? ", with initial value" : "");

		printIndented(stmt.type);

		if(stmt.init != null)
			printIndented(stmt.init);
	}

	@Override
	public void visit(ThisExpr expr) {
		System.out.println(expr.line + ": Reference to THIS");
	}

	@Override
	public void visit(NewClassExpr expr) {
		System.out.println(expr.line + ": Instantiation of class: " + expr.name);
	}

	@Override
	public void visit(NewArrayExpr expr) {
		System.out.println(expr.line + ": Array allocation");
		printIndented(expr.type);
		printIndented(expr.index);
	}
	
	@Override
	public void visit(LengthExpr expr) {
		System.out.println(expr.line + ": Reference to array length");
		printIndented(expr.context);
	}

	@Override
	public void visit(CallStmt stmt) {
		System.out.println(stmt.line + ": Method call statement");
		printIndented(stmt.call);
	}

	@Override
	public void visit(LiteralExpr expr) {
		System.out.print(expr.line + ": ");
		switch(expr.type){
		case QUOTE:
			System.out.println("String literal: \"" + expr.value + "\""); break;
		case INTEGER:
			System.out.println("Integer literal: " + expr.value); break;
		case NULL:
			System.out.println("NULL literal"); break;
		case TRUE:
		case FALSE:
			System.out.println("Boolean literal: " + expr.value);
		}
	}
}