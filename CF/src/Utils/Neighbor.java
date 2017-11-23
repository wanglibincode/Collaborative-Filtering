package Utils;

public class Neighbor implements Comparable {
    private int id;// 邻居的编号
    private double value;// 与邻居的相似度

    public Neighbor(int id, double value) {
	this.id = id;
	this.value = value;
    }

    public void setvalue(int value) {
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
	return obj instanceof Neighbor && id == ((Neighbor) obj).id;
    }

    public int compareTo(Object o) {// 覆写方法，是对象按照value降序排列
	// TODO Auto-generated method stub

	if (o instanceof Neighbor) {
	    Integer ID = ((Neighbor) o).id;
	    Double VALUE = ((Neighbor) o).value;
	    return VALUE.compareTo(value);
	} else {
	    return 2;
	}
    }
}
