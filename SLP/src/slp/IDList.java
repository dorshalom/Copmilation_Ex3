package slp;

import java.util.List;
import java.util.ArrayList;

public class IDList {
	public final List<String> idList;

	public IDList(String st){
		idList = new ArrayList<String>();
		idList.add(st);
	}

	public void add(String st){
		idList.add(st);
	}
}
