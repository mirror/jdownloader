package jd.captcha;

import jd.captcha.utils.Utilities;

public class ShowLetterDB {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
        String hoster = "canna.to";
        JAntiCaptcha jac = new JAntiCaptcha(Utilities.getMethodDir(), hoster);
        jac.displayLibrary();
	}

}
