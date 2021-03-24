package dev.sllcoding.sabrelauncher.managers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.file.Files;

public class ConfigManager {

    private static final File configFile = new File("sabrelauncher.json");

    public static JsonObject setup() throws IOException {
        if (!configFile.exists()) {
            configFile.createNewFile();
            Files.write(configFile.toPath(), ConfigManager.class.getClassLoader().getResourceAsStream("sabrelauncher.json").readAllBytes());
            System.out.println("[Sabre Launcher] A new config file has been generated, please configure Sabre Launcher before restarting.");
            System.exit(0);
        }
        return new Gson().fromJson(readFromInputStream(new FileInputStream(configFile)), JsonObject.class);
    }

    private static String readFromInputStream(InputStream inputStream)
            throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br
                     = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }

}
