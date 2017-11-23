package recommendArithmetic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jettison.json.JSONObject;

import Utils.Item;
import Utils.Neighbor;

public class RecomResourceByUserCF extends RecomResourceFather {
    // Ϊÿ���û��Ƽ�����Դ�б�
    private Map<Integer, List<Item>> mUsersPredict;
    // �û���Դ�����ļ�
    private String mResouUserAndItemFile;
    // �û���Դ��������
    private Map<Integer, List<Item>> mUserItemsScore;
    // �û���Դ��ƽ������
    private Map<Integer, Double> mUsersAverageScores;
    // �û���������û�
    private Map<Integer, List<Neighbor>> mUsersNeighbors;
    // �û�������û�����
    private int UN = 80;
    // Ϊÿ���û��Ƽ���Դ�ĸ���
    private int mItemNum = 10;
    // ����item
    private String mRecomForUsersResult;
    private String precisionAndRecall;
    private String mResouUserAndItemTestFile;
    private String rmseAndMAE;

    public RecomResourceByUserCF(String mResouUserAndItemFile, String mResouUserAndItemTestFile, int UN, int mItemNum) {
	this.mResouUserAndItemFile = mResouUserAndItemFile;
	this.UN = UN;
	this.mResouUserAndItemTestFile = mResouUserAndItemTestFile;
	this.mItemNum = mItemNum;
    }

    // ��ȡΪÿ���û��Ƽ����б�
    public String getRecomResultForUsers() {
	// �õ��û�Item���־���mUserItemsScore
	mUserItemsScore = readFile(mResouUserAndItemFile);

	if (mUserItemsScore != null) {
	    System.out.println("��ȴ������ڷ���...");
	    // �õ�average[]
	    mUsersAverageScores = getAver(mUserItemsScore);
	    // �õ�NofUser
	    mUsersNeighbors = getNeighborUsers(mUserItemsScore, UN);
	    // �õ�Ϊÿ���û��Ƽ�����Դ�б�-mUsersPredict
	    mUsersPredict = getRecommendForUsers(mResouUserAndItemFile, mUserItemsScore, mUsersNeighbors, UN);
	    // ����Ϊÿ���û��Ƽ�����Դ
	    Map<Integer, List<Integer>> recomForUsersByRecomNUm = new HashMap<Integer, List<Integer>>();
	    Set<Integer> keySet = mUsersPredict.keySet();
	    Iterator<Integer> iterator = keySet.iterator();
	    while (iterator.hasNext()) {
		int userId = iterator.next();
		List<Item> recomForUser = mUsersPredict.get(userId);
		List<Integer> recomForUserAtNum = new ArrayList<Integer>();
		for (int i = 0; i < mItemNum; i++) {
		    recomForUserAtNum.add(recomForUser.get(i).getID());
		}
		recomForUsersByRecomNUm.put(userId, recomForUserAtNum);
	    }
	    // �õ��󣬷��ظ��û�json��ʽ
	    JSONObject jsonObject = new JSONObject(recomForUsersByRecomNUm);
	    mRecomForUsersResult = jsonObject.toString();
	    // d�õ��Ƽ���׼ȷ�ʺ��ٻ���
	    precisionAndRecall = getPrecisionAndRecall(mUsersPredict, mResouUserAndItemTestFile, mItemNum);
	    // �õ��Ƽ���RMSE��MAE
	    rmseAndMAE = getRMSEAndMAE(mResouUserAndItemTestFile, UN, mUserItemsScore, mUsersNeighbors,
		    mUsersAverageScores);
	}
	return "��ȷ��/�ٻ���:  " + precisionAndRecall + "----" + "RMSE/MAE:  " + rmseAndMAE + "---  �Ƽ����"
		+ mRecomForUsersResult;
    }

    /**
     * �û����ƶȼ��㷽��,����
     * 
     * @param UseID
     * @param id
     * @param list
     * @param list2
     * @return double sim
     */
    public double CalculateSim(int UseID, int otherid, List<Item> userlist, List<Item> otherlist) {
	/**
	 * �����ҳ�DealedOfRate/rate��UseID��id�����ֵ���Ŀ������Ч
	 */
	int count = 0;
	List<Double> itemScore = new ArrayList<Double>();
	List<Double> otheritemScore = new ArrayList<Double>();

	for (int i = 0; i < userlist.size(); i++) {
	    Item userItem = userlist.get(i);
	   if (otherlist.contains(userItem)) {
		    count++;
		    int index = otherlist.indexOf(userItem);
		    itemScore.add(userItem.getValue());
		    otheritemScore.add(otherlist.get(index).getValue());
	   }
	}
	double[] x = new double[userlist.size()];
	double[] y = new double[otherlist.size()];
	for (int i = 0; i < userlist.size(); i++) {
	    x[i] = userlist.get(i).getValue();
	}
	for (int i = 0; i < otherlist.size(); i++) {
	    y[i] = otherlist.get(i).getValue();
	}
	double[] x1 = new double[count];
	double[] y1 = new double[count];
	for (int i = 0; i < count; i++) {
	    x1[i] = itemScore.get(i);
	    y1[i] = otheritemScore.get(i);
	}
	// �����û�û�й�ͬ������Ŀ
	int len = x1.length;// ��ͬ������Ŀ�ĸ���
	double result = simMethod(UseID, otherid, len, x1, y1, x, y);
	return result;

    }

    /**
     * �����������ƶ�
     * 
     */
    private double simMethod(int UseID, int id, int len, double[] x1, double[] y1, double[] x, double[] y) {
	double upside = 0.0;
	double downside_x = 0.0;
	double downside_y = 0.0;
	// �������ƶȵķ���
	for (int i = 0; i < len; i++) {
	    upside += (x1[i] - mUsersAverageScores.get(UseID)) * (y1[i] - mUsersAverageScores.get(id));
	}
	// �������ƶȵķ�ĸ,Ŀ���û�userid
	for (int xi = 0; xi < x.length; xi++) {
	    if (x[xi] != 0) {
		downside_x += Math.pow((x[xi] - mUsersAverageScores.get(UseID)), 2);
	    }
	}
	// �������ƶȵķ�ĸ,�����û�id
	for (int yi = 0; yi < y.length; yi++) {
	    if (y[yi] != 0) {
		downside_y += Math.pow((y[yi] - mUsersAverageScores.get(id)), 2);
	    }
	}

	double downside = (double) Math.sqrt(downside_x) * (double) Math.sqrt(downside_y);
	if (downside == 0.0) {
	    return 0.0;
	}
	return upside / downside;
    }

}
