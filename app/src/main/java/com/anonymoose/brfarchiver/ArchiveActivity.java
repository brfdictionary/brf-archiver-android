package com.anonymoose.brfarchiver;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.CheckBox;
import android.widget.EditText;

import com.anonymoose.brfarchiver.data.Settings;
import com.anonymoose.brfarchiver.utils.DownloadWebpageTask;

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

public class ArchiveActivity extends AppCompatActivity {

    private final String CLASSNAME = this.getClass().getName();

    private String version;
    private String url;
    private String saveFilename;
    private String login;
    private String password;
    private String users;
    private boolean squelchState;
    private boolean downloadImageState;
    private boolean isHttps;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get our progress dialog object already created and ready to go
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setIndeterminate(true);
        progressDialog.setCanceledOnTouchOutside(false);

        readPreferences();
        updateActivity();
        readActivity();
        savePreferences();
    }

    /**
     * Read the preferences from the shared resources
     */
    private void readPreferences() {
        SharedPreferences sharedPrefs = this.getPreferences(Context.MODE_PRIVATE);
        version = sharedPrefs.getString(getString(R.string.settings_version), getString(R.string.settings_default_version));
        login = sharedPrefs.getString(getString(R.string.settings_login), "");
        password = sharedPrefs.getString(getString(R.string.settings_password), "");
        users = sharedPrefs.getString(getString(R.string.settings_usernames), "");
        squelchState = sharedPrefs.getBoolean(getString(R.string.settings_squelch_noise), false);
        downloadImageState = sharedPrefs.getBoolean(getString(R.string.settings_download_images), true);
        isHttps = sharedPrefs.getBoolean(getString(R.string.settings_is_https), false);
    }

    /**
     * Update the state of the various controls on the activity, based on the member variables
     */
    private void updateActivity() {
        EditText editLogin = (EditText) findViewById(R.id.editLogin);
        editLogin.setText(login);
        EditText editPassword = (EditText) findViewById(R.id.editPassword);
        editPassword.setText(password);
        EditText editUsers = (EditText) findViewById(R.id.editUsers);
        editUsers.setText(users);
        CheckBox cbSquelchNoise = (CheckBox) findViewById(R.id.cbSquelchNoise);
        cbSquelchNoise.setChecked(squelchState);
        CheckBox cbDownloadImages = (CheckBox) findViewById(R.id.cbDownloadImages);
        cbDownloadImages.setChecked(downloadImageState);
        CheckBox cbIsHttps = (CheckBox) findViewById(R.id.cbIsHttps);
        cbIsHttps.setChecked(isHttps);
    }

    /**
     * Reads the various controls on the activity into the class members
     */
    private void readActivity() {
        EditText editUrl = (EditText) findViewById(R.id.editUrl);
        url = editUrl.getText().toString();
        EditText editSaveFile = (EditText) findViewById(R.id.editSaveFile);
        saveFilename = editSaveFile.getText().toString();
        EditText editLogin = (EditText) findViewById(R.id.editLogin);
        login = editLogin.getText().toString();
        EditText editPassword = (EditText) findViewById(R.id.editPassword);
        password = editPassword.getText().toString();
        EditText editUsers = (EditText) findViewById(R.id.editUsers);
        users = editUsers.getText().toString();
        CheckBox cbSquelchNoise = (CheckBox) findViewById(R.id.cbSquelchNoise);
        squelchState = cbSquelchNoise.isChecked();
        CheckBox cbDownloadImages = (CheckBox) findViewById(R.id.cbDownloadImages);
        downloadImageState = cbDownloadImages.isChecked();
    }

    /**
     * Saves the settings of various member variables to the prefs
     */
    private void savePreferences() {
        SharedPreferences sharedPrefs = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(getString(R.string.settings_version), version);
        editor.putString(getString(R.string.settings_login), login);
        editor.putString(getString(R.string.settings_password), password);
        editor.putBoolean(getString(R.string.settings_squelch_noise), squelchState);
        editor.putBoolean(getString(R.string.settings_download_images), downloadImageState);
        editor.putBoolean(getString(R.string.settings_is_https), isHttps);
        editor.commit();
    }

    public void onClickArchive(View view) {
        readActivity();
        if (!URLUtil.isValidUrl(url)) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                                    .setTitle("Error")
                                    .setMessage("Please enter a valid URL")
                                    .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    }).create();
            dialog.show();
            return;
        }
        else {
            Log.d(CLASSNAME, "Saved and spidering");
            savePreferences();

            progressDialog.show();

            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnected()) {
                progressDialog.setMessage("Loading. Please wait ...");
            }
            else {
                progressDialog.setMessage("No network connection available");
            }

            // Spider here
            Settings settings = new Settings.Builder()
                                        .withUsername(login)
                                        .withPassword(password)
                                        .withUrl(url)
                                        .withUsers(users)
                                        .withDownloadImages(downloadImageState)
                                        .withSquelchNoise(squelchState)
                                        .withHttps(isHttps)
                                        .withSaveFilename(saveFilename)
                                        .withActivity(this)
                                        .withContext(getApplicationContext())
                                        .build();
            new DownloadWebpageTask().execute(settings);

        }
    }

    /**
     * Validates if the URL supplied is valid or not.
     * @param url - The URL to validate
     * @return true or false
     */
    private boolean validateUrl(String url) {
        if (!URLUtil.isValidUrl(url))
            return false;

        if (url.indexOf("forums.bharat-rakshak.com") == -1)
            return false;

        return true;
    }

    /**
     * Method to update the progress dialog (called by an AsyncTask on another thread)
     * @param message - The message to display
     */
    public void updateProgress(String message) {
        progressDialog.setMessage(message);
    }

    /**
     * Method to close the progress dialog (called by an AsyncTask on another thread)
     */
    public void doneDownloadingEvent() {
        progressDialog.dismiss();
    }

}
