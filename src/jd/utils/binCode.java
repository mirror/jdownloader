//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.utils;

public class binCode {
    private static String addZero(String bin, int minCount) {
        for (int j = bin.length(); j < minCount; j++) {
            bin = "0" + bin;
        }
        return bin;
    }

    private static String[] binArrayToCodeArray(String[] binArray) {
        String[] codeArray = new String[binArray.length];
        for (int i = 0; i < binArray.length; i++) {
            codeArray[i] = binCode.prBinToCode(binArray[i]);
        }
        return codeArray;
    }

    public static String binToCode(String bin) {
        try {
            String[] sts = bin.split("\\|");
            String[] codeArray = binCode.binArrayToCodeArray(sts);
            int minCount = sts[0].length();
            StringBuilder ret = new StringBuilder();
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

    private static String[] codeArrayToBinArray(String[] codeArray) {
        String[] binArray = new String[codeArray.length - 1];
        int minCount = Integer.parseInt(codeArray[0]);
        for (int i = 1; i < codeArray.length; i++) {
            binArray[i - 1] = binCode.addZero(binCode.prCodeToBin(codeArray[i]), minCount);
        }
        return binArray;
    }

    public static String codeToString(String code) {
        try {
            String[] binArray = binCode.codeToStringArray(code);
            StringBuilder ret = new StringBuilder();
            boolean last = false;
            for (String element : binArray) {
                ret.append((last ? "|" : "") + element);
                last = true;
            }
            return ret.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String[] codeToStringArray(String code) {
        return binCode.codeArrayToBinArray(code.split("\\|"));
    }

    private static String prBinToCode(String bin) {
        return Integer.toString(Integer.parseInt(bin, 2), 36);
    }

    private static String prCodeToBin(String Code) {
        return Integer.toBinaryString(Integer.parseInt(Code, 36));
    }

}
