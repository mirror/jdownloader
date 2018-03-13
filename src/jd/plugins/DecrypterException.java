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

import org.jdownloader.translate._JDT;

public class DecrypterException extends Exception {
    private static final long serialVersionUID = -413359039728789194L;
    public static String      PASSWORD         = _JDT.T.decrypter_wrongpassword();
    public static String      ACCOUNT          = _JDT.T.decrypter_invalidaccount();

    public DecrypterException() {
        super(_JDT.T.decrypter_unknownerror());
    }

    public DecrypterException(String message, Throwable cause) {
        super(message, cause);
    }

    public DecrypterException(String errorMessage) {
        super(errorMessage);
    }
}