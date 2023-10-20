package net.onelitefeather.mergemaestro;

import net.onelitefeather.mergemaestro.models.ApplicationArguments;
import picocli.CommandLine;

public class ApplicationEntry {

    public static void main(String[] args) {
        var applicationArguments = new ApplicationArguments();
        new CommandLine(applicationArguments).parseArgs(args);

    }

}
