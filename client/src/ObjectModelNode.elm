module ObjectModelNode
  exposing
    ( DataType(..)
    , ObjectModelNode
    , ObjectModelNodeId
    , createModelNode
    , defaultModelNode
    , getObjectModelById
    , intToType
    , isSimpleType
    , typeToInt
    )

import Array exposing (..)
import Common exposing (elemIndex, intentionalCrash, sure)
import List.Extra


type DataType
  = TUndefined
  | TDescription
  | TDescriptionRef
  | TMultipleDescriptions
  | TEnumDescription
  | TEnumValue
  | TByte
  | TShort
  | TInt
  | TLong
  | TString
  | TBoolean
  | TFloat
  | TDouble
  | TBitVector
  | TUnknown
  | TObject
  | TObjectRef
  | TArray
  | TEnum
  | TNull


allDataTypes =
  Array.fromList
    [ TUndefined
    , TDescription
    , TDescriptionRef
    , TMultipleDescriptions
    , TEnumDescription
    , TEnumValue
    , TByte
    , TShort
    , TInt
    , TLong
    , TString
    , TBoolean
    , TFloat
    , TDouble
    , TBitVector
    , TUnknown
    , TObject
    , TObjectRef
    , TArray
    , TEnum
    , TNull
    ]


type alias ObjectModelNodeId =
  Int


type alias ObjectModelNode =
  { id : ObjectModelNodeId
  , parentId : Maybe ObjectModelNodeId
  , name : Maybe String
  , children : Maybe (List ObjectModelNodeId)
  , dataType : DataType
  , dataSubType : Maybe DataType
  , isTypePrimitive : Bool
  , isSubTypePrimitive : Bool
  , enumValue : Maybe Int
  }


intToType : Int -> DataType
intToType val =
  case Array.get val allDataTypes of
    Just val ->
      val

    Nothing ->
      intentionalCrash TUndefined ("Unexpected int->type conversion. Index " ++ toString val ++ " does not exist")


typeToInt : DataType -> Int
typeToInt theType =
  case elemIndex allDataTypes theType of
    Just val ->
      val

    Nothing ->
      intentionalCrash -1 "Unexpected type->int conversion."


isSimpleType : DataType -> Bool
isSimpleType t =
  case t of
    TByte ->
      True

    TShort ->
      True

    TInt ->
      True

    TLong ->
      True

    TString ->
      True

    TBoolean ->
      True

    TFloat ->
      True

    TDouble ->
      True

    TBitVector ->
      True

    _ ->
      False


createModelNode : Int -> ObjectModelNode
createModelNode id =
  { id = id
  , parentId = Nothing
  , name = Nothing
  , children = Nothing
  , dataType = TUndefined
  , dataSubType = Nothing
  , isTypePrimitive = False
  , isSubTypePrimitive = False
  , enumValue = Nothing
  }


defaultModelNode : ObjectModelNode
defaultModelNode =
  createModelNode -1


getObjectModelById : List ObjectModelNode -> ObjectModelNodeId -> ObjectModelNode
getObjectModelById models id =
  List.Extra.find (\m -> m.id == id) models
    |> sure
