package LIR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import slp.*;
import slp.Class;
import semanticTypes.*;
import symbolTable.*;


public class LIRTranslator implements PropagatingVisitor<Integer, LIRUpType> {

	
	// identifier for current while loop
	protected int currWhileIdentifier = -1;
	// count the number of labels we have seen
	protected int labelNumber = 0;
	// count the number of string literals we have seen
	protected int strLiteralsNumber = 0;
	// map each literal string to the format 'str[i]'
	protected List<String> strLiterals = new ArrayList<String>();
	// list of methods' lir code
	protected List<String> methodsCodeList = new ArrayList<String>();
	// main method's lir code
	protected String mainMethodCode="";
	// dispatch table lir code
	protected List<String> classDispatchTableCodeList = new ArrayList<String>();
	// dispatch table map - maps class names to a list of <method, belongsToClassName>
	protected Map<String, HashMap<String,String>> dispatchTableMap = new HashMap<String,HashMap<String,String>>();
	
	private ASTNode root;
	private SymbolTable symTab;
	private TypeTable typTab;
	
	private String currentThisClass;

	
	public LIRTranslator(ASTNode root, SymbolTable symTab, TypeTable typTab){
		this.root = root;
		this.symTab = symTab;
		this.typTab = typTab;
		
	}
	

