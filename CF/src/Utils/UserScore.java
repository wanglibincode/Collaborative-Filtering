package Utils;

public class UserScore {
    private int userid;
    private double score;

    public UserScore(int userid, double score) {
	this.userid = userid;
	this.score = score;
    }

    public int getUserid() {
	return userid;
    }

    public void setUserid(int userid) {
	this.userid = userid;
    }

    public double getScore() {
	return score;
    }

    public void setScore(double score) {
	this.score = score;
    }

    @Override
    public boolean equals(Object obj) {
	// TODO Auto-generated method stub
	return obj instanceof UserScore && userid == ((UserScore) obj).userid;
    }
}
