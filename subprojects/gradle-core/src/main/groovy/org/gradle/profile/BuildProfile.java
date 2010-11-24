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

import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;

import java.util.*;

/**
 * Root container for profile information about a build.  This includes summary
 * information about the overall build timing and collection of project specific
 * information.  All timing information is stored as milliseconds since epoch times.
 * <p>
 * Setters are expected to be called in the following order:
 * <ul>
 * <li>setProfilingStarted</li>
 * <li>setBuildStarted</li>
 * <li>setSettingsEvaluated</li>
 * <li>setProjectsLoaded</li>
 * <li>setProjectsEvaluated</li>
 * <li>setBuildFinished</li>
 * </ul>
 */
public class BuildProfile {
    private final Gradle gradle;
    final Map<Project, ProjectProfile> projects = new HashMap<Project, ProjectProfile>();
    long profilingStarted;
    long buildStarted;
    long settingsEvaluated;
    long projectsLoaded;
    long projectsEvaluated;
    long buildFinished;

    public BuildProfile(Gradle gradle) {
        this.gradle = gradle;
    }

    /**
     * Get a description of the tasks passed to gradle as targets from the command line
     * @return
     */
    public String getTaskDescription() {
        StringBuilder result = new StringBuilder();
        for (String name : gradle.getStartParameter().getExcludedTaskNames()) {
            result.append("-x");
            result.append(name);
            result.append(" ");
        }
        for (String name : gradle.getStartParameter().getTaskNames()) {
            result.append(name);
            result.append(" ");
        }
        return result.toString();
    }

    /**
     * Get the profiling container for the specified project
     * @param project to look up
     * @return
     */
    public ProjectProfile getProjectProfile(Project project) {
        ProjectProfile result = projects.get(project);
        if (result == null) {
            result = new ProjectProfile(project);
            projects.put(project, result);
        }
        return result;
    }

    /**
     * Get a list of the profiling containers for all projects
     * @return list
     */
    public List<ProjectProfile> getProjects() {
        return new ArrayList<ProjectProfile>(projects.values());
    }

    /**
     * Should be set with a time as soon as possible after startup.
     * @param profilingStarted
     */
    public void setProfilingStarted(long profilingStarted) {
        this.profilingStarted = profilingStarted;
    }

    /**
     * Should be set with a timestamp from a {@link org.gradle.BuildListener#buildStarted}
     * callback.
     * @param buildStarted
     */
    public void setBuildStarted(long buildStarted) {
        this.buildStarted = buildStarted;
    }

    /**
     * Should be set with a timestamp from a {@link org.gradle.BuildListener#settingsEvaluated}
     * callback.
     * @param settingsEvaluated
     */
    public void setSettingsEvaluated(long settingsEvaluated) {
        this.settingsEvaluated = settingsEvaluated;
    }

    /**
     * Should be set with a timestamp from a {@link org.gradle.BuildListener#projectsLoaded}
     * callback.
     * @param projectsLoaded
     */
    public void setProjectsLoaded(long projectsLoaded) {
        this.projectsLoaded = projectsLoaded;
    }

    /**
     * Should be set with a timestamp from a {@link org.gradle.BuildListener#projectsEvaluated}
     * callback.
     * @param projectsEvaluated
     */
    public void setProjectsEvaluated(long projectsEvaluated) {
        this.projectsEvaluated = projectsEvaluated;
    }

    /**
     * Should be set with a timestamp from a {@link org.gradle.BuildListener#buildFinished}
     * callback.
     * @param buildFinished
     */
    public void setBuildFinished(long buildFinished) {
        this.buildFinished = buildFinished;
    }

    /**
     * Get the elapsed time (in mSec) between the start of profiling and the buildStarted event.
     * @return
     */
    public long getElapsedStartup() {
        return buildStarted - profilingStarted;
    }

    /**
     * Get the total elapsed time (in mSec) between the start of profiling and the buildFinished event.
     * @return
     */
    public long getElapsedTotal() { 
    return buildFinished - profilingStarted;
    }

    /**
     * Get the elapsed time (in mSec) between the buildStarted event and the settingsEvaluated event.
     * Note that this will include processing of buildSrc as well as the settings file.
     * @return
     */
    public long getElapsedSettings() {
        return settingsEvaluated - buildStarted;
    }

    /**
     * Get the elapsed time (in mSec) between the settingsEvaluated event and the projectsLoaded event.
     * @return
     */
    public long getElapsedProjectsLoading() {
        return projectsLoaded - settingsEvaluated;
    }

    /**
     * Get the elapsed time (in mSec) between the projectsEvaluated event and the projectsLoaded event.
     * @return
     */
    public long getElapsedProjectsEvaluated() {
        return projectsEvaluated - projectsLoaded;
    }

    /**
     * Get the elapsed time (in mSec) between the buildFinished event and the projectsEvaluated event.
     * @return
     */
    public long getElapsedAfterProjectsEvaluated() {
        return buildFinished - projectsEvaluated;
    }

    /**
     * Get the total task execution time from all projects.
     * @return sum of execution times of all tasks of all projects (in millis)
     */
    public long getElapsedTotalExecutionTime() {
        long result = 0;
        for (ProjectProfile projectProfile : projects.values()) {
            result += projectProfile.getElapsedTaskExecution();
        }
        return result;
    }

    /** Returns max execution time of a project. Useful for deciding the max length of bar chart.
     *
     * @return max execution time of a project (in millis)
     */
    public long getMaxProject() {
        long max = 0;
        for (ProjectProfile projectProfile : projects.values()) {
            if (projectProfile.getElapsedTaskExecution() > max) {
                max = projectProfile.getElapsedTaskExecution();
            }
        }
        return max;
    }
}
