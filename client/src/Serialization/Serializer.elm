module Serialization.Serializer exposing (..)

import Binary.ArrayBuffer as Buffer
import Bitwise
import Serialization.Common exposing (..)
import Serialization.ObjectModelNode exposing (..)


type alias SerializationPoint =
  { pos : Int
  , len : Int

  --   , objects : JavaObjects
  , arr : Buffer.Uint8Array

  --   , models : List ObjectModelNode
  --   , valueTrees : List ValueTree
  }


beginSerialization : SerializationPoint
beginSerialization =
  let
    bufSize =
      defaultNewBufferSize

    buf =
      Buffer.new bufSize

    arr =
      Buffer.asUint8Array buf
  in
  { pos = 0
  , len = bufSize
  , arr = arr

  --   , objects = objects
  --   , models = objModels
  --   , valueTrees = valueTrees
  }


addBoolean : Bool -> SerializationPoint -> SerializationPoint
addBoolean bool ser0 =
  ser0 |> addType TBoolean |> addRawBoolean bool


addRawBoolean : Bool -> SerializationPoint -> SerializationPoint
addRawBoolean bool ser0 =
  ser0
    |> addRawByte
        (if bool then
          1
         else
          0
        )


addRawByte : Int -> SerializationPoint -> SerializationPoint
addRawByte byte ser0 =
  { ser0
    | arr = Buffer.setByte ser0.arr ser0.pos byte
    , pos = ser0.pos + 1
  }


addByte : Int -> SerializationPoint -> SerializationPoint
addByte byte ser0 =
  ser0 |> addType TInt |> addRawByte byte


addRawShort : Int -> SerializationPoint -> SerializationPoint
addRawShort short ser0 =
  ser0
    |> addRawByte (Bitwise.shiftRightBy 8 short |> Bitwise.and 0xFF)
    |> addRawByte (short |> Bitwise.and 0xFF)


addShort : Int -> SerializationPoint -> SerializationPoint
addShort short ser0 =
  ser0 |> addType TShort |> addRawShort short


addRawInt : Int -> SerializationPoint -> SerializationPoint
addRawInt int ser0 =
  ser0
    |> addRawByte (Bitwise.shiftRightBy 24 int |> Bitwise.and 0xFF)
    |> addRawByte (Bitwise.shiftRightBy 16 int |> Bitwise.and 0xFF)
    |> addRawByte (Bitwise.shiftRightBy 8 int |> Bitwise.and 0xFF)
    |> addRawByte (int |> Bitwise.and 0xFF)


addInt : Int -> SerializationPoint -> SerializationPoint
addInt int ser0 =
  ser0 |> addType TInt |> addRawInt int


{-| Saves long with 53-bits precision. It's JavaScript fault.
-}
addRawLong : Int -> SerializationPoint -> SerializationPoint
addRawLong long ser0 =
  ser0
    |> addRawByte 0
    |> addRawByte (Bitwise.shiftRightBy 48 long |> Bitwise.and 0x20)
    |> addRawByte (Bitwise.shiftRightBy 40 long |> Bitwise.and 0xFF)
    |> addRawByte (Bitwise.shiftRightBy 32 long |> Bitwise.and 0xFF)
    |> addRawByte (Bitwise.shiftRightBy 24 long |> Bitwise.and 0xFF)
    |> addRawByte (Bitwise.shiftRightBy 16 long |> Bitwise.and 0xFF)
    |> addRawByte (Bitwise.shiftRightBy 8 long |> Bitwise.and 0xFF)
    |> addRawByte (long |> Bitwise.and 0xFF)


addLong : Int -> SerializationPoint -> SerializationPoint
addLong long ser0 =
  ser0 |> addType TLong |> addRawLong long


addRawFloat : Float -> SerializationPoint -> SerializationPoint
addRawFloat float ser0 =
  ser0 |> addRawInt (floatToIntBits float)


addFloat : Float -> SerializationPoint -> SerializationPoint
addFloat float ser0 =
  ser0 |> addType TFloat |> addRawFloat float


addType : DataType -> SerializationPoint -> SerializationPoint
addType dataType ser0 =
  ser0 |> addRawByte (typeToInt dataType)
