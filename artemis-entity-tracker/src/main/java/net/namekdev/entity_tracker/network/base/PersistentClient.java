package net.namekdev.entity_tracker.network.base;


/**
 * Client that automatically reconnects.
 *
 * @author Namek
 */
public class PersistentClient extends Client {
    private volatile boolean isReconnectEnabled;

    /**
     * Delay between two reconnects, specified in milliseconds.
     */
    public int reconnectDelay = 1000;


    public PersistentClient(RawConnectionCommunicator connectionListener) {
        super.setConnectionListener(connectionListener);
    }

    @Override
    public Client connect(String serverName, int serverPort) {
        return connect(serverName, serverPort, false);
    }

    public Client connect(final String serverName, final int serverPort, final boolean manualUpdate) {
        isReconnectEnabled = true;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                tryConnect();

                while (isReconnectEnabled) {
                    if (!isConnected()) {
                        tryConnect();
                    }

                    try {
                        Thread.sleep(reconnectDelay);
                    } catch (InterruptedException e) {
                    }
                }
            }

            private void tryConnect() {
                try {
                    PersistentClient.super.connect(serverName, serverPort);

                    if (!manualUpdate)
                        PersistentClient.super.startThread();
                } catch (Exception ex) {
                }
            }
        });

        thread.start();

        return this;
    }

    @Override
    public void stop() {
        super.stop();
        isReconnectEnabled = false;
    }
}
