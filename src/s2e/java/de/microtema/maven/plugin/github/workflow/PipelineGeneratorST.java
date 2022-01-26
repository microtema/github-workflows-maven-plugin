package de.microtema.maven.plugin.github.workflow;

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PipelineGeneratorST {

    PipelineGeneratorUtil sut;

    @Mock
    MavenProject project;

    @Mock
    File basePath;

    @Test
    void hasSourceCode() {

        when(project.getBasedir()).thenReturn(basePath);
        when(basePath.getPath()).thenReturn("./target");
        when(project.getModules()).thenReturn(Collections.emptyList());

        boolean answer = PipelineGeneratorUtil.hasSourceCode(project);

        assertFalse(answer);
    }

    @Test
    void existsIntegrationTests() {

        when(project.getBasedir()).thenReturn(basePath);
        when(basePath.getPath()).thenReturn("./target");
        when(project.getModules()).thenReturn(Collections.emptyList());

        boolean answer = PipelineGeneratorUtil.existsIntegrationTests(project);

        assertFalse(answer);
    }

    @Test
    void existsRegressionTests() {

        when(project.getBasedir()).thenReturn(basePath);
        when(basePath.getPath()).thenReturn("./target");
        when(project.getModules()).thenReturn(Collections.emptyList());

        boolean answer = PipelineGeneratorUtil.existsRegressionTests(project, "s2e");

        assertFalse(answer);
    }
}
