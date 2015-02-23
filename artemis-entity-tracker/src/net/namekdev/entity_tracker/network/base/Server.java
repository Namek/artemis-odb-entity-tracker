package net.namekdev.entity_tracker.network.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.artemis.utils.Bag;

/**
 * Multi-threaded multi-client server.
 *
 * @author Namek
 */
public class Server implements Runnable {
	public static final int DEFAULT_PORT = 87;

	protected int listeningPort = DEFAULT_PORT;
	protected ServerSocket socket;
	protected boolean isRunning;
	protected Thread runningThread;
	protected final Bag<Client> clients = new Bag<Client>();

	protected RawConnectionCommunicatorProvider clientListenerProvider;
	protected int listeningBitset;


	public Server(RawConnectionCommunicatorProvider clientListenerProvider) {
		this.clientListenerProvider = clientListenerProvider;
	}

	public Server(RawConnectionCommunicatorProvider clientListenerProvider, int listeningPort) {
		this.clientListenerProvider = clientListenerProvider;
		this.listeningPort = listeningPort;
	}

	protected Server() {
	}

	/**
	 * Starts listening in new thread.
	 */
	public Server start() {
		if (socket != null && !socket.isClosed()) {
			throw new IllegalStateException("Cannot serve twice in the same time.");
		}

		try {
			socket = new ServerSocket(listeningPort);
		}
		catch (IOException e) {
			throw new RuntimeException("Couldn't start server on port " + listeningPort, e);
		}
		runningThread = new Thread(this);
		runningThread.start();

		return this;
	}

	public void stop() {
		this.isRunning = false;

		for (int i = 0, n = clients.size(); i < n; ++i) {
			Client client = clients.get(i);
			client.stop();
			clients.remove(client);
		}

		try {
			socket.close();
		}
		catch (IOException e) {
			throw new RuntimeException("Couldn't shutdown server.", e);
		}
	}

	@Override
	public void run() {
		synchronized (this) {
			this.runningThread = Thread.currentThread();
		}

		isRunning = true;

		while (isRunning) {
			Socket clientSocket = null;
			try {
				clientSocket = socket.accept();
				clientSocket.setTcpNoDelay(true);
			}
			catch (IOException e) {
				if (isRunning) {
					throw new RuntimeException("Error accepting client connection", e);
				}

				return;
			}

			Client client = createSocketListener(clientSocket);
			client.initSocket();
			Thread clientThread = new Thread(client.threadRunnable);

			clients.add(client);
			clientThread.start();
		}
	}

	protected Client createSocketListener(Socket socket) {
		RawConnectionCommunicator connectionListener = clientListenerProvider.getListener(socket.getRemoteSocketAddress().toString());
		return new Client(socket, connectionListener);
	}
}
