package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog;

import java.awt.event.ActionEvent;
import java.util.regex.Pattern;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.Files;
import org.appwork.utils.Regex;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.controlling.filter.CompiledRegexFilter;
import org.jdownloader.controlling.filter.FiletypeFilter;
import org.jdownloader.controlling.filter.RegexFilter;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class TestAction extends AppAction {

    private String                 name;
    private CompiledRegexFilter    filter;
    private CompiledFiletypeFilter filetypefilter;
    private String                 desc;
    private RegexFilter            sourceFilter;

    public TestAction(RegexFilter sourceFilter, String name) {
        super();
        setName(_GUI._.TestAction_TestAction_object_(name));
        setSmallIcon(NewTheme.I().getIcon("test", 18));
        this.name = name;
        this.sourceFilter = sourceFilter;
        desc = sourceFilter.toString();
    }

    public TestAction(FiletypeFilter filetypeFilter, String name) {
        super();
        setName(_GUI._.TestAction_TestAction_object_(name));
        setSmallIcon(NewTheme.I().getIcon("test", 18));
        this.name = name;

        filetypefilter = new CompiledFiletypeFilter(filetypeFilter);
        desc = filetypeFilter.toString();

    }

    public void actionPerformed(ActionEvent e) {

        try {
            this.filter = new CompiledRegexFilter(sourceFilter);
            if (filetypefilter == null) {
                String input = Dialog.getInstance().showInputDialog(0, _GUI._.TestAction_actionPerformed_test_title_(name), _GUI._.TestAction_actionPerformed_msg_(filter.getPattern().pattern(), name), JSonStorage.getPlainStorage("packagizertesturls").get(name, ""), NewTheme.I().getIcon("test", 32), null, null);
                JSonStorage.getPlainStorage("packagizertesturls").put(name, input);
                String[] matches;
                switch (filter.getMatchType()) {
                case CONTAINS:

                    if (!filter.getPattern().matcher(input).find()) {
                        Dialog.getInstance().showErrorDialog(_GUI._.TestAction_actionPerformed_nomatch_contain(input, filter.getPattern().pattern()));
                    } else {
                        matches = new Regex(input, filter.getPattern()).getRow(0);
                        StringBuilder sb = new StringBuilder();
                        int i = 1;
                        for (String m : matches) {
                            sb.append("\r\n" + _GUI._.TestAction_actionPerformed_match_(i, m));
                            i++;
                        }
                        Dialog.getInstance().showMessageDialog(_GUI._.TestAction_actionPerformed_object_(input, filter.getPattern().pattern(), sb.toString()));
                    }
                    return;
                case EQUALS:
                    if (!filter.getPattern().matcher(input).matches()) {
                        Dialog.getInstance().showErrorDialog(_GUI._.TestAction_actionPerformed_nomatch_(input, filter.getPattern().pattern()));
                    } else {
                        matches = new Regex(input, filter.getPattern()).getRow(0);

                        StringBuilder sb = new StringBuilder();
                        int i = 1;
                        for (String m : matches) {
                            sb.append("\r\n" + _GUI._.TestAction_actionPerformed_match_(i, m));
                            i++;
                        }
                        Dialog.getInstance().showMessageDialog(_GUI._.TestAction_actionPerformed_object_matches(input, filter.getPattern().pattern(), sb.toString()));

                    }
                    return;
                case CONTAINS_NOT:
                    if (!filter.getPattern().matcher(input).find()) {
                        Dialog.getInstance().showMessageDialog(_GUI._.TestAction_actionPerformed_nomatch_contain(input, filter.getPattern().pattern()));
                    } else {
                        Dialog.getInstance().showErrorDialog(_GUI._.TestAction_actionPerformed_contains_(input, filter.getPattern().pattern()));
                    }
                    return;
                case EQUALS_NOT:
                    if (!filter.getPattern().matcher(input).matches()) {
                        Dialog.getInstance().showMessageDialog(_GUI._.TestAction_actionPerformed_nomatch_(input, filter.getPattern().pattern()));
                    } else {
                        Dialog.getInstance().showErrorDialog(_GUI._.TestAction_actionPerformed_equals_(input, filter.getPattern().pattern()));
                    }
                    return;
                }
            } else {

                String input = Dialog.getInstance().showInputDialog(0, _GUI._.TestAction_actionPerformed_test_title_(name), _GUI._.TestAction_actionPerformed_msg_(desc, name), JSonStorage.getPlainStorage("packagizertesturls").get(name, ""), NewTheme.I().getIcon("Test", 32), null, null);
                JSonStorage.getPlainStorage("packagizertesturls").put(name, input);
                String extension = Files.getExtension(input);
                switch (filetypefilter.getMatchType()) {
                case IS:
                    for (Pattern o : this.filetypefilter.getList()) {
                        try {
                            if (o.matcher(extension).matches()) {

                                Dialog.getInstance().showMessageDialog(_GUI._.TestAction_actionPerformed_match_ext_(input, o.pattern()));
                                return;
                            }
                        } catch (Throwable e1) {
                            e1.printStackTrace();
                        }
                    }
                    Dialog.getInstance().showErrorDialog(_GUI._.TestAction_actionPerformed_nomatch_ext_(input));
                    return;
                case IS_NOT:
                    for (Pattern o : this.filetypefilter.getList()) {
                        try {
                            if (o.matcher(extension).matches()) {
                                Dialog.getInstance().showErrorDialog(_GUI._.TestAction_actionPerformed_match_ext_(input, o.pattern()));
                                return;
                            }
                        } catch (Throwable e1) {
                            e1.printStackTrace();
                        }
                    }

                    Dialog.getInstance().showMessageDialog(_GUI._.TestAction_actionPerformed_nomatch_ext_(input));
                    return;

                }

            }
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        } catch (Exception e1) {
            Dialog.getInstance().showExceptionDialog(_GUI._.lit_error_occured(), e1.getMessage(), e1);
        }
    }
}
