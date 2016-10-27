package net.namekdev.entity_tracker;

import java.util.HashMap;
import java.util.Map;

import com.artemis.Aspect;
import com.artemis.BaseComponentMapper;
import com.artemis.BaseEntitySystem;
import com.artemis.BaseSystem;
import com.artemis.Component;
import com.artemis.ComponentManager;
import com.artemis.ComponentMapper;
import com.artemis.ComponentType;
import com.artemis.ComponentTypeFactory;
import com.artemis.Entity;
import com.artemis.EntitySubscription;
import com.artemis.EntitySubscription.SubscriptionListener;
import com.artemis.Manager;
import com.artemis.utils.Bag;
import com.artemis.utils.BitVector;
import com.artemis.utils.ImmutableBag;
import com.artemis.utils.IntBag;
import com.artemis.utils.reflect.ClassReflection;
import com.artemis.utils.reflect.Field;
import com.artemis.utils.reflect.Method;
import com.artemis.utils.reflect.ReflectionException;
import net.namekdev.entity_tracker.connectors.WorldController;
import net.namekdev.entity_tracker.connectors.WorldUpdateListener;
import net.namekdev.entity_tracker.model.AspectInfo;
import net.namekdev.entity_tracker.model.ComponentTypeInfo;
import net.namekdev.entity_tracker.model.FieldInfo;
import net.namekdev.entity_tracker.model.ManagerInfo;
import net.namekdev.entity_tracker.model.SystemInfo;
import net.namekdev.entity_tracker.utils.ArrayPool;
import net.namekdev.entity_tracker.utils.ReflectionUtils;
import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization;

/**
 * @author Namek
 *
 */
public class EntityTracker extends Manager implements WorldController {
	private WorldUpdateListener updateListener;

	public final Bag<SystemInfo> systemsInfo = new Bag<SystemInfo>();
	public final Map<String, SystemInfo> systemsInfoByName = new HashMap<String, SystemInfo>();

	public final Bag<ManagerInfo> managersInfo = new Bag<ManagerInfo>();
	public final Map<String, ManagerInfo> managersInfoByName = new HashMap<String, ManagerInfo>();
	public final Map<Class<Component>, ComponentTypeInfo> allComponentTypesInfoByClass = new HashMap<Class<Component>, ComponentTypeInfo>();
	public final Bag<ComponentTypeInfo> allComponentTypesInfo = new Bag<ComponentTypeInfo>();
	public final Bag<BaseComponentMapper<Component>> allComponentMappers = new Bag<>();


	protected Method entity_getComponentBits;
	protected ComponentTypeFactory typeFactory;
	protected Bag<ComponentType> allComponentTypes;

	private int _notifiedComponentTypesCount = 0;
	private final ArrayPool<Object> _objectArrPool = new ArrayPool<>(Object.class);


	public EntityTracker() {
	}

	public EntityTracker(WorldUpdateListener listener) {
		setUpdateListener(listener);
	}

	public void setUpdateListener(WorldUpdateListener listener) {
		this.updateListener = listener;
		listener.injectWorldController(this);
	}


	@Override
	protected void initialize() {
		entity_getComponentBits = ReflectionUtils.getHiddenMethod(Entity.class, "getComponentBits");
		typeFactory = (ComponentTypeFactory) ReflectionUtils.getHiddenFieldValue(ComponentManager.class, "typeFactory", world.getComponentManager());
		allComponentTypes = (Bag<ComponentType>) ReflectionUtils.getHiddenFieldValue(ComponentTypeFactory.class, "types", typeFactory);

		find42UnicornManagers();
	}

	private void find42UnicornManagers() {
		ImmutableBag<BaseSystem> systems = world.getSystems();
		int index = 0;
		for (int i = 0, n = systems.size(); i < n; ++i) {
			BaseSystem system = systems.get(i);

			if (system instanceof Manager) {
				continue;
			}

			Class<? extends BaseSystem> systemType = system.getClass();
			String systemName = systemType.getSimpleName();
			Aspect aspect = null;
			BitVector actives = null;
			EntitySubscription subscription = null;

			if (system instanceof BaseEntitySystem) {
				BaseEntitySystem entitySystem = (BaseEntitySystem) system;

				subscription = entitySystem.getSubscription();
				aspect = subscription.getAspect();
				actives = subscription.getActiveEntityIds();
			}

			AspectInfo aspectInfo = new AspectInfo();
			if (aspect != null) {
				aspectInfo.allTypes = aspect.getAllSet();
				aspectInfo.oneTypes = aspect.getOneSet();
				aspectInfo.exclusionTypes = aspect.getExclusionSet();
			}

			SystemInfo info = new SystemInfo(index, systemName, system, aspect, aspectInfo, actives, subscription);
			systemsInfo.add(info);
			systemsInfoByName.put(systemName, info);

			if (subscription != null) {
				listenForEntitySetChanges(info);
			}

			updateListener.addedSystem(index++, systemName, aspectInfo.allTypes, aspectInfo.oneTypes, aspectInfo.exclusionTypes);
		}

		for (int i = 0, n = systems.size(); i < n; ++i) {
			BaseSystem system = systems.get(i);

			if (!(system instanceof Manager)) {
				continue;
			}

			Manager manager = (Manager) system;

			Class<? extends Manager> managerType = manager.getClass();
			String managerName = managerType.getSimpleName();

			ManagerInfo info = new ManagerInfo(managerName, manager);
			managersInfo.add(info);
			managersInfoByName.put(managerName, info);

			updateListener.addedManager(managerName);
		}
	}

