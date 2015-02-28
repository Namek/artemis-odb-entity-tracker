package net.namekdev.entity_tracker.model;

import java.util.BitSet;

import com.artemis.Aspect;
import com.artemis.BaseSystem;
import com.artemis.EntitySubscription;

public class EntitySystemInfo {
	public String systemName;
	public BaseSystem system;
	public Aspect aspect;
	public AspectInfo aspectInfo;
	public BitSet actives;
	public EntitySubscription subscription;


	public EntitySystemInfo(String systemName, BaseSystem system, Aspect aspect, AspectInfo aspectInfo, BitSet actives, EntitySubscription subscription) {
		this.systemName = systemName;
		this.system = system;
		this.aspect = aspect;
		this.aspectInfo = aspectInfo;
		this.actives = actives;
		this.subscription = subscription;
	}
}