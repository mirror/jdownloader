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

import org.appwork.utils.event.SimpleEvent;

public class AccountControllerEvent extends SimpleEvent<AccountController, Account, AccountControllerEvent.Types> {

    public AccountControllerEvent(AccountController caller, Types type, Account... parameters) {
        super(caller, type, parameters);

    }

    public static enum Types {
        ADDED,
        REMOVED,
        UPDATE,
        EXPIRED,
        INVALID
    }

}
