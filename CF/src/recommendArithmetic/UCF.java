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
 * �޸ĺ��Эͬ�����Ƽ��㷨������֪ʶ������Эͬ�����Ƽ��㷨
 * */
public class UCF {
	public static final int USERSIZE = 943;
	public static final int ITEMSIZE = 1682;
	public int UN = 10;// ĳһuser������ھ���

	public int[] num = new int[USERSIZE + 1];// ÿ���û�Ϊ�������˷�
	public int[] Itemnum = new int[ITEMSIZE + 1];// ÿ��Item�������û����˷�
	public double[] average = new double[USERSIZE + 1];// ÿ��user��ƽ�����
	public double[] ItemAverage = new double[ITEMSIZE + 1];// ÿ��item��ƽ�����
	public double[][] rate = new double[USERSIZE + 1][ITEMSIZE + 1];// ���־���
	public double[][] DealedOfRate = new double[USERSIZE + 1][ITEMSIZE + 1];// ���ϡ�����⴦�������־���
	Neighbor[][] NofUser;

	List<Double> x = new LinkedList<Double>();
	List<Double> y = new LinkedList<Double>();
	private Map<Integer, List<Item>> mUsersPredict;
	private String precisionRecall;

	public UCF(int UN) {
		this.UN = UN;
		NofUser = new Neighbor[USERSIZE + 1][this.UN + 1];// ÿ���û��������UN���ھ�
	}

