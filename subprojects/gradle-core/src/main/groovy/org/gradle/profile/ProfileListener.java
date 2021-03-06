/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.profile;

import org.gradle.BuildListener;
import org.gradle.BuildResult;
import org.gradle.api.*;
import org.gradle.api.execution.TaskExecutionListener;
import org.gradle.api.initialization.Settings;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.tasks.TaskState;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ProfileListener implements BuildListener, ProjectEvaluationListener, TaskExecutionListener {
    private BuildProfile buildProfile;
    private static final String OUTPUT_DIR = "reports/profile/";
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    private final List<ProfileOutput> outputs = new ArrayList<ProfileOutput>();

    private final long profileStarted;

    public ProfileListener(long profileStarted) {
        this.profileStarted = profileStarted;
        outputs.add(new ProfileOutput("profile.html", "ProfileTemplate.html"));
        outputs.add(new ProfileOutput("profile.txt", "ProfileTemplate.txt"));
        outputs.add(new ProfileOutput("profile.csv", "ProfileTemplate.csv"));
    }

    // BuildListener

    public void buildStarted(Gradle gradle) {
        buildProfile = new BuildProfile(gradle);
        buildProfile.setBuildStarted(System.currentTimeMillis());
        buildProfile.setProfilingStarted(profileStarted);
    }

    public void settingsEvaluated(Settings settings) {
        buildProfile.setSettingsEvaluated(System.currentTimeMillis());
    }

    public void projectsLoaded(Gradle gradle) {
        buildProfile.setProjectsLoaded(System.currentTimeMillis());
    }

    public void projectsEvaluated(Gradle gradle) {
        buildProfile.setProjectsEvaluated(System.currentTimeMillis());
    }

    public void buildFinished(BuildResult result) {
        buildProfile.setBuildFinished(System.currentTimeMillis());
        ProfileFileReport report = new ProfileFileReport(buildProfile);
        final File buildDir = result.getGradle().getRootProject().getBuildDir();
        for (ProfileOutput profile : outputs) {
            File file = new File(buildDir, OUTPUT_DIR + profile.getFilePrefix() + "-" +
                    FILE_DATE_FORMAT.format(new Date(profileStarted)) + profile.getFileExtension());
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
                report.writeTo(file, profile.getTemplateFile());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    // ProjectEvaluationListener

    public void beforeEvaluate(Project project) {
        buildProfile.getProjectProfile(project).setBeforeEvaluate(System.currentTimeMillis());
    }

    public void afterEvaluate(Project project, ProjectState state) {
        ProjectProfile projectProfile = buildProfile.getProjectProfile(project);
        projectProfile.setAfterEvaluate(System.currentTimeMillis());
        projectProfile.setState(state);
    }

    // TaskExecutionListener

    public void beforeExecute(Task task) {
        Project project = task.getProject();
        ProjectProfile projectProfile = buildProfile.getProjectProfile(project);
        projectProfile.getTaskProfile(task).setStart(System.currentTimeMillis());
    }

    public void afterExecute(Task task, TaskState state) {
        Project project = task.getProject();
        ProjectProfile projectProfile = buildProfile.getProjectProfile(project);
        TaskProfile taskProfile = projectProfile.getTaskProfile(task);
        taskProfile.setFinish(System.currentTimeMillis());
        taskProfile.setState(state);
    }

    /**
     * Container for easier manipulation of outputs. Holds information on output file and template that will be used to write into this file.
     */
    class ProfileOutput {

        private final String outputFile;
        private final String templateFile;

        public ProfileOutput(String outputFile, String templateFile) {
            this.outputFile = outputFile;
            this.templateFile = templateFile;
        }

        public String getFilePrefix() {
            return outputFile.substring(0, outputFile.lastIndexOf('.'));
        }

        public String getFileExtension() {
            return outputFile.substring(outputFile.lastIndexOf('.'));
        }

        public String getTemplateFile() {
            return templateFile;
        }
    }
}

