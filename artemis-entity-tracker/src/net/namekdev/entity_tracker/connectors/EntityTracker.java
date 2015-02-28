package net.namekdev.entity_tracker.connectors;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.namekdev.entity_tracker.model.AspectInfo;
import net.namekdev.entity_tracker.model.EntitySystemInfo;
import net.namekdev.entity_tracker.model.ManagerInfo;
import net.namekdev.entity_tracker.utils.ReflectionUtils;

import com.artemis.Aspect;
import com.artemis.AspectSubscriptionManager;
import com.artemis.BaseSystem;
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
	public WorldUpdateListener updateListener;

	public final Bag<EntitySystemInfo> systemsInfo = new Bag<EntitySystemInfo>();
	public final Map<String, EntitySystemInfo> systemsInfoByName = new HashMap<String, EntitySystemInfo>();

	public final Bag<ManagerInfo> managersInfo = new Bag<ManagerInfo>();
	public final Map<String, ManagerInfo> managersInfoByName = new HashMap<String, ManagerInfo>();

	protected Method entity_getComponentBits;
	protected ComponentTypeFactory typeFactory;
	protected Bag<ComponentType> allComponentTypes;

	private int _notifiedComponentTypesCount = 0;


	public EntityTracker() {
	}

	public EntityTracker(WorldUpdateListener listener) {
		this.updateListener = listener;
	}


	@Override
	protected void initialize() {
		entity_getComponentBits = ReflectionUtils.getHiddenMethod(Entity.class, "getComponentBits");
		typeFactory = (ComponentTypeFactory) ReflectionUtils.getHiddenFieldValue(ComponentManager.class, "typeFactory", world.getComponentManager());
		allComponentTypes = (Bag<ComponentType>) ReflectionUtils.getHiddenFieldValue(ComponentTypeFactory.class, "types", typeFactory);

		find42();
	}

	private void find42() {
		ImmutableBag<BaseSystem> systems = world.getSystems();
		for (int i = 0, n = systems.size(); i < n; ++i) {
			BaseSystem system = systems.get(i);

			Class<? extends BaseSystem> systemType = system.getClass();
			String systemName = systemType.getSimpleName();
			Aspect aspect = null;
			BitSet actives = null;
			EntitySubscription subscription = null;

			if (system instanceof EntitySystem) {
				EntitySystem entitySystem = (EntitySystem) system;

				subscription = entitySystem.getSubscription();
				aspect = subscription.getAspect();
				actives = subscription.getActiveEntityIds();
			}

			AspectInfo aspectInfo = new AspectInfo();
			if (aspect != null) {
				aspectInfo.allTypesBitset = aspect.getAllSet();
				aspectInfo.oneTypesBitset = aspect.getOneSet();
				aspectInfo.exclusionTypesBitset = aspect.getExclusionSet();
			}

			EntitySystemInfo info = new EntitySystemInfo(systemName, system, aspect, aspectInfo, actives, subscription);
			systemsInfo.add(info);
			systemsInfoByName.put(systemName, info);

			updateListener.addedSystem(systemName, aspectInfo.allTypesBitset, aspectInfo.oneTypesBitset, aspectInfo.exclusionTypesBitset);
		}

		ImmutableBag<Manager> managers = world.getManagers();
		for (int i = 0, n = managers.size(); i < n; ++i) {
			Manager manager = managers.get(i);

			Class<? extends Manager> managerType = manager.getClass();
			String managerName = managerType.getSimpleName();

			ManagerInfo info = new ManagerInfo(managerName, manager);
			managersInfo.add(info);
			managersInfoByName.put(managerName, info);

			updateListener.addedManager(managerName);
		}
	}

	private BitSet componentsToAspectBitset(Collection<Class<? extends Component>> componentTypes) {
		BitSet bitset = new BitSet(allComponentTypes.size());

		for (Class<? extends Component> componentType : componentTypes) {
			int index = typeFactory.getIndexFor(componentType);
			bitset.set(index);
		}

		return bitset;
	}

	@Override
	public void added(Entity e) {
		if (updateListener == null || (updateListener.getListeningBitset() & WorldUpdateListener.ENTITY_ADDED) == 0) {
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
			inspectNewComponentTypesAndNotify();
		}

		updateListener.addedEntity(e.id, (BitSet) componentBitset.clone());
	}

	@Override
	public void deleted(Entity e) {
		if (updateListener == null || (updateListener.getListeningBitset() & WorldUpdateListener.ENTITY_DELETED) == 0) {
			return;
		}

		updateListener.deletedEntity(e.id);
	}

	private void inspectNewComponentTypesAndNotify() {
		int index = _notifiedComponentTypesCount;
		int n = allComponentTypes.size();

		for (int i = index; i < n; ++i) {
			Class<Component> type = (Class<Component>) ReflectionUtils.getHiddenFieldValue(ComponentType.class, "type", allComponentTypes.get(i));
			String componentName = type.getSimpleName();

			updateListener.addedComponentType(i, componentName);
			++_notifiedComponentTypesCount;
		}
	}
}
