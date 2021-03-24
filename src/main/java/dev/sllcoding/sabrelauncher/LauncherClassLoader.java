package dev.sllcoding.sabrelauncher;

import java.net.URL;
import java.net.URLClassLoader;

public class LauncherClassLoader extends URLClassLoader {

    public LauncherClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

}
