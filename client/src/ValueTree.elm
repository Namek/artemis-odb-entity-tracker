module ValueTree
  exposing
    ( AValue(..)
    , AValueList(..)
    , JObjectId
    , JavaObject
    , JavaObjects
    , ValueContainer
    , ValueTree
    , ValueTreeId
    , addJObject
    , assignParentValueId
    , createValueTree
    , generateJObjectId
    , replaceValueById
    )

import Common exposing (replaceOne)
import Dict exposing (Dict)
import ObjectModelNode exposing (..)


-- Value Tree


type alias ValueTree =
  { id : Maybe ValueTreeId
  , parentId : Maybe ValueTreeId
  , modelId : Maybe ObjectModelNodeId
  , values : List ValueContainer
  }


type alias ValueTreeId =
  Int


type alias ValueContainer =
  { dataType : DataType
  , value : AValue
  , id : Maybe ValueTreeId
  }


type AValue
  = ABool Bool
  | AInt Int
  | AFloat Float
  | AString (Maybe String) --it's not made as "reference" because it's immutable
  | AReference (Maybe JObjectId)
  | AValueList AValueList


type AValueList
  = ANonPrimitiveArray (List AValue)
  | APrimitiveArray DataType (List AValue)


v1 : AValueList
v1 =
  APrimitiveArray TBoolean [ ABool False, ABool True ]


createValueTree : ValueTreeId -> Maybe ValueTreeId -> Maybe ObjectModelNodeId -> ValueTree
createValueTree id parentId objModelId =
  { id = Just id, parentId = parentId, modelId = objModelId, values = [] }


createOneValueTree : ValueContainer -> ValueTree
createOneValueTree val =
  { id = Nothing
  , parentId = Nothing
  , modelId = Nothing
  , values = [ val ]
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


type alias JavaObjects =
  { objects : Dict JObjectId JavaObject
  , lastObjectId : JObjectId
  }


type alias JObjectId =
  Int


type JavaObject
  = JObject ValueContainer


generateJObjectId : JavaObjects -> ( JavaObjects, JObjectId )
generateJObjectId javaObjects =
  let
    newId =
      javaObjects.lastObjectId + 1
  in
  ( { javaObjects | lastObjectId = newId }, newId )


addJObject : JavaObjects -> ValueContainer -> ( JavaObjects, JObjectId )
addJObject objs0 value =
  let
    ( objs1, id ) =
      generateJObjectId objs0

    objs2 : JavaObjects
    objs2 =
      { objs1 | objects = Dict.insert id (JObject value) objs1.objects }
  in
  ( objs2, id )
