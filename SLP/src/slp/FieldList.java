package slp;

import java.util.List;
import java.util.ArrayList;

public class FieldList {
	public final List<Field> fields;

	public FieldList(int line, Type type, IDList lst){
		fields = new ArrayList<Field>();
   		for (String id: lst.idList){
   			fields.add(new Field(line, type, id));
   		}
	}
	
	public void add(Field field){
		fields.add(field);
	}
}
