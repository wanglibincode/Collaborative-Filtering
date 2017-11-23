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
	// Ϊÿ���û��Ƽ�����Դ�б�
	private Map<Integer, List<Item>> mUsersPredict;
	// �û���Դ�����ļ�
	private String mResouUserAndItemFile;
	// �û���Դ���־���
	private Map<Integer, List<Item>> mUserItemsScore;
	// ÿ����Դ��ƽ������
	private Map<Integer, Double> mItemsAverageScores;
	// ÿ���û���ƽ����
	private Map<Integer, Double> mUsersAverageScores;

	// �û���������û�
	private Map<Integer, List<Neighbor>> mItemsNeighbors;
	// �û�������û�����
	private int UN = 80;
	// Ϊÿ���û��Ƽ���Դ�ĸ���
	private int mItemNum = 10;
	// ÿ����Ŀ���û�������
	private Map<Integer, List<UserScore>> mItemsUsers;
	// ����item
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
		// �õ��û�-��Դ���־���
		mUserItemsScore = readFile(mResouUserAndItemFile);
		if (mUserItemsScore != null) {
			System.out.println("��ȴ������ڷ���ItemCF...");
			// �õ�ÿ����Դ��ƽ����
			mItemsAverageScores = getAver(mUserItemsScore);
			// �õ�ÿ���û���ƽ����
			mUsersAverageScores = super.getAver(mUserItemsScore);
			// �õ�ÿ����Դ�����ƶȣ������
			mItemsNeighbors = getNeighborItems();
			// �õ�Ϊÿ���û��Ƽ�����Դ�б�-mUsersPredict
			mUsersPredict = getRecommendForUsers();
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
			rmseAndMAE = getRMSEAndMAE(mResouUserAndItemTestFile, UN, mUserItemsScore, mItemsNeighbors,
					mItemsAverageScores);
		}
		return "��ȷ��/�ٻ���:  " + precisionAndRecall + "----" + "RMSE/MAE:  " + rmseAndMAE + "---  �Ƽ����"
				+ mRecomForUsersResult;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see recommendArithmetic.RecomResourceFather#getAver(java.util.Map)
	 * ��ÿ��Item��ƽ������
	 */
	public Map<Integer, Double> getAver(Map<Integer, List<Item>> mUserItemsScore) {
		Map<Integer, Double> mItemsAverageScores = new HashMap<Integer, Double>();
		mItemsUsers = new HashMap<Integer, List<UserScore>>();

		if (mUserItemsScore != null) {

			// ���mItemsUsers
			for (Integer userid : mUserItemsScore.keySet()) {
				// ÿ���û������ֵ�Item�б�
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
			// ��ÿ����Ŀ��ƽ���� mItemsAverageScores
			Set<Integer> itemSet = mItemsUsers.keySet();
			for (Integer itemId : itemSet) {
				Double totalScore = 0.0;
				List<UserScore> userScoreList = mItemsUsers.get(itemId);
				int usersNum = userScoreList.size();
				for (UserScore userScore : userScoreList) {
					totalScore += userScore.getScore();
				}
				Double averScore = totalScore / usersNum;
				// System.out.println("ItemId:---"+itemId+"ƽ���֣�--"+averScore);
				mItemsAverageScores.put(itemId, averScore);
			}
			return mItemsAverageScores;

		} else {
			System.out.println("mUserItemsScoreΪ�գ����ȵ���readfile��ȡ");
			return mItemsAverageScores;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see recommendArithmetic.RecomResourceFather#getNeighborUsers(java.util.Map,
	 * int) ����ÿ��Item�������Item
	 */
	public Map<Integer, List<Neighbor>> getNeighborItems() {
		Map<Integer, List<Neighbor>> mItemsNeighbors = new HashMap<Integer, List<Neighbor>>();
		if (mItemsUsers == null) {
			System.out.println("mItemsUsersΪ�գ����ȵ���getAev��ȡ");
			return mItemsNeighbors;
		} else {
			for (Integer itemId : mItemsUsers.keySet()) {
				ArrayList<Neighbor> neighborList = new ArrayList<Neighbor>();// �Ὣѹ���Neighbor�ź�����
				// �õ�userid ��ÿ���û������ƶȣ������򣬴���list
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
	 * �õ�ÿ���û���δ������Դ�����֣�mUsersPredict
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
		 * ��ÿ���û�����Щ��Դ�Ѿ�����
		 */
		Map<Integer, List<Integer>> mUsersAready = new HashMap<Integer, List<Integer>>();
		// ���ļ�
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
		System.out.println("mUsersAready :�û��Ѿ����ֵ���Դ over" + mUsersAready.get(1).get(0));
		mUsersPredict = new HashMap<Integer, List<Item>>();
		for (Integer userid : mUsersAready.keySet()) {
			List<Item> mPredictItem = new ArrayList<Item>();
			Iterator<Integer> iterator = mItemsID.iterator();
			while (iterator.hasNext()) {
				Integer next = iterator.next();
				// ���mUserArray��û�е�ǰ�û�����˵����ǰ�û�û�ж��κ���Դ�����֣�������
				if (mUsersAready.containsKey(userid)) {
					List<Integer> mAreadyItem = mUsersAready.get(userid);
					// �����ǰ�û��Ե�ǰ��Դû�����֣���Ԥ�⣬�����뵽mPredictItem��
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
		System.out.println("mUsersPredict: �û���δ������Դ��Ԥ��  over");
		return mUsersPredict;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see recommendArithmetic.RecomResourceFather#CalculateSim(int, int,
	 * java.util.List, java.util.List) ����Item�����ƶ�
	 */
	@Override
	public double CalculateSim(int UseID, int otherid, List<Item> userlist, List<Item> otherlist) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @��������:����item֮������ƶ�
	 * @param UseID
	 * @param otherid
	 * @param itemUserList
	 * @param otherItemUserList
	 * @return
	 */
	public double CalculateItemSim(int itemId, int otherItemId, List<UserScore> itemUserList,
			List<UserScore> otherItemUserList) {
		/**
		 * �����ҳ�DealedOfRate/rate��UseID��id�����ֵ���Ŀ������Ч
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
	 * �����������ƶȣ�
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
		// �������ƶȵķ���
		for (int i = 0; i < itemScore.size(); i++) {
			upside += (itemScore.get(i).getScore() - mUsersAverageScores.get(itemScore.get(i).getUserid()))
					* (otheritemScore.get(i).getScore() - mUsersAverageScores.get(otheritemScore.get(i).getUserid()));
		}

		// �������ƶȵķ�ĸ,Ŀ���û�userid
		for (int xi = 0; xi < itemUserList.size(); xi++) {
			downside_x += Math.pow(
					itemUserList.get(xi).getScore() - mUsersAverageScores.get(itemUserList.get(xi).getUserid()), 2);
		}
		// �������ƶȵķ�ĸ,�����û�id
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
	 * �����������ƶ� ��ĸΪihej �Ĺ�ͬ
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
		// �������ƶȵķ���
		for (int i = 0; i < itemScore.size(); i++) {
			upside += (itemScore.get(i).getScore() - mUsersAverageScores.get(itemScore.get(i).getUserid()))
					* (otheritemScore.get(i).getScore() - mUsersAverageScores.get(otheritemScore.get(i).getUserid()));
		}

		// �������ƶȵķ�ĸ,Ŀ���û�userid
		for (int xi = 0; xi < itemScore.size(); xi++) {
			downside_x += Math
					.pow(itemScore.get(xi).getScore() - mUsersAverageScores.get(itemScore.get(xi).getUserid()), 2);
		}
		// �������ƶȵķ�ĸ,�����û�id
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
		 * �����ҳ�DealedOfRate/rate��UseID��id�����ֵ���Ŀ������Ч
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
	 * Ƥ��ѷ���ƶ�
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
		// �������ƶȵķ���
		for (int i = 0; i < len; i++) {
			upside += (itemScore.get(i).getScore() - mItemsAverageScores.get(itemId))
					* (otheritemScore.get(i).getScore() - mItemsAverageScores.get(otherItemId));

			// �������ƶȵķ�ĸ,Ŀ���û�userid
			downside_x += Math.pow((itemScore.get(i).getScore() - mItemsAverageScores.get(itemId)), 2);
			// �������ƶȵķ�ĸ,�����û�id
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
		List<Double> x = new LinkedList<Double>();// LinkedList���ն�������˳��洢
		List<Double> y = new LinkedList<Double>();
		BufferedReader reader = null;
		FileWriter writer = null;
		// ���ļ�
		try {
			File inputFile = new File(testFilePath);
			if (inputFile.exists() || !inputFile.isDirectory()) {
				reader = new BufferedReader(new FileReader(inputFile));

				// д�ļ�
				// File outputFile = new File("E:/��ҵ/ʵ��/testResultByUCF.txt");
				File outputFile = new File("F:/wlb/����/ʵ��/data/testResultByUCF.txt");
				if (!outputFile.exists()) {
					if (!outputFile.createNewFile()) {
						System.out.println("����ļ�����ʧ��");
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
				// �õ�RMSE��MAE
				EvaluateIndex evaluate = new EvaluateIndex();
				// �õ�RMSE��MAE
				double RMSE = evaluate.analyse(x, y);
				double MAE = evaluate.MAE_analyse(x, y);
				return RMSE + "/" + MAE;
			} else {
				System.out.println("���Լ�Ϊ�գ��޷�����RMSE��MAE");
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
			System.out.println("mUsersNeighborsΪ�գ����ȵ�������û����㷽����ȡ");
			return sum;
		} else {
			List<Neighbor> itemList = mItemsNeighbors.get(itemID);
			if (itemList != null) {
				for (int i = 0; i < UN; i++) {// �������UN���ھӽ��д���
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
			Map<Integer, List<Neighbor>> mItemsNeighbors, Map<Integer, Double> mItemsAverageScores) {// �����userIDΪ�û����룬��1��Ϊ�����±꣡
		double sum1 = 0;
		double sum2 = 0;
		List<Neighbor> itemIDNeighbors = mItemsNeighbors.get(itemID);
		if (itemIDNeighbors != null) {
			for (int i = 0; i < UN; i++) {// �������UN���ھӽ��д���
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
