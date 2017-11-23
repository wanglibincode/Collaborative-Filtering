package Utils;

import java.util.List;

public class EvaluateIndex {
	// 求平均绝对误差
	public double MAE_analyse(List<Double> x, List<Double> y) {
		int lenx = x.size();
		int leny = y.size();
		int len = lenx;// 小容错
		if (lenx < leny)
			len = lenx;
		else
			len = leny;
		// System.out.println(len);
		double[] tmpX = new double[len];
		double[] tmpY = new double[len];
		for (int i = 0; i < len; i++) {
			tmpX[i] = x.get(i);
			tmpY[i] = y.get(i);
		}
		return MAX(tmpX, tmpY);
		// System.out.println(tmpY[1]);
	}

	public double MAX(double[] tmpx, double[] tmpy) {
		double upSum = 0;
		int n = tmpx.length;
		for (int i = 0; i < tmpy.length; i++) {
			upSum += Math.abs(tmpy[i] - tmpx[i]);
		}
		return upSum / n;
	}

	// Chapter4:测试
	// 以u1.test的userID，itemID为输入，用以上运算再给出一组打分，与u1.test中进行比较
	// 部分测试已在main函数中做好，这里实现均方差公式RMSE
	// 它是观测值与真值偏差的平方和 与 观测次数n比值的平方根
	public double RMSE(double[] x, double[] y) {
		double rmse = 0;
		int lenx = x.length;
		int leny = y.length;
		int len = lenx;// 小容错
		if (lenx < leny)
			len = lenx;
		else
			len = leny;

		double diffSum = 0;
		double diffMutipl;
		for (int i = 0; i < len; i++) {
			diffMutipl = Math.pow((x[i] - y[i]), 2);
			diffSum += diffMutipl;
		}
		rmse = Math.sqrt(diffSum / len);
		// System.out.println(len);
		// System.out.println(diff);
		return rmse;
	}

