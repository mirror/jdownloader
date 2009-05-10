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
import jd.controlling.AccountManager;

public class Account extends Property {

    private static final long serialVersionUID = -7578649066389032068L;
    private String user;
    private String pass;

    private boolean enabled = true;
    private String status = null;
    private int tmpDisabledInterval;

    private transient boolean tempDisabled = false;
    private transient long tmpDisabledTime = 0;
    private transient boolean fireChanges = true;

    public Account(String user, String pass) {
        this.user = user;
        this.pass = pass;
        this.setTmpDisabledInterval(10 * 60 * 1000);
        if (this.user != null) this.user = this.user.trim();
        if (this.pass != null) this.pass = this.pass.trim();
    }

    public void setfireChanges(boolean b) {
        fireChanges = b;
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
        if (tempDisabled && (System.currentTimeMillis() - tmpDisabledTime) > this.getTmpDisabledInterval()) tempDisabled = false;
        return tempDisabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        System.out.println(this + ": " + enabled);
        this.enabled = enabled;
        fireChange();
    }

    public void setPass(String pass) {
        if (this.pass == pass) return;
        if (pass != null) pass = pass.trim();
        if (this.pass != null && this.pass.equals(pass)) return;

        this.pass = pass;

        setProperty(AccountInfo.PARAM_INSTANCE, null);
        fireChange();
    }

    public void setStatus(String status) {
        if (this.status == status) return;
        if (this.status != null && this.status.equals(status)) return;
        this.status = status;
        fireChange();
    }

    public void setTempDisabled(boolean tempDisabled) {
        this.tmpDisabledTime = System.currentTimeMillis();
        this.tempDisabled = tempDisabled;
        fireChange();
    }

    public void setUser(String user) {

        if (this.user == user) return;
        if (user != null) user = user.trim();
        if (this.user != null && this.user.equals(user)) return;
        this.user = user;
        setProperty(AccountInfo.PARAM_INSTANCE, null);
        fireChange();

    }

    private void fireChange() {
        if (fireChanges) AccountManager.getInstance().fireChange(this);
    }

    // @Override
    public String toString() {
        return user + ":" + pass + " " + status + " " + enabled + " " + super.toString();
    }

    /**
     * returns how lon a premiumaccount will stay disabled if he got temporary
     * disabled
     * 
     * @return
     */
    public int getTmpDisabledInterval() {
        return tmpDisabledInterval;
    }

    public void setTmpDisabledInterval(int tmpDisabledInterval) {
        this.tmpDisabledInterval = tmpDisabledInterval;
    }
}
