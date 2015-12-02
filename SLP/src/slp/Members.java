package slp;

import java.util.List;
import java.util.ArrayList;

public class Members {
	public final List<Field> fieldList;
	public final List<Method> methodList;
		
	public Members(Method method){
		fieldList = new ArrayList<Field>();
		methodList = new ArrayList<Method>();
		methodList.add(method);
	}
	
	public Members(FieldList fl){
		fieldList = fl.fields;
		methodList = new ArrayList<Method>();
	}

	public Members(List<Field> fieldList, List<Method> methodList){
		this.fieldList = fieldList;
		this.methodList = methodList;
	}
	
	public void add(Method m){
		methodList.add(m);
	}
	
	public void add(FieldList fl){
		fieldList.addAll(fl.fields);
	}
}