	public String getCoefficient(String path, String baseDataFile,
			String testDataFile) {
		double RMSE = 0.0;
		double MAE = 0.0;
		if (readFile(path + baseDataFile)) {
			System.out.println("��ȴ������ڷ���");
			getAvr();// �õ�average[]
			dealRate();// �õ�DealedOfRate
			getNofUser();// �õ�NofUser
			BufferedReader reader = null;
			FileWriter writer = null;
			// ���ļ�
			try {
				File inputFile = new File(path + testDataFile);
				if (!inputFile.exists() || inputFile.isDirectory())
					throw new FileNotFoundException();
				reader = new BufferedReader(new FileReader(inputFile));

				// д�ļ�
				File outputFile = new File(
						"F:/wlb/����/ʵ��/data/testResultByUCF.txt");
				if (!outputFile.exists()) {
					if (!outputFile.createNewFile()) {
						System.out.println("����ļ�����ʧ��");
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
				// �õ�RMSE��MAX
				RMSE = evaluate.analyse(x, y);
				MAE = evaluate.MAE_analyse(x, y);
				// �õ�Ϊÿ���û��Ƽ�����Դ�б�
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

	// 1-3�������־����ϡ�����⣨��Ҫ���������
	// �ص㴦���user��û�б����ֵ�item����򼸷�
	// ��ʱ��1-2�м������ƽ����
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

	// Chapter2�����࣬�Һ�ĳһ�û�����ͬϲ�õ�һ���û�
	// 2-1��:Pearson�������������ƶ�
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
	 * ��ÿ���û�����Ŀ���ֵ�ƽ����
	 */
	public void getAvr() {
		getLen();
		int i, j;
		for (i = 1; i <= USERSIZE; i++) {
			double sum = 0.0;
			for (j = 1; j < rate[i].length; j++) {// ÿ��length����ITEMSIZE=1682
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
	 * ÿ���û���������Ŀ����
	 */
	public void getLen() {// ����ÿ���û�Ϊ������Ӱ���
		for (int i = 1; i <= USERSIZE; i++) {
			int n = 0;
			for (int j = 1; j <= ITEMSIZE; j++) {
				if (rate[i][j] != 0)
					n++;
			}
			num[i] = n;
		}

	}

	// Chapter1:׼������
	// 1-1:��ȡ�ļ����ݣ��õ����־��� 1:��ȡ�ɹ� -1����ȡʧ��
	public boolean readFile(String filePath) {
		File inputFile = new File(filePath);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(inputFile));
		} catch (FileNotFoundException e) {
			System.out.println("�ļ�������" + e.getMessage());
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
				// �������
				rate[userID][itemID] = Rate;
			}
			reader.close();
		} catch (NumberFormatException | IOException e) {
			System.out.println("���ļ���������" + e.getMessage());
			return false;
		}
		return true;
	}

	// Chapter3:��������ھӸ���Ԥ������
	public double predict(int userID, int itemID) {// �����userIDΪ�û����룬��1��Ϊ�����±꣡
		double sum1 = 0;
		double sum2 = 0;
		for (int i = 1; i <= UN; i++) {// �������UN���ھӽ��д���
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

	// ��������ھӸ���Ԥ�����ֵڶ��ַ���
	public double predict2(int userID, int itemID) {// �����userIDΪ�û����룬��1��Ϊ�����±꣡
		double sum1 = 0;
		for (int i = 1; i <= UN; i++) {// �������UN���ھӽ��д���
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

	// 2-2��Pearson�㷨������user�Ľ����ϣ���NofUser����
	@SuppressWarnings("unchecked")
	public void getNofUser() {
		int id, userID;
		for (userID = 1; userID <= USERSIZE; userID++) {
			// ��һ�����⣬����������ͬ���û�����Ŀ���û�����ͬ���ƶ�ʱ��setĬ�ϻᱣ��һ��
			// Set<Neighbor> neighborList = new TreeSet();// �Ὣѹ���Neighbor�ź�����
			ArrayList<Neighbor> neighborList = new ArrayList<Neighbor>();// �Ὣѹ���Neighbor�ź�����
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
		 * �����ҳ�DealedOfRate/rate��UseID��id�����ֵ���Ŀ������Ч
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
		// �����û�û�й�ͬ������Ŀ
		if (j == 0) {
			return 0;
		}
		int len = x1.length;// ��ͬ������Ŀ�ĸ���

		// System.out.println(len + "----len-count----" + count);
		double result = simMethod(UseID, id, len, x1, y1, x, y);
		// double result2 = simPerMethod(UseID, id, len, x1, y1);
		// System.out.println(result + "----��������-Ƥ��ѷ----" + result2);

		return result;

	}

	private double simMethod(int UseID, int id, int len, double[] x1,
			double[] y1, double[] x, double[] y) {
		double upside = 0.0;
		double downside_x = 0.0;
		double downside_y = 0.0;
		// �������ƶȵķ���
		for (int i = 0; i < len; i++) {
			upside += (x1[i] - average[UseID]) * (y1[i] - average[id]);
		}
		// �������ƶȵķ�ĸ,Ŀ���û�userid
		for (int xi = 1; xi < x.length; xi++) {
			if (x[xi] != 0) {
				downside_x += Math.pow((x[xi] - average[UseID]), 2);
			}
		}
		// �������ƶȵķ�ĸ,�����û�id
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
	 * �õ�ÿ���û���δ������Դ�����֣�mUsersPredict
	 * */
	@SuppressWarnings("unchecked")
	public void getRecommendForUsers(String path, String baseDataFile) {
		BufferedReader reader = null;
		/**
		 * ��ÿ���û�����Щ��Դ�Ѿ�����
		 * */
		Map<Integer, List<Integer>> mUsersAready = new HashMap<Integer, List<Integer>>();
		// ���ļ�
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
		System.out.println("mUsersAready :�û��Ѿ����ֵ���Դ over"
				+ mUsersAready.get(1).get(0));
		/**
		 * ��ÿ���û��Ķ�δ������Ŀ��Ԥ������
		 * */
		mUsersPredict = new HashMap<Integer, List<Item>>();
		for (int i = 1; i < USERSIZE + 1; i++) {
			List<Item> mPredictItem = new ArrayList<Item>();
			for (int j = 1; j < ITEMSIZE + 1; j++) {
				// ���mUserArray��û�е�ǰ�û�����˵����ǰ�û�û�ж��κ���Դ�����֣�������
				if (mUsersAready.containsKey(i)) {
					List<Integer> mAreadyItem = mUsersAready.get(i);
					// �����ǰ�û��Ե�ǰ��Դû�����֣���Ԥ�⣬�����뵽mPredictItem��
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
		System.out.println("mUsersPredict: �û���δ������Դ��Ԥ��  over");
		/**
		 * ��ÿ���û��Ķ���Ŀ��Ԥ�����֣�������ĵ�
		 * */
		try {
			File outputFile = new File("F:/wlb/����/ʵ��/data/PredictForUsers.txt");
			if (!outputFile.exists()) {
				if (!outputFile.createNewFile()) {
					System.out.println("����ļ�����ʧ��");
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
			System.out.println("PredictForUsers���û���δ������Դ��Ԥ����������ĵ�   Over");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public String getPrecisionAndRecall(int num, String path,
			String testDataFile) {

		/**
		 * �õ�users�Ĳ��Լ��б��еĸ���
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
			System.out.println("getPrecisionAndRecall+��ǰ�ĵ�������");
			e.printStackTrace();
		} catch (NumberFormatException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("mTestItemList:���Լ� over"
				+ mTestItemList.get(458).size());
		/**
		 * �õ�users���Ƽ��б�Ͳ����б��еĹ�ͬ����
		 * */
		Map<Integer, Integer> mPreAndTestItemList = new HashMap<Integer, Integer>();
		Set<Integer> keySet = mUsersPredict.keySet();
		Iterator<Integer> iterator = keySet.iterator();
		while (iterator.hasNext()) {
			int mUserid = iterator.next();
			int nM = 0;
			// Ԥ������
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
		 * ����׼ȷ��
		 * */
		double precision = allNumTe / mRecommedNum;
		double recall = allNumTe / mTestNum;
		return precision + "/" + recall;
	}

}