package com.anonymoose.brfarchiver.utils;

import android.app.Activity;
import android.app.Dialog;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

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

/**
 * Shamelessly filched and modified from https://rogerkeays.com/simple-android-file-chooser
 */
public class FileChooser {
    private static final String PARENT_DIR = "..";

    private final Activity activity;
    private ListView listView;
    private Dialog dialog;
    private File currentPath;
    private FileSelectedListener listener;
    private String extension = null;

    public FileChooser(Activity activity) {
        this.activity = activity;
        dialog = new Dialog(activity);
        listView = new ListView(activity);
        listView.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            /**
             * Callback method to be invoked when an item in this AdapterView has
             * been clicked.
             * <p>
             * Implementers can call getItemAtPosition(position) if they need
             * to access the data associated with the selected item.
             *
             * @param parent   The AdapterView where the click happened.
             * @param view     The view within the AdapterView that was clicked (this
             *                 will be a view provided by the adapter)
             * @param position The position of the view in the adapter.
             * @param id       The row id of the item that was clicked.
             */
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String fileChosen = (String) listView.getItemAtPosition(position);
                File chosenFile = getChosenFile(fileChosen);
                if (chosenFile.isDirectory()) {
                    refresh(chosenFile);
                }
                else {
                    if (listener != null) {
                        listener.fileSelected(chosenFile);
                    }
                    dialog.dismiss();
                }
            }
        });
        dialog.setContentView(listView);
        dialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        refresh(activity.getApplicationContext().getFilesDir());
    }

    public void showDialog() {
        dialog.show();
    }

    /**
     * Method to refresh, filter and display the files for a given path.
     * @param path - The path of the directory
     */
    private void refresh(File path) {
        this.currentPath = path;
        if (path.exists()) {
            File[] dirs = path.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory() && file.canRead();
                }
            });
            File[] files = path.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    if (!file.isDirectory()) {
                        if (!file.canRead()) {
                            return false;
                        }
                        else if (extension == null) {
                            return true;
                        }
                        else {
                            return file.getName().toLowerCase().endsWith(extension);
                        }
                    }
                    else {
                        return false;
                    }
                }
            });

            // convert to an array for sorting.
            int i = 0;
            String[] fileList;
            if (path.getParentFile() == null) {
                int len = (dirs == null ? 0 : dirs.length) +
                        (files == null ? 0 : files.length);
                fileList = new String[len];
            }
            else {
                int len = (dirs == null ? 0 : dirs.length) +
                        (files == null ? 0 : files.length) +
                        1;
                fileList = new String[len];
                fileList[i++] = PARENT_DIR;
            }

            if (dirs != null) {
                Arrays.sort(dirs);
                for (File dir: dirs) {
                    fileList[i++] = dir.getName();
                }
            }

            if (files != null) {
                Arrays.sort(files);
                for (File file: files) {
                    fileList[i++] = file.getName();
                }
            }

            // refresh the user interface
            dialog.setTitle(currentPath.getPath());
            listView.setAdapter(new ArrayAdapter<String>(activity,
                    android.R.layout.simple_list_item_1, fileList) {
                @Override
                public View getView(int pos, View view, ViewGroup parent) {
                    view = super.getView(pos, view, parent);
                    ((TextView) view).setSingleLine(true);
                    return view;
                }
            });

        }
    }

    /**
     * Converts a relative filename to a File object with absolute pathname
     * @param fileChosen - The chosen file
     * @return A File object
     */
    private File getChosenFile(String fileChosen) {
        if (fileChosen.equals(PARENT_DIR)) {
            return currentPath.getParentFile();
        }
        else {
            return new File(currentPath, fileChosen);
        }
    }


    public void setExtension(String extension) {
        this.extension = (extension == null) ? null : extension.toLowerCase();
    }

    // file selection event handling
    public interface FileSelectedListener {
        void fileSelected(File file);
    }

    public FileChooser setFileListener(FileSelectedListener listener) {
        this.listener = listener;
        return this;
    }
}
