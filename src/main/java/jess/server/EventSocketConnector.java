package jess.server;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

class EventSocketConnector extends Thread {
    private final DebugListener m_server;
    private int m_port;

    public EventSocketConnector(DebugListener server, int port) {
        m_server = server;
        m_port = port;
    }

    public void run() {
        try {
            final ServerSocket evt = new ServerSocket(m_port);
            Socket s = evt.accept();
            m_server.setEventSink(new OutputStreamWriter(s.getOutputStream()));
        } catch (IOException e) {
            return;
        }
    }
}
