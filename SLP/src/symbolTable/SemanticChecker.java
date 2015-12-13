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
	private boolean inStatic = false;
	private SemanticType currentThisClass = null;
	private boolean writingToVar = false;
	private int controlFlows = 0;
	
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
		SemanticType operandType = (SemanticType) unary.rightOp.accept(this, null);
		if (unary.operator == UnaryOpsEnum.UMINUS){
			if(operandType == typTab.intType)
				return typTab.intType;
			else{
				System.out.println(unary.line+": Semantic error: Operand must be of type int");
	        	System.exit(1);
			}	
		}
		else{	// operator !
			if(operandType == typTab.booleanType)
				return typTab.booleanType;
			else{
				System.out.println(unary.line+": Semantic error: Operand must be of type boolean");
	        	System.exit(1);
			}
		}
		return null;	// will never get here
	}

	@Override
	public Object visit(BinaryOpExpr binary, Object d) {
		// check left op
		SemanticType leftOpType = (SemanticType) binary.leftOp.accept(this, null);
        if (leftOpType == null) return null;
        // check right op
        SemanticType rightOpType = (SemanticType) binary.rightOp.accept(this, null);
        if (rightOpType == null) return null;
        
		switch(binary.operator.name){
		case "&&": case "||":
	        if(!(leftOpType == typTab.booleanType &&  rightOpType == typTab.booleanType)){
	        	System.out.println(binary.line+": Semantic error: Operands must be of type boolean");
	        	System.exit(1);
	        }
	        return typTab.booleanType;
		case ">":case ">=" : case "<": case "<=":
	        if(!(leftOpType == typTab.intType &&  rightOpType == typTab.intType)){
	        	System.out.println(binary.line+": Semantic error: Operands must be of type int");
	        	System.exit(1);
	        }
	        return typTab.booleanType;
	      
		case "+":
			if(leftOpType == typTab.stringType && rightOpType == typTab.stringType){
				return typTab.stringType;
		    }
		    // else, fall through to check if they are both ints
		case "-": case "*": case "/": case "%":
	        if(!(leftOpType == typTab.intType &&  rightOpType == typTab.intType)){
	        	System.out.println(binary.line+": Semantic error: Operands must be of type int");
	        	System.exit(1);
	        }
	        return typTab.intType;
	    
		case "==": case "!=":
	        if(! (leftOpType.isLike(rightOpType) || (rightOpType.isLike(leftOpType))) ){
	        	System.out.println(binary.line+": Semantic error: Operands must be of similar type");
	        	System.exit(1);
	        }
	        return typTab.booleanType;
				
		}

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
				//symTab.addEntry(new ParamSymbol("this", typTab.resolveType(cl.name)));
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
		try {
			currentThisClass = typTab.resolveType(cl.name);
		} catch (SemanticError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		boolean hasReturn = false;
		controlFlows = 1;
		try{
			if (method.isStatic){
				inStatic = true;
			}
			symTab.addEntry(new ParamSymbol("return", typTab.resolveType(method.type.getName())));
			SemanticType methodType = symTab.findEntryGlobal("return").type;
			for (Formal f: method.formalList){
				symTab.addEntry(new ParamSymbol(f.name, typTab.resolveType(f.type.getName())));
			}
			
			for (Stmt s: method.statementList){
				SemanticType stmtType = (SemanticType) s.accept(this, null);
				if (s instanceof ReturnStmt){
					
					if (methodType != stmtType ){
						System.out.println(s.line + ": Semantic error: return type must be "+methodType.name);
						System.exit(1);
					}
					hasReturn = true;
					
				}				
			}
			
			if (methodType!= typTab.voidType && controlFlows > 0 && !hasReturn){
				System.out.println(method.line + ": Semantic error: method must have a return statement of type "+methodType.name);
				System.exit(1);
			}
		} catch (SemanticError se){
			System.out.println(""+method.type.line + ": "+se);
			System.exit(1);
		}

			
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
		
        // check location recursively
        writingToVar = true;
		SemanticType locationType = (SemanticType) assignStmt.lhs.accept(this, null);        
		writingToVar = false;
        if (locationType == null) return null;
        // check assignment recursively
        SemanticType assignmentType = (SemanticType) assignStmt.rhs.accept(this, null);
        if (assignmentType == null) return null;
        
        if(!assignmentType.isLike(locationType)){
        	System.out.println(assignStmt.line+": Semantic error: type mismatch, not of type "+locationType.name);
        	System.exit(1);
        }
        
        
        /* now we want to change isAssigned value of lhs to true*/
        
        // lhs is of type VarLocation
        if (assignStmt.lhs instanceof VarLocation){
        	//local var
        	if (((VarLocation)assignStmt.lhs).location == null){
            VarSymbol lhsVar = (VarSymbol)symTab.findEntryGlobal(((VarLocation)assignStmt.lhs).name);
            lhsVar.isAssigned = true;
        	}
        	
        	/*
        	//external var
        	else{
	            
				try{
		           
					typTab.resolveClassType(locationType.name);
					ClassSymbol cs = (ClassSymbol) symTab.findEntryGlobal(locationType.name);
					FieldSymbol fs = cs.getFieldSymbolRec(((VarLocation)assignStmt.lhs).name);
					fs.isAssigned = true;
				}
				catch (SemanticError se){
	
				}
        	}*/
        }    
 

		return null;
	}

	@Override
	public Object visit(ReturnStmt returnStmt, Object d) {
		--controlFlows;
		SemanticType exprType;
		SemanticType returnType = symTab.findEntryGlobal("return").type;
		if (returnStmt.expr == null){
			exprType = typTab.voidType;
		}
		else{
			exprType = (SemanticType) returnStmt.expr.accept(this, null);
		}
		if ( returnType != exprType){
			System.out.println(returnStmt.line + ": Semantic error: return type must be "+returnType.name);
			System.exit(1);
		}
		
		return exprType;
		
		/*if (methodType != stmtType ){
			System.out.println(s.line + ": Semantic error: return type must be "+methodType.name);
			System.exit(1);
		}*/

	}

	@Override
	public Object visit(StaticCall staticCall, Object d) {
		SemanticType funcType = null;
		MethodSymbol func = null;

		if (staticCall.className != null) { 
			ClassSymbol cl = (ClassSymbol) symTab.findEntryGlobal(staticCall.className);
			if (cl==null){
				System.out.println(staticCall.line+": Semantic error: class "+staticCall.className+" does not exist");
				System.exit(1);
			}
			try {
				func = cl.getMethodSymbolRec(staticCall.funcName);
			} catch (SemanticError e) {
				
			}
			if (func==null){
				System.out.println(staticCall.line+": Semantic error: method "+staticCall.funcName+" does not exist in class "+cl.name );
				System.exit(1);
			}
			funcType = func.type;
			List<SemanticType> callArgsTypes = new ArrayList<SemanticType>();
			
			for (Expr arg: staticCall.args){
				SemanticType argType  = (SemanticType) arg.accept(this, null);
				callArgsTypes.add(argType);
			}
			
			if (!func.checkParamTypes(callArgsTypes)){
	        	System.out.print(staticCall.line+": Semantic error: method " + staticCall.funcName+" expects "+func.params.size()+" argumnets");
	        	if (func.params.size() > 0){
	        		System.out.print(": (");
		        	for (ParamSymbol p: func.params){
		        		System.out.print(" "+p.type.name);
		        	}
		        	System.out.print(" )");
	        	}
	        	
	        	System.exit(1);
			}
		}

		
		return funcType;
	}

	@Override
	public Object visit(VirtCall virtCall, Object d) {
		SemanticType funcType = null;
		MethodSymbol func = null;
		List<SemanticType> callArgsTypes = new ArrayList<SemanticType>();

		// when call is local
		if (virtCall.location == null) { 
			func = (MethodSymbol) symTab.findEntryGlobal(virtCall.funcName);
			if (func==null){
				System.out.println(virtCall.line+": Semantic error: method "+virtCall.funcName+" does not exist");
				System.exit(1);
			}
		}
		
		//when call is external [when we have obj.funcName(...)]
		else { 
			SemanticType locationType = (SemanticType) virtCall.location.accept(this, null);
			if (locationType == null) return null;
			try{
				typTab.resolveClassType(locationType.name);
				ClassSymbol cs = (ClassSymbol) symTab.findEntryGlobal(locationType.name);
				
				try{
					func = cs.getMethodSymbolRec(virtCall.funcName)	;			
				}
				catch (SemanticError se){
					// there is no such method in that class
					System.out.println(virtCall.line +": Semantic error: there is no method "+ virtCall.funcName +" in class "+cs.name);
					System.exit(1);
				}				
			}
			catch (SemanticError se){
				//there is no class like this
				System.out.println(virtCall.line +": Semantic error: "+locationType+" does not exist");
				System.exit(1);
			}
		}

		// check args types
		funcType = func.type;
		
		for (Expr arg: virtCall.args){
			SemanticType argType  = (SemanticType) arg.accept(this, null);
			callArgsTypes.add(argType);
		}
		
		if (!func.checkParamTypes(callArgsTypes)){
        	System.out.print(virtCall.line+": Semantic error: method " + virtCall.funcName+" expects "+func.params.size()+" argumnets");
        	if (func.params.size() > 0){
        		System.out.print(": (");
	        	for (ParamSymbol p: func.params){
	        		System.out.print(" "+p.type.name);
	        	}
	        	System.out.print(" )");
        	}
        	
        	System.exit(1);
		}
		
		
		return funcType;
	}

	@Override
	public Object visit(VarLocation varLoc, Object d) {
		//external
		if (varLoc.location != null){ 
			SemanticType locationType = (SemanticType) varLoc.location.accept(this, null);
			if (locationType == null) return null;
			try{
				typTab.resolveClassType(locationType.name);
				ClassSymbol cs = (ClassSymbol) symTab.findEntryGlobal(locationType.name);
				
				try{
					FieldSymbol fs = cs.getFieldSymbolRec(varLoc.name);					
					return fs.type;
				}
				catch (SemanticError se){
					// there is no such field in that class
					System.out.println(varLoc.line +": Semantic error: there is no field "+ varLoc.name +" in class "+cs.name);
					System.exit(1);
				}
				

				
			}
			catch (SemanticError se){
				//there is no class like this
				System.out.println(varLoc.line +": Semantic error: "+locationType+" does not exist");
				System.exit(1);
			}
		
		// local
		}else {
			Symbol res = (Symbol) symTab.findEntryGlobal(varLoc.name);
			if(res == null){
				System.out.println(""+varLoc.line + ": Semantic error: undefined variable: " + varLoc.name);
				System.exit(1);
			}
			
			/* check if location is assigned */
			if (!(res instanceof FieldSymbol)){
				if (res instanceof VarSymbol){
					if(!writingToVar && !((VarSymbol)res).isAssigned){
						System.out.println(varLoc.line +": Semantic error: "+varLoc.name+" is not assigned");
						System.exit(1);
					}
				}
			}
			return res.type;
			
		}		
		
		return null;
	}

	@Override
	public Object visit(ArrayLocation arrayLoc, Object d) {
		SemanticType arrayElement = null;
		SemanticType arrayType = (SemanticType) arrayLoc.array.accept(this, null);
		if (!typTab.isArrayType(arrayType)){
			System.out.println(""+arrayLoc.line + ": Semantic error: array access to non-array type" );
			System.exit(1);
		}

		SemanticType indexType = (SemanticType) arrayLoc.index.accept(this, null);
		if (indexType != typTab.intType) {
			System.out.println(""+arrayLoc.line + ": Semantic error: array index is not of type int" );
			System.exit(1);
		}
		try{
			arrayElement = typTab.resolveType(arrayType.name.substring(0, arrayType.name.length()-2));
		}catch(SemanticError se){
			System.out.println(""+arrayLoc.line + ": " + se);
			System.exit(1);
		}
		return arrayElement;
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
		if(controlFlows > 0)
			++controlFlows;
		SemanticType conditionType = (SemanticType) ifStmt.condition.accept(this, null);
		if(conditionType != typTab.booleanType){
			System.out.println(ifStmt.line + ": Semantic error: if condition must be of type boolean");
			System.exit(1);
		}
		symTab.enterScope();
		ifStmt.thenStmt.accept(this, null);
		symTab.exitScope();
		
		if (ifStmt.elseStmt != null){
			symTab.enterScope();
			ifStmt.elseStmt.accept(this, null);
			symTab.exitScope();
		}
		

		return typTab.booleanType;
	}

	@Override
	public Object visit(WhileStmt whileStmt, Object d) {
		if(controlFlows > 0)
			++controlFlows;
		SemanticType conditionType = (SemanticType) whileStmt.condition.accept(this, null);
		if (conditionType != typTab.booleanType){
			System.out.println(whileStmt.line + ": Semantic error: while condition must be of type boolean");
			System.exit(1);
		}
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
			if (localVar.init == null){
			symTab.addEntry(new VarSymbol(localVar.name, typTab.resolveType(localVar.type.getName())));
			}
			else {
				symTab.addEntry(new VarSymbol(localVar.name, typTab.resolveType(localVar.type.getName()),true));
			}
		} catch (SemanticError se){
			System.out.println(""+localVar.type.line + ": "+se);
			System.exit(1);
			
		}
		
		// has initialiser
		if (localVar.init != null){
			SemanticType assignType = (SemanticType) localVar.init.accept(this, null);
		
			try {
				if(!assignType.isLike(typTab.resolveType(localVar.type.getName()))){
					System.out.println(localVar.line+": Semantic error: type mismatch, not of type "+localVar.type.getName());
					System.exit(1);
				}
			} catch (SemanticError se) {
				System.out.println(""+localVar.type.line + ": "+se);
				System.exit(1);
			}
		}
				
				
		return null;
	}

	@Override
	public Object visit(ThisExpr expr, Object d) {
		if(inStatic){
			System.out.println(""+expr.line + ": Semantic error: 'this' referenced in static method");
			System.exit(1);
		}
		//return symTab.findEntryGlobal("this").type;
		return currentThisClass;
	}

	@Override
	public Object visit(NewClassExpr newClass, Object d) {
		SemanticType classType = null;
		try{
			classType = typTab.resolveClassType(newClass.name);
		}catch(SemanticError se){
			System.out.println(""+newClass.line + ": " + se);
			System.exit(1);
		}
		return classType;
	}

	@Override
	public Object visit(NewArrayExpr newArray, Object d) {
		SemanticType arrayType = null;
		try{
			arrayType = typTab.resolveArrayType(newArray.type.getName());
		}catch(SemanticError se){
			System.out.println(""+newArray.line + ": " + se);
			System.exit(1);
		}
		SemanticType indexType = (SemanticType) newArray.index.accept(this, null);
		if (indexType != typTab.intType) {
			System.out.println(""+newArray.line + ": Semantic error: array size is not of type int" );
			System.exit(1);
		}
		return arrayType;
	}

	@Override
	public Object visit(LengthExpr len, Object d) {
		SemanticType contextType = (SemanticType) len.context.accept(this, null);
		if (!typTab.isArrayType(contextType)){
			System.out.println(""+len.line + ": Semantic error: cannot apply operator 'length' to non-array type" );
			System.exit(1);
		}
		return typTab.intType;
	}

	@Override
	public Object visit(LiteralExpr literal, Object d) {
		switch(literal.type){
		case INTEGER:
			return typTab.intType;
		case FALSE:
			return typTab.booleanType;
		case TRUE:
			return typTab.booleanType;
		case QUOTE:
			return typTab.stringType;
		case NULL:
			return typTab.nullType;
		}
		return null;
	}

}
