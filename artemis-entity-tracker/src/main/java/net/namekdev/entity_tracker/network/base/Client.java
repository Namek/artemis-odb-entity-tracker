package net.namekdev.entity_tracker.network.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 *
 * @author Namek
 */
public class Client {
	protected String remoteName;
	protected int serverPort = Server.DEFAULT_PORT;
	private static int IntegerBYTES = Integer.SIZE / 8;

	protected Socket socket;
	protected Thread thread;
	protected InputStream input;
	protected OutputStream output;

	private boolean _isRunning;
	private final byte[] _buffer = new byte[10240];
	private int _incomingSize = -1;

	public RawConnectionCommunicator connectionListener;


	public Client() {
	}

	public Client(RawConnectionCommunicator connectionListener) {
		this.connectionListener = connectionListener;
	}

	Client(Socket socket, RawConnectionCommunicator connectionListener) {
		this.socket = socket;
		this.connectionListener = connectionListener;
		_isRunning = socket.isConnected() && !socket.isClosed();
	}


	/**
	 * Connects to server. You can chain {@code #startThread()} call.
	 *
	 * @param remoteName
	 * @param serverPort
	 */
	public Client connect(String serverName, int serverPort) {
		if (socket != null && !socket.isClosed()) {
			throw new IllegalStateException("Cannot connect twice in the same time.");
		}

		this.remoteName = serverName;
		this.serverPort = serverPort;

		try {
			socket = new Socket(serverName, serverPort);
			initSocket();

			return this;
		}
		catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void startThread() {
		if (!_isRunning) {
			throw new RuntimeException("Call #connect() first!");
		}

		thread = new Thread(threadRunnable);
		thread.start();
	}

	void initSocket() {
		try {
			socket.setTcpNoDelay(true);
			input = socket.getInputStream();
			output = socket.getOutputStream();
			_isRunning = true;

			connectionListener.connected(socket.getRemoteSocketAddress(), outputListener);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Checks for new bytes in network buffer.
	 * This method can be run manually or called automatically by {@link #startThread()}.
	 */
	public void update() {
		if (_isRunning && !socket.isClosed()) {
			try {
				int n = input.available();

				do {
					if (_incomingSize <= 0 && n >= IntegerBYTES) {
						input.read(_buffer, 0, IntegerBYTES);
						_incomingSize = readRawInt(_buffer, 0);
						n -= IntegerBYTES;
						continue;
					}

					if (_incomingSize > 0 && n >= _incomingSize) {
						input.read(_buffer, 0, _incomingSize);
						connectionListener.bytesReceived(_buffer, 0, _incomingSize);
						_incomingSize = 0;
					}

					n = input.available();
				}
				while (n > 0);
			}
			catch (IOException e) {
				if (_isRunning) {
					throw new RuntimeException(e);
				}
				return;
			}
		}
	}

	public void stop() {
		_isRunning = false;

		try {
			input.close();
		} catch (Exception e) { }

		try {
			output.close();
		} catch (Exception e) { }

		try {
			socket.close();
		}
		catch (IOException e) { }
	}

	protected final static int readRawInt(byte[] buffer, int offset) {
		int value = buffer[offset++] & 0xFF;
		value <<= 8;
		value |= buffer[offset++] & 0xFF;
		value <<= 8;
		value |= buffer[offset++] & 0xFF;
		value <<= 8;
		value |= buffer[offset] & 0xFF;

		return value;
	}

	final Runnable threadRunnable = new Runnable() {
		@Override
		public void run() {
			while (_isRunning && !socket.isClosed()) {
				update();

				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e) {
					if (_isRunning) {
						throw new RuntimeException(e);
					}

					return;
				}
			}
		}
	};

	private final RawConnectionOutputListener outputListener = new RawConnectionOutputListener() {
		@Override
		public void send(byte[] buffer, int offset, int length) {
			try {
				output.write((length >> 24) & 0xFF);
				output.write((length >> 16) & 0xFF);
				output.write((length >> 8) & 0xFF);
				output.write(length & 0xFF);
				output.write(buffer, offset, length);
				output.flush();
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	};
}
