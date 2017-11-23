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

import Utils.EvaluateIndex;
import Utils.Item;
import Utils.Neighbor;
import Utils.UserScore;

/**
 * @���ߣ� wlb
 * 
 * @�����ƣ�RecomResourceFather
 * @�������������Ƽ��㷨��ĸ��� @����ʱ�䣺2017 2017��11��11�� ����6:53:57 ��
 */
public abstract class RecomResourceFather {

	/**
	 * @��������:����Ϊ�û��Ƽ����б� @���裺 1��readFile()�õ��û���Դ���־���mUserItemsScore 2��getAver�����õ�ƽ����
	 *                  3��getNeighbor�����õ������ 4��getRecommendForUsers�����õ�Ϊ�û��Ƽ�����Դ�б�
	 *                  5��getPrecisionAndRecall�����õ��Ƽ���׼ȷ�ʺͲ�ȫ�� 6��getRMSEAndMAE�����õ�
	 * @return
	 */
	public abstract String getRecomResultForUsers();

	/**
	 * @��������:�õ��û�-item���־���mUserItemsScore
	 * @param filePath
	 * @return mUserItemsScore
	 */
	public Map<Integer, List<Item>> readFile(String filePath) {
		Map<Integer, List<Item>> mUserItemsScore = new HashMap<Integer, List<Item>>();
		File inputFile = new File(filePath);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(inputFile));
		} catch (FileNotFoundException e) {
			System.out.println("�ļ�������" + e.getMessage());
			return mUserItemsScore;
		}

		String sentence = "";
		String[] part = new String[3];
		try {
			while ((sentence = reader.readLine()) != null) {
				part = sentence.split("\t");
				int userID = Integer.parseInt(part[0]);
				int itemID = Integer.parseInt(part[1]);
				double Rate = Double.parseDouble(part[2]);
				// �������
				if (mUserItemsScore.containsKey(userID)) {
					if (!mUserItemsScore.get(userID).contains(new Item(userID, itemID, Rate))) {
						mUserItemsScore.get(userID).add(new Item(userID, itemID, Rate));
					}
				} else {
					Item item = new Item(userID, itemID, Rate);
					List<Item> list = new ArrayList<Item>();
					list.add(item);
					mUserItemsScore.put(userID, list);
				}
			}
			reader.close();
		} catch (NumberFormatException | IOException e) {
			System.out.println("���ļ���������" + e.getMessage());
			return mUserItemsScore;
		}
		return mUserItemsScore;
	}

	/**
	 * @��������:�û���ƽ������
	 * @param mUserItemsScore
	 * @return mUsersAverageScores
	 */
	public Map<Integer, Double> getAver(Map<Integer, List<Item>> mUserItemsScore) {
		Map<Integer, Double> mUsersAverageScores = new HashMap<Integer, Double>();
		if (mUserItemsScore != null) {
			for (Integer userid : mUserItemsScore.keySet()) {
				List<Item> list = mUserItemsScore.get(userid);
				int itemNums = list.size();
				Double totalScore = 0.0;
				for (int i = 0; i < list.size(); i++) {
					totalScore += list.get(i).getValue();
				}
				Double averScore = totalScore / itemNums;
				mUsersAverageScores.put(userid, averScore);
			}
			return mUsersAverageScores;
		} else {
			System.out.println("mUserItemsScoreΪ�գ����ȵ���readfile��ȡ");
			return mUsersAverageScores;
		}

	}

	/**
	 * @��������:��ÿ���û��������
	 * @param mUserItemsScore
	 * @return mUsersNeighbors
	 */
	public Map<Integer, List<Neighbor>> getNeighborUsers(Map<Integer, List<Item>> mUserItemsScore, int UN) {
		Map<Integer, List<Neighbor>> mUsersNeighbors = new HashMap<Integer, List<Neighbor>>();
		if (mUserItemsScore == null) {
			System.out.println("mUserItemsScoreΪ�գ����ȵ���readfile��ȡ");
			return mUsersNeighbors;
		} else {
			for (Integer userid : mUserItemsScore.keySet()) {
				ArrayList<Neighbor> neighborList = new ArrayList<Neighbor>();// �Ὣѹ���Neighbor�ź�����
				// �õ�userid ��ÿ���û������ƶȣ������򣬴���list
				for (Integer otherUser : mUserItemsScore.keySet()) {
					if (otherUser != userid) {
						double sim = CalculateSim(userid, otherUser, mUserItemsScore.get(userid),
								mUserItemsScore.get(otherUser));
						neighborList.add(new Neighbor(otherUser, sim));
					}
				}
				Collections.sort(neighborList);
				int k = 0;
				ArrayList<Neighbor> neighborListUN = new ArrayList<Neighbor>();
				while (k < UN && neighborList.get(k) != null) {
					neighborListUN.add(neighborList.get(k));
					k++;
				}
				mUsersNeighbors.put(userid, neighborListUN);
			}
			return mUsersNeighbors;
		}
	}

	/**
	 * �õ�ÿ���û���δ������Դ�����֣�mUsersPredict
	 * 
	 * @param num
	 */
	@SuppressWarnings("unchecked")
	public Map<Integer, List<Item>> getRecommendForUsers(String mResouUserAndItemFile,
			Map<Integer, List<Item>> mUserItemsScore, Map<Integer, List<Neighbor>> mUsersNeighbors, int UN) {
		Set<Integer> mItemsID = new TreeSet<Integer>();
		Map<Integer, List<Item>> mUsersPredict = new HashMap<Integer, List<Item>>();
		if (mResouUserAndItemFile == null || mUsersNeighbors == null || mUserItemsScore == null || UN == 0) {
			return mUsersPredict;
		} else {
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
							double predict = predictForRecommend(userid, next, mUserItemsScore, mUsersNeighbors, UN);
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
	}

	/**
	 * @��������:��������ھӸ���Ԥ�����ַ���
	 * @param userID
	 * @param itemID
	 * @param mUserItemsScore
	 * @param mUsersNeighbors
	 * @param UN:����ڸ���
	 * @return
	 */
	public double predictForRecommend(int userID, int itemID, Map<Integer, List<Item>> mUserItemsScore,
			Map<Integer, List<Neighbor>> mUsersNeighbors, int UN) {// �����userIDΪ�û����룬��1��Ϊ�����±꣡
		double sum = 0;
		if (mUsersNeighbors == null || mUserItemsScore == null) {
			System.out.println("mUsersNeighborsΪ�գ����ȵ�������û����㷽����ȡ");
			return sum;
		} else {
			List<Neighbor> list = mUsersNeighbors.get(userID);
			if (list != null) {
				for (int i = 0; i < UN; i++) {// �������UN���ھӽ��д���
					int neighborID = list.get(i).getID();
					List<Item> list2 = mUserItemsScore.get(neighborID);
					int index = list2.indexOf(new Item(0, itemID, 0));
					if (index != -1) {
							Item item = list2.get(index);
							if (item.getID() == itemID) {
								double neib_sim = list.get(i).getValue();
								sum += neib_sim * item.getValue();
						}
					}
				}
			}
			return Double.parseDouble(String.format("%15f", sum));
		}
	}

	/**
	 * @��������:�������ƶ�
	 * @param UseID��Ŀ���û�ID
	 * @param otherid�������û�ID
	 * @param userlist��Ŀ���û�������
	 * @param otherlist�������û�������
	 * @return������Ŀ���û�userid��otherid�����ƶ�
	 */
	public abstract double CalculateSim(int UseID, int otherid, List<Item> userlist, List<Item> otherlist);
	public  double CalculateSim2(int UseID, int otherid, List<Item> userlist, List<Item> otherlist) {
		int count = 0;

		for (int i = 0; i < userlist.size(); i++) {
			Item item = userlist.get(i);
			if(otherlist.contains(item)){
				count++;
			}
		}

		double result = count / (double) Math.sqrt(userlist.size() * otherlist.size());
		return result;
	}

	/**
	 * @��������:����׼ȷ�ʺͲ�ȫ��
	 * @return
	 */
	public String getPrecisionAndRecall(Map<Integer, List<Item>> mUsersPredict, String mResouUserAndItemTestFile,
			int mItemNum) {
		if (mResouUserAndItemTestFile == null || mUsersPredict == null || mItemNum == 0) {
			return "0/0";
		} else {
			/**
			 * �õ�users�Ĳ��Լ��б��еĸ���
			 */
			Map<Integer, List<Integer>> mTestItemList = new HashMap<Integer, List<Integer>>();
			File inputFile = new File(mResouUserAndItemTestFile);
			try {
				if (!inputFile.exists() || inputFile.isDirectory())
					throw new FileNotFoundException();
				BufferedReader reader = new BufferedReader(new FileReader(inputFile));
				String mTestStr = "";
				while ((mTestStr = reader.readLine()) != null) {
					String[] part = mTestStr.split("\t");
					int userID = Integer.parseInt(part[0]);
					int itemID = Integer.parseInt(part[1]);
					if (mTestItemList.containsKey(userID)) {
						mTestItemList.get(userID).add(itemID);
					} else {
						List<Integer> list = new ArrayList<Integer>();
						list.add(itemID);
						mTestItemList.put(userID, list);
					}
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				System.out.println("getPrecisionAndRecall+��ǰ�ĵ�������");
				e.printStackTrace();
			} catch (NumberFormatException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("mTestItemList:���Լ� over" + mTestItemList.get(458).size());
			/**
			 * �õ�users���Ƽ��б�Ͳ����б��еĹ�ͬ����
			 */
			Map<Integer, Integer> mPreAndTestItemList = new HashMap<Integer, Integer>();
			Set<Integer> keySet = mUsersPredict.keySet();
			Iterator<Integer> iterator = keySet.iterator();
			while (iterator.hasNext()) {
				int mUserid = iterator.next();
				int nM = 0;
				// Ԥ������
				List<Item> mPredictList = mUsersPredict.get(mUserid);
				List<Integer> mTestList = mTestItemList.get(mUserid);
				for (int i = 0; i < mItemNum; i++) {
					if (mTestList != null && mTestList.contains(mPredictList.get(i).getID())) {
						nM++;
					}
				}
				mPreAndTestItemList.put(mUserid, nM);
			}

			double allNumTe = 0;
			double mRecommedNum = 0;
			double mTestNum = 0;
			Set<Integer> keySet2 = mPreAndTestItemList.keySet();
			Iterator<Integer> iterator2 = keySet2.iterator();
			while (iterator2.hasNext()) {
				allNumTe += mPreAndTestItemList.get(iterator2.next());
				mRecommedNum += mItemNum;

			}
			Iterator<Integer> iterator3 = keySet2.iterator();
			while (iterator3.hasNext()) {
				if (mTestItemList != null) {
					List<Integer> list = mTestItemList.get(iterator3.next());
					if (list != null) {
						mTestNum += list.size();
					}
				}
			}

			/**
			 * ����׼ȷ��
			 */
			double precision = allNumTe / mRecommedNum;
			double recall = allNumTe / mTestNum;
			return precision + "/" + recall;
		}
	}

	/**
	 * @param testFilePath
	 * @param UN
	 * @param mUserItemsScore
	 * @param mUsersNeighbors
	 * @param mUsersAverageScores
	 * @return RMSE��MAE ��0.2/0.3��
	 */
	public String getRMSEAndMAE(String testFilePath, int UN, Map<Integer, List<Item>> mUserItemsScore,
			Map<Integer, List<Neighbor>> mUsersNeighbors, Map<Integer, Double> mUsersAverageScores) {
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
//				File outputFile = new File("E:/��ҵ/ʵ��/testResultByUCF.txt");
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
					double predictRate = predictForRMSEandMAE(userID, itemID, UN, mUserItemsScore, mUsersNeighbors,
							mUsersAverageScores);
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

	public double predictForRMSEandMAE(int userID, int itemID, int UN, Map<Integer, List<Item>> mUserItemsScore,
			Map<Integer, List<Neighbor>> mUsersNeighbors, Map<Integer, Double> mUsersAverageScores) {// �����userIDΪ�û����룬��1��Ϊ�����±꣡
		double sum1 = 0;
		double sum2 = 0;
		List<Neighbor> userIDNeighbors = mUsersNeighbors.get(userID);
		for (int i = 0; i < UN; i++) {// �������UN���ھӽ��д���
			Neighbor neighbor = userIDNeighbors.get(i);
			if (neighbor != null) {
				int neighborID = neighbor.getID();
				double neib_sim = neighbor.getValue();
				List<Item> neighborItems = mUserItemsScore.get(neighborID);
				int index = neighborItems.indexOf(new Item(neighborID, itemID, 0));
				if (index != -1) {
					sum1 += neib_sim
							* (neighborItems.get(index).getValue() - (double) mUsersAverageScores.get(neighborID));
					sum2 += Math.abs(neib_sim);
				}
			}
		}
		if (sum2 == 0) {
			return 0;
		}
		double f1 = new BigDecimal((double) sum1 / sum2).setScale(15, BigDecimal.ROUND_HALF_UP).doubleValue();
		return Double.parseDouble(String.format("%15f", (mUsersAverageScores.get(userID) + f1)));
	}
}
