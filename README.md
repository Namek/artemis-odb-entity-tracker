# Entity Tracker
[![Build Status](https://travis-ci.org/Namek/artemis-odb-entity-tracker.svg?branch=master)](https://travis-ci.org/Namek/artemis-odb-entity-tracker)

Server and Client that provides online tracking of [artemis-odb](https://github.com/junkdog/artemis-odb) World state.

![screenshot](/screenshot.png?raw=true)

## Installation

### Maven

```xml
<dependency>
	<groupId>net.namekdev.entity_tracker</groupId>
	<artifactId>artemis-entity-tracker</artifactId>
	<version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
dependencies { compile "net.namekdev.entity_tracker:artemis-entity-tracker:0.1.0-SNAPSHOT" }
```


## Simple Usage


```java
artemisWorld.setManager(new EntityTracker(new EntityTrackerMainWindow()));
```

## Network Connection

Host your Entity Tracker Server:
```java
EntityTrackerServer entityTrackerServer = new EntityTrackerServer();
entityTrackerServer.start();
artemisWorld.setManager(new EntityTracker(entityTrackerServer));
```

For GUI see `Build` section or run [StandaloneMain.java](artemis-entity-tracker-gui/src/main/java/net/namekdev/entity_tracker/StandaloneMain.java) file or setup GUI manually:
```java
final EntityTrackerMainWindow window = new EntityTrackerMainWindow();
final Client client = new PersistentClient(new ExternalInterfaceCommunicator(window));

client.connect(serverName, serverPort);
```

## Custom Local/Networked Listener

Generally speaking, `EntityTracker` expects `WorldUpdateListener` interface implementation, e.g. it may be some window listener.

To achieve network version one can just mimic implementation of `ExternalInterfaceCommunicator` by implementing `Communicator` interface and passing it to `Client`.


## Build

Build libraries with sources:

`mvn clean package`

Build GUI client app as external executable:

`mvn clean package -P app` and you'll find `artemis-entity-tracker-gui/target/artemis-entity-tracker-gui-{version}-app.jar`
