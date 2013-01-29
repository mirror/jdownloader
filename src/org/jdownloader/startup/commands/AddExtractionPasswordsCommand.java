package org.jdownloader.startup.commands;

public class AddExtractionPasswordsCommand extends AbstractExtensionStartupCommand {

    public AddExtractionPasswordsCommand() {
        super("add-passwords", "add-password", "p");
    }

    @Override
    public String getParameterHelp() {
        return "Password1\\r\\nPassword2\\r\\n....\\r\\nContainerPath";
    }

    @Override
    public String getDescription() {
        return "Add Extraction Passwords";
    }

}
