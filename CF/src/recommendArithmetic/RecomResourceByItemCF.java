package recommendArithmetic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.codehaus.jettison.json.JSONObject;

import Utils.EvaluateIndex;
import Utils.Item;
import Utils.Neighbor;
import Utils.UserScore;

public class RecomResourceByItemCF extends RecomResourceFather {
	// 为每个用户推荐的资源列表
	private Map<Integer, List<Item>> mUsersPredict;
	// 用户资源评分文件
	private String mResouUserAndItemFile;
	// 用户资源评分矩阵
	private Map<Integer, List<Item>> mUserItemsScore;
	// 每个资源的平均评分
	private Map<Integer, Double> mItemsAverageScores;
	// 每个用户的平均分
	private Map<Integer, Double> mUsersAverageScores;

	// 用户的最近邻用户
	private Map<Integer, List<Neighbor>> mItemsNeighbors;
	// 用户最近邻用户个数
	private int UN = 80;
	// 为每个用户推荐资源的个数
	private int mItemNum = 10;
	// 每个项目的用户个数；
	private Map<Integer, List<UserScore>> mItemsUsers;
	// 所有item
	private String mRecomForUsersResult;
	private String precisionAndRecall;
	private String mResouUserAndItemTestFile;
	private String rmseAndMAE;

	public RecomResourceByItemCF(String mResouUserAndItemFile, String mResouUserAndItemTestFile, int UN, int mItemNum) {
		this.mResouUserAndItemFile = mResouUserAndItemFile;
		this.UN = UN;
		this.mResouUserAndItemTestFile = mResouUserAndItemTestFile;
		this.mItemNum = mItemNum;
	}

