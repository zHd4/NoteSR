package com.peew.notesr.server;

import android.util.Log;
import com.peew.notesr.App;
import com.peew.notesr.manager.AssignmentsManager;
import com.peew.notesr.model.FileInfo;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class VideoAssignmentViewServer implements Runnable {
    private static final int TIMEOUT = 10000;
    private long fileId;
    private ServerSocket socket;
    private Thread thread;
    private String url;
    private boolean isRunning;

    public VideoAssignmentViewServer(long fileId) {
        this.fileId = fileId;
        this.url = init(0);
    }

    public VideoAssignmentViewServer(long fileId, int port) {
        this.fileId = fileId;
        this.url = init(port);
    }

    private String init(int port) {
        String url = null;

        try {
            socket = new ServerSocket(port, 0, null);
            socket.setSoTimeout(TIMEOUT);

            port = socket.getLocalPort();
            url = "http://" + socket.getInetAddress().getHostAddress() + ":" + port;

            Log.i("NoteSR", "Server started at " + url);
        } catch (UnknownHostException e) {
            Log.e("NoteSR", "Error UnknownHostException server", e);
        } catch (IOException e) {
            Log.e("NoteSR", "Error IOException server", e);
        }

        return url;
    }

    public void start() {
        thread = new Thread(this);
        thread.start();

        isRunning = true;
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                Socket client = socket.accept();

                if (client == null) {
                    continue;
                }

                processRequest(client);

            } catch (IOException e) {
                Log.e("NoteSR", "Error IOException server", e);
            } catch (RuntimeException e) {
                Log.e("NoteSR", "Error RuntimeException server", e);
            }
        }
    }

    private void processRequest(Socket client) throws IOException {
        try (client) {
            try (OutputStream outputStream = client.getOutputStream()) {

                AssignmentsManager assignmentsManager = App.getAppContainer().getAssignmentsManager();
                FileInfo fileInfo = assignmentsManager.getInfo(fileId);

                byte[] headers = getHeaders(fileInfo).getBytes();
                outputStream.write(headers);

                assignmentsManager.read(fileId, part -> {
                    try {
                        outputStream.write(part);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                outputStream.flush();
            }
        }
    }

    private String getHeaders(FileInfo fileInfo) {
        return "HTTP/1.1 200 OK" + "\r\n" +
                "Content-Type: " + fileInfo.getType() +
                "\r\n" +
                "Accept-Ranges: bytes" + "\r\n" +
                "Content-Length: " + fileInfo.getSize() + "\r\n" +
                "\r\n";
    }

    public String getUrl() {
        return url;
    }
}
