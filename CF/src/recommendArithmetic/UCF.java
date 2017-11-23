package recommendArithmetic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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

import Utils.EvaluateIndex;
import Utils.Item;
import Utils.Neighbor;

/**
 * 修改后的协同过滤推荐算法，基于知识关联的协同过滤推荐算法
 * */
public class UCF {
	public static final int USERSIZE = 943;
	public static final int ITEMSIZE = 1682;
	public int UN = 10;// 某一user的最近邻居数

	public int[] num = new int[USERSIZE + 1];// 每个用户为几部评了分
	public int[] Itemnum = new int[ITEMSIZE + 1];// 每个Item被多少用户评了分
	public double[] average = new double[USERSIZE + 1];// 每个user的平均打分
	public double[] ItemAverage = new double[ITEMSIZE + 1];// 每个item的平均打分
	public double[][] rate = new double[USERSIZE + 1][ITEMSIZE + 1];// 评分矩阵
	public double[][] DealedOfRate = new double[USERSIZE + 1][ITEMSIZE + 1];// 针对稀疏问题处理后的评分矩阵
	Neighbor[][] NofUser;

	List<Double> x = new LinkedList<Double>();
	List<Double> y = new LinkedList<Double>();
	private Map<Integer, List<Item>> mUsersPredict;
	private String precisionRecall;

	public UCF(int UN) {
		this.UN = UN;
		NofUser = new Neighbor[USERSIZE + 1][this.UN + 1];// 每个用户的最近的UN个邻居
	}

