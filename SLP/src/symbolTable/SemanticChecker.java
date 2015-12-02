package symbolTable;

import java.util.ArrayList;
import java.util.List;

import semanticTypes.*;
import slp.*;
import slp.Class;

/**
 * Implemented checks:
 * - in-class symbol redefinitions and overloading
 * - in-method symbol redefinitions (parameters and local variables)
 * - base-derived class symbol redefinitions and overriding
 * - recursive type declarations
 * - Library functions declaration
 * - exactly one "main" method
 * - break, continue only inside loops
 */
public class SemanticChecker implements PropagatingVisitor<Object, Object> {
	private SymbolTable symTab;
	private TypeTable typTab;
	private ASTNode root;
	private boolean mainDefined = false;
	private int loopLevel = 0;
	
	public SemanticChecker(ASTNode root) {
		this.root = root;
		symTab = new SymbolTable();
		typTab = new TypeTable();
		
		addLibraryClass();
	}
	
	public void start() {
		root.accept(this, null);
	}
	
	private boolean isMain(Method m){
		if (m.isStatic)  
			if (m.type.getName().equals("void")) 
				if (m.name.equals("main")) 
					if(m.formalList.size() == 1)
						if(m.formalList.get(0).name.equals("args"))
							if(m.formalList.get(0).type.getName().equals("string[]")) 
								return true;

		return false;
	}

	// add class "Library" with all library methods to the symbol table
	private void addLibraryClass(){
		ClassSymbol sym = null;
		try{
			sym = new ClassSymbol("Library", symTab);
			List<ParamSymbol> params = new ArrayList<ParamSymbol>();

			// void param
			sym.addMethodSymbol("readi", typTab.intType, params, true);
			sym.addMethodSymbol("readln", typTab.stringType, params, true);
			sym.addMethodSymbol("eof", typTab.booleanType, params, true);
			sym.addMethodSymbol("time", typTab.intType, params, true);
			
			// (string) param
			params.add(new ParamSymbol("s", typTab.stringType));
			sym.addMethodSymbol("print", typTab.voidType, params, true);
			sym.addMethodSymbol("println", typTab.voidType, params, true);
			sym.addMethodSymbol("stoa", typTab.resolveType("int[]"), params, true);
			
			// (string, int) params
			params.add(new ParamSymbol("n", typTab.intType));
			sym.addMethodSymbol("stoi", typTab.intType, params, true);
			
			// (int) param
			params.clear();
			params.add(new ParamSymbol("i", typTab.intType));
			sym.addMethodSymbol("printi", typTab.voidType, params, true);
			sym.addMethodSymbol("itos", typTab.stringType, params, true);
			sym.addMethodSymbol("random", typTab.intType, params, true);
			sym.addMethodSymbol("exit", typTab.voidType, params, true);
			
			// (boolean) param
			params.clear();
			params.add(new ParamSymbol("b", typTab.booleanType));
			sym.addMethodSymbol("printb", typTab.voidType, params, true);
			
			// (int[]) param
			params.clear();
			params.add(new ParamSymbol("a", typTab.resolveType("int[]")));
			sym.addMethodSymbol("atos", typTab.voidType, params, true);
		} catch (SemanticError se){		// should never fail
			System.out.println("0: "+se);
			System.exit(1);
		}
			
		symTab.addEntry(sym);
	}
	
	@Override
	public Object visit(UnaryOpExpr unary, Object d) {
		unary.rightOp.accept(this, null);
		return null;
	}

	@Override
	public Object visit(BinaryOpExpr binary, Object d) {
		binary.leftOp.accept(this, null);
		binary.rightOp.accept(this, null);
		return null;
	}

	@Override
	public Object visit(Program program, Object d) {
		// check and add all class names to type table
		for (Class cl: program.classes){
			try{
				typTab.addClassType(cl.name, cl.superName);
			} catch (SemanticError se){
				System.out.println(""+cl.line + ": "+se);
				System.exit(1);
			}
		}
		
		for (Class cl: program.classes){
			ClassSymbol sym = null;
			try{
				if (cl.superName != null) {
					sym = new ClassSymbol(cl.name, cl.superName, typTab, symTab);
				} else { // no superclass
					sym = new ClassSymbol(cl.name, symTab);
				}
			} catch (SemanticError se){
				System.out.println(""+cl.line + ": "+se);
				System.exit(1);
			}
			
			for (Field f: cl.fields){
				try{
					sym.addFieldSymbol(f.name, typTab.resolveType(f.type.getName()));
				} catch (SemanticError se){
					System.out.println(""+f.line + ": "+se);
					System.exit(1);
				}
			}
			
			for (Method m: cl.methods){
				try{
					List<ParamSymbol> params = new ArrayList<ParamSymbol>();
					for (Formal f: m.formalList){
						params.add(new ParamSymbol(f.name, typTab.resolveType(f.type.getName())));
					}
					sym.addMethodSymbol(m.name, typTab.resolveType(m.type.getName()), params, m.isStatic);
				} catch (SemanticError se){
					System.out.println(""+m.line + ": "+se);
					System.exit(1);
				}
				
				if(isMain(m)){
					if(mainDefined == true){
						System.out.println(""+m.line +": Semantic error: main function already defined");
						System.exit(1);
					}
					else
						mainDefined = true;
				}
			}
			symTab.addEntry(sym);
		}
		
		if (!mainDefined){
			System.out.println(""+program.line + ": Semantic error: no main method found");
			System.exit(1);
		}
		
		for (Class cl: program.classes){
			symTab.enterScope();
			cl.accept(this, null);
			symTab.exitScope();
		}
	
		return null;
	}

