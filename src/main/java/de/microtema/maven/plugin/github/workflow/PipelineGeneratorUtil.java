package de.microtema.maven.plugin.github.workflow;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.project.MavenProject;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PipelineGeneratorUtil {

    public static String getTemplate(String templateName) {

        InputStream inputStream = PipelineGeneratorUtil.class.getResourceAsStream("/" + templateName + ".template.yaml");

        try {
            return IOUtils.toString(inputStream, Charset.defaultCharset());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to get template: " + templateName, e);
        }
    }

    public static String getRootPath(MavenProject project) {

        return project.getBasedir().getPath();
    }

    public static boolean hasMavenWrapper(MavenProject project) {

        return new File(getRootPath(project), ".mvn").exists();
    }

    public static boolean existsDockerfile(MavenProject project) {

        return new File(getRootPath(project), ".Dockerfile").exists();
    }

    public static boolean existsMavenSettings(MavenProject project) {

        return new File(getRootPath(project), "settings.xml").exists();
    }

    public static boolean existsLiquibase(MavenProject project) {

        return new File(getRootPath(project), "src/main/resources/db/changelog").exists();
    }

    public static boolean existsFlyway(MavenProject project) {

        return new File(getRootPath(project), "src/main/resources/db/migration").exists();
    }

    public static boolean existsPerformanceTests(MavenProject project) {

        return new File(getRootPath(project), "src/test/jmeter").exists();
    }

    public static boolean hasSourceCode(MavenProject project) {

        if (new File(getRootPath(project), "src/main/java").exists()) {
            return true;
        }

        List<String> modules = new ArrayList<>(project.getModules());

        return modules.stream().anyMatch(it -> new File(new File(getRootPath(project), it), "/src/main/java").exists());
    }

    public static boolean isGitRepo(MavenProject project) {

        return new File(getRootPath(project), ".git").exists();
    }

    public static List<String> getSonarExcludes(MavenProject project) {

        List<String> excludes = new ArrayList<>(project.getModules());

        excludes.removeIf(it -> it.startsWith("../"));
        excludes.removeIf(it -> new File(new File(getRootPath(project), it), "src/main/java").exists());

        return excludes;
    }

    public static boolean hasSonarProperties(MavenProject project) {

        return project.getProperties().entrySet()
                .stream().anyMatch(it -> String.valueOf(it.getKey()).startsWith("sonar."));
    }

    public static boolean existsUnitTests(MavenProject project) {

        String rootPath = getRootPath(project);

        if (CollectionUtils.isEmpty(project.getModules())) {

            File rootDir = new File(rootPath, "src/test/java");

            return findFile(rootDir.toPath(), "Test.java");
        }

        List<String> modules = new ArrayList<>(project.getModules());

        return modules.stream().anyMatch(it -> {

            File rootDir = new File(new File(getRootPath(project), it), "/src/test/java");

            return findFile(rootDir.toPath(), "Test.java");
        });
    }

    public static boolean existsIntegrationTests(MavenProject project) {

        String rootPath = getRootPath(project);

        if (CollectionUtils.isEmpty(project.getModules())) {

            File rootDir = new File(rootPath, "src/it/java");

            return findFile(rootDir.toPath(), "IT.java");
        }

        List<String> modules = new ArrayList<>(project.getModules());

        return modules.stream().anyMatch(it -> {

            File rootDir = new File(new File(getRootPath(project), it), "/src/main/java");

            return findFile(rootDir.toPath(), "IT.java");
        });
    }

    public static boolean existsRegressionTests(MavenProject project, String type) {

        String rootPath = getRootPath(project);

        if (CollectionUtils.isEmpty(project.getModules())) {

            File rootDir = new File(rootPath, "src/" + type + "/java");

            return findFile(rootDir.toPath(), "IT.java");
        }

        List<String> modules = new ArrayList<>(project.getModules());

        return modules.stream().anyMatch(it -> {

            File rootDir = new File(new File(getRootPath(project), it), "src/" + type + "/java");

            return findFile(rootDir.toPath(), "IT.java");
        });
    }

    public static List<String> getRegressionTestTypes(MavenProject project) {

        String rootPath = getRootPath(project);

        List<String> defaultFolders = Arrays.asList("main", "test", "it");

        File rootDir = new File(rootPath, "src");

        File[] files = rootDir.listFiles();

        return Stream.of(files)
                .filter(it -> it.isDirectory() && !defaultFolders.contains(it.getName().toLowerCase()))
                .map(File::getName).sorted().collect(Collectors.toList());
    }

    static boolean findFile(Path targetDir, String fileName) {
        try {
            return Files.list(targetDir).anyMatch((p) -> {
                if (Files.isRegularFile(p)) {
                    String file = p.getFileName().toString();
                    return file.contains(fileName);
                } else {
                    return findFile(p, fileName);
                }
            });
        } catch (IOException e) {
            return false;
        }
    }

    public static String trimEmptyLines(String template) {
        return trimEmptyLines(template, 0);
    }

    public static String trimEmptyLines(String template, int padding) {

        if (StringUtils.isEmpty(template)) {
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder();
        List<String> spaces = new ArrayList<>();

        while (padding-- > 0) {
            spaces.add(" ");
        }

        String paddingString = String.join("", spaces);

        try (BufferedReader reader = new BufferedReader(new StringReader(template))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(count > 0 ? "\n" : "").append(paddingString).append(line);
                count++;
            }

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return stringBuilder.toString();
    }

    public static String getProperty(MavenProject project, String propertyName, String defaultValue) {

        return project.getProperties().entrySet()
                .stream().filter(it -> StringUtils.equalsIgnoreCase(it.getKey().toString(), propertyName))
                .map(Map.Entry::getValue)
                .map(String::valueOf)
                .findFirst().orElse(defaultValue);
    }
}
