/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.im.artifacts;

import com.codenvy.im.command.Command;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.install.InstallType;
import com.codenvy.im.service.InstallationManagerConfig;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.OSUtils;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonSyntaxException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
public class TestCDECArtifact {
    public static final String TEST_HOST_DNS = "localhost";

    private CDECArtifact spyCdecArtifact;
    public static final String initialOsVersion = OSUtils.VERSION;

    @Mock
    private HttpTransport mockTransport;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        spyCdecArtifact = spy(new CDECArtifact(mockTransport));

        Path initialImProperties = Paths.get(this.getClass().getClassLoader().getResource("im.properties").getPath());
        Path testImProperties = initialImProperties.getParent().resolve("im.properties.test");
        Files.copy(initialImProperties, testImProperties, StandardCopyOption.REPLACE_EXISTING);
        InstallationManagerConfig.CONFIG_FILE = testImProperties;
    }

    @AfterMethod
    public void tearDown() {
        OSUtils.VERSION = initialOsVersion;
    }

    @Test
    public void testGetInstallSingleServerInfo() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.CODENVY_SINGLE_SERVER);
        options.setStep(1);

        List<String> info = spyCdecArtifact.getInstallInfo(options);
        assertNotNull(info);
        assertTrue(info.size() > 1);
    }

    @Test
    public void testGetInstallMultiServerInfo() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.CODENVY_MULTI_SERVER);
        options.setStep(1);

        List<String> info = spyCdecArtifact.getInstallInfo(options);
        assertNotNull(info);
        assertTrue(info.size() > 1);
    }

    @Test
    public void testGetInstallSingleServerCommandOsVersion6() throws Exception {
        OSUtils.VERSION = "6";

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.CODENVY_SINGLE_SERVER);
        options.setConfigProperties(ImmutableMap.of("some property", "some value"));

        int steps = spyCdecArtifact.getInstallInfo(options).size();
        for (int i = 0; i < steps; i++) {
            options.setStep(i);
            Command command = spyCdecArtifact.getInstallCommand(null, Paths.get("some path"), options);
            assertNotNull(command);
        }
    }

    @Test
    public void testGetInstallSingleServerCommandOsVersion7() throws Exception {
        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.CODENVY_SINGLE_SERVER);
        options.setConfigProperties(ImmutableMap.of("some property", "some value"));

        int steps = spyCdecArtifact.getInstallInfo(options).size();
        for (int i = 0; i < steps; i++) {
            options.setStep(i);
            Command command = spyCdecArtifact.getInstallCommand(null, Paths.get("some path"), options);
            assertNotNull(command);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetInstallSingleServerCommandError() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setInstallType(InstallType.CODENVY_SINGLE_SERVER);
        options.setStep(Integer.MAX_VALUE);

        spyCdecArtifact.getInstallCommand(null, Paths.get("some path"), options);
    }

    @Test
    public void testGetInstallMultiServerCommandsForMultiServer() throws IOException {
        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.CODENVY_MULTI_SERVER);
        options.setConfigProperties(ImmutableMap.of("site_host_name", "site.example.com"));

        int steps = spyCdecArtifact.getInstallInfo(options).size();
        for (int i = 0; i < steps; i++) {
            options.setStep(i);
            Command command = spyCdecArtifact.getInstallCommand(null, Paths.get("some path"), options);
            assertNotNull(command);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Site node config not found.")
    public void testGetInstallMultiServerCommandsForMultiServerError() throws IOException {
        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.CODENVY_MULTI_SERVER);
        options.setConfigProperties(ImmutableMap.of("some property", "some value"));

        int steps = spyCdecArtifact.getInstallInfo(options).size();
        for (int i = 0; i < steps; i++) {
            options.setStep(i);
            Command command = spyCdecArtifact.getInstallCommand(null, Paths.get("some path"), options);
            assertNotNull(command);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Step number .* is out of install range")
    public void testGetInstallMultiServerCommandUnexistedStepError() throws Exception {
        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setInstallType(InstallType.CODENVY_MULTI_SERVER);
        options.setStep(Integer.MAX_VALUE);

        spyCdecArtifact.getInstallCommand(null, Paths.get("some path"), options);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testGetInstallMultiServerCommandsWrongOS() throws IOException {
        OSUtils.VERSION = "6";

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.CODENVY_MULTI_SERVER);
        options.setConfigProperties(ImmutableMap.of("some property", "some value"));
        options.setStep(1);

        spyCdecArtifact.getInstallCommand(null, null, options);
    }

    @Test
    public void testGetInstalledVersion() throws Exception {
        when(mockTransport.doOption("http://localhost/api/", null)).thenReturn("{\"ideVersion\":\"3.2.0-SNAPSHOT\"}");

        Version version = spyCdecArtifact.getInstalledVersion();
        assertEquals(version, Version.valueOf("3.2.0-SNAPSHOT"));
    }

    @Test
    public void testGetInstalledVersionReturnNullIfCDECNotInstalled() throws Exception {
        doThrow(new ConnectException()).when(mockTransport).doOption("http://localhost/api/", null);
        Version version = spyCdecArtifact.getInstalledVersion();
        assertNull(version);

        InstallationManagerConfig.CONFIG_FILE = Paths.get("unexisted");
        version = spyCdecArtifact.getInstalledVersion();
        assertNull(version);
    }

    @Test(expectedExceptions = JsonSyntaxException.class,
            expectedExceptionsMessageRegExp = ".*Expected ':' at line 1 column 14.*")
    public void testGetInstalledVersionError() throws Exception {
        when(mockTransport.doOption("http://localhost/api/", null)).thenReturn("{\"some text\"}");
        spyCdecArtifact.getInstalledVersion();
    }

    @Test
    public void testGetUpdateSingleServerCommand() throws Exception {
        when(mockTransport.doOption("http://localhost/api/", null)).thenReturn("{\"ideVersion\":\"1.0.0\"}");

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(ImmutableMap.of("some property", "some value"));
        options.setInstallType(InstallType.CODENVY_SINGLE_SERVER);

        int steps = spyCdecArtifact.getUpdateInfo(options).size();
        for (int i = 0; i < steps; i++) {
            options.setStep(i);
            Command command = spyCdecArtifact.getUpdateCommand(Version.valueOf("2.0.0"), Paths.get("some path"), options);
            assertNotNull(command);
        }
    }

    @Test
    public void testGetUpdateMultiServerCommand() throws Exception {
        when(mockTransport.doOption("http://localhost/api/", null)).thenReturn("{\"ideVersion\":\"1.0.0\"}");
        InstallationManagerConfig.storeProperty(InstallationManagerConfig.PUPPET_MASTER_HOST_NAME, "some");

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(ImmutableMap.of("some property", "some value"));
        options.setInstallType(InstallType.CODENVY_MULTI_SERVER);

        int steps = spyCdecArtifact.getUpdateInfo(options).size();
        for (int i = 0; i < steps; i++) {
            options.setStep(i);
            Command command = spyCdecArtifact.getUpdateCommand(Version.valueOf("2.0.0"), Paths.get("some path"), options);
            assertNotNull(command);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Step number .* is out of update range")
    public void testGetUpdateCommandUnexistedStepError() throws Exception {
        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setInstallType(InstallType.CODENVY_SINGLE_SERVER);
        options.setStep(Integer.MAX_VALUE);

        spyCdecArtifact.getUpdateCommand(Version.valueOf("1.0.0"), Paths.get("some path"), options);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Only update to the Codenvy of the same installation type is supported")
    public void testGetUpdateInfoFromSingleToMultiServerError() throws Exception {
        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setInstallType(InstallType.CODENVY_MULTI_SERVER);
        options.setStep(0);

        spyCdecArtifact.getUpdateInfo(options);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Only update to the Codenvy of the same installation type is supported")
    public void testGetUpdateInfoFromMultiToSingleServerError() throws Exception {
        OSUtils.VERSION = "7";
        InstallationManagerConfig.storeProperty(InstallationManagerConfig.PUPPET_MASTER_HOST_NAME, "some");

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setInstallType(InstallType.CODENVY_SINGLE_SERVER);
        options.setStep(0);

        spyCdecArtifact.getUpdateInfo(options);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Only update to the Codenvy of the same installation type is supported")
    public void testGetUpdateCommandFromSingleToMultiServerError() throws Exception {
        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setInstallType(InstallType.CODENVY_MULTI_SERVER);
        options.setStep(0);

        spyCdecArtifact.getUpdateCommand(Version.valueOf("1.0.0"), Paths.get("some path"), options);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Only update to the Codenvy of the same installation type is supported")
    public void testGetUpdateCommandFromMultiToSingleServerError() throws Exception {
        OSUtils.VERSION = "7";
        InstallationManagerConfig.storeProperty(InstallationManagerConfig.PUPPET_MASTER_HOST_NAME, "some");

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setInstallType(InstallType.CODENVY_SINGLE_SERVER);
        options.setStep(0);

        spyCdecArtifact.getUpdateCommand(Version.valueOf("1.0.0"), Paths.get("some path"), options);
    }

    @Test
    public void testGetInstalledType() throws IOException {
        assertEquals(spyCdecArtifact.getInstalledType(), InstallType.CODENVY_SINGLE_SERVER);

        InstallationManagerConfig.storeProperty(InstallationManagerConfig.PUPPET_MASTER_HOST_NAME, "some");
        assertEquals(spyCdecArtifact.getInstalledType(), InstallType.CODENVY_MULTI_SERVER);
    }
}