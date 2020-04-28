/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018-2020 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 *
 */
package org.jboss.projectmanipulator.npm;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.projectmanipulator.core.ManipulationException;
import org.jboss.projectmanipulator.core.Project;
import org.junit.Test;

/**
 * Test class for {@link NpmPackageVersionManipulator}.
 *
 * @author pkocandr
 */
public class NpmPackageVersionManipulatorTest {

    /**
     * Tests that the {@link NpmPackageVersionManipulator#findHighestIncrementalNum(String, Set)} returns 0 when the available
     * version set is empty.
     */
    @Test
    public void findHighestIncrementalNumWithNoAvailableVersions() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator("jboss", 5, null, null);

        Set<String> availableSet = new HashSet<>();
        int highestNum = manipulator.findHighestIncrementalNum("1.0.0", availableSet);

        assertThat(0, is(highestNum));
    }

    /**
     * Tests that the {@link NpmPackageVersionManipulator#findHighestIncrementalNum(String, Set)} returns 0 when the available
     * version set is not empty, but contains only versions with non-matching major-minor-patch combination or with matching
     * one, but non-matching suffix.
     */
    @Test
    public void findHighestIncrementalNumWithNoMatchingAvailableVersions() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator("jboss", 5, null, null);

        Set<String> availableSet = new HashSet<>();
        availableSet.add("1.0.1-jboss-1");
        availableSet.add("1.0.0-ncl-1");
        int highestNum = manipulator.findHighestIncrementalNum("1.0.0", availableSet);

        assertThat(0, is(highestNum));
    }

    /**
     * Tests that the {@link NpmPackageVersionManipulator#findHighestIncrementalNum(String, Set)} returns 2 when the available
     * version set is not empty and contains a version with non-matching suffix and 2 matching ones with 00001 and 00002 after
     * it.
     */
    @Test
    public void findHighestIncrementalNumWithMatchingAvailableVersion() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator("jboss", 5, null, null);

        Set<String> availableSet = new HashSet<>();
        availableSet.add("1.0.0-jboss-00001");
        availableSet.add("1.0.0-jboss-00002");
        availableSet.add("1.0.0-ncl-1");
        int highestNum = manipulator.findHighestIncrementalNum("1.0.0", availableSet);

        assertThat(2, is(highestNum));
    }

    /**
     * Tests generation of new version when there is no pre-existing suffixed version. For version 1.0.0 it expects to get
     * 1.0.0-jboss-00001.
     */
    @Test
    public void generateNewVersionWhenNoSuffixedExists() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator("jboss", 5, null, null);

        String newVersion = manipulator.generateNewVersion("1.0.0", Collections.emptySet());

        assertThat("1.0.0-jboss-00001", is(newVersion));
    }

    /**
     * Tests generation of new version when there are pre-existing suffixed versions - jboss-1 and jboss-00002. For version
     * 1.0.0 it expects to get 1.0.0-jboss-00003.
     */
    @Test
    public void generateNewVersionWithAvailableSuffixedVersions() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator("jboss", 5, null, null);

        Set<String> availableSet = new HashSet<>();
        availableSet.add("1.0.0-jboss-1");
        availableSet.add("1.0.0-jboss-00002");
        String newVersion = manipulator.generateNewVersion("1.0.0", availableSet);

        assertThat("1.0.0-jboss-00003", is(newVersion));
    }

    /**
     * Tests generation of new version for version that already contains the suffix when there are pre-existing suffixed
     * versions - jboss-1 and jboss-00002. It expects that the original suffix will be removed and replaced by the generated one
     * based on the highest available version, so for version 1.0.0-jboss-00004 it expects to get 1.0.0-jboss-00003.
     */
    @Test
    public void generateNewVersionForSuffixedVersionWithPreexistingSuffixedVersions() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator("jboss", 5, null, null);

        Set<String> availableSet = new HashSet<>();
        availableSet.add("1.0.0-jboss-1");
        availableSet.add("1.0.0-jboss-00002");
        String newVersion = manipulator.generateNewVersion("1.0.0-jboss-00004", availableSet);

        assertThat("1.0.0-jboss-00003", is(newVersion));
    }

    /**
     * Tests getting new version when the complete version is set to an override. It sets also versionSuffix and
     * versionSuffixOverride, which should be both ignored and the result should be only the overriden version.
     */
    @Test
    public void getNewVersionOverride() {
        String versionOverride = "2.0.0-foo-001";
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator("jboss", 5, "bar-02", versionOverride);

        Set<String> availableSet = new HashSet<>();
        availableSet.add("1.0.0-jboss-1");
        String newVersion = manipulator.getNewVersion("1.0.0", availableSet);

        assertThat(versionOverride, is(newVersion));
    }

    /**
     * Tests applying the version update with an override. It sets also versionSuffix and versionSuffixOverride, which should be
     * both ignored and the result should be only the overriden version.
     *
     * @throws ManipulationException in case of an error
     */
    @Test
    public void applyChangesWithVersionOverride() throws ManipulationException {
        String versionOverride = "2.0.0-foo-001";
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator(null, null, null, versionOverride);

        List<Project> projects = new ArrayList<>();
        projects.add(new NpmPackage() {
            private String version = "1.0.0";

            @Override
            public void update() throws ManipulationException {
                // do nothing
            }

            @Override
            public String getName() throws ManipulationException {
                return "test";
            }

            @Override
            public String getVersion() throws ManipulationException {
                return version;
            }

            @Override
            public void setVersion(String version) throws ManipulationException {
                this.version = version;
            }

            @Override
            public Map<String, String> getDependencies() throws ManipulationException {
                return Collections.emptyMap();

            }

            @Override
            public Map<String, String> getDevDependencies() throws ManipulationException {
                return Collections.emptyMap();
            }

            @Override
            public void setDependencyVersion(String dependencyName, String version, boolean isDevelopment)
                    throws ManipulationException {
            }

        });
        Set<Project> changed = manipulator.applyChanges(projects);

        assertThat(1, is(changed.size()));
        assertThat(versionOverride, is(((NpmPackage) changed.iterator().next()).getVersion()));
    }

    /**
     * Tests applying the dependency version update with an override.
     *
     * @throws ManipulationException in case of an error
     */
    @Test
    public void applyChangesWithDependencyOverride() throws ManipulationException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("package.json");
        File packageJson = new File(url.getPath());

        assertTrue(packageJson.exists());
        assertTrue(packageJson.isFile());

        Map<String, String> userDependenciesMap = new LinkedHashMap<String, String>();
        userDependenciesMap.put("archiver", "1.2.0");
        userDependenciesMap.put("async", "1.5.2");
        userDependenciesMap.put("body-parser", "1.18.2");
        userDependenciesMap.put("cors", "2.7.1");
        userDependenciesMap.put("express", "4.16.3");
        userDependenciesMap.put("express-bunyan-logger", "1.3.3");
        userDependenciesMap.put("keycloak-admin-client", "^0.12.0");

        Map<String, String> userDevDependenciesMap = new LinkedHashMap<String, String>();
        userDevDependenciesMap.put("deep-equal", "~1.0.1");
        userDevDependenciesMap.put("express", "4.16.5");
        userDevDependenciesMap.put("grunt", "~1.0.1");
        userDevDependenciesMap.put("grunt-fh-build", "~2.0.0");
        userDevDependenciesMap.put("istanbul", "0.4.5-redhat-00001");

        NpmDependenciesManipulator manipulator = new NpmDependenciesManipulator(userDependenciesMap, userDevDependenciesMap);

        List<Project> projects = new ArrayList<>();
        NpmPackage npmPackage = new NpmPackageImpl(packageJson, null);

        assertEquals("pnc-example", npmPackage.getName());
        assertEquals("2.0.0-BUILD-NUMBER", npmPackage.getVersion());
        assertEquals(5, npmPackage.getDependencies().size());
        assertEquals(5, npmPackage.getDevDependencies().size());

        projects.add(npmPackage);

        Set<Project> changed = manipulator.applyChanges(projects);

        assertThat(1, is(changed.size()));
        NpmPackage changedProject = (NpmPackage) changed.iterator().next();

        assertEquals(changedProject.getDependencies().size(), 5);
        assertEquals(changedProject.getDevDependencies().size(), 5);
        // Overridden
        assertEquals("1.2.0", changedProject.getDependencies().get("archiver"));
        assertEquals("2.7.1", changedProject.getDependencies().get("cors"));
        assertEquals("1.3.3", changedProject.getDependencies().get("express-bunyan-logger"));
        assertEquals("^0.12.0", changedProject.getDependencies().get("keycloak-admin-client"));

        // Not overridden
        assertEquals("4.16.3", changedProject.getDependencies().get("express"));

        // Not existing (user provided but not in package.json)
        assertNull(changedProject.getDependencies().get("async"));
        assertNull(changedProject.getDependencies().get("body-parser"));

        // Overridden
        assertEquals("4.16.5", changedProject.getDevDependencies().get("express"));
        assertEquals("~1.0.1", changedProject.getDevDependencies().get("grunt"));
        assertEquals("~2.0.0", changedProject.getDevDependencies().get("grunt-fh-build"));
        assertEquals("0.4.5-redhat-00001", changedProject.getDevDependencies().get("istanbul"));

        // Not overridden
        assertEquals("~1.0.1", changedProject.getDevDependencies().get("deep-equal"));
    }
}
