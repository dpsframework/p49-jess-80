package jess.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

class CommandSocketConnector extends Thread {
    private final DebugListener m_server;
    private int m_port;

    public CommandSocketConnector(DebugListener server, int port) {
        m_server = server;
        m_port = port;
        setDaemon(true);
    }

    public void run() {
        try {
            final ServerSocket cmd = new ServerSocket(m_port);
            Socket s = cmd.accept();
            m_server.setCmdSource(new InputStreamReader(s.getInputStream()));
            m_server.setCmdSink(new OutputStreamWriter(s.getOutputStream()));
        } catch (IOException e) {
            return;
        }
    }
}
