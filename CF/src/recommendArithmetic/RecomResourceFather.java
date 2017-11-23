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
 * @作者： wlb
 * 
 * @类名称：RecomResourceFather
 * @类描述：所有推荐算法类的父类 @创建时间：2017 2017年11月11日 下午6:53:57 ：
 */
public abstract class RecomResourceFather {

	/**
	 * @功能描述:返回为用户推荐的列表 @步骤： 1、readFile()得到用户资源评分矩阵mUserItemsScore 2、getAver（）得到平均分
	 *                  3、getNeighbor（）得到最近邻 4、getRecommendForUsers（）得到为用户推荐的资源列表
	 *                  5、getPrecisionAndRecall（）得到推荐的准确率和查全率 6、getRMSEAndMAE（）得到
	 * @return
	 */
	public abstract String getRecomResultForUsers();

	/**
	 * @功能描述:得到用户-item评分矩阵：mUserItemsScore
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
			System.out.println("文件不存在" + e.getMessage());
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
				// 构造矩阵
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
			System.out.println("读文件发生错误" + e.getMessage());
			return mUserItemsScore;
		}
		return mUserItemsScore;
	}

	/**
	 * @功能描述:用户的平均评分
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
			System.out.println("mUserItemsScore为空，请先调用readfile获取");
			return mUsersAverageScores;
		}

	}

	/**
	 * @功能描述:求每个用户的最近邻
	 * @param mUserItemsScore
	 * @return mUsersNeighbors
	 */
	public Map<Integer, List<Neighbor>> getNeighborUsers(Map<Integer, List<Item>> mUserItemsScore, int UN) {
		Map<Integer, List<Neighbor>> mUsersNeighbors = new HashMap<Integer, List<Neighbor>>();
		if (mUserItemsScore == null) {
			System.out.println("mUserItemsScore为空，请先调用readfile获取");
			return mUsersNeighbors;
		} else {
			for (Integer userid : mUserItemsScore.keySet()) {
				ArrayList<Neighbor> neighborList = new ArrayList<Neighbor>();// 会将压入的Neighbor排好序存放
				// 得到userid 与每个用户的相似度，并排序，存入list
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
	 * 得到每个用户对未评分资源的评分：mUsersPredict
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
			System.out.println("mUsersPredict: 用户对未评分资源的预测  over");
			return mUsersPredict;
		}
	}

	/**
	 * @功能描述:根据最近邻居给出预测评分方法
	 * @param userID
	 * @param itemID
	 * @param mUserItemsScore
	 * @param mUsersNeighbors
	 * @param UN:最近邻个数
	 * @return
	 */
	public double predictForRecommend(int userID, int itemID, Map<Integer, List<Item>> mUserItemsScore,
			Map<Integer, List<Neighbor>> mUsersNeighbors, int UN) {// 这里的userID为用户输入，减1后为数组下标！
		double sum = 0;
		if (mUsersNeighbors == null || mUserItemsScore == null) {
			System.out.println("mUsersNeighbors为空，请先调用最近用户计算方法获取");
			return sum;
		} else {
			List<Neighbor> list = mUsersNeighbors.get(userID);
			if (list != null) {
				for (int i = 0; i < UN; i++) {// 对最近的UN个邻居进行处理
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
	 * @功能描述:计算相似度
	 * @param UseID：目标用户ID
	 * @param otherid：其他用户ID
	 * @param userlist：目标用户的数据
	 * @param otherlist：其他用户的数据
	 * @return：返回目标用户userid和otherid的相似度
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
	 * @功能描述:计算准确率和查全率
	 * @return
	 */
	public String getPrecisionAndRecall(Map<Integer, List<Item>> mUsersPredict, String mResouUserAndItemTestFile,
			int mItemNum) {
		if (mResouUserAndItemTestFile == null || mUsersPredict == null || mItemNum == 0) {
			return "0/0";
		} else {
			/**
			 * 得到users的测试集列表中的个数
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
				System.out.println("getPrecisionAndRecall+当前文档不存在");
				e.printStackTrace();
			} catch (NumberFormatException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("mTestItemList:测试集 over" + mTestItemList.get(458).size());
			/**
			 * 得到users的推荐列表和测试列表中的共同个数
			 */
			Map<Integer, Integer> mPreAndTestItemList = new HashMap<Integer, Integer>();
			Set<Integer> keySet = mUsersPredict.keySet();
			Iterator<Integer> iterator = keySet.iterator();
			while (iterator.hasNext()) {
				int mUserid = iterator.next();
				int nM = 0;
				// 预测评分
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
			 * 计算准确率
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
	 * @return RMSE和MAE “0.2/0.3”
	 */
	public String getRMSEAndMAE(String testFilePath, int UN, Map<Integer, List<Item>> mUserItemsScore,
			Map<Integer, List<Neighbor>> mUsersNeighbors, Map<Integer, Double> mUsersAverageScores) {
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
//				File outputFile = new File("E:/毕业/实验/testResultByUCF.txt");
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

	public double predictForRMSEandMAE(int userID, int itemID, int UN, Map<Integer, List<Item>> mUserItemsScore,
			Map<Integer, List<Neighbor>> mUsersNeighbors, Map<Integer, Double> mUsersAverageScores) {// 这里的userID为用户输入，减1后为数组下标！
		double sum1 = 0;
		double sum2 = 0;
		List<Neighbor> userIDNeighbors = mUsersNeighbors.get(userID);
		for (int i = 0; i < UN; i++) {// 对最近的UN个邻居进行处理
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
