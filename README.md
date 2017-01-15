# Entity Tracker
[![Build Status](https://travis-ci.org/Namek/artemis-odb-entity-tracker.svg?branch=master)](https://travis-ci.org/Namek/artemis-odb-entity-tracker)

Server and Client that provides online tracking of [artemis-odb](https://github.com/junkdog/artemis-odb) World state.

![screenshot](/screenshot.png?raw=true)

## Installation

**Note**: Due to https://github.com/Namek/artemis-odb-entity-tracker/issues/7 about artemis-odb 2.x support, the latest version of Entity Tracker is `0.4.0-SNAPSHOT`. If you need this please apply the version to the following configuration.

### Maven

```xml
<dependency>
	<groupId>net.namekdev.entity_tracker</groupId>
	<artifactId>artemis-entity-tracker</artifactId>
	<version>0.3.0</version>
</dependency>

<!-- uncomment in case you need GUI inside your game -->
<!--dependency>
	<groupId>net.namekdev.entity_tracker</groupId>
	<artifactId>artemis-entity-tracker-gui</artifactId>
	<version>0.3.0</version>
</dependency-->
```

### Gradle

```groovy
dependencies {
	compile "net.namekdev.entity_tracker:artemis-entity-tracker:0.3.0"
	
	// uncomment in case you need GUI instantiated directly from your game
	// compile "net.namekdev.entity_tracker:artemis-entity-tracker-gui:0.3.0"
}
```

# How to use

## Option 1. Simple Usage

Import both `Entity Tracker` and `Entity Tracker GUI` libraries into your project.


```java
artemisWorld.setManager(new EntityTracker(new EntityTrackerMainWindow()));
```

## Option 2. Network Connection

Host `Entity Tracker Server` inside your game:
```java
EntityTrackerServer entityTrackerServer = new EntityTrackerServer();
entityTrackerServer.start();
artemisWorld.setManager(new EntityTracker(entityTrackerServer));
```

There are 2 options to run `Entity Tracker GUI` that connects with `EntityTrackerServer`:

1. run external app (you can download `*-app` from [releases](https://github.com/Namek/artemis-odb-entity-tracker/releases)) or build it yourself (see `Build` section below)
2. run [StandaloneMain.java](artemis-entity-tracker-gui/src/main/java/net/namekdev/entity_tracker/StandaloneMain.java) file or setup GUI manually:
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
