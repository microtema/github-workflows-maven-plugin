package de.microtema.maven.plugin.github.workflow;

import org.apache.commons.io.FileUtils;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PipelineGeneratorMojoMicroserviceTest {

    static File dbDir = new File("./src/main/resources/db");
    static File dbMigrationDir = new File("./src/main/resources/db/migration");
    static File dbChangelogDir = new File("./src/main/resources/db/changelog");
    static File dockerFileDir = new File("./Dockerfile");

    @InjectMocks
    PipelineGeneratorMojo sut;

    @Mock
    MavenProject project;

    @Mock
    File basePath;

    Properties properties = new Properties();

    File pipelineFile;

    @BeforeAll
    static void beforeAll() {

        dbDir.mkdirs();
        dbMigrationDir.mkdirs();
        dbChangelogDir.mkdirs();

        dockerFileDir.mkdir();
    }

    @AfterAll
    static void afterAll() {

        dbMigrationDir.delete();
        dbChangelogDir.delete();

        dbDir.delete();

        dockerFileDir.delete();
    }

    @BeforeEach
    void setUp() {

        sut.project = project;

        sut.githubWorkflowsDir = "./target/.github/workflows";

        sut.variables.put("DOCKER_REGISTRY", "docker.registry.local");
        sut.variables.put("SERVICE_URL", "http://localhost:8080");
        sut.variables.put("ENV_STAGE_NAME", "ENV_$STAGE_NAME");

        sut.runsOn = "self-hosted,azure-runners";

        properties.put("sonar.url", "http://localhost:9000");
    }

    @Test
    void generateFeatureWorkflowFile() throws Exception {

        when(project.getBasedir()).thenReturn(basePath);
        when(basePath.getPath()).thenReturn(".");
        when(project.getName()).thenReturn("github-workflows-maven-plugin Maven Mojo");
        when(project.getArtifactId()).thenReturn("github-workflows-maven-plugin");
        when(project.getVersion()).thenReturn("1.1.0-SNAPSHOT");
        when(project.getProperties()).thenReturn(properties);

        properties.put("sonar.url", "http://localhost:9000");

        sut.stages.put("none", "feature/*");

        pipelineFile = new File(sut.githubWorkflowsDir, "feature-workflow.yaml");

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
                "  SERVICE_URL: \"http://localhost:8080\"\n" +
                "  ENV_STAGE_NAME: \"ENV_NONE\"\n" +
                "  APP_NAME: \"github-workflows-maven-plugin\"\n" +
                "  GITHUB_TOKEN: \"${{ secrets.GITHUB_TOKEN }}\"\n" +
                "  SONAR_TOKEN: \"${{ secrets.SONAR_TOKEN }}\"\n" +
                "  JAVA_VERSION: \"17.x\"\n" +
                "  MAVEN_CLI_OPTS: \"--batch-mode --errors --fail-at-end --show-version -DinstallAtEnd=true\\\n" +
                "    \\ -DdeployAtEnd=true\"\n" +
                "  VERSION: \"1.1.0-SNAPSHOT\"\n" +
                "  STAGE_NAME: \"none\"\n" +
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
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Maven: versions:set'\n" +
                "        run: |\n" +
                "          mvn release:update-versions -DdevelopmentVersion=0.0.1-SNAPSHOT $MAVEN_CLI_OPTS\n" +
                "          mvn versions:set -DnewVersion=$VERSION $MAVEN_CLI_OPTS\n" +
                "      - name: 'Artifact: prepare'\n" +
                "        run: |\n" +
                "          mkdir -p artifact\n" +
                "          cp pom.xml artifact/pom.xml\n" +
                "      - name: 'Artifact: upload'\n" +
                "        uses: actions/upload-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "          path: artifact/pom.xml\n" +
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
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Artifact: download'\n" +
                "        if: false\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "      - name: 'Maven: compile'\n" +
                "        run: mvn compile $MAVEN_CLI_OPTS\n" +
                "\n" +
                "  security-check:\n" +
                "    name: Security Check\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ compile ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Maven: dependency-check'\n" +
                "        run: mvn dependency-check:help -P security -Ddownloader.quick.query.timestamp=false $MAVEN_CLI_OPTS\n" +
                "\n" +
                "  unit-test:\n" +
                "    name: Unit Test\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ compile ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Artifact: download'\n" +
                "        if: false\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "      - name: 'Maven: test'\n" +
                "        run: mvn test $MAVEN_CLI_OPTS\n" +
                "      - name: 'Artifact: prepare'\n" +
                "        run: |\n" +
                "          mkdir -p artifact/target/surefire-reports\n" +
                "          cp -r target/surefire-reports/* artifact/target/surefire-reports/\n" +
                "          cp -r target/jacoco.exec artifact/target/surefire-reports/\n" +
                "      - name: 'Test result'\n" +
                "        uses: actions/upload-artifact@v2\n" +
                "        with:\n" +
                "          name: target-artifact\n" +
                "          path: artifact\n" +
                "\n" +
                "  it-test:\n" +
                "    name: Integration Test\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ compile ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Artifact: download'\n" +
                "        if: false\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "      - name: 'Maven: integration-test'\n" +
                "        run: mvn integration-test -P it -DtestType=IT -DsourceType=it $MAVEN_CLI_OPTS\n" +
                "      - name: 'Artifact: prepare'\n" +
                "        run: |\n" +
                "          mkdir -p artifact/target/surefire-reports/it\n" +
                "          cp -r target/surefire-reports/* artifact/target/surefire-reports/it/\n" +
                "          cp -r target/jacoco.exec artifact/target/surefire-reports/it/\n" +
                "      - name: 'Test result'\n" +
                "        uses: actions/upload-artifact@v2\n" +
                "        with:\n" +
                "          name: target-artifact\n" +
                "          path: artifact\n" +
                "\n" +
                "  quality-gate:\n" +
                "    name: Quality Gate\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ unit-test, it-test ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Artifact: download'\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: target-artifact\n" +
                "      - name: 'Maven: verify'\n" +
                "        run: mvn verify -DskipTests=true -Dcode.coverage=0.00 $MAVEN_CLI_OPTS\n" +
                "      - name: 'Maven: sonar'\n" +
                "        run: mvn sonar:sonar -Dsonar.login=$SONAR_TOKEN $MAVEN_CLI_OPTS\n" +
                "\n" +
                "  build:\n" +
                "    name: Build\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ quality-gate, security-check ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v2\n" +
                "        with:\n" +
                "          distribution: 'adopt'\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Artifact: download'\n" +
                "        if: true\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "      - name: 'Maven: package'\n" +
                "        run: mvn package -P prod -Dcode.coverage=0.00 -DskipTests=true -DskipUTs=true -DskipITs=true $MAVEN_CLI_OPTS\n" +
                "      - name: 'Artifact: prepare'\n" +
                "        run: |\n" +
                "          mkdir -p artifact/target\n" +
                "          cp target/*.jar artifact/target/\n" +
                "      - name: 'Artifact: upload'\n" +
                "        uses: actions/upload-artifact@v2\n" +
                "        with:\n" +
                "          name: target-artifact\n" +
                "          path: artifact/target\n" +
                "\n", answer);
    }

    @Test
    void generateDevelopWorkflowFile() throws Exception {

        when(project.getBasedir()).thenReturn(basePath);
        when(basePath.getPath()).thenReturn(".");
        when(project.getName()).thenReturn("github-workflows-maven-plugin Maven Mojo");
        when(project.getArtifactId()).thenReturn("github-workflows-maven-plugin");
        when(project.getVersion()).thenReturn("1.1.0-SNAPSHOT");
        when(project.getProperties()).thenReturn(properties);

        sut.stages.put("dev", "develop");

        pipelineFile = new File(sut.githubWorkflowsDir, "develop-workflow.yaml");

        sut.execute();

        String answer = FileUtils.readFileToString(pipelineFile, "UTF-8");

        assertEquals("name: github-workflows-maven-plugin Maven Mojo [DEV]\n" +
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
                "  SERVICE_URL: \"http://localhost:8080\"\n" +
                "  ENV_STAGE_NAME: \"ENV_DEV\"\n" +
                "  APP_NAME: \"github-workflows-maven-plugin\"\n" +
                "  GITHUB_TOKEN: \"${{ secrets.GITHUB_TOKEN }}\"\n" +
                "  SONAR_TOKEN: \"${{ secrets.SONAR_TOKEN }}\"\n" +
                "  JAVA_VERSION: \"17.x\"\n" +
                "  MAVEN_CLI_OPTS: \"--batch-mode --errors --fail-at-end --show-version -DinstallAtEnd=true\\\n" +
                "    \\ -DdeployAtEnd=true\"\n" +
                "  VERSION: \"1.1.0-SNAPSHOT\"\n" +
                "  STAGE_NAME: \"dev\"\n" +
                "  FOO: \"BAR\"\n" +
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
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Maven: versions:set'\n" +
                "        run: |\n" +
                "          mvn release:update-versions -DdevelopmentVersion=0.0.1-SNAPSHOT $MAVEN_CLI_OPTS\n" +
                "          mvn versions:set -DnewVersion=$VERSION $MAVEN_CLI_OPTS\n" +
                "      - name: 'Artifact: prepare'\n" +
                "        run: |\n" +
                "          mkdir -p artifact\n" +
                "          cp pom.xml artifact/pom.xml\n" +
                "      - name: 'Artifact: upload'\n" +
                "        uses: actions/upload-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "          path: artifact/pom.xml\n" +
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
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Artifact: download'\n" +
                "        if: false\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "      - name: 'Maven: compile'\n" +
                "        run: mvn compile $MAVEN_CLI_OPTS\n" +
                "\n" +
                "  security-check:\n" +
                "    name: Security Check\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ compile ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Maven: dependency-check'\n" +
                "        run: mvn dependency-check:help -P security -Ddownloader.quick.query.timestamp=false $MAVEN_CLI_OPTS\n" +
                "\n" +
                "  unit-test:\n" +
                "    name: Unit Test\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ compile ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Artifact: download'\n" +
                "        if: false\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "      - name: 'Maven: test'\n" +
                "        run: mvn test $MAVEN_CLI_OPTS\n" +
                "      - name: 'Artifact: prepare'\n" +
                "        run: |\n" +
                "          mkdir -p artifact/target/surefire-reports\n" +
                "          cp -r target/surefire-reports/* artifact/target/surefire-reports/\n" +
                "          cp -r target/jacoco.exec artifact/target/surefire-reports/\n" +
                "      - name: 'Test result'\n" +
                "        uses: actions/upload-artifact@v2\n" +
                "        with:\n" +
                "          name: target-artifact\n" +
                "          path: artifact\n" +
                "\n" +
                "  it-test:\n" +
                "    name: Integration Test\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ compile ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Artifact: download'\n" +
                "        if: false\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "      - name: 'Maven: integration-test'\n" +
                "        run: mvn integration-test -P it -DtestType=IT -DsourceType=it $MAVEN_CLI_OPTS\n" +
                "      - name: 'Artifact: prepare'\n" +
                "        run: |\n" +
                "          mkdir -p artifact/target/surefire-reports/it\n" +
                "          cp -r target/surefire-reports/* artifact/target/surefire-reports/it/\n" +
                "          cp -r target/jacoco.exec artifact/target/surefire-reports/it/\n" +
                "      - name: 'Test result'\n" +
                "        uses: actions/upload-artifact@v2\n" +
                "        with:\n" +
                "          name: target-artifact\n" +
                "          path: artifact\n" +
                "\n" +
                "  quality-gate:\n" +
                "    name: Quality Gate\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ unit-test, it-test ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Artifact: download'\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: target-artifact\n" +
                "      - name: 'Maven: verify'\n" +
                "        run: mvn verify -DskipTests=true -Dcode.coverage=0.00 $MAVEN_CLI_OPTS\n" +
                "      - name: 'Maven: sonar'\n" +
                "        run: mvn sonar:sonar -Dsonar.login=$SONAR_TOKEN $MAVEN_CLI_OPTS\n" +
                "\n" +
                "  build:\n" +
                "    name: Build\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ quality-gate, security-check ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v2\n" +
                "        with:\n" +
                "          distribution: 'adopt'\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Artifact: download'\n" +
                "        if: true\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "      - name: 'Maven: package'\n" +
                "        run: mvn package -P prod -Dcode.coverage=0.00 -DskipTests=true -DskipUTs=true -DskipITs=true $MAVEN_CLI_OPTS\n" +
                "      - name: 'Artifact: prepare'\n" +
                "        run: |\n" +
                "          mkdir -p artifact/target\n" +
                "          cp target/*.jar artifact/target/\n" +
                "      - name: 'Artifact: upload'\n" +
                "        uses: actions/upload-artifact@v2\n" +
                "        with:\n" +
                "          name: target-artifact\n" +
                "          path: artifact/target\n" +
                "\n" +
                "  package:\n" +
                "    name: Package\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ build ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v2\n" +
                "        with:\n" +
                "          distribution: 'adopt'\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Artifact: download'\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: target-artifact\n" +
                "      - name: 'Artifact: download'\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "      - name: 'Docker: login'\n" +
                "        run: docker login -u $DOCKER_REGISTRY_USER -p $DOCKER_REGISTRY_PASSWORD $DOCKER_REGISTRY\n" +
                "      - name: 'Docker: build'\n" +
                "        run: mvn compile jib:dockerBuild -Dimage=$DOCKER_REGISTRY/$APP_NAME -Djib.to.tags=$VERSION $MAVEN_CLI_OPTS\n" +
                "      - name: 'Docker: push'\n" +
                "        run: docker push $DOCKER_REGISTRY/$APP_NAME:$VERSION\n" +
                "\n" +
                "  db-migration:\n" +
                "    name: Database Changelog\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ package ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Liquibase: changelog'\n" +
                "        run: echo 'TBD'\n" +
                "\n" +
                "  promote:\n" +
                "    name: Promote\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ package ]\n" +
                "    steps:\n" +
                "      - name: 'Shell: promote'\n" +
                "        run: echo 'TBD'\n" +
                "\n" +
                "  deployment:\n" +
                "    name: Deployment\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ promote ]\n" +
                "    steps:\n" +
                "      - name: 'Artifact: download'\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: version-artifact\n" +
                "      - name: 'Shell: set env'\n" +
                "        run: export DOCKER_IMAGE_TAG=$(cat version)\n" +
                "      - name: 'Shell: deployment'\n" +
                "        uses: actions/github-script@v4\n" +
                "        with:\n" +
                "          github-token: ${{ secrets.GITHUB_TOKEN }}\n" +
                "          script: |\n" +
                "            await github.actions.createWorkflowDispatch({\n" +
                "                owner: context.repo.owner,\n" +
                "                repo: context.repo.repo,\n" +
                "                workflow_id: 'develop-workflow.yml',\n" +
                "                ref: context.ref,\n" +
                "                inputs: {\n" +
                "                  VERSION: \"$VERSION\"\n" +
                "                }\n" +
                "            });\n" +
                "\n" +
                "  readyness:\n" +
                "    name: Readiness Check\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ deployment ]\n" +
                "    timeout-minutes: 15\n" +
                "    steps:\n" +
                "      - name: 'Shell: readiness'\n" +
                "        run: while [[ \"$(curl -H X-API-KEY:$API_KEY -s $SERVICE_URL | jq -r '.commitId')\" != \"$GITHUB_SHA\" ]]; do sleep 10; done\n" +
                "\n", answer);
    }

    @Test
    void generateReleaseWorkflowFile() throws Exception {

        when(project.getBasedir()).thenReturn(basePath);
        when(basePath.getPath()).thenReturn(".");
        when(project.getName()).thenReturn("github-workflows-maven-plugin Maven Mojo");
        when(project.getArtifactId()).thenReturn("github-workflows-maven-plugin");
        when(project.getVersion()).thenReturn("1.1.0-SNAPSHOT");
        when(project.getProperties()).thenReturn(properties);

        sut.stages.put("stage", "release/*");

        pipelineFile = new File(sut.githubWorkflowsDir, "release-workflow.yaml");

        sut.execute();

        String answer = FileUtils.readFileToString(pipelineFile, "UTF-8");

        assertEquals("name: github-workflows-maven-plugin Maven Mojo [STAGE]\n" +
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
                "  SERVICE_URL: \"http://localhost:8080\"\n" +
                "  ENV_STAGE_NAME: \"ENV_STAGE\"\n" +
                "  APP_NAME: \"github-workflows-maven-plugin\"\n" +
                "  GITHUB_TOKEN: \"${{ secrets.GITHUB_TOKEN }}\"\n" +
                "  SONAR_TOKEN: \"${{ secrets.SONAR_TOKEN }}\"\n" +
                "  JAVA_VERSION: \"17.x\"\n" +
                "  MAVEN_CLI_OPTS: \"--batch-mode --errors --fail-at-end --show-version -DinstallAtEnd=true\\\n" +
                "    \\ -DdeployAtEnd=true\"\n" +
                "  VERSION: \"1.1.0-RC\"\n" +
                "  STAGE_NAME: \"stage\"\n" +
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
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Maven: versions:set'\n" +
                "        run: |\n" +
                "          mvn release:update-versions -DdevelopmentVersion=0.0.1-SNAPSHOT $MAVEN_CLI_OPTS\n" +
                "          mvn versions:set -DnewVersion=$VERSION $MAVEN_CLI_OPTS\n" +
                "      - name: 'Artifact: prepare'\n" +
                "        run: |\n" +
                "          mkdir -p artifact\n" +
                "          cp pom.xml artifact/pom.xml\n" +
                "      - name: 'Artifact: upload'\n" +
                "        uses: actions/upload-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "          path: artifact/pom.xml\n" +
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
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Artifact: download'\n" +
                "        if: true\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "      - name: 'Maven: compile'\n" +
                "        run: mvn compile $MAVEN_CLI_OPTS\n" +
                "\n" +
                "  security-check:\n" +
                "    name: Security Check\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ compile ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Maven: dependency-check'\n" +
                "        run: mvn dependency-check:help -P security -Ddownloader.quick.query.timestamp=false $MAVEN_CLI_OPTS\n" +
                "\n" +
                "  unit-test:\n" +
                "    name: Unit Test\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ compile ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Artifact: download'\n" +
                "        if: true\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "      - name: 'Maven: test'\n" +
                "        run: mvn test $MAVEN_CLI_OPTS\n" +
                "      - name: 'Artifact: prepare'\n" +
                "        run: |\n" +
                "          mkdir -p artifact/target/surefire-reports\n" +
                "          cp -r target/surefire-reports/* artifact/target/surefire-reports/\n" +
                "          cp -r target/jacoco.exec artifact/target/surefire-reports/\n" +
                "      - name: 'Test result'\n" +
                "        uses: actions/upload-artifact@v2\n" +
                "        with:\n" +
                "          name: target-artifact\n" +
                "          path: artifact\n" +
                "\n" +
                "  it-test:\n" +
                "    name: Integration Test\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ compile ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Artifact: download'\n" +
                "        if: true\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "      - name: 'Maven: integration-test'\n" +
                "        run: mvn integration-test -P it -DtestType=IT -DsourceType=it $MAVEN_CLI_OPTS\n" +
                "      - name: 'Artifact: prepare'\n" +
                "        run: |\n" +
                "          mkdir -p artifact/target/surefire-reports/it\n" +
                "          cp -r target/surefire-reports/* artifact/target/surefire-reports/it/\n" +
                "          cp -r target/jacoco.exec artifact/target/surefire-reports/it/\n" +
                "      - name: 'Test result'\n" +
                "        uses: actions/upload-artifact@v2\n" +
                "        with:\n" +
                "          name: target-artifact\n" +
                "          path: artifact\n" +
                "\n" +
                "  quality-gate:\n" +
                "    name: Quality Gate\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ unit-test, it-test ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Artifact: download'\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: target-artifact\n" +
                "      - name: 'Maven: verify'\n" +
                "        run: mvn verify -DskipTests=true -Dcode.coverage=0.00 $MAVEN_CLI_OPTS\n" +
                "      - name: 'Maven: sonar'\n" +
                "        run: mvn sonar:sonar -Dsonar.login=$SONAR_TOKEN $MAVEN_CLI_OPTS\n" +
                "\n" +
                "  build:\n" +
                "    name: Build\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ quality-gate, security-check ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v2\n" +
                "        with:\n" +
                "          distribution: 'adopt'\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Artifact: download'\n" +
                "        if: true\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "      - name: 'Maven: package'\n" +
                "        run: mvn package -P prod -Dcode.coverage=0.00 -DskipTests=true -DskipUTs=true -DskipITs=true $MAVEN_CLI_OPTS\n" +
                "      - name: 'Artifact: prepare'\n" +
                "        run: |\n" +
                "          mkdir -p artifact/target\n" +
                "          cp target/*.jar artifact/target/\n" +
                "      - name: 'Artifact: upload'\n" +
                "        uses: actions/upload-artifact@v2\n" +
                "        with:\n" +
                "          name: target-artifact\n" +
                "          path: artifact/target\n" +
                "\n" +
                "  package:\n" +
                "    name: Package\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ build ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v2\n" +
                "        with:\n" +
                "          distribution: 'adopt'\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Artifact: download'\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: target-artifact\n" +
                "      - name: 'Artifact: download'\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "      - name: 'Docker: login'\n" +
                "        run: docker login -u $DOCKER_REGISTRY_USER -p $DOCKER_REGISTRY_PASSWORD $DOCKER_REGISTRY\n" +
                "      - name: 'Docker: build'\n" +
                "        run: mvn compile jib:dockerBuild -Dimage=$DOCKER_REGISTRY/$APP_NAME -Djib.to.tags=$VERSION $MAVEN_CLI_OPTS\n" +
                "      - name: 'Docker: push'\n" +
                "        run: docker push $DOCKER_REGISTRY/$APP_NAME:$VERSION\n" +
                "\n" +
                "  db-migration:\n" +
                "    name: Database Changelog\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ package ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Liquibase: changelog'\n" +
                "        run: echo 'TBD'\n" +
                "\n" +
                "  promote:\n" +
                "    name: Promote\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ package ]\n" +
                "    steps:\n" +
                "      - name: 'Shell: promote'\n" +
                "        run: echo 'TBD'\n" +
                "\n" +
                "  deployment:\n" +
                "    name: Deployment\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ promote ]\n" +
                "    steps:\n" +
                "      - name: 'Artifact: download'\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: version-artifact\n" +
                "      - name: 'Shell: set env'\n" +
                "        run: export DOCKER_IMAGE_TAG=$(cat version)\n" +
                "      - name: 'Shell: deployment'\n" +
                "        uses: actions/github-script@v4\n" +
                "        with:\n" +
                "          github-token: ${{ secrets.GITHUB_TOKEN }}\n" +
                "          script: |\n" +
                "            await github.actions.createWorkflowDispatch({\n" +
                "                owner: context.repo.owner,\n" +
                "                repo: context.repo.repo,\n" +
                "                workflow_id: 'release-workflow.yml',\n" +
                "                ref: context.ref,\n" +
                "                inputs: {\n" +
                "                  VERSION: \"$VERSION\"\n" +
                "                }\n" +
                "            });\n" +
                "\n" +
                "  readyness:\n" +
                "    name: Readiness Check\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ deployment ]\n" +
                "    timeout-minutes: 15\n" +
                "    steps:\n" +
                "      - name: 'Shell: readiness'\n" +
                "        run: while [[ \"$(curl -H X-API-KEY:$API_KEY -s $SERVICE_URL | jq -r '.commitId')\" != \"$GITHUB_SHA\" ]]; do sleep 10; done\n" +
                "\n", answer);
    }

    @Test
    void generateMasterWorkflowFile() throws Exception {

        when(project.getBasedir()).thenReturn(basePath);
        when(basePath.getPath()).thenReturn(".");
        when(project.getName()).thenReturn("github-workflows-maven-plugin Maven Mojo");
        when(project.getArtifactId()).thenReturn("github-workflows-maven-plugin");
        when(project.getVersion()).thenReturn("1.1.0-SNAPSHOT");
        when(project.getProperties()).thenReturn(properties);

        sut.stages.put("prod", "master");

        pipelineFile = new File(sut.githubWorkflowsDir, "master-workflow.yaml");

        sut.execute();

        String answer = FileUtils.readFileToString(pipelineFile, "UTF-8");

        assertEquals("name: github-workflows-maven-plugin Maven Mojo [PROD]\n" +
                "\n" +
                "on:\n" +
                "  push:\n" +
                "    branches:\n" +
                "      - master\n" +
                "  pull_request:\n" +
                "    branches:\n" +
                "      - master\n" +
                "\n" +
                "env:\n" +
                "  DOCKER_REGISTRY: \"docker.registry.local\"\n" +
                "  SERVICE_URL: \"http://localhost:8080\"\n" +
                "  ENV_STAGE_NAME: \"ENV_PROD\"\n" +
                "  APP_NAME: \"github-workflows-maven-plugin\"\n" +
                "  GITHUB_TOKEN: \"${{ secrets.GITHUB_TOKEN }}\"\n" +
                "  SONAR_TOKEN: \"${{ secrets.SONAR_TOKEN }}\"\n" +
                "  JAVA_VERSION: \"17.x\"\n" +
                "  MAVEN_CLI_OPTS: \"--batch-mode --errors --fail-at-end --show-version -DinstallAtEnd=true\\\n" +
                "    \\ -DdeployAtEnd=true\"\n" +
                "  VERSION: \"1.1.0\"\n" +
                "  STAGE_NAME: \"prod\"\n" +
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
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Maven: versions:set'\n" +
                "        run: |\n" +
                "          mvn release:update-versions -DdevelopmentVersion=0.0.1-SNAPSHOT $MAVEN_CLI_OPTS\n" +
                "          mvn versions:set -DnewVersion=$VERSION $MAVEN_CLI_OPTS\n" +
                "      - name: 'Artifact: prepare'\n" +
                "        run: |\n" +
                "          mkdir -p artifact\n" +
                "          cp pom.xml artifact/pom.xml\n" +
                "      - name: 'Artifact: upload'\n" +
                "        uses: actions/upload-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "          path: artifact/pom.xml\n" +
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
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Artifact: download'\n" +
                "        if: true\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "      - name: 'Maven: compile'\n" +
                "        run: mvn compile $MAVEN_CLI_OPTS\n" +
                "\n" +
                "  security-check:\n" +
                "    name: Security Check\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ compile ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Maven: dependency-check'\n" +
                "        run: mvn dependency-check:help -P security -Ddownloader.quick.query.timestamp=false $MAVEN_CLI_OPTS\n" +
                "\n" +
                "  unit-test:\n" +
                "    name: Unit Test\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ compile ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Artifact: download'\n" +
                "        if: true\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "      - name: 'Maven: test'\n" +
                "        run: mvn test $MAVEN_CLI_OPTS\n" +
                "      - name: 'Artifact: prepare'\n" +
                "        run: |\n" +
                "          mkdir -p artifact/target/surefire-reports\n" +
                "          cp -r target/surefire-reports/* artifact/target/surefire-reports/\n" +
                "          cp -r target/jacoco.exec artifact/target/surefire-reports/\n" +
                "      - name: 'Test result'\n" +
                "        uses: actions/upload-artifact@v2\n" +
                "        with:\n" +
                "          name: target-artifact\n" +
                "          path: artifact\n" +
                "\n" +
                "  it-test:\n" +
                "    name: Integration Test\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ compile ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Artifact: download'\n" +
                "        if: true\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "      - name: 'Maven: integration-test'\n" +
                "        run: mvn integration-test -P it -DtestType=IT -DsourceType=it $MAVEN_CLI_OPTS\n" +
                "      - name: 'Artifact: prepare'\n" +
                "        run: |\n" +
                "          mkdir -p artifact/target/surefire-reports/it\n" +
                "          cp -r target/surefire-reports/* artifact/target/surefire-reports/it/\n" +
                "          cp -r target/jacoco.exec artifact/target/surefire-reports/it/\n" +
                "      - name: 'Test result'\n" +
                "        uses: actions/upload-artifact@v2\n" +
                "        with:\n" +
                "          name: target-artifact\n" +
                "          path: artifact\n" +
                "\n" +
                "  quality-gate:\n" +
                "    name: Quality Gate\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ unit-test, it-test ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Artifact: download'\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: target-artifact\n" +
                "      - name: 'Maven: verify'\n" +
                "        run: mvn verify -DskipTests=true -Dcode.coverage=0.00 $MAVEN_CLI_OPTS\n" +
                "      - name: 'Maven: sonar'\n" +
                "        run: mvn sonar:sonar -Dsonar.login=$SONAR_TOKEN $MAVEN_CLI_OPTS\n" +
                "\n" +
                "  build:\n" +
                "    name: Build\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ quality-gate, security-check ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v2\n" +
                "        with:\n" +
                "          distribution: 'adopt'\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Artifact: download'\n" +
                "        if: true\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "      - name: 'Maven: package'\n" +
                "        run: mvn package -P prod -Dcode.coverage=0.00 -DskipTests=true -DskipUTs=true -DskipITs=true $MAVEN_CLI_OPTS\n" +
                "      - name: 'Artifact: prepare'\n" +
                "        run: |\n" +
                "          mkdir -p artifact/target\n" +
                "          cp target/*.jar artifact/target/\n" +
                "      - name: 'Artifact: upload'\n" +
                "        uses: actions/upload-artifact@v2\n" +
                "        with:\n" +
                "          name: target-artifact\n" +
                "          path: artifact/target\n" +
                "\n" +
                "  package:\n" +
                "    name: Package\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ build ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v2\n" +
                "        with:\n" +
                "          distribution: 'adopt'\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Artifact: download'\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: target-artifact\n" +
                "      - name: 'Artifact: download'\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "      - name: 'Docker: login'\n" +
                "        run: docker login -u $DOCKER_REGISTRY_USER -p $DOCKER_REGISTRY_PASSWORD $DOCKER_REGISTRY\n" +
                "      - name: 'Docker: build'\n" +
                "        run: mvn compile jib:dockerBuild -Dimage=$DOCKER_REGISTRY/$APP_NAME -Djib.to.tags=$VERSION.$GITHUB_SHA $MAVEN_CLI_OPTS\n" +
                "      - name: 'Docker: push'\n" +
                "        run: docker push $DOCKER_REGISTRY/$APP_NAME:$VERSION.$GITHUB_SHA\n" +
                "\n" +
                "  tag:\n" +
                "    name: Tag Release\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ db-migration ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Artifact: download'\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: pom-artifact\n" +
                "      - name: 'Bump version and push tag'\n" +
                "        id: tag_version\n" +
                "        uses: mathieudutour/github-tag-action@v6.0\n" +
                "        with:\n" +
                "          github_token: ${{ secrets.GITHUB_TOKEN }}\n" +
                "      - name: Create a GitHub release\n" +
                "        uses: ncipollo/release-action@v1\n" +
                "        with:\n" +
                "          tag: ${{ steps.tag_version.outputs.new_tag }}\n" +
                "          name: Release ${{ steps.tag_version.outputs.new_tag }}\n" +
                "          body: ${{ steps.tag_version.outputs.changelog }}\n" +
                "\n" +
                "  db-migration:\n" +
                "    name: Database Changelog\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ package ]\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Liquibase: changelog'\n" +
                "        run: echo 'TBD'\n" +
                "\n" +
                "  promote:\n" +
                "    name: Promote\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ tag ]\n" +
                "    steps:\n" +
                "      - name: 'Shell: promote'\n" +
                "        run: echo 'TBD'\n" +
                "\n" +
                "  deployment:\n" +
                "    name: Deployment\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ promote ]\n" +
                "    steps:\n" +
                "      - name: 'Artifact: download'\n" +
                "        uses: actions/download-artifact@v2\n" +
                "        with:\n" +
                "          name: version-artifact\n" +
                "      - name: 'Shell: set env'\n" +
                "        run: export DOCKER_IMAGE_TAG=$(cat version)\n" +
                "      - name: 'Shell: deployment'\n" +
                "        uses: actions/github-script@v4\n" +
                "        with:\n" +
                "          github-token: ${{ secrets.GITHUB_TOKEN }}\n" +
                "          script: |\n" +
                "            await github.actions.createWorkflowDispatch({\n" +
                "                owner: context.repo.owner,\n" +
                "                repo: context.repo.repo,\n" +
                "                workflow_id: 'master-workflow.yml',\n" +
                "                ref: context.ref,\n" +
                "                inputs: {\n" +
                "                  VERSION: \"$VERSION\"\n" +
                "                }\n" +
                "            });\n" +
                "\n" +
                "  readyness:\n" +
                "    name: Readiness Check\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ deployment ]\n" +
                "    timeout-minutes: 15\n" +
                "    steps:\n" +
                "      - name: 'Shell: readiness'\n" +
                "        run: while [[ \"$(curl -H X-API-KEY:$API_KEY -s $SERVICE_URL | jq -r '.commitId')\" != \"$GITHUB_SHA\" ]]; do sleep 10; done\n" +
                "\n", answer);
    }
}
