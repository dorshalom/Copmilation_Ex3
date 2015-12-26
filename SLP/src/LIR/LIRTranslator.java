package LIR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import slp.*;
import slp.Class;
import semanticTypes.*;
import symbolTable.*;


public class LIRTranslator implements PropagatingVisitor<Object, LIRUpType> {

	// identifier for current while loop
	private int currWhileIdentifier = -1;
	// count the number of labels we have seen
	private int labelNumber = 0;
	// map each literal string to the format 'str[i]'
	private List<String> strLiterals = new ArrayList<String>();
	// list of methods' lir code
	private List<String> methodsCodeList = new ArrayList<String>();
	// main method's lir code
	private String mainMethodCode="";
	// dispatch table lir code
	private List<String> classDispatchTableCodeList = new ArrayList<String>();
	// dispatch table map - maps class names to a list of <method, belongsToClassName>
	protected Map<String, HashMap<Integer,ArrayList<String>>> dispatchTableMap = new HashMap<String,HashMap<Integer,ArrayList<String>>>();
	
	private int curReg = 1;
	private ASTNode root;
	private SymbolTable symTab;
	private TypeTable typTab;
	
	private String currentThisClass;
	private String currentMethodName;

	
	public LIRTranslator(ASTNode root, SymbolTable symTab, TypeTable typTab){
		this.root = root;
		this.symTab = symTab;
		this.typTab = typTab;
	}
	
	String runtimeChecks(){
		StringBuilder st = new StringBuilder("\n# runtime checks #\n");
		
		st.append("__checkNullRef:\n"); 
		st.append("Move a, R0\n");
		st.append("Compare 0, R0\n");
		st.append("JumpTrue _error_checkNullRef\n");
		st.append("Return Rdummy\n");
		st.append("_error_checkNullRef:\n");
		st.append("Library __println(error_null_ref), Rdummy\n");
		st.append("Library __exit(1), Rdummy\n\n");

		st.append("__checkArrayAccess:\n");
		st.append("ArrayLength a, R0\n");
		st.append("Compare i, R0\n");
		st.append("JumpLE _error_checkArrayAccess\n");
		st.append("Move i, R0\n");
		st.append("Compare 0, R0\n");
		st.append("JumpL _error_checkArrayAccess\n");
		st.append("Return Rdummy\n");
		st.append("_error_checkArrayAccess:\n");
		st.append("Library __println(error_array_bounds), Rdummy\n");
		st.append("Library __exit(1), Rdummy\n\n");
	
		st.append("__checkSize:\n");
		st.append("Move n, R0\n");
		st.append("Compare 0, R0\n");
		st.append("JumpL _error_checkSize\n");
		st.append("Return Rdummy\n");
		st.append("_error_checkSize:\n");
		st.append("Library __println(error_array_negative), Rdummy\n");
		st.append("Library __exit(1), Rdummy\n\n");

		st.append("__checkZero:\n");
		st.append("Move b, R0\n");
		st.append("Compare 0, R0\n");
		st.append("JumpTrue _error_checkZero\n");
		st.append("Return Rdummy\n");
		st.append("_error_checkZero:\n");
		st.append("Library __println(error_zero_division), Rdummy\n");
		st.append("Library __exit(1), Rdummy\n");
			
		return st.toString();
	}
	
