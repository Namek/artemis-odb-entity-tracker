module ValueTree
  exposing
    ( APrimitivesList(..)
    , AValue(..)
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
  { id : ValueTreeId
  , parentId : Maybe ValueTreeId
  , modelId : Maybe ObjectModelNodeId
  , values : Dict ValueTreeId ValueContainer
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
  | APrimitivesList APrimitivesList


type AValueList
  = ANonPrimitiveArray (List AValue)


type APrimitivesList
  = APrimitiveArray DataType (List AValue)


createValueTree : ValueTreeId -> Maybe ValueTreeId -> Maybe ObjectModelNodeId -> ValueTree
createValueTree id parentId objModelId =
  { id = id, parentId = parentId, modelId = objModelId, values = Dict.empty }


assignParentValueId : List ValueTree -> ValueTreeId -> ValueTreeId -> List ValueTree
assignParentValueId valueTrees id parentId =
  replaceOne valueTrees
    (\aTree ->
      if aTree.id == id then
        Just { aTree | parentId = Just parentId }
      else
        Nothing
    )
