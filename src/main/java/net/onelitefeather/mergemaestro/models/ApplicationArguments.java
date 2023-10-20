package net.onelitefeather.mergemaestro.models;

import picocli.CommandLine;

public class ApplicationArguments {

    @CommandLine.Option(names = "-project", description = "Project ID from Gitlab", required = true)
    public String projectId;

    @CommandLine.Option(names = "-pat", description = "Personal Access Token from Gitlab", required = true)
    public String pat;

    @CommandLine.Option(names = "-branch", description = "Base Branch", required = true)
    public String baseBranch;

    @CommandLine.Option(names = "-webhook", description = "Discord WebHook URL", required = true)
    public String webHookUrl;

}
