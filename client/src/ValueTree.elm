module ValueTree
  exposing
    ( AValue(..)
    , AValueList(..)
    , ValueContainer
    , ValueTree
    , ValueTreeId
    , assignParentValueId
    , createValueTree
    )

import Common exposing (replaceOne)
import Dict exposing (Dict)
import ObjectModelNode exposing (..)


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
  , id : ValueTreeId
  }


type AValue
  = ABool Bool
  | AInt Int
  | AFloat Float
  | AString (Maybe String) --it's not made as "reference" because it's immutable
  | AReference (Maybe ValueTreeId)
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


assignParentValueId : Dict ValueTreeId ValueTree -> ValueTreeId -> ValueTreeId -> Dict ValueTreeId ValueTree
assignParentValueId valueTrees id parentId =
  Dict.update id
    (\tree ->
      case tree of
        Just tree ->
          Just { tree | parentId = Just parentId }

        Nothing ->
          Nothing
    )
    valueTrees