	public String getCoefficient(String path, String baseDataFile,
			String testDataFile) {
		double RMSE = 0.0;
		double MAE = 0.0;
		if (readFile(path + baseDataFile)) {
			System.out.println("请等待，正在分析");
			getAvr();// 得到average[]
			dealRate();// 得到DealedOfRate
			getNofUser();// 得到NofUser
			BufferedReader reader = null;
			FileWriter writer = null;
			// 读文件
			try {
				File inputFile = new File(path + testDataFile);
				if (!inputFile.exists() || inputFile.isDirectory())
					throw new FileNotFoundException();
				reader = new BufferedReader(new FileReader(inputFile));

				// 写文件
				File outputFile = new File(
						"F:/wlb/论文/实验/data/testResultByUCF.txt");
				if (!outputFile.exists()) {
					if (!outputFile.createNewFile()) {
						System.out.println("输出文件创建失败");
					}
				}
				writer = new FileWriter(outputFile);
				String title = "UserID" + "\t" + "ItemID" + "\t"
						+ "OriginalRate" + "\t" + "PredictRate" + "\r\n";
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
					double predictRate = predict(userID, itemID);
					// if (predictRate != 0) {
					x.add(originalRate);
					y.add(predictRate);
					// }
					// System.out.println(cf.x.size()+"cf.x.size()"+cf.y.size()+"cf.y.size()");
					tmpToWrite = userID + "\t" + itemID + "\t" + originalRate
							+ "\t" + predictRate + "\r\n";
					writer.write(tmpToWrite);
					writer.flush();
				}
				writer.close();
				reader.close();
				EvaluateIndex evaluate = new EvaluateIndex();
				// 得到RMSE和MAX
				RMSE = evaluate.analyse(x, y);
				MAE = evaluate.MAE_analyse(x, y);
				// 得到为每个用户推荐的资源列表
				getRecommendForUsers(path, baseDataFile);
				precisionRecall = getPrecisionAndRecall(10, path, testDataFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return RMSE + "/" + MAE + "/" + precisionRecall;
		} else {
			return RMSE + "/" + MAE;
		}
	}

	// 1-3处理评分矩阵的稀疏问题（重要事项！！！）
	// 重点处理该user对没有被评分的item，会打几分
	// 暂时用1-2中计算出的平均分
	public void dealRate() {
		int i, j;
		for (i = 1; i <= USERSIZE; i++) {
			for (j = 1; j <= ITEMSIZE; j++) {
				if (rate[i][j] == 0) {
					DealedOfRate[i][j] = 0;
				} else {
					DealedOfRate[i][j] = rate[i][j];
				}
			}
		}
	}

	// Chapter2：聚类，找和某一用户有相同喜好的一类用户
	// 2-1：:Pearson计算向量的相似度
	public double Sum(double[] arr) {
		double total = (double) 0.0;
		for (double ele : arr)
			total += ele;
		return total;
	}

	public double Mutipl(double[] arr1, double[] arr2, int len) {
		double total = (double) 0.0;
		for (int i = 0; i < len; i++)
			total += arr1[i] * arr2[i];
		return total;
	}

	/**
	 * 求每个用户对项目评分的平均分
	 */
	public void getAvr() {
		getLen();
		int i, j;
		for (i = 1; i <= USERSIZE; i++) {
			double sum = 0.0;
			for (j = 1; j < rate[i].length; j++) {// 每个length都是ITEMSIZE=1682
				sum += rate[i][j];
			}
			if (num[i] == 0) {
				average[i] = 0;
			} else {
				double f1 = new BigDecimal((double) sum / num[i]).setScale(15,
						BigDecimal.ROUND_HALF_UP).doubleValue();
				average[i] = f1;
			}

		}
	}

	/**
	 * 每个用户的评分项目个数
	 */
	public void getLen() {// 计算每个用户为几部电影打分
		for (int i = 1; i <= USERSIZE; i++) {
			int n = 0;
			for (int j = 1; j <= ITEMSIZE; j++) {
				if (rate[i][j] != 0)
					n++;
			}
			num[i] = n;
		}

	}

	// Chapter1:准备工作
	// 1-1:读取文件内容，得到评分矩阵 1:读取成功 -1：读取失败
	public boolean readFile(String filePath) {
		File inputFile = new File(filePath);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(inputFile));
		} catch (FileNotFoundException e) {
			System.out.println("文件不存在" + e.getMessage());
			return false;
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
				rate[userID][itemID] = Rate;
			}
			reader.close();
		} catch (NumberFormatException | IOException e) {
			System.out.println("读文件发生错误" + e.getMessage());
			return false;
		}
		return true;
	}

	// Chapter3:根据最近邻居给出预测评分
	public double predict(int userID, int itemID) {// 这里的userID为用户输入，减1后为数组下标！
		double sum1 = 0;
		double sum2 = 0;
		for (int i = 1; i <= UN; i++) {// 对最近的UN个邻居进行处理
			if (NofUser[userID][i] != null) {
				int neighborID = NofUser[userID][i].getID();
				double neib_sim = NofUser[userID][i].getValue();
				if (DealedOfRate[neighborID][itemID] != 0) {
					sum1 += neib_sim
							* (DealedOfRate[neighborID][itemID] - average[neighborID]);
					sum2 += Math.abs(neib_sim);
				}
			}
		}
		if (sum2 == 0) {
			return 0;
		}
		double f1 = new BigDecimal((double) sum1 / sum2).setScale(15,
				BigDecimal.ROUND_HALF_UP).doubleValue();
		return Double
				.parseDouble(String.format("%15f", (average[userID] + f1)));
	}

	// 根据最近邻居给出预测评分第二种方法
	public double predict2(int userID, int itemID) {// 这里的userID为用户输入，减1后为数组下标！
		double sum1 = 0;
		for (int i = 1; i <= UN; i++) {// 对最近的UN个邻居进行处理
			if (NofUser[userID][i] != null) {
				int neighborID = NofUser[userID][i].getID();
				if (DealedOfRate[neighborID][itemID] != 0) {
					double neib_sim = NofUser[userID][i].getValue();
					sum1 += neib_sim * DealedOfRate[neighborID][itemID];
				}
			}
		}
		return Double.parseDouble(String.format("%15f", sum1));
	}

	// 2-2将Pearson算法用在求user的近邻上，求NofUser数组
	@SuppressWarnings("unchecked")
	public void getNofUser() {
		int id, userID;
		for (userID = 1; userID <= USERSIZE; userID++) {
			// 有一个问题，就是两个不同的用户，与目标用户有相同相似度时，set默认会保留一个
			// Set<Neighbor> neighborList = new TreeSet();// 会将压入的Neighbor排好序存放
			ArrayList<Neighbor> neighborList = new ArrayList<Neighbor>();// 会将压入的Neighbor排好序存放
			Neighbor[] tmpNeighbor = new Neighbor[USERSIZE + 1];
			for (id = 1; id <= USERSIZE; id++) {
				if (id != userID) {
					// double sim = Pearson(userID, id, DealedOfRate[userID],
					// DealedOfRate[id]);
					double sim = Pearson_A(userID, id, DealedOfRate[userID],
							DealedOfRate[id]);
					tmpNeighbor[id] = new Neighbor(id, sim);
					neighborList.add(tmpNeighbor[id]);
				} else {
					tmpNeighbor[id] = new Neighbor(id, -1000);
					neighborList.add(tmpNeighbor[id]);
				}

			}
			int k = 1;
			Collections.sort(neighborList);
			// System.out.println(UN);
			while (k <= UN && neighborList.get(k - 1) != null) {
				NofUser[userID][k] = neighborList.get(k - 1);
				// System.out.println("NofUser[userID].length" + "userID" +
				// userID
				// + "k:" + k + "------" + NofUser[userID][k].getValue());
				k++;
			}
		}
	}

	/**
	 * 
	 * @param UseID
	 * @param id
	 * @param x
	 * @param y
	 * @return
	 */
	public double Pearson_A(int UseID, int id, double[] x, double[] y) {
		/**
		 * 由于找出DealedOfRate/rate中UseID和id都评分的项目，才有效
		 */
		// System.out.println("x.length=====y.length" + x.length + "y.length"
		// + y.length);
		int count = 0;
		for (int i = 1; i < y.length; i++) {
			if (y[i] != 0 && x[i] != 0) {
				count++;
			}
		}
		double[] x1 = new double[count];
		double[] y1 = new double[count];
		int j = 0;
		for (int i = 1; i < y.length; i++) {
			if (j < count) {
				if (y[i] != 0 && x[i] != 0) {
					x1[j] = x[i];
					y1[j] = y[i];
					j++;
				}
			}
		}
		// 两个用户没有共同评分项目
		if (j == 0) {
			return 0;
		}
		int len = x1.length;// 共同评分项目的个数

		// System.out.println(len + "----len-count----" + count);
		double result = simMethod(UseID, id, len, x1, y1, x, y);
		// double result2 = simPerMethod(UseID, id, len, x1, y1);
		// System.out.println(result + "----修正余弦-皮尔逊----" + result2);

		return result;

	}

	private double simMethod(int UseID, int id, int len, double[] x1,
			double[] y1, double[] x, double[] y) {
		double upside = 0.0;
		double downside_x = 0.0;
		double downside_y = 0.0;
		// 余弦相似度的分子
		for (int i = 0; i < len; i++) {
			upside += (x1[i] - average[UseID]) * (y1[i] - average[id]);
		}
		// 余弦相似度的分母,目标用户userid
		for (int xi = 1; xi < x.length; xi++) {
			if (x[xi] != 0) {
				downside_x += Math.pow((x[xi] - average[UseID]), 2);
			}
		}
		// 余弦相似度的分母,其他用户id
		for (int yi = 1; yi < y.length; yi++) {
			if (y[yi] != 0) {
				downside_y += Math.pow((y[yi] - average[id]), 2);
			}
		}

		double downside = (double) Math.sqrt(downside_x)
				* (double) Math.sqrt(downside_y);
		if (downside == 0) {
			return 0;
		}
		return upside / downside;
	}

	/**
	 * 得到每个用户对未评分资源的评分：mUsersPredict
	 * */
	@SuppressWarnings("unchecked")
	public void getRecommendForUsers(String path, String baseDataFile) {
		BufferedReader reader = null;
		/**
		 * 求每个用户对哪些资源已经评分
		 * */
		Map<Integer, List<Integer>> mUsersAready = new HashMap<Integer, List<Integer>>();
		// 读文件
		try {
			File inputFile = new File(path + baseDataFile);
			if (!inputFile.exists() || inputFile.isDirectory())
				throw new FileNotFoundException();
			reader = new BufferedReader(new FileReader(inputFile));
			String[] part = new String[3];
			String tmpToRead = "";
			while ((tmpToRead = reader.readLine()) != null) {
				part = tmpToRead.split("\t");
				int userID = Integer.parseInt(part[0]);
				int itemID = Integer.parseInt(part[1]);
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
		System.out.println("mUsersAready :用户已经评分的资源 over"
				+ mUsersAready.get(1).get(0));
		/**
		 * 将每个用户的对未评分项目的预测评分
		 * */
		mUsersPredict = new HashMap<Integer, List<Item>>();
		for (int i = 1; i < USERSIZE + 1; i++) {
			List<Item> mPredictItem = new ArrayList<Item>();
			for (int j = 1; j < ITEMSIZE + 1; j++) {
				// 如果mUserArray中没有当前用户，就说明当前用户没有对任何资源评过分，冷启动
				if (mUsersAready.containsKey(i)) {
					List<Integer> mAreadyItem = mUsersAready.get(i);
					// 如果当前用户对当前资源没有评分，则预测，并加入到mPredictItem中
					if (!mAreadyItem.contains(j)) {
						double predict = predict2(i, j);
						Item item = new Item(i, j, predict);
						mPredictItem.add(item);
					}
				}
			}
			if (mPredictItem.size() != 0) {
				Collections.sort(mPredictItem);
				mUsersPredict.put(i, mPredictItem);
			}
		}
		System.out.println("mUsersPredict: 用户对未评分资源的预测  over");
		/**
		 * 将每个用户的对项目的预测评分，输出成文档
		 * */
		try {
			File outputFile = new File("F:/wlb/论文/实验/data/PredictForUsers.txt");
			if (!outputFile.exists()) {
				if (!outputFile.createNewFile()) {
					System.out.println("输出文件创建失败");
				}
			}
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(
					outputFile));
			for (int i = 1; i < USERSIZE + 1; i++) {
				if (mUsersPredict.containsKey(i)) {
					List<Item> mPredictForI = mUsersPredict.get(i);
					for (int j = 0; j < mPredictForI.size(); j++) {
						String tmpToWrite = i + "\t"
								+ mPredictForI.get(j).getID() + "\t"
								+ mPredictForI.get(j).getValue() + "\r\n";
						bufferedWriter.write(tmpToWrite);
						bufferedWriter.flush();
					}
				}
			}
			bufferedWriter.close();
			System.out.println("PredictForUsers：用户对未评分资源的预测评分输出文档   Over");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public String getPrecisionAndRecall(int num, String path,
			String testDataFile) {

		/**
		 * 得到users的测试集列表中的个数
		 * */
		Map<Integer, List<Integer>> mTestItemList = new HashMap<Integer, List<Integer>>();
		File inputFile = new File(path + testDataFile);
		try {
			if (!inputFile.exists() || inputFile.isDirectory())
				throw new FileNotFoundException();
			BufferedReader reader = new BufferedReader(
					new FileReader(inputFile));
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
		System.out.println("mTestItemList:测试集 over"
				+ mTestItemList.get(458).size());
		/**
		 * 得到users的推荐列表和测试列表中的共同个数
		 * */
		Map<Integer, Integer> mPreAndTestItemList = new HashMap<Integer, Integer>();
		Set<Integer> keySet = mUsersPredict.keySet();
		Iterator<Integer> iterator = keySet.iterator();
		while (iterator.hasNext()) {
			int mUserid = iterator.next();
			int nM = 0;
			// 预测评分
			List<Item> mPredictList = mUsersPredict.get(mUserid);
			List<Integer> mTestList = mTestItemList.get(mUserid);
			for (int i = 0; i < num; i++) {
				if (mTestList != null
						&& mTestList.contains(mPredictList.get(i).getID())) {
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
			mRecommedNum += num;

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
		 * */
		double precision = allNumTe / mRecommedNum;
		double recall = allNumTe / mTestNum;
		return precision + "/" + recall;
	}

}