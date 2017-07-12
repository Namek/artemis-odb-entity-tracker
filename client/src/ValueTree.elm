module ValueTree
  exposing
    ( AValue
    , ValueHolder
    , ValueTree
    , ValueTreeId
    , createValueTree
    )

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
  | ACharList List Char
  | AIntList List Int
  | AFloatList List Float
  | AValueList List AValue


createValueTree : ValueTreeId -> Maybe ValueTreeId -> Maybe ObjectModelNodeId -> ValueTree
createValueTree id parentId objModelId =
  { id = id, parentId = parentId, modelId = objModelId, values = [] }


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
