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

package jd.plugins;

import jd.utils.locale.JDL;

public class DecrypterException extends Exception {

    private static final long serialVersionUID = -413359039728789194L;
    private String errorMessage = null;

    public static String CAPTCHA = JDL.L("decrypter.wrongcaptcha", "Wrong captcha code");
    public static String PASSWORD = JDL.L("decrypter.wrongpassword", "Wrong password");
    public static String ACCOUNT = JDL.L("decrypter.invalidaccount", "No valid account found");

    public DecrypterException() {
        this.errorMessage = JDL.L("decrypter.unknownerror", "Unknown error");
    }

    public DecrypterException(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
