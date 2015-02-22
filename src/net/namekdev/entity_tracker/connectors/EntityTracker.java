package net.namekdev.entity_tracker.connectors;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.namekdev.entity_tracker.model.AspectInfo;
import net.namekdev.entity_tracker.model.EntitySystemInfo;
import net.namekdev.entity_tracker.utils.ReflectionUtils;

import com.artemis.Aspect;
import com.artemis.AspectSubscriptionManager;
import com.artemis.Component;
import com.artemis.ComponentManager;
import com.artemis.ComponentType;
import com.artemis.ComponentTypeFactory;
import com.artemis.Entity;
import com.artemis.EntitySubscription;
import com.artemis.EntitySystem;
import com.artemis.Manager;
import com.artemis.utils.Bag;
import com.artemis.utils.ImmutableBag;
import com.artemis.utils.IntBag;
import com.artemis.utils.reflect.Method;
import com.artemis.utils.reflect.ReflectionException;

public class EntityTracker extends Manager {
	public UpdateListener updateListener;

	public final Bag<EntitySystemInfo> systemsInfo = new Bag<EntitySystemInfo>();
	public Map<String, EntitySystemInfo> systemsInfoByName = new HashMap<String, EntitySystemInfo>();

	protected Method entity_getComponentBits;
	protected ComponentTypeFactory typeFactory;
	protected Bag<ComponentType> allComponentTypes;

	private int _notifiedComponentTypesCount = 0;


	public EntityTracker() {
	}

	public EntityTracker(UpdateListener listener) {
		this.updateListener = listener;
	}


	@Override
	protected void initialize() {
		entity_getComponentBits = ReflectionUtils.getHiddenMethod(Entity.class, "getComponentBits");
		typeFactory = (ComponentTypeFactory) ReflectionUtils.getHiddenFieldValue(ComponentManager.class, "typeFactory", world.getComponentManager());
		allComponentTypes = (Bag<ComponentType>) ReflectionUtils.getHiddenFieldValue(ComponentTypeFactory.class, "types", typeFactory);

		findAllAspects();
	}

	private void findAllAspects() {
		AspectSubscriptionManager am = world.getManager(AspectSubscriptionManager.class);

		ImmutableBag<EntitySystem> systems = world.getSystems();
		for (int i = 0, n = systems.size(); i < n; ++i) {
			EntitySystem system = systems.get(i);

			Class<? extends EntitySystem> systemType = system.getClass();
			String systemName = systemType.getSimpleName();
			Aspect.Builder aspect = (Aspect.Builder) ReflectionUtils.getHiddenFieldValue(EntitySystem.class, "aspectConfiguration", system);
			IntBag actives = (IntBag) ReflectionUtils.getHiddenFieldValue(EntitySystem.class, "actives", system);
			EntitySubscription subscription = aspect != null ? am.get(aspect) : null;

			AspectInfo aspectInfo = new AspectInfo();
			if (aspect != null) {
				aspectInfo.allTypes = findComponents("allTypes", aspect);
				aspectInfo.oneTypes = findComponents("oneTypes", aspect);
				aspectInfo.exclusionTypes = findComponents("exclusionTypes", aspect);

				aspectInfo.allTypesBitset = componentsToAspectBitset(aspectInfo.allTypes.values());
				aspectInfo.oneTypesBitset = componentsToAspectBitset(aspectInfo.oneTypes.values());
				aspectInfo.exclusionTypesBitset = componentsToAspectBitset(aspectInfo.exclusionTypes.values());
			}

			EntitySystemInfo info = new EntitySystemInfo(systemName, system, aspect, aspectInfo, actives, subscription);
			systemsInfo.add(info);
			systemsInfoByName.put(systemName, info);

			updateListener.addedEntitySystem(systemName, aspectInfo.allTypesBitset, aspectInfo.oneTypesBitset, aspectInfo.exclusionTypesBitset);
		}
	}

	private Map<String, Class<? extends Component>> findComponents(String fieldName, Aspect.Builder aspect) {
		@SuppressWarnings("unchecked")
		Bag<Class<? extends Component>> types = (Bag<Class<? extends Component>>) ReflectionUtils.getHiddenFieldValue(Aspect.Builder.class, fieldName, aspect);
		Map<String, Class<? extends Component>> namedTypes = new HashMap<String, Class<? extends Component>>();

		for (int i = 0, n = types.size(); i < n; ++i) {
			Class<? extends Component> type = types.get(i);
			String name = type.getSimpleName();

			namedTypes.put(name, type);
		}

		return namedTypes;
	}

	private BitSet componentsToAspectBitset(Collection<Class<? extends Component>> componentTypes) {
		BitSet bitset = new BitSet(allComponentTypes.size());

		for (Class<? extends Component> componentType : componentTypes) {
			int index = typeFactory.getIndexFor(componentType);;
			bitset.set(index);
		}

		return bitset;
	}

	@Override
	public void added(Entity e) {
		if (updateListener == null || (updateListener.getListeningBitset() & UpdateListener.ADDED) == 0) {
			return;
		}

		BitSet componentBitset = null;
		try {
			componentBitset = (BitSet) entity_getComponentBits.invoke(e);
		}
		catch (ReflectionException exc) {
			throw new RuntimeException(exc);
		}

		if (componentBitset.size() > _notifiedComponentTypesCount) {
			inspectNewComponentTypesAndInform();
		}

		updateListener.added(e.id, componentBitset);
	}

	@Override
	public void deleted(Entity e) {
		if (updateListener == null || (updateListener.getListeningBitset() & UpdateListener.DELETED) == 0) {
			return;
		}

		updateListener.deleted(e.id);
	}

	private void inspectNewComponentTypesAndInform() {
		int index = _notifiedComponentTypesCount;
		int n = allComponentTypes.size();

		for (int i = index; i < n; ++i) {
			Class<Component> type = (Class<Component>) ReflectionUtils.getHiddenFieldValue(ComponentType.class, "type", allComponentTypes.get(i));
			String componentName = type.getSimpleName();

			updateListener.addedComponentType(componentName);
			++_notifiedComponentTypesCount;
		}
	}
}
