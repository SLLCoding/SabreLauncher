package dev.sllcoding.sabrelauncher;

import com.google.gson.JsonObject;
import dev.sllcoding.sabrelauncher.managers.ConfigManager;
import dev.sllcoding.sabrelauncher.managers.GitHubManager;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarInputStream;

public class SabreLauncher {

    private static final File folder = new File(".sabrelauncher");
    private static final File jar = new File(folder, "sabre.jar");

    private static final LauncherClassLoader classLoader = new LauncherClassLoader(SabreLauncher.class.getClassLoader());

    public static void main(String[] args) {
        try {
            start(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[Sabre Launcher] An error occurred while starting up. Please report to the GitHub repository.");
            System.exit(1);
        }
    }

    private static void start(String[] args) throws Exception {
        ProgressBar pb = new ProgressBar("Sabre Launcher", 9, 1000, System.out, ProgressBarStyle.ASCII, "", 1L, false, null, ChronoUnit.SECONDS, 0L, Duration.ZERO);

        pb.setExtraMessage("Creating folders.");
        pb.step();
        if (!folder.exists()) folder.mkdirs();

        pb.setExtraMessage("Setting up config.");
        pb.step();
        JsonObject config = ConfigManager.setup();

        pb.setExtraMessage("Setting up GitHub intergration.");
        pb.step();
        GHRepository repository = GitHubManager.setup();

        pb.setExtraMessage("Getting version from config.");
        pb.step();
        String version = config.get("version").getAsString();
        GHRelease release;

        pb.setExtraMessage("Getting release data.");
        pb.step();
        String knownVersion;
        if (version.equalsIgnoreCase("latest")) {
            release = repository.getLatestRelease();
            knownVersion = release.getTagName();
        } else if (version.equalsIgnoreCase("dev")) {
            release = repository.getReleaseByTagName("latest");
            knownVersion = "development";
        } else {
            release = repository.getReleaseByTagName(version);
            knownVersion = release.getTagName();
        }
        if (release == null) {
            pb.close();
            System.err.println("[Sabre Launcher] Version \"" + version + "\" doesn't exist.");
            System.exit(1);
        }

        pb.setExtraMessage("Getting asset details.");
        pb.step();
        GHAsset jarAsset = null;
        for (GHAsset asset : release.listAssets()) {
            if (asset.getContentType().equals("application/x-java-archive")) {
                jarAsset = asset;
                break;
            }
        }
        if (jarAsset == null) {
            pb.close();
            System.err.println("[Sabre Launcher] Version \"" + knownVersion + "\" doesn't provide a downloadable jar.");
            System.exit(1);
        }

        pb.setExtraMessage("Downloading jar.");
        pb.step();
        InputStream jarData = new URL(jarAsset.getBrowserDownloadUrl()).openStream();
        Files.copy(jarData, jar.toPath(), StandardCopyOption.REPLACE_EXISTING);
        if (!jar.exists()) {
            pb.close();
            System.err.println("[Sabre Launcher] Failed to download jar.");
            System.exit(1);
        }

        pb.setExtraMessage("Adding to classpath.");
        pb.step();
        classLoader.addURL(jar.toURI().toURL());

        pb.setExtraMessage("Done. Launching Sabre.");
        pb.step();
        TimeUnit.SECONDS.sleep(10);
        pb.close();
        try {
            getMainMethod(getMainClass(jar.toPath())).invoke(null, new Object[]{args});
            System.out.println("[Sabre Launcher] Sabre " + knownVersion + " launched!");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[Sabre Launcher] Failed to launch jar.");
            System.exit(1);
        }
    }

    /**
     * Gets the main class of a jar.
     * @param jar Path of the jar file.
     * @return classpath
     * @author ServerJars
     */
    private static String getMainClass(final Path jar) {
        try (
                final InputStream is = new BufferedInputStream(Files.newInputStream(jar));
                final JarInputStream js = new JarInputStream(is)
        ) {
            return js.getManifest().getMainAttributes().getValue("Main-Class");
        } catch (final IOException e) {
            System.err.println("[ServerJars] Error reading from patched jar");
            e.printStackTrace();
            System.exit(1);
            throw new InternalError();
        }
    }

    /**
     * Gets the main method of a main class.
     * @param mainClass Main classpath.
     * @return method
     * @author ServerJars
     */
    private static Method getMainMethod(final String mainClass) {
        try {
            final Class<?> cls = Class.forName(mainClass, true, classLoader);
            return cls.getMethod("main", String[].class);
        } catch (final NoSuchMethodException | ClassNotFoundException e) {
            System.err.println("[ServerJars] Failed to find main method in patched jar");
            e.printStackTrace();
            System.exit(1);
            throw new InternalError();
        }
    }

}
