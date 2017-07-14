module ValueTree
  exposing
    ( AValue
    , ValueHolder(..)
    , ValueTree
    , ValueTreeId
    , assignParentValueId
    , createValueTree
    )

import Common exposing (replaceOne)
import ObjectModelNode exposing (..)


type alias ValueTree =
  { id : ValueTreeId
  , parentId : Maybe ValueTreeId
  , modelId : Maybe ObjectModelNodeId
  , values : List AValue
  }


type alias ValueTreeId =
  Int


type alias AValue =
  { dataType : DataType
  , value : ValueHolder
  }


type ValueHolder
  = ANull
  | AValueTreeRef Int
  | AString String
  | AInt Int
  | AChar Char
  | AFloat Float
  | ABooleanList (List Bool)
  | ACharList (List Char)
  | AIntList (List Int)
  | AFloatList (List Float)
  | AValueList (List AValue)


createValueTree : ValueTreeId -> Maybe ValueTreeId -> Maybe ObjectModelNodeId -> ValueTree
createValueTree id parentId objModelId =
  { id = id, parentId = parentId, modelId = objModelId, values = [] }


assignParentValueId : List ValueTree -> ValueTreeId -> ValueTreeId -> List ValueTree
assignParentValueId valueTrees id parentId =
  replaceOne valueTrees
    (\aTree ->
      if aTree.id == id then
        Just { aTree | parentId = Just parentId }
      else
        Nothing
    )


d1 : ValueTree
d1 =
  { id = 1
  , parentId = Just 2
  , modelId = Just 10
  , values =
      [ { dataType = TString, value = AString "omg" }
      , { dataType = TInt, value = AInt 2 }
      ]
  }
