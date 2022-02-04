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
                "\n" +
                "env:\n" +
                "  APP_NAME: \"github-workflows-maven-plugin\"\n" +
                "  GITHUB_TOKEN: \"${{ secrets.GITHUB_TOKEN }}\"\n" +
                "  SONAR_TOKEN: \"${{ secrets.SONAR_TOKEN }}\"\n" +
                "  JAVA_VERSION: \"17.x\"\n" +
                "  MAVEN_CLI_OPTS: \"--batch-mode --errors --fail-at-end --show-version -DinstallAtEnd=true\\\n" +
                "    \\ -DdeployAtEnd=true\"\n" +
                "  VERSION: \"1.1.0-SNAPSHOT\"\n" +
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
                "        run: mvn package -P prod -Dcode.coverage=0.00 -DskipTests=true $MAVEN_CLI_OPTS\n" +
                "      - name: 'Artifact: prepare'\n" +
                "        run: |\n" +
                "          mkdir -p artifact/target\n" +
                "          cp target/*.jar artifact/target/\n" +
                "      - name: 'Artifact: upload'\n" +
                "        uses: actions/upload-artifact@v2\n" +
                "        with:\n" +
                "          name: target-artifact\n" +
                "          path: artifact\n" +
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
        sut.downStreams.put("dev", "E2E Test:e2e-develop-workflow.yaml");

        pipelineFile = new File(sut.githubWorkflowsDir, "develop-workflow.yaml");

        sut.execute();

        String answer = FileUtils.readFileToString(pipelineFile, "UTF-8");

        assertEquals("name: github-workflows-maven-plugin Maven Mojo [DEV]\n" +
                "\n" +
                "on:\n" +
                "  push:\n" +
                "    branches:\n" +
                "      - develop\n" +
                "\n" +
                "env:\n" +
                "  APP_NAME: \"github-workflows-maven-plugin\"\n" +
                "  GITHUB_TOKEN: \"${{ secrets.GITHUB_TOKEN }}\"\n" +
                "  SONAR_TOKEN: \"${{ secrets.SONAR_TOKEN }}\"\n" +
                "  JAVA_VERSION: \"17.x\"\n" +
                "  MAVEN_CLI_OPTS: \"--batch-mode --errors --fail-at-end --show-version -DinstallAtEnd=true\\\n" +
                "    \\ -DdeployAtEnd=true\"\n" +
                "  VERSION: \"1.1.0-SNAPSHOT\"\n" +
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
                "        run: mvn package -P prod -Dcode.coverage=0.00 -DskipTests=true $MAVEN_CLI_OPTS\n" +
                "      - name: 'Artifact: prepare'\n" +
                "        run: |\n" +
                "          mkdir -p artifact/target\n" +
                "          cp target/*.jar artifact/target/\n" +
                "      - name: 'Artifact: upload'\n" +
                "        uses: actions/upload-artifact@v2\n" +
                "        with:\n" +
                "          name: target-artifact\n" +
                "          path: artifact\n" +
                "\n" +
                "  package:\n" +
                "    name: Package\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ build ]\n" +
                "    env:\n" +
                "      DOCKER_REGISTRY: %DOCKER_REGISTRY%\n" +
                "      DOCKER_REGISTRY_USER: %DOCKER_REGISTRY_USER%\n" +
                "      DOCKER_REGISTRY_PASSWORD: %DOCKER_REGISTRY_PASSWORD%\n" +
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
                "      - name: 'Docker: login'\n" +
                "        run: docker login -u $DOCKER_REGISTRY_USER -p $DOCKER_REGISTRY_PASSWORD $DOCKER_REGISTRY\n" +
                "      - name: 'Docker: build'\n" +
                "        run: docker build -t $DOCKER_REGISTRY/$APP_NAME:$VERSION .\n" +
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
                "  readiness:\n" +
                "    name: Readiness Check\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ deployment ]\n" +
                "    timeout-minutes: 15\n" +
                "    env:\n" +
                "      API_KEY: dev.key\n" +
                "      SERVICE_URL: http://dev:8080/git/info\n" +
                "    steps:\n" +
                "      - name: 'Shell: readiness'\n" +
                "        run: while [[ \"$(curl -H X-API-KEY:$API_KEY -s $SERVICE_URL | jq -r '.commitId')\" != \"$GITHUB_SHA\" ]]; do sleep 10; done\n" +
                "\n" +
                "  system-test:\n" +
                "    name: System Test\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ readiness ]\n" +
                "    env:\n" +
                "      API_KEY: dev.key\n" +
                "      STAGE_NAME: dev\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Maven: system test'\n" +
                "        run: mvn integration-test -P it -DtestType=ST -DsourceType=st -DstageName=$STAGE_NAME -DapiKey=$API_KEY $MAVEN_CLI_OPTS\n" +
                "\n" +
                "  performance-test:\n" +
                "    name: Performance Test\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ system-test, readiness ]\n" +
                "    env:\n" +
                "      API_KEY: dev.key\n" +
                "      STAGE_NAME: dev\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Maven: jmeter'\n" +
                "        run: mvn compile jmeter:jmeter -P performance-$STAGE_NAME -DstageName=$STAGE_NAME -DapiKey=$API_KEY $MAVEN_CLI_OPTS\n" +
                "\n" +
                "  downstream:\n" +
                "    name: 'E2E Test'\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ system-test, performance-test ]\n" +
                "    steps:\n" +
                "      - name: 'Shell: script'\n" +
                "        run: echo \"e2e-develop-workflow.yaml\"\n" +
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
                "\n" +
                "env:\n" +
                "  APP_NAME: \"github-workflows-maven-plugin\"\n" +
                "  GITHUB_TOKEN: \"${{ secrets.GITHUB_TOKEN }}\"\n" +
                "  SONAR_TOKEN: \"${{ secrets.SONAR_TOKEN }}\"\n" +
                "  JAVA_VERSION: \"17.x\"\n" +
                "  MAVEN_CLI_OPTS: \"--batch-mode --errors --fail-at-end --show-version -DinstallAtEnd=true\\\n" +
                "    \\ -DdeployAtEnd=true\"\n" +
                "  VERSION: \"1.1.0-RC\"\n" +
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
                "        run: mvn package -P prod -Dcode.coverage=0.00 -DskipTests=true $MAVEN_CLI_OPTS\n" +
                "      - name: 'Artifact: prepare'\n" +
                "        run: |\n" +
                "          mkdir -p artifact/target\n" +
                "          cp target/*.jar artifact/target/\n" +
                "      - name: 'Artifact: upload'\n" +
                "        uses: actions/upload-artifact@v2\n" +
                "        with:\n" +
                "          name: target-artifact\n" +
                "          path: artifact\n" +
                "\n" +
                "  package:\n" +
                "    name: Package\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ build ]\n" +
                "    env:\n" +
                "      DOCKER_REGISTRY: %DOCKER_REGISTRY%\n" +
                "      DOCKER_REGISTRY_USER: %DOCKER_REGISTRY_USER%\n" +
                "      DOCKER_REGISTRY_PASSWORD: %DOCKER_REGISTRY_PASSWORD%\n" +
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
                "      - name: 'Docker: login'\n" +
                "        run: docker login -u $DOCKER_REGISTRY_USER -p $DOCKER_REGISTRY_PASSWORD $DOCKER_REGISTRY\n" +
                "      - name: 'Docker: build'\n" +
                "        run: docker build -t $DOCKER_REGISTRY/$APP_NAME:$VERSION .\n" +
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
                "  readiness:\n" +
                "    name: Readiness Check\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ deployment ]\n" +
                "    timeout-minutes: 15\n" +
                "    env:\n" +
                "      API_KEY: stage.key\n" +
                "      SERVICE_URL: http://stage:8080/git/info\n" +
                "    steps:\n" +
                "      - name: 'Shell: readiness'\n" +
                "        run: while [[ \"$(curl -H X-API-KEY:$API_KEY -s $SERVICE_URL | jq -r '.commitId')\" != \"$GITHUB_SHA\" ]]; do sleep 10; done\n" +
                "\n" +
                "  system-test:\n" +
                "    name: System Test\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ readiness ]\n" +
                "    env:\n" +
                "      API_KEY: stage.key\n" +
                "      STAGE_NAME: stage\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Maven: system test'\n" +
                "        run: mvn integration-test -P it -DtestType=ST -DsourceType=st -DstageName=$STAGE_NAME -DapiKey=$API_KEY $MAVEN_CLI_OPTS\n" +
                "\n" +
                "  performance-test:\n" +
                "    name: Performance Test\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ system-test, readiness ]\n" +
                "    env:\n" +
                "      API_KEY: stage.key\n" +
                "      STAGE_NAME: stage\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Maven: jmeter'\n" +
                "        run: mvn compile jmeter:jmeter -P performance-$STAGE_NAME -DstageName=$STAGE_NAME -DapiKey=$API_KEY $MAVEN_CLI_OPTS\n" +
                "\n", answer);
    }

    @Test
    void generateMultipleReleaseWorkflowFile() throws Exception {

        when(project.getBasedir()).thenReturn(basePath);
        when(basePath.getPath()).thenReturn(".");
        when(project.getName()).thenReturn("github-workflows-maven-plugin Maven Mojo");
        when(project.getArtifactId()).thenReturn("github-workflows-maven-plugin");
        when(project.getVersion()).thenReturn("1.1.0-SNAPSHOT");
        when(project.getProperties()).thenReturn(properties);

        sut.stages.put("stage", "release/*");
        sut.stages.put("qa", "release/*");

        sut.downStreams.put("qa", "E2E Test:qa-e2e-workflow.yaml");

        pipelineFile = new File(sut.githubWorkflowsDir, "release-workflow.yaml");

        sut.execute();

        String answer = FileUtils.readFileToString(pipelineFile, "UTF-8");

        assertEquals("name: github-workflows-maven-plugin Maven Mojo [STAGE, QA]\n" +
                "\n" +
                "on:\n" +
                "  push:\n" +
                "    branches:\n" +
                "      - release/*\n" +
                "\n" +
                "env:\n" +
                "  APP_NAME: \"github-workflows-maven-plugin\"\n" +
                "  GITHUB_TOKEN: \"${{ secrets.GITHUB_TOKEN }}\"\n" +
                "  SONAR_TOKEN: \"${{ secrets.SONAR_TOKEN }}\"\n" +
                "  JAVA_VERSION: \"17.x\"\n" +
                "  MAVEN_CLI_OPTS: \"--batch-mode --errors --fail-at-end --show-version -DinstallAtEnd=true\\\n" +
                "    \\ -DdeployAtEnd=true\"\n" +
                "  VERSION: \"1.1.0-RC\"\n" +
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
                "        run: mvn package -P prod -Dcode.coverage=0.00 -DskipTests=true $MAVEN_CLI_OPTS\n" +
                "      - name: 'Artifact: prepare'\n" +
                "        run: |\n" +
                "          mkdir -p artifact/target\n" +
                "          cp target/*.jar artifact/target/\n" +
                "      - name: 'Artifact: upload'\n" +
                "        uses: actions/upload-artifact@v2\n" +
                "        with:\n" +
                "          name: target-artifact\n" +
                "          path: artifact\n" +
                "\n" +
                "  package:\n" +
                "    name: Package\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ build ]\n" +
                "    env:\n" +
                "      DOCKER_REGISTRY: %DOCKER_REGISTRY%\n" +
                "      DOCKER_REGISTRY_USER: %DOCKER_REGISTRY_USER%\n" +
                "      DOCKER_REGISTRY_PASSWORD: %DOCKER_REGISTRY_PASSWORD%\n" +
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
                "      - name: 'Docker: login'\n" +
                "        run: docker login -u $DOCKER_REGISTRY_USER -p $DOCKER_REGISTRY_PASSWORD $DOCKER_REGISTRY\n" +
                "      - name: 'Docker: build'\n" +
                "        run: docker build -t $DOCKER_REGISTRY/$APP_NAME:$VERSION .\n" +
                "      - name: 'Docker: push'\n" +
                "        run: docker push $DOCKER_REGISTRY/$APP_NAME:$VERSION\n" +
                "\n" +
                "  db-migration-stage:\n" +
                "    name: Database Changelog [STAGE]\n" +
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
                "  \n" +
                "  db-migration-qa:\n" +
                "    name: Database Changelog [QA]\n" +
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
                "  readiness-stage:\n" +
                "    name: Readiness Check [STAGE]\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ deployment-stage ]\n" +
                "    timeout-minutes: 15\n" +
                "    env:\n" +
                "      API_KEY: stage.key\n" +
                "      SERVICE_URL: http://stage:8080/git/info\n" +
                "    steps:\n" +
                "      - name: 'Shell: readiness'\n" +
                "        run: while [[ \"$(curl -H X-API-KEY:$API_KEY -s $SERVICE_URL | jq -r '.commitId')\" != \"$GITHUB_SHA\" ]]; do sleep 10; done\n" +
                "  \n" +
                "  readiness-qa:\n" +
                "    name: Readiness Check [QA]\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ deployment-qa ]\n" +
                "    timeout-minutes: 15\n" +
                "    env:\n" +
                "      API_KEY: qa.key\n" +
                "      SERVICE_URL: http://qa:8080/git/info\n" +
                "    steps:\n" +
                "      - name: 'Shell: readiness'\n" +
                "        run: while [[ \"$(curl -H X-API-KEY:$API_KEY -s $SERVICE_URL | jq -r '.commitId')\" != \"$GITHUB_SHA\" ]]; do sleep 10; done\n" +
                "\n" +
                "  system-test-stage:\n" +
                "    name: System Test [STAGE]\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ readiness-stage ]\n" +
                "    env:\n" +
                "      API_KEY: stage.key\n" +
                "      STAGE_NAME: stage\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Maven: system test'\n" +
                "        run: mvn integration-test -P it -DtestType=ST -DsourceType=st -DstageName=$STAGE_NAME -DapiKey=$API_KEY $MAVEN_CLI_OPTS\n" +
                "  \n" +
                "  system-test-qa:\n" +
                "    name: System Test [QA]\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ readiness-qa ]\n" +
                "    env:\n" +
                "      API_KEY: qa.key\n" +
                "      STAGE_NAME: qa\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Maven: system test'\n" +
                "        run: mvn integration-test -P it -DtestType=ST -DsourceType=st -DstageName=$STAGE_NAME -DapiKey=$API_KEY $MAVEN_CLI_OPTS\n" +
                "\n" +
                "  performance-test-stage:\n" +
                "    name: Performance Test [STAGE]\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ system-test-stage, readiness-stage ]\n" +
                "    env:\n" +
                "      API_KEY: stage.key\n" +
                "      STAGE_NAME: stage\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Maven: jmeter'\n" +
                "        run: mvn compile jmeter:jmeter -P performance-$STAGE_NAME -DstageName=$STAGE_NAME -DapiKey=$API_KEY $MAVEN_CLI_OPTS\n" +
                "  \n" +
                "  performance-test-qa:\n" +
                "    name: Performance Test [QA]\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ system-test-qa, readiness-qa ]\n" +
                "    env:\n" +
                "      API_KEY: qa.key\n" +
                "      STAGE_NAME: qa\n" +
                "    steps:\n" +
                "      - name: 'Checkout'\n" +
                "        uses: actions/checkout@v2\n" +
                "      - name: 'Java: Setup'\n" +
                "        uses: actions/setup-java@v1\n" +
                "        with:\n" +
                "          java-version: ${{ env.JAVA_VERSION }}\n" +
                "      - name: 'Maven: jmeter'\n" +
                "        run: mvn compile jmeter:jmeter -P performance-$STAGE_NAME -DstageName=$STAGE_NAME -DapiKey=$API_KEY $MAVEN_CLI_OPTS\n" +
                "\n" +
                "  downstream-qa-e2e-test:\n" +
                "    name: 'E2E Test [QA]'\n" +
                "    runs-on: [ self-hosted, azure-runners ]\n" +
                "    needs: [ system-test-qa, performance-test-qa ]\n" +
                "    steps:\n" +
                "      - name: 'Shell: script'\n" +
                "        run: echo \"qa-e2e-workflow.yaml\"\n" +
                "\n", answer);
    }
}
