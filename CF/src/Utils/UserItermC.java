package Utils;

import java.util.ArrayList;

public class UserItermC {
	private int id;
	private ArrayList<Item> set ;

	public UserItermC(int id, ArrayList<Item> set) {
		this.id = id;
		this.set = set;
	}
	public ArrayList<Item> getSet(){
		return set;
	}
	public void setSet(ArrayList<Item> set){
		this.set = set;
	}
}
