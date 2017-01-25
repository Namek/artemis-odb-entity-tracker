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
		
		// TODO public String name

		// when it's null it defines a class, otherwise it's field
		public RegisteredModel parent;
	}
	
	
	/**
	 * Returns tree description of class type.
	 */
	public ObjectModelNode inspect(Class<?> type) {
		assert(NetworkSerialization.determineSimpleType(type) == TYPE_UNKNOWN);
		
		return inspectLevels(type, null);
	}

	private ObjectModelNode inspectLevels(Class<?> type, RegisteredModel parentOfRoot) {
		RegisteredModel root = findModel(type, parentOfRoot);

		if (root != null) {
			return root.model;
		}

		if (!type.isArray()) {
			Field[] fields = ClassReflection.getDeclaredFields(type);
	
			ObjectModelNode model = new ObjectModelNode(++lastId);
			model.networkType = TYPE_TREE;
			model.children = new Vector<>(fields.length);
		
			root = rememberType(type, model, parentOfRoot);
	
			for (Field field : fields) {
				Class<?> fieldType = field.getType();
				ObjectModelNode child = null;
	
				if (fieldType.isArray()) {
					child = inspectArrayType(fieldType, root);
				}
				else {
					byte networkType = NetworkSerialization.determineSimpleType(fieldType);
	
					if (networkType == TYPE_UNKNOWN) {
						child = new ObjectModelNode(++lastId).copyFrom(
							inspectLevels(fieldType, root)
						);
					}
					else {
						child = new ObjectModelNode(++lastId);
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

	private ObjectModelNode inspectArrayType(Class<?> fieldType, RegisteredModel root) {
		ObjectModelNode model = new ObjectModelNode(++lastId);
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
			
			arrayType = arrayElType.isArray() ? TYPE_ARRAY : TYPE_TREE;
		}

		model.networkType = TYPE_ARRAY;
		model.arrayType = arrayType;

		return model;
	}

	private RegisteredModel findModel(final Class<?> type, RegisteredModel parent) {
		for (RegisteredModel registered : registeredModels) {
			boolean sameParentModel = (parent == null && registered.parent == null)
				|| (parent != null && parent.model.equals(registered.model));
			
			if (registered.type.equals(type)) {
				boolean isCyclicModel = false; 
				
				// go through parents models to find out a (indirect?) cyclic dependency
				RegisteredModel par = parent;
				while (par != null) {
					if (par.model.equals(registered.model)) {
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

	private RegisteredModel rememberType(Class<?> type, ObjectModelNode model, RegisteredModel parent) {
		RegisteredModel newModel = new RegisteredModel();
		newModel.type = type;
		newModel.model = model;
		newModel.parent = parent;
		this.registeredModels.add(newModel);
		return newModel;
	}
}