	private void listenForEntitySetChanges(final SystemInfo info) {
		info.subscription.addSubscriptionListener(new SubscriptionListener() {
			@Override
			public void removed(IntBag entities) {
				info.entitiesCount -= entities.size();

				if (updateListener != null && (updateListener.getListeningBitset() & WorldUpdateListener.ENTITY_SYSTEM_STATS) != 0) {
					updateListener.updatedEntitySystem(info.systemIndex, info.entitiesCount, info.maxEntitiesCount);
				}
			}

			@Override
			public void inserted(IntBag entities) {
				info.entitiesCount += entities.size();

				if (info.entitiesCount > info.maxEntitiesCount) {
					info.maxEntitiesCount = info.entitiesCount;
				}

				if (updateListener != null && (updateListener.getListeningBitset() & WorldUpdateListener.ENTITY_SYSTEM_STATS) != 0) {
					updateListener.updatedEntitySystem(info.systemIndex, info.entitiesCount, info.maxEntitiesCount);
				}
			}
		});
	}

	@Override
	public void added(Entity e) {
		if (updateListener == null || (updateListener.getListeningBitset() & WorldUpdateListener.ENTITY_ADDED) == 0) {
			return;
		}

		BitVector componentBitVector = null;
		try {
			componentBitVector = (BitVector) entity_getComponentBits.invoke(e);
		}
		catch (ReflectionException exc) {
			throw new RuntimeException(exc);
		}

		if (componentBitVector.length() > _notifiedComponentTypesCount) {
			inspectNewComponentTypesAndNotify();
		}

		updateListener.addedEntity(e.getId(), new BitVector(componentBitVector));
	}

	@Override
	public void deleted(Entity e) {
		if (updateListener == null || (updateListener.getListeningBitset() & WorldUpdateListener.ENTITY_DELETED) == 0) {
			return;
		}

		updateListener.deletedEntity(e.getId());
	}

	private void inspectNewComponentTypesAndNotify() {
		int index = _notifiedComponentTypesCount;
		int n = allComponentTypes.size();

		for (int i = index; i < n; ++i) {
			Class<Component> type = (Class<Component>) ReflectionUtils.getHiddenFieldValue(ComponentType.class, "type", allComponentTypes.get(i));

			ComponentTypeInfo info = inspectComponentType(type);
			info.index = i;

			allComponentTypesInfoByClass.put(type, info);
			allComponentTypesInfo.set(i, info);
			allComponentMappers.set(i, ComponentMapper.getFor(type, world));

			updateListener.addedComponentType(i, info);
			++_notifiedComponentTypesCount;
		}
	}

	private ComponentTypeInfo inspectComponentType(Class<Component> type) {
		Field[] fields = ClassReflection.getDeclaredFields(type);

		ComponentTypeInfo info = new ComponentTypeInfo(type);

		for (Field field : fields) {
			info.fields.add(FieldInfo.reflectField(field));
		}

		return info;
	}


	//////////////////////////////////////
	// World Controller interface

	@Override
	public void setSystemState(String name, boolean isOn) {
		SystemInfo info = systemsInfoByName.get(name);
		info.system.setEnabled(isOn);
	}
	
	@Override
	public void setManagerState(String name, boolean isOn) {
		ManagerInfo info = managersInfoByName.get(name);
		info.manager.setEnabled(isOn);
	}

	@Override
	public void requestComponentState(int entityId, int componentIndex) {
		final ComponentTypeInfo info = allComponentTypesInfo.get(componentIndex);
		final BaseComponentMapper<Component> mapper = allComponentMappers.get(componentIndex);

		Object component = mapper.get(entityId);

		int size = info.fields.size();
		Object[] values = _objectArrPool.obtain(size, true);

		for (int i = 0; i < size; ++i) {
			FieldInfo fieldInfo = info.fields.get(i);

			// TODO no support for arrays, yet
			if (fieldInfo.isArray) {
				values[i] = null;
			}
			else {
				values[i] = ReflectionUtils.getFieldValue(fieldInfo.field, component);
			}
		}

		updateListener.updatedComponentState(entityId, componentIndex, values);
		_objectArrPool.free(values, true);
	}

	@Override
	public void setComponentFieldValue(int entityId, int componentIndex, int fieldIndex, Object value) {
		final ComponentTypeInfo info = allComponentTypesInfo.get(componentIndex);
		final BaseComponentMapper<Component> mapper = allComponentMappers.get(componentIndex);

		Object component = mapper.get(entityId);
		FieldInfo fieldInfo = info.fields.get(fieldIndex);

		// TODO we should receive appropriate type here. Convert Strings on GUI side.
		if (value instanceof String) {
			value = NetworkSerialization.convertStringToTypedValue((String) value, fieldInfo.valueType);
		}

		try {
			fieldInfo.field.set(component, value);
		}
		catch (ReflectionException e) {
			e.printStackTrace();
		}
	}
}
