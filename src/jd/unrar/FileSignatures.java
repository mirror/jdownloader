//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.unrar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileSignatures {
	/**
	 * Objektarray in dem alle Filesignaturen eingetragen sind
	 */
	
	public final static Integer[] SIGNATURE_7Z = new Integer[] { 55, 122, 0, 0,
			39, 28 };
	public final static Integer[] SIGNATURE_AVI = new Integer[] { 82, 73, 70,
			70 };
	public final static Integer[] SIGNATURE_BZ2 = new Integer[] { 66, 90, 104,
			54, 49, 65 };
	/**
	 * Documents
	 */
	public final static Integer[] SIGNATURE_DOC = new Integer[] { 208, 207, 17,
			224, 161, 177 };
	/**
	 * Binary
	 */
	public final static Integer[] SIGNATURE_EXE = new Integer[] { 77, 90, 144,
			0, 3, 0 };
	public final static Integer[] SIGNATURE_GZ = new Integer[] { 31, 139, 8, 0 };
	/**
	 * Media
	 */
	public final static Integer[][] SIGNATURE_JPG = new Integer[][] {
			{ 255, 216, 255, 224, 0, 16 }, { 255, 216, 255, 225, 39, 222 } };
	public final static Integer[] SIGNATURE_M4A = new Integer[] { 0, 0, 0, 32,
			102, 116 };
	public final static Integer[] SIGNATURE_MDF = { 0, 255, 255, 255, 255, 255 };
	public final static Integer[] SIGNATURE_MKV = new Integer[] { 26, 69, 2019,
			0, 66, 0 };
	public final static Integer[][] SIGNATURE_MP3 = new Integer[][] {
			{ 73, 68, 51, 4, 0 }, { 0, 0, 64, -1, -1, 0 },
			{ 0, 0, 100, 0, 0, 0 }, { 0, 0, 68, -1, -1, 0 },
			{ 73, 68, 51, 3, 0, -1 }, { 255, 251, 104, -1, 0, -1 },
			{ 255, 251, 64, -1, 0, -1 } };
	public final static Integer[] SIGNATURE_MP4 = new Integer[] { 0, 0, 0, -1,
			102, 116 };
	public final static Integer[][] SIGNATURE_MPEG = new Integer[][] {
			{ 0, 0, 1, 186, -1, 0 }, { 0, 0, 1, 0, 33, 0 } };
	public final static Integer[] SIGNATURE_NRG = new Integer[] { 0, 0, 0, 0,
			0, 0 };
	public final static Integer[] SIGNATURE_PDF = new Integer[] { 37, 80, 68,
			70, 45, 49 };
	/**
	 * Archive
	 */
	public final static Integer[] SIGNATURE_RAR = new Integer[] { 82, 97, 114,
			33, 26, 7 };
	public final static Integer[] SIGNATURE_WMA = new Integer[] { 48, 38, 178,
			117, 142, 102 };
	public final static Integer[] SIGNATURE_WMV = new Integer[] { 48, 38, -1,
			117, -1, 102 };
	public final static Integer[] SIGNATURE_XCF = new Integer[] { 103, 105,
			109, 112, 32, 120 };
	public final static Integer[] SIGNATURE_ZIP = new Integer[] { 80, 75, 3, 4,
			20, 0 };
	
	
	public final static Object[][] filesignatures = {
        { "avi", new Integer[][] { SIGNATURE_AVI } },
        { "divx", new Integer[][] { SIGNATURE_AVI } },
        { "mpg", SIGNATURE_MPEG }, { "mpeg", SIGNATURE_MPEG },
        { "rar", new Integer[][] { SIGNATURE_RAR } },
        { "wmv", new Integer[][] { SIGNATURE_WMV } },
        { "mp3", SIGNATURE_MP3 },
        { "exe", new Integer[][] { SIGNATURE_EXE } },
        { "bz2", new Integer[][] { SIGNATURE_BZ2 } },
        { "gz", new Integer[][] { SIGNATURE_GZ } },
        { "doc", new Integer[][] { SIGNATURE_DOC } },
        { "pdf", new Integer[][] { SIGNATURE_PDF } },
        { "nrg", new Integer[][] { SIGNATURE_NRG } },
        { "wma", new Integer[][] { SIGNATURE_WMA } },
        { "jpg", SIGNATURE_JPG },
        { "m4a", new Integer[][] { SIGNATURE_M4A } },
        { "mdf", new Integer[][] { SIGNATURE_MDF } },
        { "mp4", new Integer[][] { SIGNATURE_MP4 } },
        { "mkv", new Integer[][] { SIGNATURE_MKV } },
        { "xcf", new Integer[][] { SIGNATURE_XCF } },
        { "zip", new Integer[][] { SIGNATURE_ZIP } },
        { "7z", new Integer[][] { SIGNATURE_7Z } } };
	/**
	 * Scheint manchmal 0 und manchmal 65533 zu sein
	 * 
	 * @param a
	 * @param i
	 * @return
	 */
	private static boolean checkZero(int a, int i) {
		if (a == 0 && (i == 0 || i == 65533))
			return true;
		return false;

	}

	/**
	 * prüft ob zwei Filesignaturen vom gleichen typ sind
	 * @param sig
	 * @param sig2
	 * @return
	 */
	public static boolean fileSignatureEquals(Integer[] sig, Integer[] sig2) {
		if (sig.length != sig2.length)
			return false;
		for (int i = 0; i < sig2.length; i++) {
			if (sig[i] != -1 && sig2[i] != -1 && !sig[i].equals(sig2[i])
					&& !checkZero(sig[i], sig2[i]))
				return false;
		}
		return true;
	}
	/**
	 * prüft ob eine der in param 1 angegebenen Filesignaturen mit der Filesignatur aus param 2 übereinstimmt
	 * @param sigs
	 * @param sig2
	 * @return
	 */
	public static boolean fileSignatureEquals(Integer[][] sigs, Integer[] sig2) {
		for (int i = 0; i < sigs.length; i++) {
			if(fileSignatureEquals(sigs[i], sig2))
				return true;
		}
		return false;
	}
	/**
	 * gibt die Dateisignatur einer Datei aus
	 * @param file
	 * @return
	 */
	public static Integer[] getFileSignature(File file) {
		int ad;
		FileInputStream fin;
		try {
			fin = new FileInputStream(file);
			int c = 0;
			BufferedReader myInput = new BufferedReader(new InputStreamReader(
					fin));
			Integer[] ret = new Integer[6];
			while ((ad = myInput.read()) != -1) {

				ret[c] = ad;

				if (c++ == 5)
					break;
			}
			return ret;
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) {
		File[] file = new File("/home/dwd/.jd_home").listFiles();
		for (int i = 0; i < file.length; i++) {
			if (file[i].isFile()) {
				try {
					Integer[] ret = getFileSignature(file[i]);
					System.out.println("\n" + file[i].getName() + " ");

					System.out.print("{");
					System.out.print(ret[0]);
					for (int j = 1; j < ret.length; j++) {
						System.out.print("," + ret[j]);
					}
					System.out.println("}");
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}

	}

}
