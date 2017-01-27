package net.namekdev.entity_tracker.utils.serialization;

import static net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.*;

import java.util.ArrayList;
import java.util.Vector;

import com.artemis.utils.reflect.ClassReflection;
import com.artemis.utils.reflect.Field;

public class ObjectTypeInspector {
	private ArrayList<RegisteredModel> registeredModels = new ArrayList<RegisteredModel>();
	private int lastId = 0;
	
	private static class RegisteredModel {
		public Class<?> type;
		public ObjectModelNode model;
	}
	
	public int getRegisteredModelsCount() {
		return registeredModels.size();
	}
	
	public ObjectModelNode getRegisteredModelByIndex(int index) {
		RegisteredModel model = registeredModels.get(index);
		return model != null ? model.model : null;
	}
	
	
	/**
	 * Returns tree description of class type.
	 */
	public ObjectModelNode inspect(Class<?> type) {
		assert(NetworkSerialization.determineSimpleType(type) == TYPE_UNKNOWN);
		
		return inspectLevels(type, null);
	}

	private ObjectModelNode inspectLevels(Class<?> type, ObjectModelNode parentOfRoot) {
		RegisteredModel registeredModel = findModel(type, parentOfRoot);

		if (registeredModel != null) {
			return registeredModel.model;
		}

		ObjectModelNode root = null;

		if (!type.isArray()) {
			Field[] fields = ClassReflection.getDeclaredFields(type);
	
			ObjectModelNode model = new ObjectModelNode(++lastId, root);
			model.networkType = TYPE_OBJECT;
			model.children = new Vector<>(fields.length);
		
			root = rememberType(type, model).model;
	
			for (Field field : fields) {
				Class<?> fieldType = field.getType();
				ObjectModelNode child = null;
	
				if (fieldType.isArray()) {
					child = inspectArrayType(fieldType, root);
				}
				else {
					byte networkType = NetworkSerialization.determineSimpleType(fieldType);
	
					if (networkType == TYPE_UNKNOWN) {
						child = new ObjectModelNode(++lastId, root).copyFrom(
							inspectLevels(fieldType, root)
						);
					}
					else {
						child = new ObjectModelNode(++lastId, root);
						child.networkType = networkType;
					}
				}
	
				// TODO because of this we may have to clone what's inside of RegisteredModel
				assert(child.name == null);
				child.name = field.getName();
	
				model.children.addElement(child);
			}
			
			return model;
		}
		else {
			return inspectArrayType(type, parentOfRoot);
		}
	}

	private ObjectModelNode inspectArrayType(Class<?> fieldType, ObjectModelNode parent) {
		ObjectModelNode model = new ObjectModelNode(++lastId, parent);
		// TODO rememberType here ? or maybe if arrayElType == TYPE_TREE
		
		Class<?> arrayElType = fieldType.getComponentType();
		byte arrayType = determineSimpleType(arrayElType);

		
		// TODO probably that should inspect deeper anyway!
		if (!(arrayElType instanceof Object) && !isSimpleType(arrayType)) {
//			model = inspectLevels(arrayElType, root);
//
//			if (model.networkType == TYPE_TREE) {
//				arrayType = TYPE_TREE;
//			}
			
			arrayType = arrayElType.isArray() ? TYPE_ARRAY : TYPE_OBJECT;
		}

		model.networkType = TYPE_ARRAY;
		model.arrayType = arrayType;

		return model;
	}

	private RegisteredModel findModel(final Class<?> type, ObjectModelNode parent) {
		for (RegisteredModel registered : registeredModels) {
			boolean sameParentModel = (parent == null && registered.model.parent == null)
				|| (parent != null && parent.equals(registered.model));
			
			if (registered.type.equals(type)) {
				boolean isCyclicModel = false; 
				
				// go through parents models to find out a (indirect?) cyclic dependency
				ObjectModelNode par = parent;
				while (par != null) {
					if (par.equals(registered.model)) {
						isCyclicModel = true;
					}
					par = par.parent;
				}
				
				if (sameParentModel || isCyclicModel) {
					return registered;
				}
			}
		}
		
		return null;
	}

	private RegisteredModel rememberType(Class<?> type, ObjectModelNode model) {
		RegisteredModel newModel = new RegisteredModel();
		newModel.type = type;
		newModel.model = model;
		this.registeredModels.add(newModel);
		return newModel;
	}
}
