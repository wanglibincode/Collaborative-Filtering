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

public class RecomResourceByknowRelation extends RecomResourceFather {
	// 为每个用户推荐的资源列表
	private Map<Integer, List<Item>> mUsersPredict;
	// 用户资源评分文件
	private String mResouUserAndItemFile;
	// 用户资源评分数组
	private Map<Integer, List<Item>> mUserItemsScore;
	// 用户资源的平均评分
	private Map<Integer, Double> mUsersAverageScores;
	// 用户的最近邻用户
	private Map<Integer, List<Neighbor>> mUsersNeighbors;
	// 用户最近邻用户个数
	private int UN = 80;
	// 为每个用户推荐资源的个数
	private int mItemNum = 10;
	//取每个学习资源的相关知识点个数
	private int knowledgeNum = 5;
	// 所有item
	private String mRecomForUsersResult;
	private String precisionAndRecall;
	private String mResouUserAndItemTestFile;
	private String rmseAndMAE;

	public RecomResourceByknowRelation(String mResouUserAndItemFile, String mResouUserAndItemTestFile, int UN, int mItemNum) {
		this.mResouUserAndItemFile = mResouUserAndItemFile;
		this.UN = UN;
		this.mResouUserAndItemTestFile = mResouUserAndItemTestFile;
		this.mItemNum = mItemNum;
	}

	// 获取为每个用户推荐的列表
	public String getRecomResultForUsers() {
		// 得到用户Item评分矩阵：mUserItemsScore
		mUserItemsScore = readFile(mResouUserAndItemFile);

		if (mUserItemsScore != null) {
			System.out.println("请等待，正在分析...");
			// 得到average[]
			mUsersAverageScores = getAver(mUserItemsScore);
			// 得到NofUser
			mUsersNeighbors = getNeighborUsers(mUserItemsScore, UN);
			// 得到为每个用户推荐的资源列表-mUsersPredict
			mUsersPredict = getRecommendForUsers(mResouUserAndItemFile, mUserItemsScore, mUsersNeighbors, UN);
			// 返回为每个用户推荐的资源
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
			// 得到后，返回给用户json格式
			JSONObject jsonObject = new JSONObject(recomForUsersByRecomNUm);
			mRecomForUsersResult = jsonObject.toString();
			precisionAndRecall = getPrecisionAndRecall(mUsersPredict, mResouUserAndItemTestFile, mItemNum);
			rmseAndMAE = getRMSEAndMAE(mResouUserAndItemTestFile, UN, mUserItemsScore, mUsersNeighbors,
					mUsersAverageScores);
		}
		return "正确率/召回率:  " + precisionAndRecall + "----" + "RMSE/MAE:  " + rmseAndMAE + "---  推荐结果"
				+ mRecomForUsersResult;
	}

	/**
	 * 相似度计算方法
	 * @param UseID
	 * @param id
	 * @param list
	 * @param list2
	 * @return
	 */
	public double CalculateSim(int UseID, int otherid, List<Item> userlist, List<Item> otherlist) {
		/**
		 * 第一步：由用户的访问资源列表，得到用户学习的知识点集合
		 * 第二步：根据用户的知识点集合得到两个用户的相似度
		 */
		int count = 0;
		List<Integer> item = new ArrayList<Integer>();
		List<Double> itemScore = new ArrayList<Double>();
		List<Double> otheritemScore = new ArrayList<Double>();

		for (int i = 0; i < userlist.size(); i++) {
			Item userItem = userlist.get(i);
			for (int j = 0; j < otherlist.size(); j++) {
				int otherItemId = otherlist.get(j).getID();
				if (otherItemId == userItem.getID()) {
					count++;
					item.add(otherItemId);
					itemScore.add(userItem.getValue());
					otheritemScore.add(otherlist.get(j).getValue());
				}
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
		for (int i = 0; i < item.size(); i++) {
			x1[i] = itemScore.get(i);
			y1[i] = otheritemScore.get(i);
		}
		// 两个用户没有共同评分项目
		int len = x1.length;// 共同评分项目的个数
		double result = simMethod(UseID, otherid, len, x1, y1, x, y);
		return result;

	}

	/**
	 * 修正余弦相似度
	 * 
	 */
	private double simMethod(int UseID, int id, int len, double[] x1, double[] y1, double[] x, double[] y) {
		double upside = 0.0;
		double downside_x = 0.0;
		double downside_y = 0.0;
		// 余弦相似度的分子
		for (int i = 0; i < len; i++) {
			upside += (x1[i] - mUsersAverageScores.get(UseID)) * (y1[i] - mUsersAverageScores.get(id));
		}
		// 余弦相似度的分母,目标用户userid
		for (int xi = 0; xi < x.length; xi++) {
			if (x[xi] != 0) {
				downside_x += Math.pow((x[xi] - mUsersAverageScores.get(UseID)), 2);
			}
		}
		// 余弦相似度的分母,其他用户id
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
