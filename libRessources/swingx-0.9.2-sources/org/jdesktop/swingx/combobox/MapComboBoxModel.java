/*
 * $Id: MapComboBoxModel.java,v 1.3 2007/12/30 20:42:42 kschaefe Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jdesktop.swingx.combobox;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@code ComboBoxModel} for {@code Map}s.
 * 
 * @author jm158417
 * @author Karl George Schaefer
 *
 * @param <K> the type of keys maintained by the map backing this model
 * @param <V> the type of mapped values
 */
public class MapComboBoxModel<K, V> extends ListComboBoxModel<K> {

    protected Map<K, V> map_data;
    protected boolean inverted;
    
    public MapComboBoxModel() {
        this(new HashMap<K, V>());
    }
    
    public MapComboBoxModel(Map<K, V> map) {
        this.map_data = map;
        
        buildIndex();
        
        if(data.size() > 0) {
            selected = data.get(0);
        }
    }
    
    protected void buildIndex() {
        data = new ArrayList<K>(map_data.keySet());
    }
    
    /**
     * {@inheritDoc}
     */
    public int getSize() {
        return map_data.size();
    }
    
    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent evt) {
        if(evt.getActionCommand().equals(UPDATE)) {
            buildIndex();
            fireContentsChanged(this, 0, getSize());
        }
    }

    /**
     * @param selectedItem
     * @return
     */
    public V getValue(Object selectedItem) {
        return map_data.get(selectedItem);
    }
    
    /**
     * @param selectedItem
     * @return
     */
    public V getValue(int selectedItem) {
        return getValue(getElementAt(selectedItem));
    }
    
}
