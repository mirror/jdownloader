package jd.plugins;

import java.util.ArrayList;

import jd.plugins.infogenerator.PluginInfoGenerator;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.swing.dialog.Dialog;

public class KeyValueInfoGenerator extends PluginInfoGenerator {
    private ArrayList<String[]> pairs;
    private String              name;

    public KeyValueInfoGenerator(String title) {
        this.name = title;
        pairs = new ArrayList<String[]>();
    }

    public void addPair(String key, String value) {
        pairs.add(new String[] { key, value });
    }

    @Override
    public void show() {

        Dialog.getInstance().showMessageDialog(name, JSonStorage.toString(pairs));
    }

}
