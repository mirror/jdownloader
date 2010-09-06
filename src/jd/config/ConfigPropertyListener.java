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

package jd.config;

import jd.event.ControlEvent;
import jd.event.ControlListener;

public abstract class ConfigPropertyListener implements ControlListener {
    private final String[] list;
    private boolean strict;

    public ConfigPropertyListener(final String... args) {
        super();
        list = args;
        strict = true;
    }

    public boolean isStrict() {
        return strict;
    }

    /**
     * Sets if the propertykey value should get compared by == nor equals
     * 
     * @param strict
     */
    public void setStrict(final boolean strict) {
        this.strict = strict;
    }

    public void controlEvent(final ControlEvent event) {
        if (event.getEventID() == ControlEvent.CONTROL_JDPROPERTY_CHANGED) {
            for (final String key : list) {
                if (strict) {
                    // if an addon sets property.setProperty(null,bla) rthis
                    // leads to nullpointers here
                    if ((event.getParameter() == null && key == null) || (event.getParameter() != null && event.getParameter().equals(key))) {
                        onPropertyChanged((Property) event.getCaller(), key);
                    }
                } else {
                    if (event.getParameter() == key) {
                        onPropertyChanged((Property) event.getCaller(), key);
                    }
                }
            }

        }
    }

    abstract public void onPropertyChanged(Property source, String key);

}
