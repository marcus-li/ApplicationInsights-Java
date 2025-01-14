package com.microsoft.applicationinsights.agent.bootstrap.diagnostics;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class SdkVersionFinder extends CachedDiagnosticsValueFinder {

    private static volatile String value;

    @Override
    public String getName() {
        return "sdkVersion";
    }

    @Override
    protected String populateValue() {
        return value;
    }

    public static String getTheValue() {
        return value;
    }

    public static String initVersion(Path agentPath) {
        value = readVersion(agentPath);
        return value;
    }

    private static String readVersion(Path agentPath) {
        try {
            // reading from file instead of from classpath, in order to avoid triggering jar file signature verification
            try (JarFile jarFile = new JarFile(agentPath.toFile(), false)) {
                JarEntry entry = jarFile.getJarEntry("ai.sdk-version.properties");
                try (InputStream in = jarFile.getInputStream(entry)) {
                    Properties props = new Properties();
                    props.load(in);
                    return props.getProperty("version");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "unknown";
    }
}