	@Override
	public LIRUpType visit(Program program, Object o) {
		/////// fill dispatch table: ///////
		for(Class cl: program.classes){
			dispatchTableMap.put(cl.name, new HashMap<Integer,ArrayList<String>>());
			int j = 0; // j will be the methods offset
			
			// has super - clone methods list from super class, if the method doesn't exist in the current class.
			if(cl.superName != null){ 	
					
				HashMap<Integer,ArrayList<String>> supersMethodsMap = dispatchTableMap.get(cl.superName);
				for (int i=0;i<supersMethodsMap.keySet().size();i++){
					String mName = supersMethodsMap.get(i).get(0);
					String belongName = supersMethodsMap.get(i).get(1);
					if (!cl.hasMethodWithName(mName)){  // if the method doesn't exist in the current class
						ArrayList<String> methodDetails = new ArrayList<String>(2);
						methodDetails.add(mName); methodDetails.add(belongName);
						dispatchTableMap.get(cl.name).put(j, methodDetails);
						j++;
					}
				}	
			}
			
			// insert new methods into dispatch table map (if not static)
			for (Method m: cl.methods){
				
				if(!m.isStatic){
					ArrayList<String> methodDetails = new ArrayList<String>(2);
					methodDetails.add(m.name); methodDetails.add(cl.name);
					dispatchTableMap.get(cl.name).put(j, methodDetails);
					j++;			
				}
			}
		}
		
		/////// visit all classes in the program //////
		for(Class cl: program.classes){
			symTab.enterScope();
			cl.accept(this, null);
			symTab.exitScope();
		}
		
		String lirCode = "";
		
		// insert string literals
		lirCode += "# string literals #\n";
		int i = 1;
		for (String str: this.strLiterals){
			lirCode += "str"+i+": \""+str+"\"\n";
			i++;
		}
		lirCode += "\n# error messages #\n";
		lirCode += "error_null_ref: \"Runtime Error: Null pointer dereference!\"\n";
		lirCode += "error_array_bounds: \"Runtime Error: Array index out of bounds!\"\n";
		lirCode += "error_array_negative: \"Runtime Error: Array allocation with negative array size!\"\n";
		lirCode += "error_zero_division: \"Runtime Error: Division by zero!\"\n";
		
		lirCode += "\n# dispatch table #\n";
		// insert dispatch table
		buildDispatchTableCode();
		for (String line: this.classDispatchTableCodeList){
			lirCode += line+"\n";
		}
		
		lirCode += runtimeChecks();
		lirCode+= "\n";
		
		// insert all methods (except main)
		for (String method: this.methodsCodeList){
			lirCode += method+"\n";
		}
		
		lirCode+= "\n";
		
		// insert main method
		lirCode += this.mainMethodCode;
		
		
		return new LIRUpType(lirCode, LIRAstNodeType.EXPLICIT,"");
	}

