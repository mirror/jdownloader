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

import jd.config.Property;

public class Account extends Property {

    private static final long serialVersionUID = -7578649066389032068L;
    private String user;
    private String pass;

    private boolean enabled = true;
    private String status = null;
    private transient boolean tempDisabled = false;

    public Account(String user, String pass) {
        this.user = user;
        this.pass = pass;

        if (this.user != null) this.user = this.user.trim();
        if (this.pass != null) this.pass = this.pass.trim();
    }

    public String getPass() {
        if (pass != null) return pass.trim();
        return null;
    }

    public String getStatus() {
        return status;
    }

    public String getUser() {
        if (user != null) return user.trim();
        return null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isTempDisabled() {
        return tempDisabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setPass(String pass) {
        this.pass = pass;
        if (this.pass != null) this.pass = this.pass.trim();
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTempDisabled(boolean tempDisabled) {
        this.tempDisabled = tempDisabled;
    }

    public void setUser(String user) {
        this.user = user;
        if (this.user != null) this.user = this.user.trim();
    }
}
