package net.namekdev.entity_tracker.network.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

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

	protected boolean _isRunning;
	private final byte[] _buffer = new byte[10240];
	private int _incomingSize = -1;

	private static final byte[] heartbeat;
	private long _lastHeartbeatTime = System.currentTimeMillis();

	static {
		heartbeat = new byte[IntegerBYTES];
		Arrays.fill(heartbeat, (byte) 0);
	}

	public RawConnectionCommunicator connectionListener;

	/**
	 * Time between heartbeats, specified in milliseconds.
	 */
	public int heartbeatDelay = 1000;


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
	 * @param serverName
	 * @param serverPort
	 */
	public Client connect(String serverName, int serverPort) {
		if (isConnected()) {
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
	 * Checks for new bytes in network buffer. Also sends hearbeats.
	 *
	 * <p>This method can be run manually or used through {@link #startThread()}.</p>
	 */
	public boolean update() {
		if (_isRunning && isConnected()) {
			try {
				int n = input.available();

				if (n == 0) {
					long currentTime = System.currentTimeMillis();

					if (currentTime - _lastHeartbeatTime > heartbeatDelay) {
						_lastHeartbeatTime = currentTime;

						output.write(heartbeat, 0, heartbeat.length);
						output.flush();
					}
				}
				else do {
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
			catch (Exception e) {
				_isRunning = false;
			}
		}

		return _isRunning;
	}

	public boolean isConnected() {
		return _isRunning && socket != null && !socket.isClosed() && !socket.isOutputShutdown();
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

	public void send(byte[] buffer, int offset, int length) {
		outputListener.send(buffer, offset, length);
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
				if (!update()) {
					connectionListener.disconnected();
				}

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
			_isRunning = false;
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