	@SuppressWarnings("unchecked")
	@Override
	public LIRUpType visit(Class cl, Object o) {
		
		currentThisClass = cl.name; //update current class
		
		// TODO: change offsets to support inheritance
		int fieldOffset = 0;
		for (Field f: cl.fields){
			try{
				symTab.addEntry(new FieldSymbol(f.name, typTab.resolveType(f.type.getName()),fieldOffset));
			} catch (SemanticError se){}
			fieldOffset++;
		}
		
		for(Method m: cl.methods){

			//visit all methods
			symTab.enterScope();
			m.accept(this,1);
			symTab.exitScope();
		}
		
		

		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(Field field, Object o) {
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(Formal formal, Object o) {
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(Type type, Object o) {
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(Method method, Object o) {
		boolean ismain = isMain(method); 
		String methodCode = "";
		currentMethodName = method.name;
		
		// get method headline
		String methodHeadLine="";
		if(ismain){
			methodHeadLine+="_ic_main:\n";
		}
		else{
			methodHeadLine+="_"+currentThisClass+"_"+method.name+":\n";
		}
		methodCode+=methodHeadLine;
		
		try{
			for (Formal f: method.formalList){
				symTab.addEntry(new ParamSymbol("__p_"+f.name, typTab.resolveType(f.type.getName())));
			}
		}catch(SemanticError se){}
		
		// visit all statements and add their code to methodCode
		for (Stmt s: method.statementList){
			methodCode += s.accept(this, null).lirCode;
		}
		
		// as the specs says, if the method is void, we add "Return 9999"
		if(!ismain && method.type.getName().equals("void")){
			methodCode += "Return 9999\n";
		}
		
		// update methodsCode list / main method
		if (ismain){
			mainMethodCode = methodCode;
		} else {
			methodsCodeList.add(methodCode);
		}
		
		//empty return... (because we already updated the methodsCodeList.. no need to return code..)
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(AssignStmt assignStmt, Object o) {
		String str = "";
		
		// translate rhs
		LIRUpType rhs = assignStmt.rhs.accept(this, null);
		str += rhs.lirCode;
		str += getMoveType(rhs.astNodeType);
		str += rhs.register+", ";
		str += "R"+curReg+"\n";
		
		// translate lhs
		++curReg;
		LIRUpType lhs = assignStmt.lhs.accept(this, null);
		--curReg;
		str+= lhs.lirCode;
		
		// handle all variable cases
		str += getMoveType(lhs.astNodeType);
		str += "R"+curReg+", "+lhs.register+"\n";
		
		return new LIRUpType(str, LIRAstNodeType.STATEMENT,"");
	}

	@Override
	public LIRUpType visit(ReturnStmt retStmt, Object o) {
		String str = "";
		if (retStmt.expr != null){
			LIRUpType returnVal = retStmt.expr.accept(this, null);
			str += returnVal.lirCode;
			if (returnVal.astNodeType == LIRAstNodeType.EXTERNALVARLOC){
				str += "MoveField "+returnVal.register+", R"+curReg+"\n";
				str += "Return R"+curReg+"\n";
			}
			else
				str += "Return "+returnVal.register+"\n";
		} else {
			str += "Return 9999\n";
		}
		
		return new LIRUpType(str, LIRAstNodeType.STATEMENT, "");
	}
	
	@Override
	public LIRUpType visit(UnaryOpExpr unaryOp, Object o) {
		String str = "";
		String trueLabel = "_true_label"+labelNumber;
		String endLabel = "_end_label"+(labelNumber++);
		
		LIRUpType rightOp = unaryOp.rightOp.accept(this, null);
		str += rightOp.lirCode;
		str += getMoveType(rightOp.astNodeType);
		str += rightOp.register+", R"+curReg+"\n";
		if (unaryOp.operator == UnaryOpsEnum.LNEG){//This is logical unary operation
			// recursive call to rightOp

			str += "Compare 0, R"+curReg+"\n";
			str += "JumpTrue "+trueLabel+"\n";
			str += "Move 0, R"+curReg+"\n";
			str += "Jump "+endLabel+"\n";
			str += trueLabel+":\n";
			str += "Move 1, R"+curReg+"\n";
			str += endLabel+":\n";

			return new LIRUpType(str, LIRAstNodeType.REGISTER,"R"+curReg);
		}
		else{//This is math unary operation			
			str += "Neg R"+curReg+"\n";
			return new LIRUpType(str, LIRAstNodeType.REGISTER,"R"+curReg);
			
		}
	}

	@Override
	public LIRUpType visit(BinaryOpExpr binaryOp, Object o) {
		String trueLabel = "_true_label"+labelNumber;
		String falseLabel = "_false_label"+labelNumber;
		String endLabel = "_end_label"+(labelNumber++);
		String str = "";
		
		// recursive call to leftOp
		LIRUpType leftOp = binaryOp.leftOp.accept(this, null);
		str += leftOp.lirCode;
		str += getMoveType(leftOp.astNodeType);
		str += leftOp.register+", R"+curReg+"\n";

		++curReg;
		LIRUpType rightOp = binaryOp.rightOp.accept(this, null);
		--curReg;
		str += rightOp.lirCode;
		str += getMoveType(rightOp.astNodeType);
		str += rightOp.register+", R"+(curReg+1)+"\n";

		if (binaryOp.operator.type == "Logical"){
			if (binaryOp.operator != BinaryOpsEnum.LAND && binaryOp.operator != BinaryOpsEnum.LOR){
				str += "Compare R"+(curReg+1)+", R"+curReg+"\n";
			}
			switch (binaryOp.operator){
			case EQUAL:
				str += "JumpTrue "+trueLabel+"\n";
				break;
			case NEQUAL:
				str += "JumpFalse "+trueLabel+"\n";
				break;
			case GT:
				str += "JumpG "+trueLabel+"\n";
				break;
			case GTE:
				str += "JumpGE "+trueLabel+"\n";
				break;
			case LT:
				str += "JumpL "+trueLabel+"\n";
				break;
			case LTE:
				str += "JumpLE "+trueLabel+"\n";
				break;
			case LAND:
				str += "Compare 0, R"+curReg+"\n";
				str += "JumpTrue "+falseLabel+"\n";
				str += "Compare 0, R"+(curReg+1)+"\n";
				str += "JumpTrue "+falseLabel+"\n";
				str += "Jump "+trueLabel+"\n";
				str += falseLabel+":\n"; 
				break;
			case LOR:
				str += "Compare 0, R"+curReg+"\n";
				str += "JumpFalse "+trueLabel+"\n";
				str += "Compare 0, R"+(curReg+1)+"\n";
				str += "JumpFalse "+trueLabel+"\n"; 
				break;
			default:
				System.err.println("* Error in logical binaryOP *");	
			}
			str += "Move 0, R"+curReg+"\n";
			str += "Jump "+endLabel+"\n";
			str += trueLabel+":\n";
			str += "Move 1, R"+curReg+"\n";
			str += endLabel+":\n";

			return new LIRUpType(str, LIRAstNodeType.REGISTER,"R"+curReg);
		}
		else{
			switch (binaryOp.operator){
			case PLUS:
				// check if operation is on strings or on integers
				SemanticType operandType = (SemanticType) binaryOp.rightOp.accept(new ExprTypeResolver(symTab, typTab, currentThisClass, currentMethodName), null);
				if (operandType == typTab.intType){
					str += "Add R"+(curReg+1)+", R"+curReg+"\n";
				} else { // strings
					str += "Library __stringCat(R"+curReg+", R"+(curReg+1)+"), R"+curReg+"\n";
				}
				break;
			case MINUS:
				str += "Sub R"+(curReg+1)+", R"+curReg+"\n";
				break;
			case MULTIPLY:
				str += "Mul R"+(curReg+1)+", R"+curReg+"\n";
				break;
			case DIVIDE:
				// check division by zero
				str += "StaticCall __checkZero(b=R"+(curReg+1)+"), Rdummy\n";
				str += "Div R"+(curReg+1)+", R"+curReg+"\n";
				break;
			case MOD:
				str += "Mod R"+(curReg+1)+", R"+curReg+"\n";
				break;
			default:
				System.err.println("*** YOUR PARSER SUCKS ***");
			}
			
			return new LIRUpType(str, LIRAstNodeType.REGISTER,"R"+curReg);
		}

	}

	@Override
	public LIRUpType visit(StaticCall staticCall, Object o) {
		String str = "";
		
		// recursive calls to all arguments
		int curRegBackup = curReg;
		for (Expr arg: staticCall.args){
			LIRUpType argExp = arg.accept(this, null);
			str += argExp.lirCode;
			if(argExp.astNodeType != LIRAstNodeType.REGISTER){
				str += getMoveType(argExp.astNodeType);
				str += argExp.register+", R"+curReg+"\n";
			}
			curReg++;
		}
		curReg = curRegBackup;
				
		// Library method call
		if (staticCall.className.equals("Library")){
			str += "Library __"+staticCall.funcName+"(";
			// iterate over values (registers)
			for(int i = 0; i < staticCall.args.size(); i++){
				str += "R"+(i+curReg);
				if (i < staticCall.args.size()-1)
					str += ", ";
			}
			str += "), R"+curReg+"\n";
			
			return new LIRUpType(str, LIRAstNodeType.REGISTER, "R"+curReg);
		}
		
		// other static methods
		ClassSymbol cs = (ClassSymbol) symTab.findEntryGlobal(staticCall.className);
		MethodSymbol ms = null;
		try {
			ms = cs.getMethodSymbol(staticCall.funcName);
		} catch (SemanticError se) {}

		// construct method label
		String methodName = "_"+cs.name+"_"+ms.name;
		str += "StaticCall "+methodName+"(";
		// insert <formal>=<argument register>
		for(int i = 0; i < staticCall.args.size(); i++){
			str += ms.params.get(i).name+"=R"+(curReg+i);
			if (i < staticCall.args.size()-1)
				str += ", ";
		}
		str += "), R"+curReg+"\n";
		
		return new LIRUpType(str, LIRAstNodeType.REGISTER, "R"+curReg);
	}

	@Override
	public LIRUpType visit(VirtCall virtCall, Object o) {
		String str = "";
		String className;
		
		// recursive call to call location
		if (virtCall.location != null){
			className = ((SemanticType)virtCall.location.accept(new ExprTypeResolver(symTab, typTab, currentThisClass, currentMethodName), null)).name;
			LIRUpType location = virtCall.location.accept(this, null);
			str += location.lirCode;
			if(location.astNodeType != LIRAstNodeType.REGISTER){
				str += getMoveType(location.astNodeType);
				str += location.register+", R"+curReg+"\n";
			}
			// check location null reference
			str += "StaticCall __checkNullRef(a=R"+curReg+"), Rdummy\n";
		} else {
			className = currentThisClass;
			str += "Move this, R"+curReg+"\n";
		}
		
		int curRegBackup = curReg;
		for (Expr arg: virtCall.args){
			++curReg;
			LIRUpType argExp = arg.accept(this, null);
			str += argExp.lirCode;
			if(argExp.astNodeType != LIRAstNodeType.REGISTER){
				str += getMoveType(argExp.astNodeType);
				str += argExp.register+", R"+curReg+"\n";
			}
		}
		curReg = curRegBackup;
		
		ClassSymbol cs = (ClassSymbol) symTab.findEntryGlobal(className);
		MethodSymbol ms = null;
		try {
			ms = cs.getMethodSymbolRec(virtCall.funcName);
		} catch (SemanticError e) {}
		int offset = 0;
		// find method's offset according to dispatch table
		HashMap<Integer,ArrayList<String>> offsetToMethod = dispatchTableMap.get(cs.name);
		for (int i=0;i<offsetToMethod.size();i++){
			if (offsetToMethod.get(i).get(0).equals(ms.name)){
				offset = i;
				break;
			}
		}
		
		str += "VirtualCall R"+curReg+"."+offset+"(";
		// insert <formal>=<argument register>
		for(int i = 0; i < virtCall.args.size(); i++){
			str += ms.params.get(i).name+"=R"+(curReg+i+1);
			if (i < virtCall.args.size()-1)
				str += ", ";
		}
		str += "), R"+curReg+"\n";
		
		return new LIRUpType(str, LIRAstNodeType.REGISTER, "R"+curReg);	
	}

	@Override
	public LIRUpType visit(VarLocation varLoc, Object o) {
		String str = "";
		
		// location.ID
		if (varLoc.location != null){
			// translate the location
			LIRUpType loc = varLoc.location.accept(this, null);
			// add code to translation
			str += loc.lirCode;
			
			// get the type of the location
			SemanticType locationType = (SemanticType) varLoc.location.accept(new ExprTypeResolver(symTab, typTab, currentThisClass, currentMethodName), null);
			
			// get the field offset
			ClassSymbol cs = (ClassSymbol) symTab.findEntryGlobal(locationType.name);	
			FieldSymbol fs = null;
			try{	// will always succeed
				fs = cs.getFieldSymbolRec(varLoc.name);
			}catch (SemanticError se){}			 
			int fieldOffset = fs.getOffset();
			
			// translate this step
			if(loc.astNodeType != LIRAstNodeType.REGISTER){
				str += getMoveType(loc.astNodeType);
				str += loc.register+", R"+curReg+"\n";
			}
			
			// check external location null reference
			str += "StaticCall __checkNullRef(a=R"+curReg+"), Rdummy\n";
			
			return new LIRUpType(str, LIRAstNodeType.EXTERNALVARLOC, "R"+curReg+"."+fieldOffset);
		// ID
		}else{
			String localVarName = "";
			int scopeLevel = symTab.findScopeLevel(varLoc.name);
			if (scopeLevel > 2)	// its' a local variable
				localVarName = varLoc.name + scopeLevel;
			else if (symTab.findEntryGlobal("__p_"+varLoc.name) != null) // it's a function parameter => leave it's name as is
				localVarName = varLoc.name;
			else if (scopeLevel == 2){	// it's a field of THIS
				ClassSymbol cs = (ClassSymbol) symTab.findEntryGlobal(currentThisClass);
				FieldSymbol fs = null;
				try{
					fs = cs.getFieldSymbolRec(varLoc.name);
				}catch(SemanticError se){}
				int fieldOffset = fs.getOffset();
				
				str += "Move this, R"+curReg+"\n";

				return new LIRUpType(str, LIRAstNodeType.EXTERNALVARLOC, "R"+curReg+"."+fieldOffset);
			}
			return new LIRUpType("",LIRAstNodeType.LOCALVARLOC, localVarName);
		}
	}

	@Override
	public LIRUpType visit(ArrayLocation arrLoc, Object o) {
		String str = "";
		
		// visit array
		LIRUpType array = arrLoc.array.accept(this, null);
		str += array.lirCode;
		if(array.astNodeType != LIRAstNodeType.REGISTER){
			str += getMoveType(array.astNodeType);
			str += array.register+", R"+curReg+"\n";
		}
		// check array null reference
		str += "StaticCall __checkNullRef(a=R"+curReg+"), Rdummy\n";
		
		// visit index
		++curReg;
		LIRUpType index = arrLoc.index.accept(this, null);
		--curReg;
		str += index.lirCode;
		if(index.astNodeType != LIRAstNodeType.REGISTER){
			str += getMoveType(index.astNodeType);
			str += index.register+", R"+(curReg+1)+"\n";
		}
		
		// check array access
		str += "StaticCall __checkArrayAccess(a=R"+curReg+", i=R"+(curReg+1)+"), Rdummy\n";
		
		return new LIRUpType(str, LIRAstNodeType.ARRAYLOC,"R"+curReg+"[R"+(curReg+1)+"]");
	}

	@Override
	public LIRUpType visit(CallStmt callStmt, Object o) {
		return callStmt.call.accept(this, null);
	}

	@Override
	public LIRUpType visit(StmtList stmtList, Object o) {
		String str = "";
		
		symTab.enterScope();
		for (Stmt s: stmtList.statements)
			str += s.accept(this, null).lirCode;
		
		symTab.exitScope();
		return new LIRUpType(str, LIRAstNodeType.STATEMENT,"");
	}

	@Override
	public LIRUpType visit(IfStmt ifStatement, Object o) {
		String str = "";
		String falseLabel = "_false_label"+labelNumber;
		String endLabel = "_end_label"+(labelNumber++);
		
		// recursive call the condition expression
		LIRUpType condExp = ifStatement.condition.accept(this, null);
		str += condExp.lirCode;
		if (condExp.astNodeType != LIRAstNodeType.REGISTER){
			str += getMoveType(condExp.astNodeType);
			str += condExp.register+", R"+curReg+"\n";
		}
		// check condition
		str += "Compare 0, R"+curReg+"\n";
		if (ifStatement.elseStmt!=null) str += "JumpTrue "+falseLabel+"\n";
		else str += "JumpTrue "+endLabel+"\n";
		
		// recursive call to the then statement
		symTab.enterScope();
		LIRUpType thenStat = ifStatement.thenStmt.accept(this, null);
		symTab.exitScope();
		str += thenStat.lirCode;
		
		if (ifStatement.elseStmt!=null){
			str += "Jump "+endLabel+"\n";

			// recursive call to the else statement
			str += falseLabel+":\n";
			symTab.enterScope();
			LIRUpType elseStat = ifStatement.elseStmt.accept(this, null);
			symTab.exitScope();
			str += elseStat.lirCode;
		}
		
		str += endLabel+":\n";
		
		return new LIRUpType(str, LIRAstNodeType.STATEMENT,"");
	}

	@Override
	public LIRUpType visit(WhileStmt whileStmt, Object o) {
		int prevWhileID = currWhileIdentifier;
		currWhileIdentifier = labelNumber;
		
		String str = "";
		String whileLabel = "_while_cond_label"+labelNumber;
		String endLabel = "_end_label"+(labelNumber++);
		
		str += whileLabel+":\n";
		// recursive call to condition statement
		LIRUpType conditionExp = whileStmt.condition.accept(this, null);
		str += conditionExp.lirCode;
		if (conditionExp.astNodeType != LIRAstNodeType.REGISTER){
			str += getMoveType(conditionExp.astNodeType);
			str += conditionExp.register+", R"+curReg+"\n";
		}
		// check condition
		str += "Compare 0, R"+curReg+"\n";
		str += "JumpTrue "+endLabel+"\n";
		
		// recursive call to operation statement
		symTab.enterScope();
		str += whileStmt.thenStmt.accept(this, null).lirCode;
		symTab.exitScope();
		str += "Jump "+whileLabel+"\n";
		str += endLabel+":\n";
		
		currWhileIdentifier = prevWhileID;
		return new LIRUpType(str, LIRAstNodeType.STATEMENT,"");
	}

	@Override
	public LIRUpType visit(BreakStmt breakStmt, Object o) {
		String str = "Jump _end_label"+currWhileIdentifier+"\n";
		return new LIRUpType(str, LIRAstNodeType.STATEMENT,"");
	}

	@Override
	public LIRUpType visit(ContinueStmt contStmt, Object o) {
		String str = "Jump _while_cond_label"+currWhileIdentifier+"\n";
		return new LIRUpType(str, LIRAstNodeType.STATEMENT,"");
	}

	@Override
	public LIRUpType visit(LocalVarStmt localVarStmt, Object o) {
		try{
			symTab.addEntry(new VarSymbol(localVarStmt.name, typTab.resolveType(localVarStmt.type.getName())));
		}catch(SemanticError se) {}
		
		String str = "";
		String reg = "R"+curReg;
		
		if (localVarStmt.init != null){
			LIRUpType initVal = localVarStmt.init.accept(this, null);
			str += initVal.lirCode;
			if (initVal.astNodeType != LIRAstNodeType.REGISTER){
				str += getMoveType(initVal.astNodeType);
				str += initVal.register+", "+reg+"\n";
			}
			else
				reg = initVal.register;
			str += "Move "+reg+", "+localVarStmt.name+symTab.scopeLevel+"\n";
		}
		
		return new LIRUpType(str, LIRAstNodeType.STATEMENT,"");
	}

	@Override
	public LIRUpType visit(ThisExpr thisExp, Object o) {
		String str = "Move this, R"+curReg+"\n";
		return new LIRUpType(str, LIRAstNodeType.REGISTER, "R"+curReg);
	}

	@Override
	public LIRUpType visit(NewClassExpr newClassExp, Object o) {
		ClassSymbol cs = (ClassSymbol) symTab.findEntryGlobal(newClassExp.name);
		String str = "Library __allocateObject("+cs.bytesInMemory()+"), R"+curReg+"\n";
		str += "MoveField _DV_"+cs.name+", R"+curReg+".0\n";
		
		return new LIRUpType(str, LIRAstNodeType.REGISTER, "R"+curReg);
	}

	@Override
	public LIRUpType visit(NewArrayExpr newArrExp, Object o) {
		String str = "";
		
		LIRUpType size = newArrExp.index.accept(this, null);
		str += size.lirCode;
		if (size.astNodeType != LIRAstNodeType.REGISTER){
			str += getMoveType(size.astNodeType);
			str += size.register+", R"+curReg+"\n";
		}
		str += "Mul 4, R"+curReg+"\n";
		
		// make sure index is non-negative
		str += "StaticCall __checkSize(n=R"+curReg+"), Rdummy\n";
		str += "Library __allocateArray(R"+curReg+"), R"+curReg+"\n";
		
		return new LIRUpType(str, LIRAstNodeType.REGISTER, "R"+curReg);
	}

	@Override
	public LIRUpType visit(LengthExpr lengthExpr, Object o) {
		String str = "";

		LIRUpType array = lengthExpr.context.accept(this, null);
		str += array.lirCode;
		if (array.astNodeType != LIRAstNodeType.REGISTER){
			str += getMoveType(array.astNodeType);
			str += array.register+", R"+curReg+"\n";
		}
		
		// make sure context is non-null
		str += "StaticCall __checkNullRef(a=R"+curReg+"), Rdummy\n";
		str += "ArrayLength R"+curReg+", R"+curReg+"\n";
		
		return new LIRUpType(str, LIRAstNodeType.REGISTER, "R"+curReg);
	}

	@Override
	public LIRUpType visit(LiteralExpr expr, Object o) {
		String strLiteral = "";
		LiteralsEnum type = expr.type;
		if (type == LiteralsEnum.QUOTE){
			String strVal = ((String) expr.value);
			if (!strLiterals.contains(strVal))
				strLiterals.add(strVal);
			strLiteral = "str"+(strLiterals.indexOf(strVal)+1);	
		}	
		if (type == LiteralsEnum.INTEGER){
			strLiteral = expr.value.toString();
		}
		if (type == LiteralsEnum.NULL){
			strLiteral = "0";
		}
		if (type == LiteralsEnum.FALSE){
			strLiteral = "0";
		}
		if (type == LiteralsEnum.TRUE){
			strLiteral = "1";
		}		
		return new LIRUpType("", LIRAstNodeType.LITERAL,strLiteral);
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
	
	private void buildDispatchTableCode(){
		
		
		
		
		for (Map.Entry<String, HashMap<Integer,ArrayList<String>>> ce: dispatchTableMap.entrySet()){ //go through each class entry		
			String str="";
			str+= "_DV_"+ ce.getKey() +": [";
			 // go through each method entry
			int i;
			for (i=0; i<ce.getValue().keySet().size();i++){
				ArrayList<String> me = ce.getValue().get(i); // me is [methodName,BelgonsToClass]
				str+="_"+me.get(1)+"_"+me.get(0)+",";
			}
			if (i > 0){
				str = str.substring(0, str.length()-1); //chop the last ','
			}
			str+="]";
			classDispatchTableCodeList.add(str);
		}
	
	}
	
	private String getMoveType(LIRAstNodeType type){
		switch(type) {
		case LITERAL: 
		case REGISTER: 
		case LOCALVARLOC: 	 return "Move ";
		case ARRAYLOC: 		 return "MoveArray ";
		case EXTERNALVARLOC: return "MoveField ";
		default:
			System.out.println("Unhandled LIR instruction type");
			return null;
		}
	}
}
