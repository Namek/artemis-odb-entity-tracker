package net.namekdev.entity_tracker.utils.serialization;

import static net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.*;

import java.util.ArrayList;
import java.util.Vector;

import com.artemis.utils.reflect.ClassReflection;
import com.artemis.utils.reflect.Field;

public class ObjectTypeInspector {
	private ArrayList<RegisteredModel> registeredModels = new ArrayList<RegisteredModel>();
	private int lastId = 0;

	private ObjectModelsCollection registeredModelsAsCollection = new ObjectModelsCollection() {
		
		@Override
		public int size() {
			return getRegisteredModelsCount();
		}
		
		@Override
		public ObjectModelNode get(int index) {
			return getRegisteredModelByIndex(index);
		}
		
		@Override
		public ObjectModelNode get(Class<?> type) {
			return inspect(type);
		}
		
		@Override
		public void add(ObjectModelNode model) {
			throw new RuntimeException("this implementation shouldn't manually add models. Inspector should do that automatically.");
		}
	};

	
	private static class RegisteredModel {
		public Class<?> type, parentType;
		public ObjectModelNode model;
		
		public RegisteredModel parent;
		public ArrayList<RegisteredModel> children = new ArrayList<>();
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
		
		return inspectLevels(type, null, null);
	}

	private ObjectModelNode inspectLevels(Class<?> type, Class<?> parentType, ObjectModelNode parentOfRoot) {
		RegisteredModel registeredModel = findModel(type, parentType, parentOfRoot);

		if (registeredModel != null) {
			return registeredModel.model;
		}

		ObjectModelNode root = null;

		if (!type.isArray()) {
			Field[] fields = ClassReflection.getDeclaredFields(type);
	
			ObjectModelNode model = new ObjectModelNode(registeredModelsAsCollection, ++lastId, root);
			model.networkType = TYPE_OBJECT;
			model.children = new Vector<>(fields.length);
		
			root = rememberType(type, parentType, model, registeredModel).model;
	
			for (Field field : fields) {
				Class<?> fieldType = field.getType();
				ObjectModelNode child = null;
	
				if (fieldType.isArray()) {
					child = inspectArrayType(fieldType, root);
				}
				else {
					byte networkType = NetworkSerialization.determineSimpleType(fieldType);
	
					if (networkType == TYPE_UNKNOWN) {
						RegisteredModel registeredChildModel = findModel(fieldType, type, root);
						
						if (registeredChildModel == null) {
							child = inspectLevels(fieldType, type, root);
						}
						else {
							child = new ObjectModelNode(registeredModelsAsCollection, ++lastId, root).copyFrom(
								registeredChildModel.model
							);
						}
						
						
						// TODO handle the case when there are two cyclic fields of different names?
//						child = new ObjectModelNode(++lastId, root);
//						child.copyFrom(
//							inspectLevels(fieldType, type, root)
//						);
					}
					else {
						child = new ObjectModelNode(registeredModelsAsCollection, ++lastId, root);
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
		ObjectModelNode model = new ObjectModelNode(registeredModelsAsCollection, ++lastId, parent);
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

	private RegisteredModel findModel(final Class<?> type, final Class<?> parentType, ObjectModelNode parent) {
		for (RegisteredModel registered : registeredModels) {
			boolean sameParentModel = (parent == null && registered.model.parent == null)
				|| (parent != null && parent.equals(registered.model));

			if (registered.type.equals(type)) {
				boolean isCyclicModel = false; 
				
				RegisteredModel cur = findChildType(registered, type);
				isCyclicModel = cur != null;

				// TODO this code below searches UP but probably should be searching down to check if this type is a parent of potential parentType (???)
				//  but top-down would be much less efficient due to BFS algorithm. 
				
				// go through parents models to find out a (indirect?) cyclic dependency
//				RegisteredModel par = registered.parent;
//				while (par != null) {
//					if (par.equals(registered)) {
//						isCyclicModel = true;
//					}
//					par = par.parent;
//				}
				
				if (sameParentModel || isCyclicModel) {
					return registered;
				}
			}
		}
		
		return null;
	}
	
	private RegisteredModel findChildType(RegisteredModel registered, final Class<?> type) {
		RegisteredModel cur = registered;
		for (RegisteredModel child : cur.children) {
			if (child.parentType.equals(type)) {
				return child;
			}
			else {
				return findChildType(child, type);
			}
		}
		
		return null;
	}

	private RegisteredModel rememberType(Class<?> type, Class<?> parentType, ObjectModelNode model, RegisteredModel parentRegisteredModel) {
		RegisteredModel newModel = new RegisteredModel();
		newModel.type = type;
		newModel.model = model;
		newModel.parent = parentRegisteredModel;
		newModel.parentType = parentType;
		
		if (parentRegisteredModel != null) {
			parentRegisteredModel.children.add(newModel);
		}

		this.registeredModels.add(newModel);
		return newModel;
	}
}
