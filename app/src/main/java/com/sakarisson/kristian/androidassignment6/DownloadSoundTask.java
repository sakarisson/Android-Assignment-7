package com.sakarisson.kristian.androidassignment6;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DownloadSoundTask extends AsyncTask<String, Integer, String> {
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        System.out.println("Starting download");
    }

    @Override
    protected String doInBackground(String... sUrl) {
        InputStream input = null;
        OutputStream output = null;
        File downloadsDirectory;
        File outputFile = null;
        HttpURLConnection connection = null;
        URL url;
        String downloadPath;
        File extractPath = null;
        String fileName;
        try {
            url = new URL(sUrl[0]);
            downloadPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/dialpad/downloads//";
            extractPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/dialpad/sounds//");
            fileName = sUrl[0].substring(sUrl[0].lastIndexOf('/') + 1);
            downloadsDirectory = new File(downloadPath);
            downloadsDirectory.mkdirs();
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage();
            }

            // this will be useful to display download percentage
            // might be -1: server did not report the length
            int fileLength = connection.getContentLength();

            // download the file
            input = connection.getInputStream();
            outputFile = new File(downloadsDirectory, fileName);
            output = new FileOutputStream(outputFile);

            byte data[] = new byte[4096];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                // allow canceling with back button
                if (isCancelled()) {
                    input.close();
                    return null;
                }
                total += count;
                // publishing the progress....
                if (fileLength > 0) // only if total length is known
                    publishProgress((int) (total * 100 / fileLength));
                output.write(data, 0, count);
            }
        } catch (Exception e) {
            return e.toString();
        } finally {
            try {
                if (output != null) {
                    output.close();
                }
                if (input != null) {
                    input.close();
                }
                if (outputFile != null) {
                    unzip(outputFile, extractPath);
                }
            } catch (IOException ignored) {
                // ignore...
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(String file_url) {
        System.out.println("Downloaded");
    }

    // Since we have already been provided a Java file that does zip handling
    // I figured that it would probably be okay to copy similar code from SO:
    // https://stackoverflow.com/a/27050680/8857465
    private static void unzip(File zipFile, File targetDirectory) throws IOException {
        ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile)));
        try {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(targetDirectory, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new FileNotFoundException("Failed to ensure directory: " +
                            dir.getAbsolutePath());
                if (ze.isDirectory())
                    continue;
                FileOutputStream fout = new FileOutputStream(file);
                try {
                    while ((count = zis.read(buffer)) != -1)
                        fout.write(buffer, 0, count);
                } finally {
                    fout.close();
                }
            }
        } finally {
            zis.close();
        }
    }
}
