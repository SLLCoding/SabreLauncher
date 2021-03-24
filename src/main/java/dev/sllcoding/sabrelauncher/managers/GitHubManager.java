package dev.sllcoding.sabrelauncher.managers;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;

public class GitHubManager {

    private static GitHub github;

    public static GHRepository setup() throws IOException {
        if (github == null) github = GitHub.connect();
        return github.getOrganization("Project-Cepi").getRepository("Sabre");
    }

    public static GitHub getGithub() {
        return github;
    }

}
