package net.namekdev.entity_tracker.network;

import java.util.BitSet;

import net.namekdev.entity_tracker.connectors.UpdateListener;

/**
 * Deserializes data from network and serializes data sent to the network.
 * Manages between logic events and pure network bytes.
 *
 * Communicator used by EntityTracker manager (server).
 * Direction: server to client = entity tracker to window
 *
 * @author Namek
 */
public class EntityTrackerCommunicator extends Communicator implements UpdateListener {
	@Override
	public void disconnected() {
		// TODO Auto-generated method stub

	}

	@Override
	public void bytesReceived(byte[] bytes, int offset, int length) {
		// TODO deserialize data
	}


	@Override
	public int getListeningBitset() {
		return ENTITY_ADDED | ENTITY_DELETED;
	}

	@Override
	public void addedSystem(String name, BitSet allTypes, BitSet oneTypes, BitSet notTypes) {
		send(
			beginPacket(TYPE_ADDED_ENTITY_SYSTEM)
			.addString(name)
			.addBitSet(allTypes)
			.addBitSet(oneTypes)
			.addBitSet(notTypes)
		);
	}

	@Override
	public void addedManager(String name) {
		send(
			beginPacket(TYPE_ADDED_MANAGER)
			.addString(name)
		);
	}

	@Override
	public void addedComponentType(int index, String name) {
		send(
			beginPacket(TYPE_ADDED_COMPONENT_TYPE)
			.addInt(index)
			.addString(name)
		);
	}

	@Override
	public void addedEntity(int entityId, BitSet components) {
		send(
			beginPacket(TYPE_ADDED_ENTITY)
			.addInt(entityId)
			.addBitSet(components)
		);
	}

	@Override
	public void deletedEntity(int entityId) {
		send(
			beginPacket(TYPE_DELETED_ENTITY)
			.addInt(entityId)
		);
	}
}
