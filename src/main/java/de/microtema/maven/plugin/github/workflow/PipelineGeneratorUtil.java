package de.microtema.maven.plugin.github.workflow;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PipelineGeneratorUtil {

    private final static MustacheFactory MUSTACHE_FACTORY = new DefaultMustacheFactory();

    public static String compileTemplate(String templateName, Object context) {

        Mustache mustache = MUSTACHE_FACTORY.compile(templateName + ".template.yaml");

        StringWriter stringWriter = new StringWriter();

        Writer execute = mustache.execute(stringWriter, context);

        try {
            execute.flush();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return stringWriter.toString();
    }

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

        return new File(getRootPath(project), "Dockerfile").exists();
    }

    public static boolean existsHelmFile(MavenProject project) {

        return new File(getRootPath(project), "helm").exists();
    }

    public static boolean existsTerraformFile(MavenProject project) {

        return new File(getRootPath(project), "terraform").exists();
    }

    public static boolean existsMavenSettings(MavenProject project) {

        return new File(getRootPath(project), "settings.xml").exists();
    }

    @SuppressWarnings("unchecked")
    public static boolean existsLiquibase(MavenProject project) {

        boolean changelog = new File(getRootPath(project), "src/main/resources/db/changelog").exists();

        if (!changelog) {
            return false;
        }

        List<Dependency> runtimeDependencies = project.getDependencies();

        if (CollectionUtils.isEmpty(runtimeDependencies)) {
            return false;
        }

        return runtimeDependencies
                .stream()
                .noneMatch(it -> StringUtils.equalsIgnoreCase("liquibase-core", it.getArtifactId()) && StringUtils.equalsIgnoreCase("compile", it.getScope()));
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

        if (new File(getRootPath(project), "src/main/kotlin").exists()) {
            return true;
        }

        List<String> modules = new ArrayList<>(project.getModules());

        return modules.stream().anyMatch(it -> new File(new File(getRootPath(project), it), "/src/main/java").exists()) ||
                modules.stream().anyMatch(it -> new File(new File(getRootPath(project), it), "/src/main/kotlin").exists());
    }

    public static boolean isGitRepo(MavenProject project) {

        return new File(getRootPath(project), ".git").exists();
    }

    public static List<String> getSonarExcludes(MavenProject project) {

        List<String> excludes = new ArrayList<>(project.getModules());

        excludes.removeIf(it -> it.startsWith("../"));
        excludes.removeIf(it -> new File(new File(getRootPath(project), it), "src/main/java").exists());
        excludes.removeIf(it -> new File(new File(getRootPath(project), it), "src/main/kotlin").exists());

        return excludes;
    }

    public static boolean hasSonarProperties(MavenProject project) {

        String projectKey = getProperty(project, "sonar.host.url", "false");

        if (StringUtils.equalsIgnoreCase(projectKey, "false")) {
            return false;
        }

        return project.getProperties().entrySet()
                .stream().anyMatch(it -> String.valueOf(it.getKey()).startsWith("sonar."));
    }

    public static boolean existsUnitTests(MavenProject project) {

        String rootPath = getRootPath(project);

        if (CollectionUtils.isEmpty(project.getModules())) {

            return Stream.of("java", "kotlin", "groovy").anyMatch(it -> new File(rootPath, "src/test/" + it).exists());
        }

        List<String> modules = new ArrayList<>(project.getModules());

        return modules.stream().anyMatch(it -> Stream.of("java", "kotlin", "groovy").anyMatch(m -> new File(new File(getRootPath(project), it), "/src/test/" + m).exists()));
    }

    public static boolean existsIntegrationTests(MavenProject project) {

        return existsRegressionTests(project, "it");
    }

    public static boolean existsRegressionTests(MavenProject project, String type) {

        String testType = parseTestType(type) + ".java";

        String rootPath = getRootPath(project);
        String sourceDir = "src/test/" + type + "/java".toLowerCase();

        if (CollectionUtils.isEmpty(project.getModules())) {

            File rootDir = new File(rootPath, sourceDir);

            return findFile(rootDir.toPath(), testType);
        }

        List<String> modules = new ArrayList<>(project.getModules());

        return modules.stream().anyMatch(it -> {

            File rootDir = new File(new File(getRootPath(project), it), sourceDir);

            return findFile(rootDir.toPath(), testType);
        });
    }

    public static List<String> getRegressionTestTypes(MavenProject project) {

        String rootPath = getRootPath(project);

        List<String> defaultFolders = Arrays.asList("java", "it");

        File rootDir = new File(rootPath, "src/test");

        File[] files = Optional.ofNullable(rootDir.listFiles()).orElseGet(() -> new File[0]);

        List<String> list = Stream.of(files)
                .filter(it -> it.isDirectory() && !defaultFolders.contains(it.getName().toLowerCase()))
                .filter(it -> new File(it, "java").exists())
                .map(File::getName)
                .sorted()
                .collect(Collectors.toList());

        list.removeIf(it -> !PipelineGeneratorUtil.existsRegressionTests(project, it));

        return list;
    }

    static boolean findFile(Path targetDir, String... fileNames) {
        try {
            return Files.list(targetDir).anyMatch((p) -> {
                if (Files.isRegularFile(p)) {
                    String file = p.getFileName().toString();
                    return Stream.of(fileNames).anyMatch(file::endsWith);
                } else {
                    return findFile(p, fileNames);
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

    public static String removeEmptyLines(String text) {
        final String[] strings = text.split("\n");
        StringBuilder result = new StringBuilder();
        for (int i = 0, stringsLength = strings.length; i < stringsLength; i++) {
            String str = strings[i];
            if (str.isEmpty()) continue;
            result.append(str);
            if (i + 1 == stringsLength) continue;
            result.append("\n");
        }
        return result.toString();
    }

    public static String getProperty(MavenProject project, String propertyName, String defaultValue) {

        return project.getProperties().entrySet()
                .stream().filter(it -> StringUtils.equalsIgnoreCase(it.getKey().toString(), propertyName))
                .map(Map.Entry::getValue)
                .map(String::valueOf)
                .findFirst().orElse(defaultValue);
    }

    public static boolean isDeploymentRepo(MavenProject project) {

        if (hasSourceCode(project)) {
            return false;
        }

        return existsHelmFile(project) || existsTerraformFile(project);
    }

    public static boolean isMicroserviceRepo(MavenProject project) {

        return (existsHelmFile(project) || existsDockerfile(project)) && hasSourceCode(project);
    }

    public static boolean isMavenArtifactRepo(MavenProject project) {

        return (!existsHelmFile(project) && !existsDockerfile(project));
    }

    public static boolean isMavenPomRepo(MavenProject project) {

        return !existsHelmFile(project) && !existsDockerfile(project) && !hasSourceCode(project);
    }

    public static boolean isMavenLibraryRepo(MavenProject project) {

        return !existsHelmFile(project) && !existsDockerfile(project) && hasSourceCode(project);
    }

    public static Properties findProperties(String stageName) {

        if (StringUtils.isEmpty(stageName)) {
            return null;
        }

        String fileName = ("." + stageName).toLowerCase();

        String envFolder = ".github/env";

        File file = new File(envFolder, fileName);

        if (!file.exists()) {
            return null;
        }

        Properties properties = new Properties();

        try (InputStream inputStream = new FileInputStream(file)) {
            properties.load(inputStream);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return properties;
    }

    public static boolean isPrivateNetwork(String stageName) {

        Properties properties = findProperties(stageName);

        if (Objects.isNull(properties)) {

            return false;
        }

        String serviceUrl = properties.getProperty("SERVICE_URL", "false");

        return StringUtils.contains(serviceUrl, "localhost");
    }

    /**
     * Convert integration-test to IT
     *
     * @param testType may not be null
     * @return String
     */
    public static String parseTestType(String testType) {

        String[] tokens = StringUtils.split(testType, "-");

        if (tokens.length == 1) {
            return testType.toUpperCase();
        }

        return Stream.of(tokens)
                .map(it -> it.charAt(0))
                .map(String::valueOf)
                .map(String::toUpperCase)
                .collect(Collectors.joining());
    }

    public static String applyProperties(String template, String stageName) {

        return applyProperties(template, stageName, Collections.emptyMap());
    }

    public static String applyProperties(String template, Map<String, Object> globalVariables) {

        for (Map.Entry<String, Object> entry : globalVariables.entrySet()) {
            template = template.replace("%" + entry.getKey() + "%", String.valueOf(entry.getValue()));
        }

        return template;
    }

    public static String applyProperties(String template, String stageName, Map<String, Object> globalVariables) {

        Properties properties = PipelineGeneratorUtil.findProperties(stageName);

        if (Objects.isNull(properties)) {
            return template;
        }

        properties.putIfAbsent("STAGE_NAME", stageName);

        globalVariables.entrySet().stream().filter(it -> !properties.containsKey(it.getKey())).forEach(it -> properties.put(it.getKey(), it.getValue()));

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            template = template.replace("%" + entry.getKey() + "%", String.valueOf(entry.getValue()));
        }

        return template;
    }

    public static boolean isSpeedBranch(String branchName) {

        return StringUtils.startsWith(branchName, "speed");
    }

    public static boolean isSameDockerRegistry(List<String> stageNames) {

        if (stageNames.size() == 1) {
            return true;
        }

        return stageNames.stream()
                .map(PipelineGeneratorUtil::findProperties)
                .filter(Objects::nonNull)
                .map(it -> it.getProperty("DOCKER_REGISTRY"))
                .collect(Collectors.toSet()).size() == 1;
    }

    public static String getJobName(String jobName, String stageName, boolean multipleStages) {

        if (multipleStages) {

            return "[" + stageName.toUpperCase() + "] " + jobName;
        }

        return jobName;
    }
}
