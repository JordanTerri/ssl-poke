package org.jtbox.sslpoke;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * An improved version of the SSLPoke found on internet and HTTP oriented.
 */
public class HttpsPoke {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: " + HttpsPoke.class.getName() + " <host> <port>");
            System.exit(1);
        }

        var tunnelHost = System.getProperty("https.proxyHost", null);
        var tunnelPort = Integer.getInteger("https.proxyPort", 0);
        var sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket sslSocket = connectTlsSocket(
                sslSocketFactory,
                tunnelHost,
                tunnelPort,
                args[0],
                Integer.parseInt(args[1]))) {
            var sslParams = new SSLParameters();
            sslParams.setEndpointIdentificationAlgorithm("HTTPS");
            sslSocket.setSSLParameters(sslParams);
            var in = sslSocket.getInputStream();
            var out = sslSocket.getOutputStream();
            out.write(1); // Write a test byte to get a reaction :)
            while (in.available() > 0) {
                System.out.print(in.read());
            }
            System.out.println("Successfully connected");
        } catch (Exception exception) {
            exception.printStackTrace();
            System.exit(1);
        }
    }

    private static SSLSocket connectTlsSocket(SSLSocketFactory sslSocketFactory, String tunnelHost, int tunnelPort, String host, int port) 
            throws IOException {
        if (tunnelHost != null || tunnelPort != 0) {
            var tunnel = new Socket(tunnelHost, tunnelPort);
            connectTunnel(tunnel, host, port);
            return (SSLSocket) sslSocketFactory.createSocket(tunnel, host, port, true);
        }
        
        return (SSLSocket) sslSocketFactory.createSocket(host, port);
    }


    private static void connectTunnel(Socket proxySocket, String proxyHost, int proxyPort)
            throws IOException {
        var bw = new BufferedWriter(new OutputStreamWriter(proxySocket.getOutputStream(), "ASCII7"));
        bw.write("CONNECT " + proxyHost + ":" + proxyPort + " HTTP/1.1\n"
                 + "Proxy-Connection: Keep-Alive"
                 + "User-Agent: " + HttpsPoke.class.getName()
                 + "\r\n\r\n");
        bw.flush();

        var br = new BufferedReader(new InputStreamReader(proxySocket.getInputStream(), "ASCII7"));
        var statusLine = br.lines()
                           .findFirst()
                           .orElseThrow(() -> new IOException("Unexpected EOF from proxy"));

        if (!statusLine.startsWith("HTTP/1.1 200")) {
            throw new IOException("Unable to tunnel through "
                                  + proxySocket.getInetAddress().getHostAddress() + ":" + proxySocket.getPort()
                                  + ".  Proxy returns \"" + statusLine + "\"");
        }
    }
}
