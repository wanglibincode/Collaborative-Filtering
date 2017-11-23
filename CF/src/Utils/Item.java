package Utils;

public class Item implements Comparable {
	private int userid;
	private int id;// Item的编号
	private double value;// Item的评分

	public Item(int userid,int id, double value) {
		this.userid = userid;
		this.id = id;
		this.value = value;
	}

	public int getID() {
		return id;
	}

	public double getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		int result = 17;
		result = 37 * result + id;
		result = (int) (37 * result + Double.doubleToLongBits(value));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		return obj instanceof Item && id == ((Item) obj).id;
	}

	public int compareTo(Object o) {// 覆写方法，是对象按照value降序排列
		if (o instanceof Item) {
			Integer ID = ((Item) o).id;
			Double VALUE = ((Item) o).value;
			return VALUE.compareTo(value);
		} else {
			return 2;
		}
	}

	public int getUserid() {
		return userid;
	}

	public void setUserid(int userid) {
		this.userid = userid;
	}
}
