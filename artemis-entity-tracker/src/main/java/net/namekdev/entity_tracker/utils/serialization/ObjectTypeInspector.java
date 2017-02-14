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
		public ObjectModelNode getById(int id) {
			for (RegisteredModel m : registeredModels) {
				if (m.model.id == id)
					return m.model;
			}

			return null;
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
	
	public ObjectModelNode getModelById(int id) {
		for (RegisteredModel model : registeredModels) {
			if (model.model.id == id) {
				return model.model;
			}
		}
		
		return null;
	}
	
	
	/**
	 * Returns tree description of class type.
	 */
	public ObjectModelNode inspect(Class<?> type) {
		assert(NetworkSerialization.determineType(type) == Type.Unknown);
		
		return inspectLevels(type, null, null, null);
	}

	private ObjectModelNode inspectLevels(Class<?> type, Class<?> parentType, ObjectModelNode parentOfRoot, RegisteredModel parentRegisteredModel) {
		RegisteredModel registeredModel = findModel(type, parentType, parentOfRoot);

		if (registeredModel != null) {
			return registeredModel.model;
		}

		ObjectModelNode root = null;

		if (!type.isArray()) {
			Field[] fields = ClassReflection.getDeclaredFields(type);
	
			ObjectModelNode model = new ObjectModelNode(registeredModelsAsCollection, ++lastId, root);
			model.networkType = Type.Object;
			model.children = new Vector<>(fields.length);
		
			registeredModel = rememberType(type, parentType, model, parentRegisteredModel);
			root = registeredModel.model;
	
			for (Field field : fields) {
				Class<?> fieldType = field.getType();
				ObjectModelNode child = null;
	
				if (fieldType.isArray()) {
					child = inspectArrayType(fieldType, type, registeredModel);
				}
				else {
					Type networkType = NetworkSerialization.determineType(fieldType);
	
					if (networkType == Type.Unknown) {
						RegisteredModel registeredChildModel = findModel(fieldType, type, root);
						
						if (registeredChildModel == null) {
							child = inspectLevels(fieldType, type, root, registeredModel);
						}
						else {
							child = new ObjectModelNode(registeredModelsAsCollection, ++lastId, root).copyFrom(
								registeredChildModel.model
							);
						}
					}
					else if (networkType == Type.Enum) {
						 child = inspectEnum((Class<Enum>) fieldType, type, registeredModel);
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
			return inspectArrayType(type, parentType, parentRegisteredModel);
		}
	}

	private ObjectModelNode inspectArrayType(Class<?> fieldType, Class<?> parentType, RegisteredModel parentRegisteredModel) {
		ObjectModelNode model = new ObjectModelNode(registeredModelsAsCollection, ++lastId, parentRegisteredModel != null ? parentRegisteredModel.model : null);
		RegisteredModel registeredModel = rememberType(fieldType, parentType, model, parentRegisteredModel);
		
		Class<?> arrayElType = fieldType.getComponentType();
		Type arrayType = determineType(arrayElType);

		
		if (arrayType == Type.Enum) {
			// TODO!
//			throw new RuntimeException("TODO array of enums");
			ObjectModelNode enumFieldModel = inspectEnum((Class<Enum>) arrayElType, fieldType, registeredModel);
			model.children = new Vector<>(1);
			model.children.addElement(enumFieldModel);
		}
		
		// TODO probably that should inspect deeper anyway!
		else if (!isSimpleType(arrayType)) {
//			model = inspectLevels(arrayElType, root);
//
//			if (model.networkType == TYPE_TREE) {
//				arrayType = TYPE_TREE;
//			}
			
			arrayType = arrayElType.isArray() ? Type.Array : Type.Object;
		}

		model.networkType = Type.Array;
		model.childType = (short) arrayType.ordinal();

		return model;
	}
	
	private ObjectModelNode inspectEnum(final Class<Enum> enumType, final Class<?> parentType, final RegisteredModel parentRegisteredModel) {
		// algorithm: will create enum field definition anyway,
		// but first check if there is a need to create a model for enum type (list of possible values) 

		RegisteredModel registeredEnumTypeModel = findModel(enumType, null, null);
		
		if (registeredEnumTypeModel == null) {
			ObjectModelNode enumTypeModel = new ObjectModelNode(registeredModelsAsCollection, ++lastId, null);
			enumTypeModel.networkType = Type.EnumDescription;
			enumTypeModel.name = enumType.getSimpleName();
			
			Enum<?>[] possibleValues = enumType.getEnumConstants();
			enumTypeModel.children = new Vector<>(possibleValues.length);
			registeredEnumTypeModel = rememberType(enumType, null, enumTypeModel, null);
			
			for (int i = 0; i < possibleValues.length; ++i) {
				ObjectModelNode enumValueModel = new ObjectModelNode(registeredModelsAsCollection, ++lastId, enumTypeModel);
				enumValueModel.networkType = Type.EnumValue;
				Enum<?> val = possibleValues[i];
				
				// Note: we cut bytes here, it's not nice but let's believe that no one creates enums greater than 127.
				enumValueModel.childType = (short) val.ordinal();
				enumValueModel.name = val.name();
				enumTypeModel.children.addElement(enumValueModel);
				
				rememberType(null, enumType, enumValueModel, registeredEnumTypeModel);
			}
		}
		
		ObjectModelNode enumFieldModel = new ObjectModelNode(registeredModelsAsCollection, ++lastId, parentRegisteredModel.model);
		enumFieldModel.networkType = Type.Enum;
		
		ObjectModelNode enumModelRef = new ObjectModelNode(registeredModelsAsCollection, registeredEnumTypeModel.model.id, enumFieldModel);
		enumFieldModel.children = new Vector<>(1);
		enumFieldModel.children.addElement(enumModelRef);
		
		rememberType(enumType, parentType, enumFieldModel, parentRegisteredModel);
		
		return enumFieldModel;
	}

	private RegisteredModel findModel(final Class<?> type, final Class<?> parentType, final ObjectModelNode parent) {
		for (RegisteredModel registered : registeredModels) {
			boolean sameParentModel = (parent == null && registered.model.parent == null)
				|| (parent != null && parent.equals(registered.model));

			if (registered.type != null && registered.type.equals(type) || registered.type == null && type == null) {
				boolean isCyclicModel = false; 

				RegisteredModel cur = findChildType(registered, type);
				isCyclicModel = cur != null;

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
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[");

		for (RegisteredModel m : this.registeredModels) {
			sb.append("\n{\n  type: " + toString(m.type));
			sb.append("\n  parentType:" + toString(m.parentType));
			sb.append("\n  model:");
			
			if (m.model == null) {
				sb.append(" null\n");
			}
			else {
				sb.append("\n    id: " + m.model.id);
				sb.append("\n    networkType: " + m.model.networkType);
				sb.append("\n    childType: " + m.model.childType);
				sb.append("\n    name: \"" + m.model.name + "\"");
				sb.append("\n    parent:");
				
				if (m.model.parent != null) {
					sb.append(" (id=" + m.model.parent.id + ")");
				}
				else {
					sb.append(" null");
				}
				
				sb.append("\n    children");
				
				if (m.model.children != null) {
					sb.append(" (" + m.model.children.size() + ")");
					
					for (int i = 0, n = m.model.children.size(); i < n; ++i) {
						ObjectModelNode node = m.model.children.get(i);
						sb.append("\n    {\n      id: " + node.id);
						sb.append("\n      name: " + node.name);
						sb.append("\n    }");
					}
				}
				else {
					sb.append(": null");
				}
			}
			
			sb.append("\n}");
		}
		
		sb.append("\n]");
		
		return sb.toString();
	}
	
	private String toString(Object obj) {
		if (obj == null)
			return "null";
		
		return obj.toString();
	}
}
