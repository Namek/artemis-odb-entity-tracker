package net.namekdev.entity_tracker.model;

import java.util.BitSet;

import com.artemis.Aspect;
import com.artemis.BaseSystem;
import com.artemis.EntitySubscription;

public class SystemInfo {
	public int systemIndex;
	public String systemName;
	public BaseSystem system;
	public Aspect aspect;
	public AspectInfo aspectInfo;
	public BitSet actives;
	public EntitySubscription subscription;
	public int entitiesCount = 0;
	public int maxEntitiesCount = 0;


	public SystemInfo(int systemIndex, String systemName, BaseSystem system, Aspect aspect, AspectInfo aspectInfo, BitSet actives, EntitySubscription subscription) {
		this.systemIndex = systemIndex;
		this.systemName = systemName;
		this.system = system;
		this.aspect = aspect;
		this.aspectInfo = aspectInfo;
		this.actives = actives;
		this.subscription = subscription;
	}
}