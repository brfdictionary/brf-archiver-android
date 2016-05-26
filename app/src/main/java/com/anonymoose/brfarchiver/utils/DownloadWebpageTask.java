package com.anonymoose.brfarchiver.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.anonymoose.brfarchiver.ArchiveActivity;
import com.anonymoose.brfarchiver.BuildConfig;
import com.anonymoose.brfarchiver.data.Settings;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.FilenameUtils;

/**
 * Copyright (c) 2016, Armen Tanzarian
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of Armen Tanzarian nor the names of other
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

public class DownloadWebpageTask extends AsyncTask<Settings, String, String> {
    final private String CLASS_NAME = this.getClass().getName();
    private Settings settings;
    private boolean     archiveSuccess;
    private CookieManager cookieManager;
    private String      nextPageUrl;
    private String      data;
    private String      errorMessage;
    private boolean     firstPass;
    private int         pageCounter;
    private int         maxPages;
    private Map<String, String> imageCache;
    private String      imageFolder;
    private int         imageCacheCounter;
    private final int   MIN_SQUELCH_LIMIT = 10;
    private final String LOGIN_URL = "://forums.bharat-rakshak.com/ucp.php?mode=login";

    private final int READ_TIMEOUT = 30000; /* Milliseconds */
    private final int CONNECT_TIMEOUT = 15000; /* Milliseconds */

    private final int READ_SIZE    = 8192; /* Size of read buffers */

    /**
     * Override this method to perform a computation on a background thread. The
     * specified parameters are the parameters passed to {@link #execute}
     * by the caller of this task.
     * <p>
     * This method can call {@link #publishProgress} to publish updates
     * on the UI thread.
     *
     * @param values The parameters of the task.
     * @return A result, defined by the subclass of this task.
     * @see #onPreExecute()
     * @see #onPostExecute
     * @see #publishProgress
     */
    @Override
    protected String doInBackground(Settings... values) {
        this.settings = values[0];
        String url = settings.getUrl();

        return downloadPages(url);
    }

    /**
     * Gets a HttpUrlConnection object setup to perform a GET request for a given URL
     * @param url - The URL that the HttpUrlConnection object is hooked to.
     * @return A HttpUrlConnection object
     * @throws IOException
     */
    private HttpURLConnection getHttpConnection(String url) throws IOException {
        URL urlObj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        try {
            conn.setRequestMethod("GET");
        }
        catch (ProtocolException e) {
            // Ignore - "Cannot happen", since we are hardcoding the request method.
        }
        conn.setDoInput(true);
        return conn;
    }

    /**
     * Get a HttpsUrlConnection object setup to perform a GET request for a https URL.
     * @param url - The URL that the HttpsUrlConnection object is hooked to.
     * @return A connection object
     * @throws IOException
     */
    private HttpsURLConnection getHttpsConnection(String url) throws IOException {
        URL urlObj = new URL(url);
        HttpsURLConnection conn = (HttpsURLConnection) urlObj.openConnection();
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        try {
            conn.setRequestMethod("GET");
        }
        catch (ProtocolException e) {
            // Ignore - "Cannot happen", since we are hardcoding the request method.
        }
        conn.setDoInput(true);
        return conn;
    }

    /**
     * Gets a HttpURLConnection to perform a POST request to a HTTP URL
     * @param url - The URL that the HttpURLConnection is hooked to
     * @return A connectin object
     * @throws IOException
     */
    private HttpURLConnection getHttpPostConnection(String url) throws IOException {
        HttpURLConnection conn = getHttpConnection(url);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        try {
            conn.setRequestMethod("POST");
        }
        catch (ProtocolException e) {
            // Ignore - "Cannot happen", since we are hardcoding the request method.
        }
        return conn;
    }

    /**
     * Get a HttpsUrlConnection object setup to perform a POST request for a https URL.
     * @param url - The URL that the HttpsUrlConnection object is hooked to.
     * @return A connection object
     * @throws IOException
     */
    private HttpsURLConnection getHttpsPostConnection(String url) throws IOException {
        HttpsURLConnection conn = getHttpsConnection(url);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        try {
            conn.setRequestMethod("POST");
        }
        catch (ProtocolException e) {
            // Ignore - "Cannot happen", since we are hardcoding the request method.
        }
        return conn;
    }

    /**
     * Returns a HttpUrlConnection or a HttpsUrlConnection for GET depending on the setting. Yes, the method
     * signature is HttpUrlConnection, but HttpsUrlConnection is a subclass of HttpConnection, instead
     * of both of them subclassing UrlConnection (which doesn't contain a getResponse() method).
     * @param url - The URL to fetch the data from
     * @return A HttpUrlConnection or HttpsUrlConnection object
     * @throws IOException
     */
    private HttpURLConnection getConnection(String url) throws IOException {
        if (settings.isHttps()) {
            return getHttpsConnection(url);
        }
        else {
            return getHttpConnection(url);
        }
    }

    /**
     * Returns a HttpUrlConnection or a HttpsUrlConnection for POST depending on the setting. Yes, the method
     * signature is HttpUrlConnection, but HttpsUrlConnection is a subclass of HttpConnection, instead
     * of both of them subclassing UrlConnection (which doesn't contain a getResponse() method).
     * @param url - The URL to fetch the data from
     * @return A HttpUrlConnection or HttpsUrlConnection object
     * @throws IOException
     */
    private HttpURLConnection getPostConnection(String url) throws IOException {
        if (settings.isHttps()) {
            return getHttpsPostConnection(url);
        }
        else {
            return getHttpPostConnection(url);
        }
    }


    /**
     * Top level routine to download a list of pages from the top level URL.
     * @param url The top-level URL to start downloading from.
     * @return A concatenated list of web pages as a single string
     */
    private String downloadPages(String url) {
        cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        archiveSuccess = false;

        if (settings.getUsername() != null && !settings.getUsername().isEmpty()) {
            if (!loginToForum()) {
                publishProgress("Could not login to forum. Please check login and password");
                return "";
            }
        }

        String content = "";
        String nextUrl = url;
        if (settings.isHttps()) {
            url = url.replace("http://", "https://");
        }
        resetParser();
        while (true) {
            if (!firstPass) {
                publishProgress(String.format("Downloading page %d of %d", pageCounter, maxPages));
            }
            else {
                publishProgress("Downloading...");
            }
            String htmlPage = downloadPage(nextUrl);
            if (!parsePage(nextUrl, htmlPage)) {
                publishProgress("Error while fetching pages: " + errorMessage);
                break;
            }

            content += data;

            nextUrl = nextPageUrl;
            if (nextUrl == null) {
                break;
            }
            pageCounter++;
        }

        archiveSuccess = true;
        return content;
    }

    /**
     * Downloads a page via HTTP
     * @param url - The URL to download the page from
     * @return The data from the page.
     */
    private String downloadPage(String url) {
        HttpURLConnection conn = null;
        try {
            conn = getConnection(url);

            conn.connect();
            int response = conn.getResponseCode();
            if (response == 200) {
                InputStream stream = null;
                BufferedReader reader = null;
                StringBuilder strbuf = new StringBuilder();

                try {
                    stream = conn.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                    String line;

                    while ((line = reader.readLine()) != null) {
                        strbuf.append(line);
                    }
                }
                finally {
                    if (reader != null) {
                        reader.close();
                    }
                    if (stream != null) {
                        stream.close();
                    }
                }
                return strbuf.toString();
            }
            else {
                return "";
            }
        }
        catch (IOException e) {
            Log.e(CLASS_NAME, e.getMessage());
        }
        finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return "";
    }

    /**
     * Downloads an image file to the local cache
     * @param url - The URL to download the image from
     * @param fileName - The filename to save the image file to
     * @return true on success, false on failure.
     */
    private boolean getImageFile(String url, String fileName) {
        HttpURLConnection conn = null;
        try {
            conn = getConnection(url);
            conn.connect();
            int response = conn.getResponseCode();
            if (response == 200) {
                final int contentLength = conn.getContentLength();
                if (contentLength < 0) {
                    return false;
                }
                InputStream is  = null;
                OutputStream os = null;

                try {
                    is = new BufferedInputStream(conn.getInputStream(), READ_SIZE);
                    os = new FileOutputStream( new File(fileName));
                    byte bytes[] = new byte[READ_SIZE];
                    int bytesRead;
                    int bytesCount = 0;
                    while ( (bytesRead = is.read(bytes)) != -1) {
                        bytesCount += bytesRead;
                        os.write(bytes, 0, bytesRead);
                    }
                    if (BuildConfig.DEBUG) {
                        Log.d(CLASS_NAME, String.format("Read in %d bytes", bytesCount));
                    }

                    return true;
                }
                finally {
                    if (is != null) {
                        is.close();
                    }
                    if (os != null) {
                        os.close();
                    }
                }
            }
            else {
                return false;
            }
        }
        catch (IOException e) {
            Log.e(CLASS_NAME, e.getMessage());
            return false;
        }
        finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Logs a user into a forum
     * @return True on success, false on failure.
     */
    private boolean loginToForum() {
        try {
            String protocol = (settings.isHttps()) ? "https" : "http";
            String loginUrl = protocol + LOGIN_URL;
            HttpURLConnection conn = getPostConnection(loginUrl);

            Map<String, String> params = new HashMap<>();
            params.put("username", settings.getUsername());
            params.put("password", settings.getPassword());
            params.put("login", "Login");


            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter( new OutputStreamWriter(os, "UTF-8"));
            writer.write(preparePostDataString(params));
            writer.flush();
            writer.close();
            os.close();

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {

                String response = "";
                String line;
                BufferedReader reader = new BufferedReader( new InputStreamReader( conn.getInputStream() ));
                while ( (line = reader.readLine()) != null) {
                    response += line;
                }
                if (BuildConfig.DEBUG) {
                    Log.d(CLASS_NAME, "Response from login: " + response);
                }
                reader.close();

                if (response.contains("successfully logged in")) {
                    return true;
                }
                else {
                    return false;
                }
            }

            return false;
        }
        catch (IOException e) {
            Log.e(CLASS_NAME, "Download: LoginToForum: " + e.getMessage());
            return false;
        }
    }

    /**
     * Prepare a string to be sent via the POST request.
     * @param params A Map of (name, value) parameters
     * @return A string that can be sent by the POST request.
     */
    private String preparePostDataString(Map<String, String>params) {
        StringBuilder result = new StringBuilder();
        boolean firstPass = true;

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (firstPass) {
                firstPass = false;
            }
            else {
                result.append('&');
            }

            try {
                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
            catch (UnsupportedEncodingException e) {
                // This really "cannot happen" because UTF-8 is one of the encoder formats that is
                // required to be universally supported by Java's specification. The Sun guys really
                // should have implemented an overloaded URLEncoder.encode() with the second param
                // as an enum for commonly known encoding formats. Hindsight is 20/20
                Log.e(CLASS_NAME, "preparePostDataString: " + e.getMessage());
            }
        }

        return result.toString();
    }

    /**
     * Routine that updates the main activity's status bar
     * @param statusMsg The message to stick onto the status bar
     */
    private void updateProgress(String statusMsg) {
        ArchiveActivity activity = (ArchiveActivity) settings.getActivity();
        activity.updateProgress(statusMsg);
    }

    @Override
    protected void onProgressUpdate(String... status) {
        updateProgress(status[0]);
    }

    @Override
    protected void onPostExecute(String result) {
        FileOutputStream outputFile = null;
        String errorMessage = "";
        try {
            outputFile = settings.getContext().openFileOutput(settings.getSaveFilename(), Context.MODE_PRIVATE);
            String htmlContent = formatHtmlPage(settings.getUrl(), result);
            outputFile.write(htmlContent.getBytes());
        }
        catch (IOException e) {
            errorMessage = "File write failed: " + e.getMessage();
            archiveSuccess = false;
        }
        finally {
            try {
                if (outputFile != null) {
                    outputFile.close();
                }
            }
            catch (IOException e) {
                errorMessage = "Could not close file: " + e.getMessage();
                archiveSuccess = false;
            }
        }

        ArchiveActivity activity = (ArchiveActivity) settings.getActivity();

        if (archiveSuccess) {
            activity.doneDownloadingEvent();
        }
        else {
            activity.updateProgress(errorMessage);
        }
    }

    /**
     * Builds a HTML page from a template
     * @param url - The URL that we spidered
     * @param content - The content to paste into the page.
     * @return A templatized page.
     */
    private String formatHtmlPage(String url, String content) {
        String htmlPage = "";
        BufferedReader reader = null;
        try {
            reader = new BufferedReader( new InputStreamReader(settings.getContext().getAssets().open("html_file_template.txt")));

            String line;
            while ( (line = reader.readLine()) != null) {
                htmlPage += line;
            }

            htmlPage = htmlPage.replace("###URL###", url);
            htmlPage = htmlPage.replace("###CONTENT###", content);
        }
        catch (IOException e) {
            publishProgress("Could not format HTML page: " + e.getMessage());
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e) {
                    publishProgress("Could not close file: " + e.getMessage());
                }
            }
        }

        return htmlPage;
    }

    /**
     * Resets a bunch of state variables.
     */
    public void resetParser() {
        nextPageUrl = null;
        data = "";
        errorMessage = "";
        firstPass = true;
        pageCounter = 1;
        maxPages = 1;
        imageCacheCounter = 1;
        imageCache = new HashMap<>();
        File tmpFolder = new File(settings.getContext().getFilesDir(), FilenameUtils.getBaseName(settings.getSaveFilename()));
        if (!tmpFolder.isDirectory()) {
            if (! tmpFolder.mkdir()) {
                Log.e(CLASS_NAME, "Could not create temp folder for storing images");
            }
        }
        imageFolder = tmpFolder.getAbsolutePath();
    }

    /**
     * Parses a page and extracts content
     * @param url The URL to
     * @param htmlPage
     * @return
     */
    private boolean parsePage(String url, String htmlPage) {
        data = "";
        // First, look for the next page link
        nextPageUrl = null;
        int pos = htmlPage.indexOf("Next</a>");
        if (pos > -1) {
            pos -= 2;
        }
        int pos2;
        if (pos > -1) {
            pos2 = htmlPage.lastIndexOf("<a href=", pos);
            if (pos2 == -1) {
                errorMessage = String.format("Parse error on '%s', while parsing Next page link", url);
                return false;
            }
            pos2 += "<a href=\"./".length();
            nextPageUrl = (settings.isHttps() ? "https" : "http") + "://forums.bharat-rakshak.com/" + htmlPage.substring(pos2, pos);

            // Also get the last page #, if we can get it.
            if (firstPass) {
                pos = htmlPage.lastIndexOf("</a>", pos2);
                if (pos > -1) {
                    pos2 = htmlPage.lastIndexOf(">", pos);
                    if (pos2 > -1) {
                        pos2++;

                        String tmp = htmlPage.substring(pos2, pos);
                        try {
                            maxPages = Integer.valueOf(tmp);
                        } catch (NumberFormatException e) {
                            maxPages = 1;
                        }
                    }
                }
                firstPass = false;
            }

            if (nextPageUrl.contains("phpbb.com")) {
                nextPageUrl = null;
            } else {
                nextPageUrl = nextPageUrl.replace("&amp;", "&");
            }
        }

        // Now to trim the HTML a bit.
        pos = htmlPage.indexOf("<div id=\"pagefooter\"></div>");
        if (pos > -1) {
            htmlPage = htmlPage.substring(0, pos);
        }

        // Now look for the start of the posts
        pos = htmlPage.indexOf("<table class=\"tablebg\"");
        if (pos == -1) {
            data = "";
            return true;
        }
        pos = htmlPage.indexOf("<table class=\"tablebg\"", pos + 1);
        if (pos == -1) {
            data = "";
            return true;
        }
        pos = htmlPage.indexOf("<table class=\"tablebg\"", pos + 1);

        // Now get the chunks we need.
        boolean lastChunk = false;
        String chunk;
        while (! lastChunk) {
            publishProgress("...");
            pos2 = htmlPage.indexOf("<table class=\"tablebg\"", pos + 1);
            if (pos2 == -1) {
                pos2 = htmlPage.lastIndexOf("</table>");
                pos2 = htmlPage.lastIndexOf("</table>", pos2 - 1);
                lastChunk = true;
            }
            chunk = htmlPage.substring(pos, pos2);
            pos = pos2;

            int pos3 = chunk.indexOf("<b class=\"postauthor\">");
            if (pos3 > -1) {
                pos3 += "<b class=\"postauthor\">".length();
                int pos4 = chunk.indexOf("</b>", pos3);
                if (pos4 > -1) {
                    String author = chunk.substring(pos3, pos4);
                    if (!settings.isWantedUser(author)) {
                        continue;
                    }
                }
            }

            if (settings.isSquelchNoise() && detectNoise(chunk)) {
                continue;
            }

            if (settings.isDownloadImages()) {
                chunk = downloadImages(chunk);
            }

            data += chunk;
        }

        return true;
    }

    /**
     * Detects whether a post is noise or not. It does this by stripping the HTML tags and checking
     * the # of words in the post. Anything less than MIN_SQUELCH_LIMIT is considered as junk.
     * @param chunk The chunk of data to test against
     * @return true if this post is determined to be junk, false otherwise
     */
    boolean detectNoise(String chunk) {
        int pos = chunk.indexOf("<div class=\"postbody\">");
        int pos2 = chunk.indexOf("</table>", pos);

        if (pos > -1 && pos2 > -1) {
            chunk = chunk.substring(pos, pos2);
        }

        chunk = chunk.replace("\n", "");
        chunk = chunk.replace("\r", "");

        // Now for some low-tech HTML stripping, instead of using an XML parser or something, in case
        // the HTML is not well formed :).
        pos = chunk.indexOf('<');
        while (pos > -1) {
            pos2 = chunk.indexOf('>', pos + 1);
            if (pos2 > -1) {
                chunk = chunk.substring(0, pos) + chunk.substring(pos2 + 1);
                pos = chunk.indexOf('<');
            }
            else {
                break;
            }
        }

        String[] words = chunk.trim().split("\\s+");
        return (words.length < MIN_SQUELCH_LIMIT);
    }

    /**
     * Downloads images locally and modifies the image links as required.
     * @param chunk - The chunk of HTML to parse
     * @return - A modified chunk of HTML, with all image tags modified to local files.
     */
    String downloadImages(String chunk) {
        int pos = chunk.indexOf("<img src=\"");
        while (pos > -1) {
            int pos2 = chunk.indexOf(">", pos + 1);
            if (pos2 > -1) {
                String imgLink = chunk.substring(pos, pos2 + 1);
                int pos3 = "<img src=\"".length();
                int pos4 = imgLink.indexOf('"', pos3);
                String imgUrl = imgLink.substring(pos3, pos4);
                // BRF forum images don't have http:// in front, so we need to add that.
                // Also, already downloaded images have the scheme "file://", so we need to
                // skip these URLs.

                String scheme = imgUrl.substring(0, 4);
                if (scheme.equals("file")) {
                    pos = chunk.indexOf("<img src=\"", pos2);
                    continue;
                }
                if (!scheme.equals("http")) {
                    imgUrl = (settings.isHttps() ? "https" : "http") + "://forums.bharat-rakshak.com/" + imgUrl;
                }

                // Check if we've already downloaded this image or not and download it if needed
                if (! imageCache.containsKey(imgUrl)) {
                    // Figure out a new image name for ths file
                    pos3 = imgUrl.lastIndexOf("/");
                    if (pos3 == -1) {
                        throw new RuntimeException("Could not figure out the image name");
                    }
                    String imgFileName = imgUrl.substring(pos3 + 1);
                    // Some images have ? in the URL. Remove these from imgFileName if present.
                    pos3 = imgFileName.indexOf('?');
                    if (pos3 > -1) {
                        imgFileName = imgFileName.substring(0, pos3 - 1);
                    }
                    publishProgress("Downloading " + imgFileName);
                    // Mogrify the img name a bit, so that filenames from different URLs with
                    // the same file name do not get overwritten.
                    imgFileName = imageFolder + "/-brfarchive-" + String.valueOf(imageCacheCounter) + "-" + imgFileName;
                    imageCacheCounter++;
                    // Download the file here.
                    getImageFile(imgUrl, imgFileName);

                    imageCache.put(imgUrl, imgFileName);
                }

                    // Now replace the image_link with a local image link
                String localImgLink = "<img src=\"file:///" + imageCache.get(imgUrl) + "\" alt=\"Local Download Image\">";
                chunk = chunk.replace(imgLink, localImgLink);

            }
            pos = chunk.indexOf("<img src=\"", pos + 1);
        }

        return chunk;
    }

}