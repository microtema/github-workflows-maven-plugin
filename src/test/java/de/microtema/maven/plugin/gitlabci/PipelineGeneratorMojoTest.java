package de.microtema.maven.plugin.gitlabci;

import org.apache.commons.io.FileUtils;
import org.apache.maven.project.MavenProject;
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

        sut.stages.put("local", "feature/*");
        sut.stages.put("dev", "develop");
        sut.stages.put("int", "release/*,hotfix/*");
        sut.stages.put("prod", "master");

        sut.clusters.put("DEV", "dev");
        sut.clusters.put("STAGE", "release");
        sut.clusters.put("PROD", "prod");

        sut.variables.put("KUBERNETES_VERSION", "${KUBERNETES_VERSION:-1.11}");
        sut.variables.put("SERVICE_PORT_2376_TCP_PORT", "2375");
        sut.variables.put("DOCKER_REGISTRY", "docker.registry.local");

        sut.serviceUrl = "http://$STAGE.$CLUSTER.local/supplier/git/info";
    }

    @Test
    void executeOnNonUpdateFalse() throws Exception {

        when(project.getBasedir()).thenReturn(basePath);
        when(basePath.getPath()).thenReturn(".");
        when(project.getName()).thenReturn("github-workflows-maven-plugin Maven Mojo");
        //  when(project.getProperties()).thenReturn(properties);
        // Map<Object, Object> stringStringMap = Collections.singletonMap("sonar.url", "http://localhost:9000");
        // when(properties.entrySet()).thenReturn(stringStringMap.entrySet());

        sut.stages.put("dev", "develop");

        pipelineFile = new File(sut.githubWorkflowsDir, "develop.yaml");

        sut.execute();

        String answer = FileUtils.readFileToString(pipelineFile, "UTF-8");

        assertEquals("name: github-workflows-maven-plugin Maven Mojo\n" +
                "\n", answer);
    }

    @Test
    void unMask() {

        String answer = sut.unMask("\"${KUBERNETES_VERSION:-1.11}\"");

        assertEquals("${KUBERNETES_VERSION:-1.11}", answer);
    }
}
