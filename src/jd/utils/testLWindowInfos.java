package jd.utils;

import jd.utils.LWindowInfos.WindowInformations;

public class testLWindowInfos {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		WindowInformations[] infos = new LWindowInfos().getWindowInformations();
		for (int i = 0; i < infos.length; i++) {
			System.out.println(infos[i].toString());
		}

	}

}