	@Override
	public LIRUpType visit(Program program, Integer d) {
		// visit all classes in the program
		for(Class cl: program.classes){
			// TODO: this check is redundant. we won't have a "Library" class in our program ever.
			if (!cl.name.equals("Library")){ //skip library class
				symTab.enterScope();
				cl.accept(this, 1);
				symTab.exitScope();
			}
		}
		
		String lirCode = "";
		
		// insert string literals
		lirCode += "# string literals #\n";
		int i = 1;
		for (String str: this.strLiterals){
			lirCode += "str"+i+": \""+str+"\"\n";
			i++;
		}
		lirCode+= "\n # dispatch table #\n";
		
		
		
		// insert dispatch table
		buildDispatchTableCode();
		for (String line: this.classDispatchTableCodeList){
			lirCode += line+"\n";
		}
		
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
	public LIRUpType visit(Class cl, Integer d) {
		
		currentThisClass = cl.name; //update current class
		
		
		// fill dispatch table:
		if(cl.superName == null){ // has no super 
			dispatchTableMap.put(cl.name, new HashMap<String,String>());
		}
		else{ // has super - clone methods list from super class
			dispatchTableMap.put(cl.name, (HashMap<String, String>) dispatchTableMap.get(cl.superName).clone());
		}
		
		// TODO: change offsets to support inheritance
		int fieldOffset = 0;
		for (Field f: cl.fields){
			try{
				symTab.addEntry(new FieldSymbol(f.name, typTab.resolveType(f.type.getName()),fieldOffset));
			} catch (SemanticError se){}
			fieldOffset++;
		}
		
		
		for(Method m: cl.methods){
			// insert new methods into dispatch table map (if not static)
			if(!m.isStatic){
				// if method already exists because of super class, than it will be overridden
				dispatchTableMap.get(cl.name).put(m.name, cl.name);
			}
			
			//visit all methods
			symTab.enterScope();
			m.accept(this,1);
			symTab.exitScope();

		}
		
		

		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(Field field, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(Formal formal, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(Type type, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(Method method, Integer d) {
		boolean ismain = isMain(method); 
		String methodCode = "";
		
		// get method headline
		String methodHeadLine="";
		if(ismain){
			methodHeadLine+="_ic_main:\n";
		}
		else{
			methodHeadLine+="_"+currentThisClass+"_"+method.name+":\n";
		}
		methodCode+=methodHeadLine;
		
		// visit all statements and add their code to methodCode
		for (Stmt s: method.statementList){
			methodCode += s.accept(this,1).lirCode;
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
	public LIRUpType visit(AssignStmt assignStmt, Integer d) {
		String str = "";
		// translate rhs
		LIRUpType rhs = assignStmt.rhs.accept(this, d);
		str += rhs.lirCode;
		str += getMoveType(rhs.astNodeType);
		str += rhs.register+", ";
		str += "R"+d+"\n";
		
		// translate lhs
		LIRUpType lhs = assignStmt.lhs.accept(this, d+1);
		str+= lhs.lirCode;
		
		// handle all variable cases
		str += getMoveType(lhs.astNodeType);
		str += "R"+d+", "+lhs.register+"\n";
		
		return new LIRUpType(str, LIRAstNodeType.STATEMENT,"");
	}

	@Override
	public LIRUpType visit(ReturnStmt retStmt, Integer d) {
		String str = "";
		if (retStmt.expr != null){
			LIRUpType returnVal = retStmt.expr.accept(this, d);
			str += returnVal.lirCode;
			str += "Return "+returnVal.register+"\n";
		} else {
			str += "Return 9999\n";
		}
		
		return new LIRUpType(str, LIRAstNodeType.STATEMENT, "");
	}
	
	@Override
	public LIRUpType visit(UnaryOpExpr unaryOp, Integer d) {
		String str = "";
		String trueLabel = "_true_label"+labelNumber;
		String endLabel = "_end_label"+(labelNumber++);
		
		LIRUpType rightOp = unaryOp.rightOp.accept(this, d);
		str += rightOp.lirCode;
		str += getMoveType(rightOp.astNodeType);
		str += rightOp.register+",R"+d+"\n";
		if (unaryOp.operator == UnaryOpsEnum.LNEG){//This is logical unary operation
			// recursive call to rightOp

			str += "Compare 0,R"+d+"\n";
			str += "JumpTrue "+trueLabel+"\n";
			str += "Move 0,R"+d+"\n";
			str += "Jump "+endLabel+"\n";
			str += trueLabel+":\n";
			str += "Move 1,R"+d+"\n";
			str += endLabel+":\n";

			return new LIRUpType(str, LIRAstNodeType.REGISTER,"R"+d);
		}
		else{//This is math unary operation			
			str += "Neg R"+d+"\n";
			return new LIRUpType(str, LIRAstNodeType.REGISTER,"R"+d);
			
		}
	}

	@Override
	public LIRUpType visit(BinaryOpExpr binaryOp, Integer d) {
		String trueLabel = "_true_label"+labelNumber;
		String falseLabel = "_false_label"+labelNumber;
		String endLabel = "_end_label"+(labelNumber++);
		String str = "";
		
		// recursive call to leftOp
		LIRUpType leftOp = binaryOp.leftOp.accept(this, d);
		str += leftOp.lirCode;
		str += getMoveType(leftOp.astNodeType);
		str += leftOp.register+",R"+d+"\n";

		LIRUpType rightOp = binaryOp.rightOp.accept(this, d+1);
		str += rightOp.lirCode;
		str += getMoveType(rightOp.astNodeType);
		str += rightOp.register+",R"+(d+1)+"\n";

		if (binaryOp.operator.type == "Logical"){
			if (binaryOp.operator != BinaryOpsEnum.LAND && binaryOp.operator != BinaryOpsEnum.LOR){
				str += "Compare R"+(d+1)+",R"+d+"\n";
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
				str += "Compare 0,R"+d+"\n";
				str += "JumpTrue "+falseLabel+"\n";
				str += "Compare 0,R"+(d+1)+"\n";
				str += "JumpTrue "+falseLabel+"\n";
				str += "Jump "+trueLabel+"\n";
				str += falseLabel+":\n"; 
				break;
			case LOR:
				str += "Compare 0,R"+d+"\n";
				str += "JumpFalse "+trueLabel+"\n";
				str += "Compare 0,R"+(d+1)+"\n";
				str += "JumpFalse "+trueLabel+"\n"; 
				break;
			default:
				System.err.println("* Error in logical binaryOP *");	
			}
			str += "Move 0,R"+d+"\n";
			str += "Jump "+endLabel+"\n";
			str += trueLabel+":\n";
			str += "Move 1,R"+d+"\n";
			str += endLabel+":\n";

			return new LIRUpType(str, LIRAstNodeType.REGISTER,"R"+d);
		}
		else{
			switch (binaryOp.operator){
			case PLUS:
				// check if operation is on strings or on integers
				SemanticType operandType = (SemanticType) binaryOp.rightOp.accept(new SemanticChecker(binaryOp,symTab,typTab), null);
				if (operandType == typTab.intType){
					str += "Add R"+(d+1)+",R"+d+"\n";
				} else { // strings
					str += "Library __stringCat(R"+d+",R"+(d+1)+"),R"+d+"\n";
				}
				break;
			case MINUS:
				str += "Sub R"+(d+1)+",R"+d+"\n";
				break;
			case MULTIPLY:
				str += "Mul R"+(d+1)+",R"+d+"\n";
				break;
			case DIVIDE:
				// check division by zero
				str += "StaticCall __checkZero(b=R"+(d+1)+"),Rdummy\n";
				
				str += "Div R"+(d+1)+",R"+d+"\n";
				break;
			case MOD:
				str += "Mod R"+(d+1)+",R"+d+"\n";
				break;
			default:
				System.err.println("*** YOUR PARSER SUCKS ***");
			}
			
			return new LIRUpType(str, LIRAstNodeType.REGISTER,"R"+d);
		}

	}

	@Override
	public LIRUpType visit(StaticCall staticCall, Integer d) {
		// TODO: not finished. Only deals with Library calls
		String str = "";
		
		// recursive calls to all arguments
		int reg = d;
		for (Expr arg: staticCall.args){
			LIRUpType argExp = arg.accept(this, reg);
			str += argExp.lirCode;
			str += getMoveType(argExp.astNodeType);
			str += argExp.register+", R"+reg+"\n";
			reg++;
		}
		
		// Library method call
		if (staticCall.className.equals("Library")){
			str += "Library __"+staticCall.funcName+"(";
			// iterate over values (registers)
			for(int i = 0; i < staticCall.args.size(); i++){
				str += "R"+(i+d);
				if (i < staticCall.args.size()-1)
					str += ", ";
			}
			str += "), R"+d+"\n";
			
			return new LIRUpType(str, LIRAstNodeType.REGISTER,"R"+d);
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
			str += ms.params.get(i).name+"=R"+(d+i);
			if (i < staticCall.args.size()-1)
				str += ", ";
		}
		str += "), R"+d+"\n";
		
		return new LIRUpType(str, LIRAstNodeType.REGISTER,"R"+d);
	}

	@Override
	public LIRUpType visit(VirtCall virtCall, Integer d) {
		String str = "";
		String className;
		
		// recursive call to call location
		if (virtCall.location != null){
			className = ((SemanticType)virtCall.location.accept(new ExprTypeResolver(symTab, typTab, currentThisClass), null)).name;
			LIRUpType location = virtCall.location.accept(this, d);
			str += location.lirCode;
			str += getMoveType(location.astNodeType);
			str += location.register+", R"+d+"\n";
			
			// check location null reference
			//TODO: str += "StaticCall __checkNullRef(a=R"+d+"),Rdummy\n";
		} else {
			className = currentThisClass;
			str += "Move this, R"+d+"\n";
		}
		
		int reg = d+1;
		for (Expr arg: virtCall.args){
			LIRUpType argExp = arg.accept(this, reg);
			str += argExp.lirCode;
			str += getMoveType(argExp.astNodeType);
			str += argExp.register+", R"+reg+"\n";
			reg++;
		}

		ClassSymbol cs = (ClassSymbol) symTab.findEntryGlobal(className);
		MethodSymbol ms = null;
		try {
			ms = cs.getMethodSymbolRec(virtCall.funcName);
		} catch (SemanticError e) {}
		int offset = ms.getOffset();
		
		str += "VirtualCall R"+d+"."+offset+"(";
		// insert <formal>=<argument register>
		for(int i = 0; i < virtCall.args.size(); i++){
			str += ms.params.get(i).name+"=R"+(d+i+1);
			if (i < virtCall.args.size()-1)
				str += ", ";
		}
		str += "), R"+d+"\n";
		
		return new LIRUpType(str, LIRAstNodeType.REGISTER,"R"+d);	
	}

	@Override
	public LIRUpType visit(VarLocation varLoc, Integer d) {
		String str = "";
		
		// location.ID
		if (varLoc.location != null){
			// translate the location
			LIRUpType loc = varLoc.location.accept(this, d);
			// add code to translation
			str += loc.lirCode;
			
			// get the type of the location
			SemanticType locationType = (SemanticType) varLoc.location.accept(new ExprTypeResolver(symTab, typTab, currentThisClass), null);
			
			// get the field offset
			ClassSymbol cs = (ClassSymbol) symTab.findEntryGlobal(locationType.name);	
			FieldSymbol fs = null;
			try{	// will always succeed
				fs = cs.getFieldSymbolRec(varLoc.name);
			}catch (SemanticError se){}			 
			int fieldOffset = fs.getOffset();
			
			// translate this step
			str += getMoveType(loc.astNodeType);
			String locReg = "R"+d;
			str += loc.register+", "+locReg+"\n";
			
			// check external location null reference
			//TODO: str += "StaticCall __checkNullRef(a=R"+d+"), Rdummy\n";
			
			return new LIRUpType(str, LIRAstNodeType.EXTERNALVARLOC, locReg+"."+fieldOffset);
		// ID
		}else{
			int scopeLevel = symTab.findScopeLevel(varLoc.name);
			if (scopeLevel == 2){	// it is a field of THIS
				ClassSymbol cs = (ClassSymbol) symTab.findEntryGlobal(currentThisClass);
				FieldSymbol fs = null;
				try{
					fs = cs.getFieldSymbolRec(varLoc.name);
				}catch(SemanticError se){}
				int fieldOffset = fs.getOffset();
				
				str += "Move this, R"+d+"\n";
				String tgtLoc = "R"+d+"."+fieldOffset;
				
				return new LIRUpType(str, LIRAstNodeType.EXTERNALVARLOC, tgtLoc);
			}else{
				// it's not a field, so it must be local variable
				// scopeLevel of -1 means this variable is not in symTable => it's a function parameter => leave it's name as is
				String localVarName = (scopeLevel == -1 ? varLoc.name : varLoc.name + scopeLevel);
				return new LIRUpType("",LIRAstNodeType.LOCALVARLOC, localVarName);
			}
		}
	}

	@Override
	public LIRUpType visit(ArrayLocation arrLoc, Integer d) {
		String str = "";
		
		// visit array
		LIRUpType array = arrLoc.array.accept(this, d);
		str += array.lirCode;
		str += getMoveType(array.astNodeType);
		str += array.register+", R"+d+"\n";
		
		// check array null reference
		//TODO: str += "StaticCall __checkNullRef(a=R"+d+"), Rdummy\n";
		
		// visit index
		LIRUpType index = arrLoc.index.accept(this, d+1);
		str += index.lirCode;
		str += getMoveType(index.astNodeType);
		str += index.register+", R"+(d+1)+"\n";
		
		// check array access
		//TODO: str += "StaticCall __checkArrayAccess(a=R"+d+",i=R"+(d+1)+"), Rdummy\n";
		
		return new LIRUpType(str, LIRAstNodeType.ARRAYLOC,"R"+d+"[R"+(d+1)+"]");
	}

	@Override
	public LIRUpType visit(CallStmt callStmt, Integer d) {
		return callStmt.call.accept(this, d);
	}

	@Override
	public LIRUpType visit(StmtList stmtList, Integer d) {
		String str = "";
		
		symTab.enterScope();
		for (Stmt s: stmtList.statements)
			str += s.accept(this, d).lirCode;
		
		symTab.exitScope();
		return new LIRUpType(str, LIRAstNodeType.STATEMENT,"");
	}

	@Override
	public LIRUpType visit(IfStmt ifStatement, Integer d) {
//		// TODO Auto-generated method stub
//		symTab.enterScope();
//		ifStmt.thenStmt.accept(this, null);
//		symTab.exitScope();
//		
//		if (ifStmt.elseStmt != null){
//			symTab.enterScope();
//			ifStmt.elseStmt.accept(this, null);
//			symTab.exitScope();
//		}
//		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
		String tr = "";
		String falseLabel = "_false_label"+labelNumber;
		String endLabel = "_end_label"+(labelNumber++);
		
		// recursive call the condition expression
		LIRUpType condExp = ifStatement.condition.accept(this, d);
		tr += condExp.lirCode;
		tr += getMoveType(condExp.astNodeType);
		tr += condExp.register+",R"+d+"\n";
		// check condition
		tr += "Compare 0,R"+d+"\n";
		if (ifStatement.elseStmt!=null) tr += "JumpTrue "+falseLabel+"\n";
		else tr += "JumpTrue "+endLabel+"\n";
		
		// recursive call to the then statement
		LIRUpType thenStat = ifStatement.thenStmt.accept(this, d);
		tr += thenStat.lirCode;
		
		if (ifStatement.elseStmt!=null){
			tr += "Jump "+endLabel+"\n";

			// recursive call to the else statement
			tr += falseLabel+":\n";
			LIRUpType elseStat = ifStatement.elseStmt.accept(this, d);
			tr += elseStat.lirCode;
		}
		
		tr += endLabel+":\n";
		
		return new LIRUpType(tr, LIRAstNodeType.STATEMENT,"");
	}

	@Override
	public LIRUpType visit(WhileStmt whileStmt, Integer d) {
		int prevWhileID = currWhileIdentifier;
		currWhileIdentifier = labelNumber;
		
		String str = "";
		String whileLabel = "_while_cond_label"+labelNumber;
		String endLabel = "_end_label"+(labelNumber++);
		
		str += whileLabel+":\n";
		// recursive call to condition statement
		LIRUpType conditionExp = whileStmt.condition.accept(this, d);
		str += conditionExp.lirCode;
		str += getMoveType(conditionExp.astNodeType);
		str += conditionExp.register+",R"+d+"\n";
		
		// check condition
		str += "Compare 0,R"+d+"\n";
		str += "JumpTrue "+endLabel+"\n";
		
		// recursive call to operation statement
		str += whileStmt.thenStmt.accept(this,d).lirCode;
		str += "Jump "+whileLabel+"\n";
		str += endLabel+":\n";
		
		currWhileIdentifier = prevWhileID;
		return new LIRUpType(str, LIRAstNodeType.STATEMENT,"");
	}

	@Override
	public LIRUpType visit(BreakStmt breakStmt, Integer d) {
		String tr = "Jump _end_label"+currWhileIdentifier+"\n";
		return new LIRUpType(tr, LIRAstNodeType.STATEMENT,"");
	}

	@Override
	public LIRUpType visit(ContinueStmt contStmt, Integer d) {
		// TODO 
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(LocalVarStmt localVarStmt, Integer d) {
		try{
			symTab.addEntry(new VarSymbol(localVarStmt.name, typTab.resolveType(localVarStmt.type.getName())));
		}catch(SemanticError se) {}
		
		String str = "";
		
		if (localVarStmt.init != null){
			LIRUpType initVal = localVarStmt.init.accept(this, d);
			str += initVal.lirCode;
			str += getMoveType(initVal.astNodeType);
			str += initVal.register+", R"+d+"\n";
			str += "Move R"+d+", "+localVarStmt.name+symTab.scopeLevel+"\n";
		}
		
		return new LIRUpType(str, LIRAstNodeType.STATEMENT,"");
	}

	@Override
	public LIRUpType visit(ThisExpr thisExp, Integer d) {
		String str = "Move this, R"+d+"\n";
		return new LIRUpType(str, LIRAstNodeType.REGISTER,"R"+d);
	}

	@Override
	public LIRUpType visit(NewClassExpr newClassExp, Integer d) {
		ClassSymbol cs = (ClassSymbol) symTab.findEntryGlobal(newClassExp.name);
		String str = "Library __allocateObject("+cs.bytesInMemory()+"), R"+d+"\n";
		str += "MoveField _DV_"+cs.name+", R"+d+".0\n";
		
		return new LIRUpType(str, LIRAstNodeType.REGISTER,"R"+d);
	}

	@Override
	public LIRUpType visit(NewArrayExpr newArrExp, Integer d) {
		String str = "";
		
		LIRUpType size = newArrExp.index.accept(this, d);
		str += size.lirCode;
		str += getMoveType(size.astNodeType);
		str += size.register+", R"+d+"\n";
		str += "Mul 4, R"+d+"\n";
		
		// make sure index is non-negative
		//TODO: str += "StaticCall __checkSize(n=R"+d+"), Rdummy\n";
		str += "Library __allocateArray(R"+d+"), R"+d+"\n";
		
		return new LIRUpType(str, LIRAstNodeType.REGISTER,"R"+d);
	}

	@Override
	public LIRUpType visit(LengthExpr lengthExpr, Integer d) {
		String str = "";

		LIRUpType array = lengthExpr.context.accept(this, d);
		str += array.lirCode;
		str += getMoveType(array.astNodeType);
		str += array.register+", R"+d+"\n";
		
		// make sure context is non-null
		//TODO: str += "StaticCall __checkNullRef(a=R"+d+"), Rdummy\n";
		str += "ArrayLength R"+d+", R"+d+"\n";
		
		return new LIRUpType(str, LIRAstNodeType.REGISTER,"R"+d);
	}

	@Override
	public LIRUpType visit(LiteralExpr expr, Integer d) {
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
		
		for (Map.Entry<String, HashMap<String,String>> ce: dispatchTableMap.entrySet()){ //go through each class entry		
			String str="";
			str+= "_DV_"+ ce.getKey() +": [";
			for (Map.Entry<String, String> me: ce.getValue().entrySet()){ // go through each method entry
				str+="_"+me.getValue()+"_"+me.getKey()+",";
			}
			str = str.substring(0, str.length()-1); //chop the last ','
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
