package de.microtema.maven.plugin.github.workflow;

import org.apache.commons.io.FileUtils;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PipelineGeneratorMojoTest {

    @InjectMocks
    PipelineGeneratorMojo sut;

    @Mock
    MavenProject project;

    @Mock
    File basePath;

    @Mock
    Properties properties;

    File pipelineFile;

    @BeforeEach
    void setUp() {

        sut.project = project;

        sut.githubWorkflowsDir = "./target/.github/workflows";

        sut.variables.put("DOCKER_REGISTRY", "docker.registry.local");

        sut.serviceUrl = "http://$STAGE.$CLUSTER.local/supplier/git/info";

        sut.runsOn = "self-hosted,azure-runners";
    }

    @Test
    void generateFeatureDeployment() throws Exception {

        when(project.getBasedir()).thenReturn(basePath);
        when(basePath.getPath()).thenReturn(".");
        when(project.getName()).thenReturn("github-workflows-maven-plugin Maven Mojo");
        when(project.getProperties()).thenReturn(properties);
        Map<Object, Object> stringStringMap = Collections.singletonMap("sonar.url", "http://localhost:9000");
        when(properties.entrySet()).thenReturn(stringStringMap.entrySet());

        sut.stages.put("local", "feature/*");

        pipelineFile = new File(sut.githubWorkflowsDir, "feature.yaml");

        sut.execute();

        String answer = FileUtils.readFileToString(pipelineFile, "UTF-8");

        assertEquals("name: github-workflows-maven-plugin Maven Mojo\n" +
                "\n" +
                "on:\n" +
                "  push:\n" +
                "    branches:\n" +
                "      - feature/*\n" +
                "  pull_request:\n" +
                "    branches:\n" +
                "      - feature/*\n" +
                "\n" +
                "env:\n" +
                "  DOCKER_REGISTRY: \"docker.registry.local\"\n" +
                "  JAVA_VERSION: \"17.x\"\n" +
                "  GIT_STRATEGY: \"clone\"\n" +
                "  GIT_DEPTH: \"10\"\n" +
                "  MAVEN_CLI_OPTS: \"--batch-mode --errors --fail-at-end --show-version -DinstallAtEnd=true\\\n" +
                "    \\ -DdeployAtEnd=true\"\n" +
                "\n" +
                "jobs:\n" +
                "  compile:\n" +
                "    name: Compile\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ versioning ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ JAVA_VERSION }}\n" +
                "      - name: 'Maven: Compile'\n" +
                "        run: mvn compile $MAVEN_CLI_OPTS\n" +
                "\n", answer);
    }

    @Test
    void generateDevelopmentDeployment() throws Exception {

        when(project.getBasedir()).thenReturn(basePath);
        when(basePath.getPath()).thenReturn(".");
        when(project.getName()).thenReturn("github-workflows-maven-plugin Maven Mojo");
        when(project.getProperties()).thenReturn(properties);
        Map<Object, Object> stringStringMap = Collections.singletonMap("sonar.url", "http://localhost:9000");
        when(properties.entrySet()).thenReturn(stringStringMap.entrySet());

        sut.stages.put("dev", "develop");

        pipelineFile = new File(sut.githubWorkflowsDir, "develop.yaml");

        sut.execute();

        String answer = FileUtils.readFileToString(pipelineFile, "UTF-8");

        assertEquals("name: github-workflows-maven-plugin Maven Mojo\n" +
                "\n" +
                "on:\n" +
                "  push:\n" +
                "    branches:\n" +
                "      - develop\n" +
                "  pull_request:\n" +
                "    branches:\n" +
                "      - develop\n" +
                "\n" +
                "env:\n" +
                "  DOCKER_REGISTRY: \"docker.registry.local\"\n" +
                "  JAVA_VERSION: \"17.x\"\n" +
                "  GIT_STRATEGY: \"clone\"\n" +
                "  GIT_DEPTH: \"10\"\n" +
                "  MAVEN_CLI_OPTS: \"--batch-mode --errors --fail-at-end --show-version -DinstallAtEnd=true\\\n" +
                "    \\ -DdeployAtEnd=true\"\n" +
                "\n" +
                "jobs:\n" +
                "  compile:\n" +
                "    name: Compile\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ JAVA_VERSION }}\n" +
                "      - name: 'Maven: Compile'\n" +
                "        run: mvn compile $MAVEN_CLI_OPTS\n" +
                "\n", answer);
    }

    @Test
    void generateReleaseDeployment() throws Exception {

        when(project.getBasedir()).thenReturn(basePath);
        when(basePath.getPath()).thenReturn(".");
        when(project.getName()).thenReturn("github-workflows-maven-plugin Maven Mojo");
        when(project.getProperties()).thenReturn(properties);
        Map<Object, Object> stringStringMap = Collections.singletonMap("sonar.url", "http://localhost:9000");
        when(properties.entrySet()).thenReturn(stringStringMap.entrySet());

        sut.stages.put("stage", "release/*");

        pipelineFile = new File(sut.githubWorkflowsDir, "release.yaml");

        sut.execute();

        String answer = FileUtils.readFileToString(pipelineFile, "UTF-8");

        assertEquals("name: github-workflows-maven-plugin Maven Mojo\n" +
                "\n" +
                "on:\n" +
                "  push:\n" +
                "    branches:\n" +
                "      - release/*\n" +
                "  pull_request:\n" +
                "    branches:\n" +
                "      - release/*\n" +
                "\n" +
                "env:\n" +
                "  DOCKER_REGISTRY: \"docker.registry.local\"\n" +
                "  JAVA_VERSION: \"17.x\"\n" +
                "  GIT_STRATEGY: \"clone\"\n" +
                "  GIT_DEPTH: \"10\"\n" +
                "  MAVEN_CLI_OPTS: \"--batch-mode --errors --fail-at-end --show-version -DinstallAtEnd=true\\\n" +
                "    \\ -DdeployAtEnd=true\"\n" +
                "\n" +
                "jobs:\n" +
                "  versioning:\n" +
                "    name: Versioning\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: $JAVA_VERSION\n" +
                "      - name: 'Shell: sed pom.xml'\n" +
                "        run: |\n" +
                "          export POM_PARENT_VERSION=$(mvn help:evaluate -Dexpression=project.parent.version -q -DforceStdout $MAVEN_CLI_OPTS | tail -n 1)\n" +
                "          export POM_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout $MAVEN_CLI_OPTS | tail -n 1)\n" +
                "          export NEW_VERSION=${POM_VERSION/-SNAPSHOT/-RC}\n" +
                "          sed \"s/<version>$POM_PARENT_VERSION<\\/version>/<version>$NEW_VERSION<\\/version>/g\" pom.xml > pom.xml.bac\n" +
                "          mv pom.xml.bac pom.xml\n" +
                "      - name: 'Maven: versions:set'\n" +
                "        run: |\n" +
                "          mvn release:update-versions -DdevelopmentVersion=0.0.1-SNAPSHOT $MAVEN_CLI_OPTS\n" +
                "          mvn versions:set -DnewVersion=$NEW_VERSION $MAVEN_CLI_OPTS\n" +
                "          echo ::set-output name=VERSION::$NEW_VERSION\n" +
                "\n" +
                "  compile:\n" +
                "    name: Compile\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ versioning ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ JAVA_VERSION }}\n" +
                "      - name: 'Maven: Compile'\n" +
                "        run: mvn compile $MAVEN_CLI_OPTS\n" +
                "\n", answer);
    }

    @Test
    void unMask() {

        String answer = sut.unMask("\"${KUBERNETES_VERSION:-1.11}\"");

        assertEquals("${KUBERNETES_VERSION:-1.11}", answer);
    }
}