	@Override
	public String getRecomResultForUsers() {
		// 得到用户-资源评分矩阵
		mUserItemsScore = readFile(mResouUserAndItemFile);
		if (mUserItemsScore != null) {
			System.out.println("请等待，正在分析ItemCF...");
			// 得到每个资源的平均分
			mItemsAverageScores = getAver(mUserItemsScore);
			// 得到每个用户的平均分
			mUsersAverageScores = super.getAver(mUserItemsScore);
			// 得到每个资源的相似度，最近邻
			mItemsNeighbors = getNeighborItems();
			// 得到为每个用户推荐的资源列表-mUsersPredict
			mUsersPredict = getRecommendForUsers();
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
			// d得到推荐的准确率和召回率
			precisionAndRecall = getPrecisionAndRecall(mUsersPredict, mResouUserAndItemTestFile, mItemNum);
			// 得到推荐的RMSE和MAE
			rmseAndMAE = getRMSEAndMAE(mResouUserAndItemTestFile, UN, mUserItemsScore, mItemsNeighbors,
					mItemsAverageScores);
		}
		return "正确率/召回率:  " + precisionAndRecall + "----" + "RMSE/MAE:  " + rmseAndMAE + "---  推荐结果"
				+ mRecomForUsersResult;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see recommendArithmetic.RecomResourceFather#getAver(java.util.Map)
	 * 求每个Item的平均评分
	 */
	public Map<Integer, Double> getAver(Map<Integer, List<Item>> mUserItemsScore) {
		Map<Integer, Double> mItemsAverageScores = new HashMap<Integer, Double>();
		mItemsUsers = new HashMap<Integer, List<UserScore>>();

		if (mUserItemsScore != null) {

			// 求得mItemsUsers
			for (Integer userid : mUserItemsScore.keySet()) {
				// 每个用户评过分的Item列表
				List<Item> list = mUserItemsScore.get(userid);
				int itemNums = list.size();
				for (int i = 0; i < itemNums; i++) {
					// item id
					int itemId = list.get(i).getID();
					double itemScore = list.get(i).getValue();
					if (mItemsUsers.containsKey(itemId)) {
						mItemsUsers.get(itemId).add(new UserScore(userid, itemScore));
					} else {
						List<UserScore> userScoreList = new ArrayList<>();
						userScoreList.add(new UserScore(userid, itemScore));
						mItemsUsers.put(itemId, userScoreList);
					}
				}

			}
			// 求每个项目的平均分 mItemsAverageScores
			Set<Integer> itemSet = mItemsUsers.keySet();
			for (Integer itemId : itemSet) {
				Double totalScore = 0.0;
				List<UserScore> userScoreList = mItemsUsers.get(itemId);
				int usersNum = userScoreList.size();
				for (UserScore userScore : userScoreList) {
					totalScore += userScore.getScore();
				}
				Double averScore = totalScore / usersNum;
				// System.out.println("ItemId:---"+itemId+"平均分：--"+averScore);
				mItemsAverageScores.put(itemId, averScore);
			}
			return mItemsAverageScores;

		} else {
			System.out.println("mUserItemsScore为空，请先调用readfile获取");
			return mItemsAverageScores;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see recommendArithmetic.RecomResourceFather#getNeighborUsers(java.util.Map,
	 * int) 计算每个Item的最近邻Item
	 */
	public Map<Integer, List<Neighbor>> getNeighborItems() {
		Map<Integer, List<Neighbor>> mItemsNeighbors = new HashMap<Integer, List<Neighbor>>();
		if (mItemsUsers == null) {
			System.out.println("mItemsUsers为空，请先调用getAev获取");
			return mItemsNeighbors;
		} else {
			for (Integer itemId : mItemsUsers.keySet()) {
				ArrayList<Neighbor> neighborList = new ArrayList<Neighbor>();// 会将压入的Neighbor排好序存放
				// 得到userid 与每个用户的相似度，并排序，存入list
				for (Integer otherItem : mItemsUsers.keySet()) {
					if (otherItem != itemId) {
						double sim = CalculateItemSim(itemId, otherItem, mItemsUsers.get(itemId),
								mItemsUsers.get(otherItem));
						neighborList.add(new Neighbor(otherItem, sim));
					}
				}
				Collections.sort(neighborList);
				int k = 0;
				ArrayList<Neighbor> neighborListUN = new ArrayList<Neighbor>();
				while (k < UN && neighborList.get(k) != null) {
					neighborListUN.add(neighborList.get(k));
					k++;
				}
				mItemsNeighbors.put(itemId, neighborListUN);
			}
			return mItemsNeighbors;
		}
	}

	/**
	 * 得到每个用户对未评分资源的评分：mUsersPredict
	 * 
	 * @param num
	 */
	public Map<Integer, List<Item>> getRecommendForUsers(String mResouUserAndItemFile,
			Map<Integer, List<Item>> mUserItemsScore, Map<Integer, List<Neighbor>> mItemsNeighbors, int UN) {
		return null;
	}

	public Map<Integer, List<Item>> getRecommendForUsers() {
		Set<Integer> mItemsID = new TreeSet<Integer>();
		Map<Integer, List<Item>> mUsersPredict = new HashMap<Integer, List<Item>>();
		BufferedReader reader = null;
		/**
		 * 求每个用户对哪些资源已经评分
		 */
		Map<Integer, List<Integer>> mUsersAready = new HashMap<Integer, List<Integer>>();
		// 读文件
		try {
			File inputFile = new File(mResouUserAndItemFile);
			if (!inputFile.exists() || inputFile.isDirectory())
				throw new FileNotFoundException();
			reader = new BufferedReader(new FileReader(inputFile));
			String[] part = new String[3];
			String tmpToRead = "";
			while ((tmpToRead = reader.readLine()) != null) {
				part = tmpToRead.split("\t");
				int userID = Integer.parseInt(part[0]);
				int itemID = Integer.parseInt(part[1]);
				mItemsID.add(itemID);
				// System.out.println(itemID);
				if (mUsersAready.containsKey(userID)) {
					mUsersAready.get(userID).add(itemID);
				} else {
					List<Integer> list = new ArrayList<Integer>();
					list.add(itemID);
					mUsersAready.put(userID, list);
				}
			}
			reader.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("mUsersAready :用户已经评分的资源 over" + mUsersAready.get(1).get(0));
		mUsersPredict = new HashMap<Integer, List<Item>>();
		for (Integer userid : mUsersAready.keySet()) {
			List<Item> mPredictItem = new ArrayList<Item>();
			Iterator<Integer> iterator = mItemsID.iterator();
			while (iterator.hasNext()) {
				Integer next = iterator.next();
				// 如果mUserArray中没有当前用户，就说明当前用户没有对任何资源评过分，冷启动
				if (mUsersAready.containsKey(userid)) {
					List<Integer> mAreadyItem = mUsersAready.get(userid);
					// 如果当前用户对当前资源没有评分，则预测，并加入到mPredictItem中
					if (!mAreadyItem.contains(next)) {
						double predict = predictForRecommend(userid, next);
						Item item = new Item(userid, next, predict);
						mPredictItem.add(item);
					}
				}
			}

			if (mPredictItem.size() != 0) {
				Collections.sort(mPredictItem);
				mUsersPredict.put(userid, mPredictItem);
			}
		}
		System.out.println("mUsersPredict: 用户对未评分资源的预测  over");
		return mUsersPredict;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see recommendArithmetic.RecomResourceFather#CalculateSim(int, int,
	 * java.util.List, java.util.List) 计算Item的相似度
	 */
	@Override
	public double CalculateSim(int UseID, int otherid, List<Item> userlist, List<Item> otherlist) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @功能描述:计算item之间的相似度
	 * @param UseID
	 * @param otherid
	 * @param itemUserList
	 * @param otherItemUserList
	 * @return
	 */
	public double CalculateItemSim(int itemId, int otherItemId, List<UserScore> itemUserList,
			List<UserScore> otherItemUserList) {
		/**
		 * 由于找出DealedOfRate/rate中UseID和id都评分的项目，才有效
		 */
		int count = 0;
		List<UserScore> itemScore = new ArrayList<UserScore>();
		List<UserScore> otheritemScore = new ArrayList<UserScore>();

		for (int i = 0; i < itemUserList.size(); i++) {
			UserScore user = itemUserList.get(i);
			if (otherItemUserList.contains(user)) {
				int index = otherItemUserList.indexOf(user);
				count++;
				itemScore.add(user);
				otheritemScore.add(otherItemUserList.get(index));
			}
		}
		double result = simMethod(itemId, otherItemId, itemScore, otheritemScore, itemUserList, otherItemUserList);
//		double result = simMethod(itemId, otherItemId, count,itemScore, otheritemScore);
		return result;
	}

	/**
	 * 修正余弦相似度：
	 * 
	 * @param itemId
	 * @param otherItemId
	 * @param itemScore
	 * @param otheritemScore
	 * @param itemUserList
	 * @param otherItemUserList
	 * @return
	 */
	private double simMethod(int itemId, int otherItemId, List<UserScore> itemScore, List<UserScore> otheritemScore,
			List<UserScore> itemUserList, List<UserScore> otherItemUserList) {
		double upside = 0.0;
		double downside_x = 0.0;
		double downside_y = 0.0;
		// 余弦相似度的分子
		for (int i = 0; i < itemScore.size(); i++) {
			upside += (itemScore.get(i).getScore() - mUsersAverageScores.get(itemScore.get(i).getUserid()))
					* (otheritemScore.get(i).getScore() - mUsersAverageScores.get(otheritemScore.get(i).getUserid()));
		}

		// 余弦相似度的分母,目标用户userid
		for (int xi = 0; xi < itemUserList.size(); xi++) {
			downside_x += Math.pow(
					itemUserList.get(xi).getScore() - mUsersAverageScores.get(itemUserList.get(xi).getUserid()), 2);
		}
		// 余弦相似度的分母,其他用户id
		for (int yi = 0; yi < otherItemUserList.size(); yi++) {
			downside_y += Math.pow(otherItemUserList.get(yi).getScore()
					- mUsersAverageScores.get(otherItemUserList.get(yi).getUserid()), 2);
		}

		double downside = (double) Math.sqrt(downside_x) * (double) Math.sqrt(downside_y);
		if (downside == 0.0) {
			return 0.0;
		}
		return upside / downside;
	}

	/**
	 * 修正余弦相似度 分母为ihej 的共同
	 * 
	 * @param itemId
	 * @param otherItemId
	 * @param itemScore
	 * @param otheritemScore
	 * @return
	 */
	private double simMethod(int itemId, int otherItemId, List<UserScore> itemScore, List<UserScore> otheritemScore) { // TODO
																														// Auto-generated
																														// method
																														// stub
		double upside = 0.0;
		double downside_x = 0.0;
		double downside_y = 0.0;
		// 余弦相似度的分子
		for (int i = 0; i < itemScore.size(); i++) {
			upside += (itemScore.get(i).getScore() - mUsersAverageScores.get(itemScore.get(i).getUserid()))
					* (otheritemScore.get(i).getScore() - mUsersAverageScores.get(otheritemScore.get(i).getUserid()));
		}

		// 余弦相似度的分母,目标用户userid
		for (int xi = 0; xi < itemScore.size(); xi++) {
			downside_x += Math
					.pow(itemScore.get(xi).getScore() - mUsersAverageScores.get(itemScore.get(xi).getUserid()), 2);
		}
		// 余弦相似度的分母,其他用户id
		for (int yi = 0; yi < itemScore.size(); yi++) {
			downside_y += Math.pow(
					otheritemScore.get(yi).getScore() - mUsersAverageScores.get(otheritemScore.get(yi).getUserid()), 2);
		}

		double downside = (double) Math.sqrt(downside_x) * (double) Math.sqrt(downside_y);
		if (downside == 0.0) {
			return 0.0;
		}
		return upside / downside;
	}

	public double CalculateItemSim2(int itemId, int otherItemId, List<UserScore> itemUserList,
			List<UserScore> otherItemUserList) {
		/**
		 * 由于找出DealedOfRate/rate中UseID和id都评分的项目，才有效
		 */
		int count = 0;

		for (int i = 0; i < itemUserList.size(); i++) {
			UserScore user = itemUserList.get(i);
			for (int j = 0; j < otherItemUserList.size(); j++) {
				int otherItemsUserid = otherItemUserList.get(j).getUserid();
				if (otherItemsUserid == user.getUserid()) {
					count++;
					break;
				}
			}
		}

		double result = count / (double) Math.sqrt(itemUserList.size() * otherItemUserList.size());
		return result;
	}

	/**
	 * 皮尔逊相似度
	 * 
	 * @param itemId
	 * @param otherItemId
	 * @param len
	 * @param x1
	 * @param y1
	 * @param x
	 * @param y
	 * @return
	 */
	private double simMethod(int itemId, int otherItemId, int len, List<UserScore> itemScore,
			List<UserScore> otheritemScore) {
		double upside = 0.0;
		double downside_x = 0.0;
		double downside_y = 0.0;
		// 余弦相似度的分子
		for (int i = 0; i < len; i++) {
			upside += (itemScore.get(i).getScore() - mItemsAverageScores.get(itemId))
					* (otheritemScore.get(i).getScore() - mItemsAverageScores.get(otherItemId));

			// 余弦相似度的分母,目标用户userid
			downside_x += Math.pow((itemScore.get(i).getScore() - mItemsAverageScores.get(itemId)), 2);
			// 余弦相似度的分母,其他用户id
			downside_y += Math.pow((otheritemScore.get(i).getScore() - mItemsAverageScores.get(otherItemId)), 2);
		}

		double downside = (double) Math.sqrt(downside_x) * (double) Math.sqrt(downside_y);
		if (downside == 0.0)
			return 0.0;
		return upside / downside;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see recommendArithmetic.RecomResourceFather#getRMSEAndMAE(java.lang.String,
	 * int, java.util.Map, java.util.Map, java.util.Map)
	 */
	public String getRMSEAndMAE(String testFilePath, int UN, Map<Integer, List<Item>> mUserItemsScore,
			Map<Integer, List<Neighbor>> mItemsNeighbors, Map<Integer, Double> mItemsAverageScores) {
		List<Double> x = new LinkedList<Double>();// LinkedList按照对象加入的顺序存储
		List<Double> y = new LinkedList<Double>();
		BufferedReader reader = null;
		FileWriter writer = null;
		// 读文件
		try {
			File inputFile = new File(testFilePath);
			if (inputFile.exists() || !inputFile.isDirectory()) {
				reader = new BufferedReader(new FileReader(inputFile));

				// 写文件
				// File outputFile = new File("E:/毕业/实验/testResultByUCF.txt");
				File outputFile = new File("F:/wlb/论文/实验/data/testResultByUCF.txt");
				if (!outputFile.exists()) {
					if (!outputFile.createNewFile()) {
						System.out.println("输出文件创建失败");
					}
				}
				writer = new FileWriter(outputFile);
				String title = "UserID" + "\t" + "ItemID" + "\t" + "OriginalRate" + "\t" + "PredictRate" + "\r\n";
				writer.write(title);
				writer.flush();

				String[] part = new String[3];
				String tmpToRead = "";
				String tmpToWrite = "";
				while ((tmpToRead = reader.readLine()) != null) {
					part = tmpToRead.split("\t");
					int userID = Integer.parseInt(part[0]);
					int itemID = Integer.parseInt(part[1]);
					double originalRate = Double.parseDouble(part[2]);
					double predictRate = predictForRMSEAndMAE(userID, itemID, UN, mItemsUsers, mItemsNeighbors,
							mItemsAverageScores);
					// double predictRate = predictForRecommend(userID, itemID);
					// if (predictRate != 0) {
					x.add(originalRate);
					y.add(predictRate);
					// }
					// System.out.println(cf.x.size()+"cf.x.size()"+cf.y.size()+"cf.y.size()");
					tmpToWrite = userID + "\t" + itemID + "\t" + originalRate + "\t" + predictRate + "\r\n";
					writer.write(tmpToWrite);
					writer.flush();
				}
				writer.close();
				reader.close();
				// 得到RMSE和MAE
				EvaluateIndex evaluate = new EvaluateIndex();
				// 得到RMSE和MAE
				double RMSE = evaluate.analyse(x, y);
				double MAE = evaluate.MAE_analyse(x, y);
				return RMSE + "/" + MAE;
			} else {
				System.out.println("测试集为空，无法计算RMSE和MAE");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0.0 + "/" + 0.0;
	}

	public double predictForRecommend(int userID, int itemID, Map<Integer, List<Item>> mUserItemsScore,
			Map<Integer, List<Neighbor>> mItemsNeighbors, int UN) {
		return 0.0;
	}

	private double predictForRecommend(int userID, int itemID) {
		double sum = 0;
		if (mItemsNeighbors == null || mItemsUsers == null) {
			System.out.println("mUsersNeighbors为空，请先调用最近用户计算方法获取");
			return sum;
		} else {
			List<Neighbor> itemList = mItemsNeighbors.get(itemID);
			if (itemList != null) {
				for (int i = 0; i < UN; i++) {// 对最近的UN个邻居进行处理
					int neighborID = itemList.get(i).getID();
					double simValue = itemList.get(i).getValue();
					List<UserScore> list2 = mItemsUsers.get(neighborID);
					if (list2 != null && list2.contains(new UserScore(userID, 0.0))) {
						int indexOf = list2.indexOf(new UserScore(userID, 0.0));
						UserScore userScore = list2.get(indexOf);
						sum += simValue * userScore.getScore();
					}
				}
			}
			return Double.parseDouble(String.format("%15f", sum));
		}
	}

	public double predictForRMSEAndMAE(int userID, int itemID, int UN, Map<Integer, List<UserScore>> mItemsUsers,
			Map<Integer, List<Neighbor>> mItemsNeighbors, Map<Integer, Double> mItemsAverageScores) {// 这里的userID为用户输入，减1后为数组下标！
		double sum1 = 0;
		double sum2 = 0;
		List<Neighbor> itemIDNeighbors = mItemsNeighbors.get(itemID);
		if (itemIDNeighbors != null) {
			for (int i = 0; i < UN; i++) {// 对最近的UN个邻居进行处理
				Neighbor neighbor = itemIDNeighbors.get(i);
				if (neighbor != null) {
					int neighborID = neighbor.getID();
					double neib_sim = neighbor.getValue();
					List<UserScore> neighborItemUsers = mItemsUsers.get(neighborID);
					int index = neighborItemUsers.indexOf(new UserScore(userID, 0));
					if (index != -1) {
						sum1 += neib_sim
								* (neighborItemUsers.get(index).getScore() - mItemsAverageScores.get(neighborID));
						sum2 += Math.abs(neib_sim);
					}
				}
			}
			if (sum2 == 0) {
				return 0;
			}
			double f1 = new BigDecimal((double) sum1 / sum2).setScale(15, BigDecimal.ROUND_HALF_UP).doubleValue();
			if (mItemsAverageScores.get(itemID) == null) {
				return Double.parseDouble(String.format("%15f", (0 + f1)));
			} else {
				return Double.parseDouble(String.format("%15f", (mItemsAverageScores.get(itemID) + f1)));
			}
			// return f1;
		} else {
			return 0.0;
		}
	}
}
