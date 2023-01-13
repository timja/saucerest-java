package com.saucelabs.saucerest.integration;

import com.saucelabs.saucerest.DataCenter;
import com.saucelabs.saucerest.SauceREST;
import com.saucelabs.saucerest.api.SauceConnect;
import com.saucelabs.saucerest.model.sauceconnect.Versions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.saucelabs.saucerest.DataCenter.*;
public class SauceConnectTest {

    @ParameterizedTest
    @EnumSource(DataCenter.class)
    public void getLatestVersionTest(DataCenter dataCenter) throws IOException {
        SauceREST sauceREST = new SauceREST(dataCenter);
        SauceConnect sauceConnect = sauceREST.getSauceConnect();
        Versions versions = sauceConnect.getLatestVersions();

        Assertions.assertFalse(versions.latestVersion.isEmpty());
        Assertions.assertFalse(versions.infoUrl.isEmpty());
        Assertions.assertFalse(versions.warning.isEmpty());
        Assertions.assertFalse(versions.downloads.linux.downloadUrl.isEmpty());
        Assertions.assertFalse(versions.downloads.linux.sha1.isEmpty());
        Assertions.assertFalse(versions.downloads.linuxArm64.downloadUrl.isEmpty());
        Assertions.assertFalse(versions.downloads.linuxArm64.sha1.isEmpty());
        Assertions.assertFalse(versions.downloads.osx.downloadUrl.isEmpty());
        Assertions.assertFalse(versions.downloads.osx.sha1.isEmpty());
        Assertions.assertFalse(versions.downloads.win32.downloadUrl.isEmpty());
        Assertions.assertFalse(versions.downloads.win32.sha1.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(DataCenter.class)
    public void getLatestVersionWithoutCredentialsTest(DataCenter dataCenter) throws IOException {
        SauceREST sauceREST = new SauceREST("", "", dataCenter);
        SauceConnect sauceConnect = sauceREST.getSauceConnect();
        Versions versions = sauceConnect.getLatestVersions();

        Assertions.assertFalse(versions.latestVersion.isEmpty());
        Assertions.assertFalse(versions.infoUrl.isEmpty());
        Assertions.assertFalse(versions.warning.isEmpty());
        Assertions.assertFalse(versions.downloads.linux.downloadUrl.isEmpty());
        Assertions.assertFalse(versions.downloads.linux.sha1.isEmpty());
        Assertions.assertFalse(versions.downloads.linuxArm64.downloadUrl.isEmpty());
        Assertions.assertFalse(versions.downloads.linuxArm64.sha1.isEmpty());
        Assertions.assertFalse(versions.downloads.osx.downloadUrl.isEmpty());
        Assertions.assertFalse(versions.downloads.osx.sha1.isEmpty());
        Assertions.assertFalse(versions.downloads.win32.downloadUrl.isEmpty());
        Assertions.assertFalse(versions.downloads.win32.sha1.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(DataCenter.class)
    public void getTunnelsForAUserTest(DataCenter dataCenter) throws IOException {
        SauceREST sauceREST = new SauceREST(dataCenter);
        SauceConnect sauceConnect = sauceREST.getSauceConnect();

        List<String> tunnelIDs = sauceConnect.getTunnelsForAUser();

        if (dataCenter.equals(EU)) {
            Assertions.assertEquals(1, tunnelIDs.size());
        } else if (dataCenter.equals(US)) {
            Assertions.assertEquals(1, tunnelIDs.size());
        } else {
            Assertions.assertEquals(0, tunnelIDs.size());
        }
    }
}
