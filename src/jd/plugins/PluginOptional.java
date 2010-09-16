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

import java.util.regex.Pattern;

import jd.OptionalPluginWrapper;
import jd.PluginWrapper;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.event.ControlEvent;
import jd.event.ControlListener;

public abstract class PluginOptional extends Plugin implements ControlListener {

    public static final int ADDON_INTERFACE_VERSION = 7;

    /**
     * is the optional plugin running
     */
    private boolean         running                 = false;

    public PluginOptional(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public final void controlEvent(final ControlEvent event) {
        if (this.isRunning()) {
            this.onControlEvent(event);

            // Deaktiviert das Plugin beim Beenden
            if (event.getEventID() == ControlEvent.CONTROL_SYSTEM_EXIT) {
                final String id = JDController.requestDelayExit(this.getWrapper().getID());
                try {
                    this.stopAddon();
                } catch (final Exception e) {
                    JDLogger.exception(e);
                }
                JDController.releaseDelayExit(id);
            }
        }
    }

    @Override
    public String getHost() {
        return this.getWrapper().getHost();
    }

    public String getIconKey() {
        return "gui.images.config.home";
    }

    /**
     * Should ALWAYS return a lifetime id String.
     * 
     * @return
     */
    public String getID() {
        return this.getHost();
    }

    @Override
    public Pattern getSupportedLinks() {
        return null;
    }

    @Override
    public long getVersion() {
        return this.getWrapper().getVersion();
    }

    @Override
    public OptionalPluginWrapper getWrapper() {
        return (OptionalPluginWrapper) super.getWrapper();
    }

    public abstract boolean initAddon();

    public Object interact(final String command, final Object parameter) {
        return null;
    }

    public boolean isRunning() {
        return this.running;
    }

    /**
     * Overwrite this, when your addon should react on ControlEvents
     */
    public void onControlEvent(final ControlEvent event) {
    }

    public abstract void onExit();

    /**
     * should be overridden by addons with gui
     * 
     * @param b
     */
    public void setGuiEnable(final boolean b) {
    }

    public boolean startAddon() {
        if (this.isRunning()) { return true; }

        this.running = this.initAddon();

        /*
         * if addon was inited successfully, add it as a ControlListener
         */
        if (this.running) {
            JDController.getInstance().addControlListener(this);
        }
        return this.running;
    }

    public void stopAddon() {
        if (!this.isRunning()) { return; }
        this.onExit();

        /*
         * if addon is running, remove it from the ControlListener list
         */
        JDController.getInstance().removeControlListener(this);
        this.running = false;
    }
}
