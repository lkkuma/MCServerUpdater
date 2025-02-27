package me.hsgamer.mcserverupdater.api;

import me.hsgamer.hscore.web.UserAgent;
import me.hsgamer.hscore.web.WebUtils;
import me.hsgamer.mcserverupdater.UpdateBuilder;
import me.hsgamer.mcserverupdater.util.VersionQuery;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.regex.Pattern;

public abstract class JenkinsUpdater implements LocalChecksum, InputStreamUpdater {
    protected final UpdateBuilder updateBuilder;
    protected final String version;
    protected final String build;
    private final String jenkinsUrl;

    protected JenkinsUpdater(VersionQuery versionQuery, String jenkinsUrl) {
        this.jenkinsUrl = jenkinsUrl.endsWith("/") ? jenkinsUrl : jenkinsUrl + "/";
        this.updateBuilder = versionQuery.updateBuilder;
        this.version = versionQuery.isLatest ? getDefaultVersion() : versionQuery.version;
        this.build = getBuild();
    }

    public abstract String[] getJob();

    public abstract Pattern getArtifactRegex();

    public abstract String getDefaultVersion();

    private String getBuild() {
        String url = getJobUrl();
        String api = url + "api/json";
        String treeUrl = api + "?tree=lastSuccessfulBuild[number]";
        getUpdateBuilder().debug("Getting latest build from " + treeUrl);
        try {
            URLConnection connection = UserAgent.CHROME.assignToConnection(WebUtils.createConnection(treeUrl));
            InputStream inputStream = connection.getInputStream();
            JSONObject jsonObject = new JSONObject(new JSONTokener(inputStream));
            JSONObject build = jsonObject.getJSONObject("lastSuccessfulBuild");
            String buildNumber = Integer.toString(build.getInt("number"));
            getUpdateBuilder().debug("Latest build: " + buildNumber);
            return buildNumber;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getChecksum() {
        return String.join("||", version, build, String.join("_", getJob()), getJenkinsUrl());
    }

    @Override
    public InputStream getInputStream() {
        String url = getArtifactUrl();
        getUpdateBuilder().debug("Downloading " + url);
        try {
            URLConnection connection = UserAgent.CHROME.assignToConnection(WebUtils.createConnection(url));
            return connection.getInputStream();
        } catch (IOException e) {
            getUpdateBuilder().debug(e);
            return null;
        }
    }

    private String getJenkinsUrl() {
        return jenkinsUrl;
    }

    private String getJobUrl() {
        String[] job = getJob();
        StringBuilder builder = new StringBuilder();
        builder.append(jenkinsUrl);
        for (String s : job) {
            builder.append("job/").append(s).append("/");
        }
        return builder.toString();
    }

    private String getArtifactUrl() {
        Pattern artifactRegex = getArtifactRegex();
        String jobUrl = getJobUrl();
        String artifactListUrl = jobUrl + build + "/api/json?tree=artifacts[fileName,relativePath]";
        String artifact = "INVALID";
        getUpdateBuilder().debug("Getting artifact from " + artifactListUrl);
        try {
            URLConnection connection = UserAgent.CHROME.assignToConnection(WebUtils.createConnection(artifactListUrl));
            InputStream inputStream = connection.getInputStream();
            JSONObject jsonObject = new JSONObject(new JSONTokener(inputStream));
            JSONArray artifacts = jsonObject.getJSONArray("artifacts");
            for (int i = 0; i < artifacts.length(); i++) {
                JSONObject artifactObject = artifacts.getJSONObject(i);
                String fileName = artifactObject.getString("fileName");
                if (artifactRegex.matcher(fileName).matches()) {
                    artifact = artifactObject.getString("relativePath");
                    break;
                }
            }
        } catch (IOException e) {
            getUpdateBuilder().debug(e);
        }
        String artifactUrl = jobUrl + "%s/artifact/%s";
        String formattedArtifactUrl = String.format(artifactUrl, build, artifact);
        getUpdateBuilder().debug("Artifact URL: " + formattedArtifactUrl);
        return formattedArtifactUrl;
    }

    @Override
    public UpdateBuilder getUpdateBuilder() {
        return updateBuilder;
    }
}
