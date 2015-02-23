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

	protected Socket socket;
	protected Thread thread;
	protected InputStream input;
	protected OutputStream output;

	private boolean _isRunning;
	private final byte[] _buffer = new byte[10240];
	private int _posLeft = 0, _posRight = 0;

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
		while (_isRunning && !socket.isClosed()) {
			int n = 0;
			try {
				n = input.available();
			}
			catch (IOException e) {
				if (_isRunning) {
					throw new RuntimeException(e);
				}
				return;
			}

			if (_posRight == _buffer.length || n == 0) {
				if (_posRight > 0) {
					int consumed = 0;

					do {
						int length = _posRight - _posLeft;
						consumed = connectionListener.bytesReceived(_buffer, _posLeft, length);
						_posLeft += consumed;
					}
					while (consumed > 0 && _posLeft < _posRight);
				}

				if (_posRight == _posLeft) {
					_posLeft = _posRight = 0;
				}

				return;
			}

			n = Math.min(n, _buffer.length - _posRight);

			try {
				input.read(_buffer, _posRight, n);
			}
			catch (IOException e) {
				if (_isRunning) {
					throw new RuntimeException(e);
				}
				return;
			}

			_posRight += n;
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
				output.write(buffer, offset, length);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	};
}
