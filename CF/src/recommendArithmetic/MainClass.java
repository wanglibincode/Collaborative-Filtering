package recommendArithmetic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import Utils.Neighbor;
import Utils.UserScore;

public class MainClass {
	public static void main(String args[]) {
		// // UCF
		// int[] mUN_Array = new int[] { 40, 80, 160 };
		// // for (int i = 0; i < 3; i++) {
		// // UserCF useCf = new UserCF(160,10,943,1682);
//		 UserCF useCf = new UserCF(5, 10, 943, 1682);
//		 String result = useCf.getCoefficient("F:/wlb/论文/实验/data/ml-100k/",
//		 "u1.base", "u1.test");
//		 System.out.println("K=" + 80 + "----" + result);
		// //
		// RecomResourceForUsers recomResourceForUsers = new
		// RecomResourceForUsers(
		// "F:/wlb/论文/实验/data/ml-100k/u1.base");
//		RecomResourceByUserCF erForUsers = new RecomResourceByUserCF("F:/wlb/论文/实验/data/ml-100k/u5.base",
//				"F:/wlb/论文/实验/data/ml-100k/u5.test", 5, 10);
		//// RecomResourceByUserCF erForUsers = new
		// RecomResourceByUserCF("E:/毕业/实验/ml-100k/u1.base",
		//// "E:/毕业/实验/ml-100k/u1.test",40, 10);
		//// // recomResourceForUsers.getRecomResultForUsers(10);
		//// // List<Item> list = new ArrayList<Item>();
		//// // list.add(new Item(1, 1, 2));
//		System.out.println(erForUsers.getRecomResultForUsers());
		// int[] mUN_Array = new int[] { 5, 10, 20, 40, 80, 160 };
		// for (int i = 0; i < mUN_Array.length; i++) {
		RecomResourceByItemCF erForUsers = new RecomResourceByItemCF("F:/wlb/论文/实验/data/ml-100k/u5.base",
				"F:/wlb/论文/实验/data/ml-100k/u5.test",160, 10);
		System.out.println(erForUsers.getRecomResultForUsers());
		// }
	}
}
