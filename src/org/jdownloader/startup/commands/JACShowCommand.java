package org.jdownloader.startup.commands;

import jd.captcha.JACController;

public class JACShowCommand extends AbstractStartupCommand {

    public JACShowCommand() {
        super("jacShow", "jacTrain");
    }

    @Override
    public void run(String command, String... parameters) {
        JACController.showDialog(true);
    }

    @Override
    public String getDescription() {
        return "Show the JAntiCaptcha Trainer";
    }

}
