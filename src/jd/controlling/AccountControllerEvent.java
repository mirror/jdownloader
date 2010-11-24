//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.controlling;

import jd.plugins.Account;

import org.appwork.utils.event.DefaultIntEvent;

public class AccountControllerEvent extends DefaultIntEvent {

    private final Account account;
    private final String  host;

    public AccountControllerEvent(final Object source, final int ID, final String host, final Account account) {
        super(source, ID);
        this.account = account;
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public Account getAccount() {
        return account;
    }

    public static final byte ACCOUNT_ADDED   = 10;
    public static final byte ACCOUNT_REMOVED = 11;

    public static final byte ACCOUNT_UPDATE  = 20;

    public static final byte ACCOUNT_INVALID = 30;
    public static final byte ACCOUNT_EXPIRED = 31;

}
