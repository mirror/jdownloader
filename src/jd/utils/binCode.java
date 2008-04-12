//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.utils;



public class binCode {
	/**
	 * @param args
	 */
	private static String prBinToCode(String bin) {
		return Integer.toString(Integer.parseInt(bin, 2), 36);
	}

	private static String prCodeToBin(String Code) {
		return Integer.toBinaryString(Integer.parseInt(Code, 36));
	}

	private static String addZero(String bin, int minCount) {
		for (int j = bin.length(); j < minCount; j++) {
			bin = "0" + bin;
		}
		return bin;
	}

	private static String[] codeArrayToBinArray(String[] codeArray) {
		String[] binArray = new String[codeArray.length-1];
		int minCount = Integer.parseInt(codeArray[0]);
		for (int i = 1; i < codeArray.length; i++) {
			binArray[i-1] = addZero(prCodeToBin(codeArray[i]), minCount);
		}
		return binArray;
	}

	private static String[] binArrayToCodeArray(String[] binArray) {
		String[] codeArray = new String[binArray.length];
		for (int i = 0; i < binArray.length; i++) {
			codeArray[i] = prBinToCode(binArray[i]);
		}
		return codeArray;
	}

	public static void main(String[] args) {

		String bin = "1111111110001111111|1111110010000000011|1111110000000000001|1110000000000000001|1110000100000000000|1110000011111000001|1100000011111110000|1100000111111110001|1100000111111110001|1100000111111100000|1100000001110000000|1111000001100000000|1111000000000000011|1110000000000000011|1000000000000000001|1000000011100000001|1000000011111000001|0000001111111000110|0000011111111110110|0000001111111100000|1000000111111100000|1100000111110000000|1000000000110000000|1000000000000000001|1111000000000000111|1111000000000001111|1111100000011111111";
		String code = binToCode(bin);
		System.out.println(code);
		System.out.println(codeToString(code));
		System.out.println(bin);

	}

	public static String binToCode(String bin) {
		try {
			String[] sts = bin.split("\\|");
			String[] codeArray = binArrayToCodeArray(sts);
			int minCount = sts[0].length();
			StringBuffer ret = new StringBuffer();
			ret.append(minCount);
			for (int i = 0; i < sts.length; i++) {
				ret.append("|" + codeArray[i]);
			}
			return ret.toString();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}
	public static String codeToString(String code)
	{
		try {
			String[] binArray = codeToStringArray(code);
			StringBuffer ret = new StringBuffer();
			boolean last = false;
			for (int i = 0; i < binArray.length; i++) {
				ret.append(((last)?"|":"") + binArray[i]);
				last=true;
			}
			return ret.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public static String[] codeToStringArray(String code)
	{
		return codeArrayToBinArray(code.split("\\|"));
	}

}