	public double analyse(List<Double> x, List<Double> y) {
		int lenx = x.size();
		int leny = y.size();
		int len = lenx;// 小容错
		if (lenx < leny)
			len = lenx;
		else
			len = leny;
		// System.out.println(len);
		double[] tmpX = new double[len];
		double[] tmpY = new double[len];
		for (int i = 0; i < len; i++) {
			tmpX[i] = x.get(i);
			tmpY[i] = y.get(i);
		}
		return RMSE(tmpX, tmpY);
		// System.out.println(tmpY[1]);
	}

//	/**
//	 * 求准确率和召回率(目前是错的，应该先求出为每个用户的推荐列表)
//	 */
//	public double PrecisionAndRecall(String pathTestFile, String pathPredFile,
//			int N) {
//		Map<Integer, UserItermC> userTestItemCMap = getUserTestItemCMap(pathTestFile);
//		Map<Integer, UserItermC> userPredItemCMap = getUserPredItemCMap(pathPredFile);
//		double R = 0.0, T = 0.0, RT = 0.0;
//		// 准确率
//		Set<Integer> TestkeySet = userTestItemCMap.keySet();
//		Set<Integer> PredkeySet = userPredItemCMap.keySet();
//		Iterator<Integer> TestIterator = TestkeySet.iterator();
//		Iterator<Integer> PredIterator = PredkeySet.iterator();
//		while (TestIterator.hasNext()) {
//			int n = N;
//			int next = TestIterator.next();
//			ArrayList<Item> predSet;
//			ArrayList<Item> TestSet = userTestItemCMap.get(next).getSet();
//			if (PredkeySet.contains(next)) {
//				predSet = userPredItemCMap.get(next).getSet();
//				// 求R，训练集上得出的预测评分，推荐列表
//				R += N;
//				// 求T，测试集中用户的评分列表
//				T += TestSet.size();
//				if (predSet.size() < n && TestSet.size() < n) {
//					if (TestSet.size() < predSet.size()) {
//						n = TestSet.size();
//					} else {
//						n = predSet.size();
//					}
//				} else if (predSet.size() < n) {
//					n = predSet.size();
//				} else if (TestSet.size() < n) {
//					n = TestSet.size();
//				}
//				// 求交集
//				RT += getIntersection(TestSet, predSet, n);
//			} else {
//				continue;
//			}
//		}
//		System.out.println("推荐的准确率的结果为：" + RT);
//		System.out.println("推荐的准确率的结果为：" + RT / R);
//		System.out.println("推荐的回召率的结果为：" + RT / T);
//		return 0.0;
//	}
//
//	private int getIntersection(ArrayList<Item> TestSet,
//			ArrayList<Item> predSet, int N) {
//		int total = 0;
//		for (int i = 0; i < N; i++) {
//			int temp = TestSet.get(i).getID();
//			for (int j = 0; j < N; j++) {
//				if (temp == predSet.get(j).getID()) {
//					total++;
//				}
//			}
//		}
//		return total;
//	}

//	private Map<Integer, UserItermC> getUserTestItemCMap(String pathTestFile) {
//		Map<Integer, UserItermC> userItemCMap = new HashMap<Integer, UserItermC>();
//		// TODO Auto-generated method stub
//		File testFile = new File(pathTestFile);
//		try {
//			if (!testFile.exists() || testFile.isDirectory()) {
//				throw new FileNotFoundException();
//			}
//			BufferedReader reader = new BufferedReader(new FileReader(testFile));
//			String tmpToRead = "";
//			String[] part = new String[3];
//			while ((tmpToRead = reader.readLine()) != null) {
//				part = tmpToRead.split("\t");
//				if (part[0].equals("UserID")) {
//					continue;
//				}
//				int userID = Integer.parseInt(part[0]);
//				int itemID = Integer.parseInt(part[1]);
//				// System.out.println("userID--" + userID);
//				double originalRate = Double.parseDouble(part[2]);
//
//				Set<Integer> keySet = userItemCMap.keySet();
//
//				if (keySet == null || (keySet != null && keySet.isEmpty())) {
//					ArrayList<Item> itemCset = new ArrayList<Item>();
//					itemCset.add(new Item(itemID, originalRate));
//					Collections.sort(itemCset);
//					userItemCMap.put(userID, new UserItermC(userID, itemCset));
//				} else {
//					if (keySet.contains(userID)) {
//						UserItermC userItermC = userItemCMap.get(userID);
//						ArrayList<Item> set = userItermC.getSet();
//						set.add(new Item(itemID, originalRate));
//						Collections.sort(set);
//					} else {
//						ArrayList<Item> itemCset = new ArrayList<Item>();
//						itemCset.add(new Item(itemID, originalRate));
//						Collections.sort(itemCset);
//						userItemCMap.put(userID, new UserItermC(userID,
//								itemCset));
//					}
//				}
//			}
//			// for (int i = 1; i <= 462; i++) {
//			// if (userItemCMap.keySet().contains(i)) {
//			// // System.out.println("userItemCMap.get(i).getSet().size()--"
//			// // + i + "--" + userItemCMap.get(i).getSet().size());
//			// for (int j = 0; j < userItemCMap.get(i).getSet().size(); j++) {
//			// System.out.println(userItemCMap.get(i).getSet().get(j)
//			// .getID()
//			// + "--"
//			// + userItemCMap.get(i).getSet().get(j)
//			// .getValue());
//			// }
//			// }
//			//
//			// }
//			// System.out.println("userItemCMap.keySet().size()--"
//			// + userItemCMap.keySet().size());
//			reader.close();
//
//		} catch (FileNotFoundException e1) {
//			// TODO Auto-generated catch block
//			System.out.println("EvaluateIndex----FileNotFoundException");
//			e1.printStackTrace();
//		} catch (IOException e) {
//			System.out.println("EvaluateIndex----IOException");
//			e.printStackTrace();
//		}
//		return userItemCMap;
//	}

//	private Map<Integer, UserItermC> getUserPredItemCMap(String pathTestFile) {
//		Map<Integer, UserItermC> userItemCMap = new HashMap<Integer, UserItermC>();
//		// TODO Auto-generated method stub
//		File testFile = new File(pathTestFile);
//		try {
//			if (!testFile.exists() || testFile.isDirectory()) {
//				throw new FileNotFoundException();
//			}
//			BufferedReader reader = new BufferedReader(new FileReader(testFile));
//			String tmpToRead = "";
//			String[] part = new String[4];
//			while ((tmpToRead = reader.readLine()) != null) {
//				part = tmpToRead.split("\t");
//				if (part[0].equals("UserID")) {
//					continue;
//				}
//				int userID = Integer.parseInt(part[0]);
//				int itemID = Integer.parseInt(part[1]);
//				// System.out.println("userID--" + userID);
//				double originalRate = Double.parseDouble(part[3]);
//
//				Set<Integer> keySet = userItemCMap.keySet();
//
//				if (keySet == null || (keySet != null && keySet.isEmpty())) {
//					ArrayList<Item> itemCset = new ArrayList<Item>();
//					itemCset.add(new Item(itemID, originalRate));
//					Collections.sort(itemCset);
//					userItemCMap.put(userID, new UserItermC(userID, itemCset));
//				} else {
//					if (keySet.contains(userID)) {
//						UserItermC userItermC = userItemCMap.get(userID);
//						ArrayList<Item> set = userItermC.getSet();
//						set.add(new Item(itemID, originalRate));
//						Collections.sort(set);
//					} else {
//						ArrayList<Item> itemCset = new ArrayList<Item>();
//						itemCset.add(new Item(itemID, originalRate));
//						Collections.sort(itemCset);
//						userItemCMap.put(userID, new UserItermC(userID,
//								itemCset));
//					}
//				}
//			}
//			// for (int i = 1; i <= 462; i++) {
//			// if (userItemCMap.keySet().contains(i)) {
//			// System.out.println("userItemCMap.get(i).getSet().size()--"
//			// + i + "--" + userItemCMap.get(i).getSet().size());
//			// for (int j = 0; j < userItemCMap.get(i).getSet().size(); j++) {
//			// System.out.println(userItemCMap.get(i).getSet().get(j)
//			// .getID()
//			// + "--"
//			// + userItemCMap.get(i).getSet().get(j)
//			// .getValue());
//			// }
//			// }
//			//
//			// }
//			// System.out.println("userItemCMap.keySet().size()--"
//			// + userItemCMap.keySet().size());
//			reader.close();
//
//		} catch (FileNotFoundException e1) {
//			// TODO Auto-generated catch block
//			System.out.println("EvaluateIndex----FileNotFoundException");
//			e1.printStackTrace();
//		} catch (IOException e) {
//			System.out.println("EvaluateIndex----IOException");
//			e.printStackTrace();
//		}
//		return userItemCMap;
//	}
}
