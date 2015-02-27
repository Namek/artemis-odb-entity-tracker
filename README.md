# Entity Tracker

Server and Client that provides online artemis-odb World state.


# Simple Usage


```java
artemisWorld.setManager(new EntityTracker(new EntityTrackerMainWindow()));
```

# Network Connection

Host your Entity Tracker Server:
```java
EntityTrackerServer entityTrackerServer = new EntityTrackerServer();
entityTrackerServer.start();
artemisWorld.setManager(new EntityTracker(entityTrackerServer));
```

For GUI run [StandaloneMain.java](artemis-entity-tracker-gui/src/net/namekdev/entity_tracker/StandaloneMain.java) in GUI project or:
```java
final EntityTrackerMainWindow window = new EntityTrackerMainWindow();
final Client client = new Client(new ExternalInterfaceCommunicator(window));

client.connect(serverName, serverPort);
client.startThread();
```
