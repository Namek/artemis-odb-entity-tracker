module Serialization.ValueTree
  exposing
    ( AValue(..)
    , AValueList(..)
    , JObjectId
    , JavaObjectsCollection
    , JavaType(..)
    , ValueContainer
    , ValueTree
    , ValueTreeId
    , assignParentValueId
    , createOneValueTree
    , createValueTree
    , generateJObjectId
    , repackJavaObjectId
    , replaceValueById
    , saveValueAsJObject
    )

import Common exposing (replaceOne)
import Dict exposing (Dict)
import Serialization.ObjectModelNode exposing (..)


-- Value Tree


type alias ValueTree =
  { id : Maybe ValueTreeId
  , parentId : Maybe ValueTreeId
  , modelId : Maybe ObjectModelNodeId
  , values : Maybe (List JavaType) --old: List ValueContainer
  }


type alias ValueTreeId =
  Int



--
-- type ValueTreeNodeValues
--   = SomeValues (List ValueContainer)
--   | SomeReferences (List (Maybe JObjectId))


type alias ValueContainer =
  { dataType : DataType
  , value : AValue
  }


type AValue
  = ABool Bool
  | AInt Int
  | AFloat Float
  | AString (Maybe String) --it's not made as "reference" because it's immutable
  | AReference (Maybe JObjectId)
  | AReferenceList (List (Maybe JObjectId))
  | AValueList AValueList
  | AValueTree ValueTree
  | AJavaObjectList (List JavaType) --TODO: replace reference/list all above with this one?
  | AJavaObject JavaType


type AValueList
  = ANonPrimitiveArray (List AValue)
  | APrimitiveArray DataType (List AValue)


v1 : AValueList
v1 =
  APrimitiveArray TBoolean [ ABool False, ABool True ]


createValueTree : ValueTreeId -> Maybe ValueTreeId -> Maybe ObjectModelNodeId -> ValueTree
createValueTree id parentId objModelId =
  { id = Just id, parentId = parentId, modelId = objModelId, values = Nothing }


createOneValueTree : ValueContainer -> ValueTree
createOneValueTree val =
  { id = Nothing
  , parentId = Nothing
  , modelId = Nothing
  , values = Just [ JavaSimple val ]
  }


assignParentValueId : List ValueTree -> ValueTreeId -> ValueTreeId -> List ValueTree
assignParentValueId valueTrees id parentId =
  replaceValueById valueTrees
    id
    (\tree -> { tree | parentId = Just parentId })


replaceValueById : List ValueTree -> ValueTreeId -> (ValueTree -> ValueTree) -> List ValueTree
replaceValueById valueTrees id replaceFunc =
  replaceOne valueTrees
    (\tree ->
      case tree.id of
        Just anId ->
          if anId == id then
            Just <| replaceFunc tree
          else
            Nothing

        Nothing ->
          Nothing
    )



-- Java objects


type alias JavaObjectsCollection =
  { objects : Dict JObjectId ValueContainer
  , lastObjectId : JObjectId
  }


type alias JObjectId =
  Int



--
-- type JavaObject
--   = JObject ValueContainer


type JavaType
  = JavaNull
  | JavaObject JObjectId
  | JavaSimple ValueContainer


repackJavaObjectId : Maybe JObjectId -> JavaType
repackJavaObjectId jObjectIdMaybe =
  case jObjectIdMaybe of
    Just jObjectId ->
      JavaObject jObjectId

    Nothing ->
      JavaNull


generateJObjectId : JavaObjectsCollection -> ( JavaObjectsCollection, JObjectId )
generateJObjectId javaObjects =
  let
    newId =
      javaObjects.lastObjectId + 1
  in
  ( { javaObjects | lastObjectId = newId }, newId )


saveValueAsJObject : JavaObjectsCollection -> ValueContainer -> ( JavaObjectsCollection, JObjectId )
saveValueAsJObject objs0 value =
  let
    ( objs1, id ) =
      generateJObjectId objs0

    objs2 : JavaObjectsCollection
    objs2 =
      { objs1 | objects = Dict.insert id value objs1.objects }
  in
  ( objs2, id )



--
--
--
-- saveJavaArray : JavaObjectsCollection -> List JavaType -> (JavaObjectsCollection)
