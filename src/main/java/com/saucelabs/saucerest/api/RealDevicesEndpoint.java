package com.saucelabs.saucerest.api;

import com.google.common.collect.ImmutableMap;
import com.saucelabs.saucerest.*;
import com.saucelabs.saucerest.model.realdevices.*;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.Moshi;
import okhttp3.Response;
import okio.BufferedSource;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RealDevicesEndpoint extends AbstractEndpoint {
    private static final Logger logger = Logger.getLogger(RealDevicesEndpoint.class.getName());

    public RealDevicesEndpoint(DataCenter dataCenter) {
        super(dataCenter);
    }

    public RealDevicesEndpoint(String apiServer) {
        super(apiServer);
    }

    public RealDevicesEndpoint(String username, String accessKey, DataCenter dataCenter) {
        super(username, accessKey, dataCenter);
    }

    public RealDevicesEndpoint(String username, String accessKey, String apiServer) {
        super(username, accessKey, apiServer);
    }

    /**
     * Returns all real devices in Sauce Labs. Documentation is
     * <a href="https://docs.saucelabs.com/dev/api/rdc/#get-devices">here</a>
     *
     * @return {@link List} of {@link Device}
     * @throws IOException API request failed
     */
    public List<Device> getDevices() throws IOException {
        String url = getBaseEndpoint() + "/devices";

        return deserializeJSONArray(request(url, HttpMethod.GET), Device.class);
    }

    /**
     * Returns a specific device based on its ID. Documentation is
     * <a href="https://docs.saucelabs.com/dev/api/rdc/#get-a-specific-device">here</a>
     *
     * @return {@link Device}
     * @throws IOException API request failed
     */
    public Device getSpecificDevice(String deviceID) throws IOException {
        String url = getBaseEndpoint() + "/devices/" + deviceID;

        return deserializeJSONObject(request(url, HttpMethod.GET), Device.class);
    }

    /**
     * Returns all available devices in Sauce Labs. Documentation is
     * <a href="https://docs.saucelabs.com/dev/api/rdc/#get-available-devices">here</a>
     *
     * @return {@link AvailableDevices}
     * @throws IOException API request failed
     */
    public AvailableDevices getAvailableDevices() throws IOException {
        String url = getBaseEndpoint() + "/devices/available";

        return new AvailableDevices(deserializeJSONArray(request(url, HttpMethod.GET), String.class));
    }

    /**
     * Returns all device jobs/tests run on real devices. Documentation is
     * <a href="https://docs.saucelabs.com/dev/api/rdc/#get-real-device-jobs">here</a>
     *
     * @return {@link DeviceJobs}
     * @throws IOException API request failed
     */
    public DeviceJobs getDeviceJobs() throws IOException {
        String url = getBaseEndpoint() + "/jobs";

        return deserializeJSONObject(request(url, HttpMethod.GET), DeviceJobs.class);
    }

    /**
     * Returns all device jobs/tests run on real devices. Result can be limited by providing optional parameters.
     * Documentation is
     * <a href="https://docs.saucelabs.com/dev/api/rdc/#get-real-device-jobs">here</a>
     *
     * @param params Optional parameters
     * @return {@link DeviceJobs}
     * @throws IOException API request failed
     */
    public DeviceJobs getDeviceJobs(ImmutableMap<String, Object> params) throws IOException {
        String url = getBaseEndpoint() + "/jobs";

        return deserializeJSONObject(requestWithQueryParameters(url, HttpMethod.GET, params), DeviceJobs.class);
    }

    /**
     * Returns a specific job/test based on its ID.
     * Documentation is
     * <a href="https://docs.saucelabs.com/dev/api/rdc/#get-a-specific-real-device-job">here</a>
     *
     * @param jobID The ID of the job/test
     * @return {@link DeviceJob}
     * @throws IOException API request failed
     */
    public DeviceJob getSpecificDeviceJob(String jobID) throws IOException {
        String url = getBaseEndpoint() + "/jobs/" + jobID;

        return deserializeJSONObject(request(url, HttpMethod.GET), DeviceJob.class);
    }

    /**
     * TODO: This endpoint is currently not documented and also does not return any response whatsoever.
     *  Update this method and add integration tests including a model and so on when above is fixed.
     * Deletes a real device job/test by ID.
     * Documentation is
     * <a href="https://docs.saucelabs.com/dev/api/rdc/">here</a>
     *
     * @param jobID The ID of the job/test to delete
     */
    public void deleteSpecificRealDeviceJob(String jobID) {
        String url = getBaseEndpoint() + "/jobs/" + jobID;

        try {
            request(url, HttpMethod.DELETE);
        } catch (Exception e) {
            // do nothing
        }
    }

    /**
     * Returns details about the current in-use real devices along with the maximum allowed values.
     *
     * @return {@link Concurrency}
     * @throws IOException API request failed
     */
    public Concurrency getConcurrency() throws IOException {
        String url = getBaseEndpoint() + "/concurrency";

        return deserializeJSONObject(request(url, HttpMethod.GET), Concurrency.class);
    }

    // Helper methods

    public void downloadVideo(String jobID, String path) throws IOException {
        String url = retryUntilTestAssetAvailable(getSpecificDeviceJob(jobID), TestAsset.VIDEO).videoUrl;

        downloadFile(url, path, TestAsset.VIDEO.label);
    }

    public void downloadHARFile(String jobID, String path) throws IOException {
        String url = retryUntilTestAssetAvailable(getSpecificDeviceJob(jobID), TestAsset.HAR).networkLogUrl;

        downloadFile(url, path, TestAsset.HAR.label);
    }

    public void downloadAppiumLog(String jobID, String path) throws IOException {
        String url = retryUntilTestAssetAvailable(getSpecificDeviceJob(jobID), TestAsset.APPIUM_LOG).frameworkLogUrl;
        Path directoryPath = getDirectoryPath(path);
        Path filePath = getFilePath(directoryPath, TestAsset.APPIUM_LOG.label);

        downloadNonMalformedLog(filePath, request(url, HttpMethod.GET));
    }

    public void downloadDeviceLog(String jobID, String path) throws IOException {
        String url = retryUntilTestAssetAvailable(getSpecificDeviceJob(jobID), TestAsset.DEVICE_LOG).deviceLogUrl;
        Path directoryPath = getDirectoryPath(path);
        Path filePath = getFilePath(directoryPath, TestAsset.DEVICE_LOG.label);

        downloadNonMalformedLog(filePath, request(url, HttpMethod.GET));
    }

    public void downloadCommandsLog(String jobID, String path) throws IOException {
        String url = retryUntilTestAssetAvailable(getSpecificDeviceJob(jobID), TestAsset.COMMANDS_LOG).requestsUrl;
        Path directoryPath = getDirectoryPath(path);

        downloadFile(url, directoryPath.toString(), TestAsset.COMMANDS_LOG.label);
    }

    public void downloadDeviceVitals(String jobID, String path) throws IOException {
        String url = retryUntilTestAssetAvailable(getSpecificDeviceJob(jobID), TestAsset.INSIGHTS_LOG).testfairyLogUrl;

        downloadFile(url, path, TestAsset.INSIGHTS_LOG.label);
    }

    public void downloadScreenshots(String jobID, String path) throws IOException {
        List<Object> list = retryUntilTestAssetAvailable(getSpecificDeviceJob(jobID), TestAsset.SCREENSHOTS).screenshots;

        for (int i = 0; i < list.size(); i++) {
            Object pairs = list.get(i);
            String url = ((Map<String, String>) pairs).get("url");
            downloadFile(url, path, i + ".png");
        }
    }

    /**
     * Returns the Appium server version used for the test.
     *
     * @param jobID The ID of the job/test
     * @return The Appium server version
     * @throws IOException API request failed
     */
    public String getAppiumServerVersion(String jobID) throws IOException {
        String url = retryUntilTestAssetAvailable(getSpecificDeviceJob(jobID), TestAsset.APPIUM_LOG).frameworkLogUrl;

        try (Response response = request(url, HttpMethod.GET);
             BufferedSource source = response.body().source()) {

            Moshi moshi = MoshiSingleton.getInstance();
            JsonReader reader = JsonReader.of(source);
            List<String> versions = new ArrayList<>();

            JsonAdapter<LogEntry> adapter = moshi.adapter(LogEntry.class);
            reader.beginArray();
            while (reader.hasNext()) {
                JsonReader.Token token = reader.peek();
                if (token == JsonReader.Token.BEGIN_OBJECT) {
                    LogEntry logEntry = adapter.fromJson(reader);
                    if (logEntry != null && logEntry.getTime() != null && logEntry.getLevel() != null && logEntry.getMessage() != null) {
                        versions.add(logEntry + System.lineSeparator());
                    }
                } else {
                    reader.skipValue();
                }
            }
            reader.endArray();

            String appiumVersion = extractAppiumVersion(versions);
            if (appiumVersion != null) {
                return appiumVersion;
            } else {
                logger.log(Level.WARNING, "Appium version not found in the log file");
                return null;
            }
        }
    }

    private static String extractAppiumVersion(List<String> versions) {
        String regex = "Appium v(\\d+\\.\\d+\\.\\d+)";
        Pattern pattern = Pattern.compile(regex);

        for (String line : versions) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null; // Return null if the Appium version is not found in the log file
    }

    /**
     * The base endpoint of the Platform endpoint APIs.
     */
    @Override
    protected String getBaseEndpoint() {
        return super.getBaseEndpoint() + "v1/rdc";
    }

    private DeviceJob retryUntilTestAssetAvailable(DeviceJob deviceJob, TestAsset testAsset) throws IOException {
        try {
            Awaitility.await()
                    .pollInterval(1, TimeUnit.SECONDS)
                    .atMost(1, TimeUnit.MINUTES)
                    .until(testAssetAvailable(deviceJob, testAsset));
        } catch (ConditionTimeoutException e) {
            logger.severe(String.format("Timed out waiting for %s to be available for ID %s", testAsset.label, deviceJob.id));
        }

        return getSpecificDeviceJob(deviceJob.id);
    }

    private Callable<Boolean> testAssetAvailable(DeviceJob deviceJob, TestAsset testAsset) {
        return () -> {
            switch (testAsset) {
                case VIDEO:
                    return getSpecificDeviceJob(deviceJob.id).videoUrl != null;
                case HAR:
                    return getSpecificDeviceJob(deviceJob.id).networkLogUrl != null;
                case APPIUM_LOG:
                    return getSpecificDeviceJob(deviceJob.id).frameworkLogUrl != null;
                case INSIGHTS_LOG:
                    return getSpecificDeviceJob(deviceJob.id).testfairyLogUrl != null;
                case CRASH_LOG:
                    return getSpecificDeviceJob(deviceJob.id).crashLogUrl != null;
                case DEVICE_LOG:
                    return getSpecificDeviceJob(deviceJob.id).deviceLogUrl != null;
                case COMMANDS_LOG:
                    return getSpecificDeviceJob(deviceJob.id).requestsUrl != null;
                case SCREENSHOTS:
                    return getSpecificDeviceJob(deviceJob.id).screenshots != null;
                default:
                    return false;
            }
        };
    }

    /**
     * Some log files are malformed and have noise in them making it difficult to read.
     * This method will download these logs and parse them so it is readable and in an expected format.
     *
     * @param path     The path to the directory to download the log file to.
     * @param response The response from the request to download the log file.
     * @throws IOException If there is an issue writing to the file.
     */
    private void downloadNonMalformedLog(Path path, Response response) throws IOException {
        if (response == null) {
            logger.warning(String.format("Response is null for %s", path));
            throw new IOException("Response is null");
        }

        if (response.body() == null) {
            logger.warning(String.format("Response body is null for %s", path));
            throw new IOException("Response body is null");
        }

        Moshi moshi = MoshiSingleton.getInstance();
        BufferedSource source = response.body().source();
        JsonReader reader = JsonReader.of(source);

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            JsonAdapter<LogEntry> adapter = moshi.adapter(LogEntry.class);
            reader.beginArray();
            while (reader.hasNext()) {
                JsonReader.Token token = reader.peek();
                if (token == JsonReader.Token.BEGIN_OBJECT) {
                    LogEntry logEntry = adapter.fromJson(reader);
                    if (logEntry != null && logEntry.getTime() != null && logEntry.getLevel() != null && logEntry.getMessage() != null) {
                        writer.write(logEntry + System.lineSeparator());
                    }
                } else {
                    reader.skipValue();
                }
            }
            reader.endArray();
        } catch (IOException e) {
            logger.warning(String.format("Failed to write to file %s", path));
            throw e;
        } catch (JsonDataException e) {
            logger.warning(String.format("Failed to parse JSON response: %s", e.getMessage()));
            throw e;
        }
    }
}