	@Override
	public Object visit(Class cl, Object d) {
		for (Method m: cl.methods){
			try{
				List<ParamSymbol> params = new ArrayList<ParamSymbol>();
				for (Formal f: m.formalList){
					params.add(new ParamSymbol(f.name, typTab.resolveType(f.type.getName())));
				}
				symTab.addEntry(new MethodSymbol(m.name, typTab.resolveType(m.type.getName()), params, m.isStatic));
			} catch (SemanticError se){
				System.out.println(""+m.line + ": "+se);
				System.exit(1);
			}
		}
		for (Field f: cl.fields){
			try{
			symTab.addEntry(new FieldSymbol(f.name, typTab.resolveType(f.type.getName())));
			} catch (SemanticError se){
				System.out.println(""+f.line + ": "+se);
				System.exit(1);
			}
		}
		
		for (Method m: cl.methods){
			symTab.enterScope();
			m.accept(this, null);
			symTab.exitScope();
		}

		return null;
	}

	@Override
	public Object visit(Method method, Object d) {
		try{
			symTab.addEntry(new ParamSymbol("return", typTab.resolveType(method.type.getName())));
			for (Formal f: method.formalList)
				symTab.addEntry(new ParamSymbol(f.name, typTab.resolveType(f.type.getName())));
		} catch (SemanticError se){
			System.out.println(""+method.type.line + ": "+se);
			System.exit(1);
		}
		for (Stmt s: method.statementList)
			s.accept(this, null);
		return null;
	}
	
	@Override
	public Object visit(Field expr, Object d) {
		// nothing to to here
		return null;
	}

	@Override
	public Object visit(Formal expr, Object d) {
		// nothing to to here
		return null;
	}

	@Override
	public Object visit(Type expr, Object d) {
		// nothing to to here
		return null;
	}

	@Override
	public Object visit(AssignStmt assignStmt, Object d) {
		assignStmt.lhs.accept(this, null);
		assignStmt.rhs.accept(this, null);
		return null;
	}

	@Override
	public Object visit(ReturnStmt returnStmt, Object d) {
		if (returnStmt.expr != null)
			returnStmt.expr.accept(this, null);
		return null;
	}

	@Override
	public Object visit(StaticCall staticCall, Object d) {
		for (Expr arg: staticCall.args)
			arg.accept(this, null);
		return null;
	}

	@Override
	public Object visit(VirtCall virtCall, Object d) {
		if (virtCall.location != null) { 
			virtCall.location.accept(this, null);
		}
		
		for (Expr arg: virtCall.args)
			arg.accept(this, null);
		return null;
	}

	@Override
	public Object visit(VarLocation varLoc, Object d) {
		if (varLoc.location != null){
			varLoc.location.accept(this, null);
		}else if(symTab.findEntryGlobal(varLoc.name) == null){
			System.out.println(""+varLoc.line + ": Semantic error: undefined variable: " + varLoc.name);
			System.exit(1);
		}
		return null;
	}

	@Override
	public Object visit(ArrayLocation arrayLoc, Object d) {
		arrayLoc.array.accept(this, null);
		arrayLoc.index.accept(this, null);
		return null;
	}

	@Override
	public Object visit(CallStmt callStmt, Object d) {
		callStmt.call.accept(this, null);
		return null;
	}

	@Override
	public Object visit(StmtList stmtList, Object d) {
		symTab.enterScope();

		for (Stmt s: stmtList.statements)
			s.accept(this, null);
		
		symTab.exitScope();
		return null;
	}

	@Override
	public Object visit(IfStmt ifStmt, Object d) {
		ifStmt.condition.accept(this, null);
		
		symTab.enterScope();
		ifStmt.thenStmt.accept(this, null);
		symTab.exitScope();
		
		if (ifStmt.elseStmt != null){
			symTab.enterScope();
			ifStmt.elseStmt.accept(this, null);
			symTab.exitScope();
		}
		return null;
	}

	@Override
	public Object visit(WhileStmt whileStmt, Object d) {
		whileStmt.condition.accept(this, null);
		
		symTab.enterScope();
		++loopLevel;
		whileStmt.thenStmt.accept(this, null);
		--loopLevel;
		symTab.exitScope();
		return null;
	}

	@Override
	public Object visit(BreakStmt breakStmt, Object d) {
		if (loopLevel == 0){
			System.out.println(""+breakStmt.line + ": Semantic error: break statement outside of loop");
			System.exit(1);
		}
		
		return null;
	}

	@Override
	public Object visit(ContinueStmt contStmt, Object d) {
		if (loopLevel == 0){
			System.out.println(""+contStmt.line + ": Semantic error: continue statement outside of loop");
			System.exit(1);
		}
		
		return null;
	}

	@Override
	public Object visit(LocalVarStmt localVar, Object d) {
		if(symTab.findEntryLocal(localVar.name) != null){
			System.out.println(""+localVar.line + ": Semantic error: variable redefinition: "+localVar.name);
			System.exit(1);
		}

		try{
			symTab.addEntry(new VarSymbol(localVar.name, typTab.resolveType(localVar.type.getName())));
		} catch (SemanticError se){
			System.out.println(""+localVar.type.line + ": "+se);
			System.exit(1);
		}
		
		// has initializer
		if (localVar.init != null)
			localVar.init.accept(this, null);
		
		return null;
	}

	@Override
	public Object visit(ThisExpr expr, Object d) {
		return null;
	}

	@Override
	public Object visit(NewClassExpr newClass, Object d) {
		return null;
	}

	@Override
	public Object visit(NewArrayExpr newArray, Object d) {
		newArray.type.accept(this, null);
		newArray.index.accept(this, null);
		return null;
	}

	@Override
	public Object visit(LengthExpr len, Object d) {
		len.context.accept(this, null);
		return null;
	}

	@Override
	public Object visit(LiteralExpr literal, Object d) {
		return null;
	}

}
