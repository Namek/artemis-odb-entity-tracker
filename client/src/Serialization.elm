module Serialization exposing (..)

import Array exposing (Array)
import Bitwise
import Native.Serialization
import Binary.ArrayBuffer as Buffer
import Common exposing (intentionalCrash)
import ObjectModelNode exposing (..)


type alias DeserializationPoint =
  { pos : Int
  , len : Int
  , arr : Buffer.Uint8Array
  , models : List ObjectModelNode
  }


type alias LongContainer =
  ( Int, Int )


type alias BitVector =
  Array Bool


beginDeserialization : Buffer.ArrayBuffer -> DeserializationPoint
beginDeserialization buf =
  let
    arr =
      Buffer.asUint8Array buf
  in
    { pos = 0, len = Buffer.byteLength buf, arr = arr, models = [] }


intBitsToFloat : Int -> Float
intBitsToFloat int =
  Native.Serialization.intBitsToFloat int


isDone : DeserializationPoint -> Bool
isDone des =
  des.pos >= des.len


readRawByte : DeserializationPoint -> ( DeserializationPoint, Int )
readRawByte des =
  let
    byte =
      Buffer.getByte des.arr des.pos
  in
    ( { des | pos = des.pos + 1 }, byte )


readByte : DeserializationPoint -> ( DeserializationPoint, Int )
readByte des =
  let
    newDes =
      checkType des TByte
  in
    readRawByte newDes


readRawShort : DeserializationPoint -> ( DeserializationPoint, Int )
readRawShort des =
  let
    byte1 =
      Buffer.getByte des.arr des.pos

    byte2 =
      Buffer.getByte des.arr (des.pos + 1)

    newDes =
      { des | pos = des.pos + 2 }

    val1 =
      Bitwise.shiftLeftBy 8 (Bitwise.and byte1 0xFF)

    val2 =
      Bitwise.and byte2 0xFF

    val =
      Bitwise.or val1 val2
  in
    ( newDes, val )


readShort : DeserializationPoint -> ( DeserializationPoint, Int )
readShort des =
  let
    newDes =
      checkType des TShort
  in
    readRawShort newDes


readRawInt : DeserializationPoint -> ( DeserializationPoint, Int )
readRawInt des =
  let
    byte1 =
      Buffer.getByte des.arr des.pos

    byte2 =
      Buffer.getByte des.arr (des.pos + 1)

    byte3 =
      Buffer.getByte des.arr (des.pos + 2)

    byte4 =
      Buffer.getByte des.arr (des.pos + 3)

    newDes =
      { des | pos = des.pos + 4 }

    val1 =
      Bitwise.shiftLeftBy 24 (Bitwise.and byte1 0xFF)

    val2 =
      Bitwise.shiftLeftBy 16 (Bitwise.and byte2 0xFF)

    val3 =
      Bitwise.shiftLeftBy 8 (Bitwise.and byte3 0xFF)

    val4 =
      Bitwise.and byte4 0xFF

    val =
      Bitwise.or (Bitwise.or (Bitwise.or val1 val2) val3) val4
  in
    ( newDes, val )


readInt : DeserializationPoint -> ( DeserializationPoint, Int )
readInt des =
  let
    newDes =
      checkType des TInt
  in
    readRawInt newDes


readRawLong : DeserializationPoint -> ( DeserializationPoint, LongContainer )
readRawLong des =
  let
    ( des1, int1 ) =
      readRawInt des

    ( des2, int2 ) =
      readRawInt des1
  in
    ( des2, ( int1, int2 ) )


readLong : DeserializationPoint -> ( DeserializationPoint, LongContainer )
readLong des =
  let
    newDes =
      checkType des TLong
  in
    readRawLong newDes


readRawBytes : DeserializationPoint -> Int -> ( DeserializationPoint, Buffer.ArrayBuffer )
readRawBytes des0 len =
  let
    buf =
      Buffer.new len

    arr =
      Buffer.asUint8Array buf

    read des left pos arr =
      if left > 0 then
        let
          ( newDes, byte ) =
            readRawByte des

          newBuf =
            Buffer.setByte arr pos byte
        in
          read newDes (left - 1) (pos + 1) newBuf
      else
        ( des, buf )
  in
    read des0 len 0 arr


readString : DeserializationPoint -> ( DeserializationPoint, Maybe String )
readString des0 =
  let
    ( des1, isNull ) =
      (checkNull des0)
  in
    if isNull then
      ( des1, Nothing )
    else
      let
        des2 =
          checkType des1 TString

        ( des3, len ) =
          readRawInt des2

        ( des4, strBuf ) =
          readRawBytes des3 len
      in
        ( des4, Just (Buffer.bytesToString strBuf) )


readRawBoolean : DeserializationPoint -> ( DeserializationPoint, Bool )
readRawBoolean des0 =
  let
    ( des1, byte ) =
      readRawByte des0
  in
    ( des1, byte /= 0 )


readBoolean : DeserializationPoint -> ( DeserializationPoint, Bool )
readBoolean des0 =
  let
    des1 =
      checkType des0 TBoolean
  in
    readRawBoolean des1


readRawFloat : DeserializationPoint -> ( DeserializationPoint, Float )
readRawFloat des0 =
  let
    ( des1, int ) =
      readRawInt des0
  in
    ( des1, intBitsToFloat int )


readFloat : DeserializationPoint -> ( DeserializationPoint, Float )
readFloat des0 =
  let
    des1 =
      checkType des0 TFloat
  in
    readRawFloat des1


-- TODO: readDouble??? there is no such type in Elm


readBitVector : DeserializationPoint -> ( DeserializationPoint, Maybe BitVector )
readBitVector des0 =
  let
    ( des1, isNull ) =
      checkNull des0
  in
    if isNull then
      ( des1, Nothing )
    else
      let
        des2 =
          checkType des1 TBitVector

        ( des3, allBitsCount ) =
          readRawShort des2

        arr =
          Array.initialize allBitsCount (always False)
      in
        -- TODO: read bits from integers and push them to the array
        ( des1, Nothing )


readType : DeserializationPoint -> ( DeserializationPoint, DataType )
readType des0 =
  let
    ( des1, byte ) =
      readRawByte des0
  in
    ( des1, intToType byte )


checkType : DeserializationPoint -> DataType -> DeserializationPoint
checkType des0 expectedType =
  let
    ( des1, aType ) =
      readType des0
  in
    if aType == expectedType then
      des1
    else
      intentionalCrash des0 ("Types are divergent, expected: " ++ (toString expectedType) ++ ", got: " ++ (toString aType))


peekType : DeserializationPoint -> DataType -> Bool
peekType des expectedType =
  (typeToInt expectedType) == (Buffer.getByte des.arr des.pos)


expectTypeOrNull : DeserializationPoint -> DataType -> ( DeserializationPoint, Bool )
expectTypeOrNull des expectedType =
  let
    ( newDes, aType ) =
      readType des

    isNull =
      aType == TNull
  in
    if aType == expectedType || isNull then
      ( newDes, not isNull )
    else
      ( des, intentionalCrash False ("Types are divergent, expected: " ++ (toString expectedType) ++ ", got: " ++ (toString aType)) )


checkNull : DeserializationPoint -> ( DeserializationPoint, Bool )
checkNull des =
  if (peekType des TNull) then
    let
      ( newDes, byte ) =
        readRawByte des
    in
      ( newDes, True )
  else
    ( des, False )


{-
   # TODO:
    * readDataDescription
    * readRawDataDescription
    * readObject
    * readRawObject
    * possiblyReadDescriptions
    * readArray
    * readPrimitive*Array
    * ObjectReadSession
-}
