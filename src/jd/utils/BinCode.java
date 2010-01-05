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

import jd.controlling.JDLogger;

public final class BinCode {
    /**
     * Don't let anyone instantiate this class.
     */
    private BinCode() {
    }

    private static String addZero(final String bin, final int minCount) {
        final StringBuilder ret = new StringBuilder(bin == null ? "" : bin);
        for (int j = ret.length(); j < minCount; j++) {
            ret.insert(0, '0');
        }
        return ret.toString();
    }

    private static String[] binArrayToCodeArray(final String[] binArray) {
        final int length = binArray.length;
        final String[] codeArray = new String[length];
        for (int i = 0; i < length; i++) {
            codeArray[i] = BinCode.prBinToCode(binArray[i]);
        }
        return codeArray;
    }

    public static String binToCode(final String bin) {
        try {
            final String[] sts = bin.split("\\|");
            final String[] codeArray = BinCode.binArrayToCodeArray(sts);
            final int minCount = sts[0].length();
            final StringBuilder ret = new StringBuilder();
            ret.append(minCount);
            final int stsLength = sts.length;
            for (int i = 0; i < stsLength; i++) {
                ret.append("|" + codeArray[i]);
            }
            return ret.toString();
        } catch (Exception e) {
            JDLogger.exception(e);
        }
        return null;
    }

    private static String[] codeArrayToBinArray(final String[] codeArray) {
        final int length = codeArray.length;
        final String[] binArray = new String[length - 1];
        final int minCount = Integer.parseInt(codeArray[0]);
        for (int i = 1; i < length; i++) {
            binArray[i - 1] = BinCode.addZero(BinCode.prCodeToBin(codeArray[i]), minCount);
        }
        return binArray;
    }

    public static String codeToString(final String code) {
        try {
            final String[] binArray = BinCode.codeToStringArray(code);
            final StringBuilder ret = new StringBuilder();
            boolean last = false;
            for (String element : binArray) {
                ret.append((last ? "|" : "") + element);
                last = true;
            }
            return ret.toString();
        } catch (Exception e) {
            JDLogger.exception(e);
        }
        return null;
    }

    public static String[] codeToStringArray(final String code) {
        return BinCode.codeArrayToBinArray(code.split("\\|"));
    }

    private static String prBinToCode(final String bin) {
        return Integer.toString(Integer.parseInt(bin, 2), 36);
    }

    private static String prCodeToBin(final String Code) {
        return Integer.toBinaryString(Integer.parseInt(Code, 36));
    }

}
