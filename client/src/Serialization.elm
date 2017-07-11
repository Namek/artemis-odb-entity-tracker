module Serialization exposing (..)

import Array exposing (Array)
import Binary.ArrayBuffer as Buffer
import Bitwise
import Common exposing (intentionalCrash, iterateFoldl)
import List.Extra
import Native.Serialization
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


integerSize : number
integerSize =
  32


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
      checkNull des0
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

      intsToRead =
        allBitsCount % integerSize

      saveIntToBits : Int -> Array Bool -> Int -> Int -> Array Bool
      saveIntToBits int arr offset bitsCount =
        if bitsCount > 0 then
          let
            bit =
              Bitwise.and int 1

            newArr =
              Array.set offset (bit == 1) arr
          in
          saveIntToBits (Bitwise.shiftRightBy 1 int) newArr (offset + 1) (bitsCount - 1)
        else
          arr

      readBits leftBits des offset out_arr =
        if leftBits > 0 then
          let
            ( newDes, int ) =
              readRawInt des

            out_newArr =
              saveIntToBits int out_arr offset (leftBits % 32)
          in
          readBits (leftBits - integerSize) newDes (offset + integerSize) out_newArr
        else
          ( des, out_arr )

      ( finalDes, finalArr ) =
        readBits intsToRead des3 0 arr
    in
    ( finalDes, Just finalArr )


bitVectorToDebugString : BitVector -> String
bitVectorToDebugString bits =
  Array.foldl
    (\a acc ->
      acc
        ++ toString
            (if a == True then
              1
             else
              0
            )
    )
    ""
    bits


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
    intentionalCrash des0 ("Types are divergent, expected: " ++ toString expectedType ++ ", got: " ++ toString aType)


peekType : DeserializationPoint -> DataType -> Bool
peekType des expectedType =
  typeToInt expectedType == Buffer.getByte des.arr des.pos


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
    ( des, intentionalCrash False ("Types are divergent, expected: " ++ toString expectedType ++ ", got: " ++ toString aType) )


checkNull : DeserializationPoint -> ( DeserializationPoint, Bool )
checkNull des =
  if peekType des TNull then
    let
      ( newDes, byte ) =
        readRawByte des
    in
    ( newDes, True )
  else
    ( des, False )


readDataDescription : DeserializationPoint -> ( DeserializationPoint, ObjectModelNodeId )
readDataDescription des0 =
  let
    ( des1, aType ) =
      readType des0
  in
  if aType == TDescription then
    readRawDataDescription des1
  else if aType == TDescriptionRef then
    readRawInt des1
  else
    intentionalCrash ( des0, 0 ) ("unexpectedType" ++ toString aType)


readRawDataDescription : DeserializationPoint -> ( DeserializationPoint, ObjectModelNodeId )
readRawDataDescription des0 =
  let
    ( des1, objModelId ) =
      readRawInt des0

    ( des2, name ) =
      readString des1

    ( des3, isTypePrimitive ) =
      readBoolean des2

    ( des4, nodeType ) =
      readType des3

    newObjModel : ObjectModelNode
    newObjModel =
      createModelNode objModelId

    updatedObjModel0 : ObjectModelNode
    updatedObjModel0 =
      { newObjModel
        | name = name
        , isTypePrimitive = isTypePrimitive
        , dataType = nodeType
      }
  in
  if nodeType == TObject then
    let
      ( des5, n ) =
        readRawInt des4

      ( des6, childrenIds ) =
        iterateFoldl
          (\( des, childrenIds ) idx ->
            let
              ( newDes, childObjModelId ) =
                readDataDescription des
            in
            Just ( newDes, childObjModelId :: childrenIds )
          )
          ( des5, [] )
          0
          (n - 1)

      updatedObjModel1 =
        { updatedObjModel0 | children = Just childrenIds }
    in
    ( { des6 | models = updatedObjModel1 :: des6.models }, objModelId )
  else if nodeType == TArray then
    let
      ( des5, dataSubType ) =
        readType des4

      updatedObjModel1 =
        { updatedObjModel0 | dataSubType = Just dataSubType }
    in
    if isSimpleType dataSubType then
      ( { des5 | models = updatedObjModel1 :: des5.models }, objModelId )
    else if dataSubType == TObject then
      -- nothing special here
      ( { des5 | models = updatedObjModel1 :: des5.models }, objModelId )
    else if dataSubType == TEnum then
      Debug.crash "TODO" ( des5, objModelId )
    else if dataSubType == TArray then
      Debug.crash "TODO" ( des5, objModelId )
    else
      intentionalCrash ( des0, 0 ) ("unsupported array type: " ++ toString dataSubType)
  else if nodeType == TEnum then
    Debug.crash "TODO" ( des4, objModelId )
  else if nodeType == TEnumValue then
    Debug.crash "TODO" ( des4, objModelId )
  else if nodeType == TEnumDescription then
    Debug.crash "TODO" ( des4, objModelId )
  else if isSimpleType nodeType then
    -- nothing special here
    ( { des4 | models = updatedObjModel0 :: des4.models }, objModelId )
  else
    intentionalCrash ( des0, 0 ) ("unsupported type: " ++ toString nodeType)



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
