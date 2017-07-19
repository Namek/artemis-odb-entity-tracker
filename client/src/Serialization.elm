module Serialization
  exposing
    ( BitVector
    , DeserializationPoint
    , ObjectReadSession
    , beginDeserialization
    , bitVectorToDebugString
    , checkNull
    , expectTypeOrNull
    , isDone
    , readArrayByType
    , readBitVector
    , readBoolean
    , readBooleanArray
    , readByte
    , readByteArray
    , readDataDescription
    , readFloat
    , readFloatArray
    , readInt
    , readIntArray
    , readLong
    , readObject
    , readRawBoolean
    , readRawByte
    , readRawBytes
    , readRawDataDescription
    , readRawFloat
    , readRawInt
    , readRawLong
    , readRawObject
    , readRawShort
    , readShort
    , readShortArray
    , readString
    , readType
    )

import Array exposing (Array)
import Binary.ArrayBuffer as Buffer
import Bitwise
import Common exposing (assert, intentionalCrash, iterateFoldl, replaceOne, sure)
import List.Extra
import Native.Serialization
import ObjectModelNode exposing (..)
import ValueTree exposing (..)


type alias DeserializationPoint =
  { pos : Int
  , len : Int
  , arr : Buffer.Uint8Array
  , models : List ObjectModelNode
  , valueTrees : List ValueTree
  }


type alias ObjectReadSession =
  { valueTrees : List ( ValueTreeId, Maybe ObjectModelNodeId )
  }


type alias LongContainer =
  ( Int, Int )


type alias BitVector =
  Array Bool


integerSize : number
integerSize =
  32


beginDeserialization : List ObjectModelNode -> List ValueTree -> Buffer.ArrayBuffer -> DeserializationPoint
beginDeserialization objModels valueTrees buf =
  let
    arr =
      Buffer.asUint8Array buf
  in
  { pos = 0, len = Buffer.byteLength buf, arr = arr, models = objModels, valueTrees = valueTrees }


intBitsToFloat : Int -> Float
intBitsToFloat int =
  Native.Serialization.intBitsToFloat int


intBitsToDouble : Int -> Int -> Float
intBitsToDouble int1 int2 =
  Native.Serialization.intBitsToDouble int1 int2


isDone : DeserializationPoint -> Bool
isDone des =
  des.pos >= des.len


repackValue packFunc readFunc =
  let
    ( arg0, val ) =
      readFunc
  in
  ( arg0, packFunc val )


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


readRawDouble : DeserializationPoint -> ( DeserializationPoint, Float )
readRawDouble des0 =
  let
    ( des1, int1 ) =
      readRawInt des0

    ( des2, int2 ) =
      readRawInt des1
  in
  ( des2, intBitsToDouble int1 int2 )


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
            ( newDes, childObjModelId :: childrenIds )
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


readObject : DeserializationPoint -> ObjectModelNodeId -> ( DeserializationPoint, ObjectReadSession, Maybe ValueTreeId )
readObject des0 objModelId =
  let
    session : ObjectReadSession
    session =
      { valueTrees = [] }
  in
  readObjectWithSession des0 objModelId session


readObjectWithSession : DeserializationPoint -> ObjectModelNodeId -> ObjectReadSession -> ( DeserializationPoint, ObjectReadSession, Maybe ValueTreeId )
readObjectWithSession des0 objModelId objReadSession =
  let
    des1 =
      checkType des0 TObject
  in
  readRawObject des1 objModelId Nothing objReadSession


readRawObject : DeserializationPoint -> ObjectModelNodeId -> Maybe ValueTreeId -> ObjectReadSession -> ( DeserializationPoint, ObjectReadSession, Maybe ValueTreeId )
readRawObject des0 objModelId maybeParentValueTreeId objReadSession0 =
  let
    ( des1, isNull ) =
      checkNull des0

    objModel =
      getObjectModelById des0.models objModelId
  in
  if isNull then
    ( des1, objReadSession0, Nothing )
  else if objModel.dataType == TObject || objModel.dataType == TUnknown then
    let
      ( des2, dataType ) =
        readType des1

      ( des3, id ) =
        readRawShort des2
    in
    if dataType == TObject then
      let
        n =
          List.length (Maybe.withDefault [] objModel.children)

        tree : ValueTree
        tree =
          createValueTree id maybeParentValueTreeId (Just objModelId)

        objReadSession1 =
          rememberInSession objReadSession0 id (Just objModelId)

        ( des4, objReadSession2, valueTreeIds ) =
          iterateFoldl
            (\( des, session, valueTreeIds ) idx ->
              let
                childObjModelId =
                  List.Extra.getAt idx (sure objModel.children)

                ( des1, session1, valueTreeId ) =
                  readRawObject des (sure childObjModelId) (Just id) session
              in
              ( des1, session1, valueTreeId :: valueTreeIds )
            )
            ( des3, objReadSession1, [] )
            0
            (n - 1)
      in
      -- other valueTrees are saved inside at this point
      ( { des4 | valueTrees = tree :: des4.valueTrees }, objReadSession2, Just id )
    else if dataType == TObjectRef then
      ( des3, objReadSession0, Just id )
    else if dataType == TArray then
      let
        {- TODO HACK: we should identify every array. A hack copied from original project. -}
        des4 =
          { des3 | pos = des3.pos - 3 }

        ( des5, objReadSession1, maybeArrayValueId ) =
          readArrayWithSession des4 objReadSession0
      in
      case maybeArrayValueId of
        Nothing ->
          ( des5, objReadSession1, Nothing )

        Just arrayValueId ->
          -- assign parent vtree
          let
            updatedValueTrees =
              assignParentValueId des5.valueTrees id arrayValueId

            des6 =
              { des5 | valueTrees = updatedValueTrees }
          in
          ( des6, objReadSession1, Just id )
    else
      intentionalCrash ( des3, objReadSession0, Nothing ) ("Types are divergent, expected: " ++ toString TObject ++ " or " ++ toString TObjectRef ++ ", got: " ++ toString dataType)
  else if isSimpleType objModel.dataType then
    let
      _ =
        assert objModel.isTypePrimitive

      ( des2, valueId ) =
        readRawByType des1 objModel.dataType
    in
    ( des2, objReadSession0, Nothing )
  else if objModel.dataType == TEnum then
    -- TODO
    ( des1, objReadSession0, Nothing )
  else if objModel.dataType == TArray then
    -- TODO
    ( des1, objReadSession0, Nothing )
  else
    intentionalCrash ( des1, objReadSession0, Nothing ) ("unsupported type:" ++ (toString objModel.dataType ++ ", subtype: " ++ toString objModel.dataSubType))


beginArray :
  DeserializationPoint
  -> ( DeserializationPoint, Bool, DataType, Int )
beginArray des0 =
  let
    des1 =
      checkType des0 TArray

    ( des2, isPrimitive ) =
      readRawBoolean des1

    ( des3, elementType ) =
      readType des2

    ( des4, size ) =
      readRawInt des3
  in
  ( des1, isPrimitive, elementType, size )


beginTypedArray : DeserializationPoint -> DataType -> Bool -> ( DeserializationPoint, Int )
beginTypedArray des0 arrayElType shouldBePrimitive =
  let
    des1 =
      checkType des0 TArray

    ( des2, isPrimitive ) =
      readRawBoolean des1
  in
  if isPrimitive /= shouldBePrimitive then
    intentionalCrash ( des0, 0 ) ("Array primitiveness was expected be: " ++ toString shouldBePrimitive ++ ", got: " ++ toString isPrimitive)
  else
    let
      des3 =
        checkType des2 arrayElType
    in
    readRawInt des3


peekArray :
  DeserializationPoint
  -> ( Bool, DataType, Int )
peekArray des =
  let
    ( _, isPrimitive, elementType, size ) =
      beginArray des
  in
  ( isPrimitive, elementType, size )


readArray :
  DeserializationPoint
  -> ( DeserializationPoint, ObjectReadSession, Maybe ValueTreeId )
readArray des0 =
  let
    session : ObjectReadSession
    session =
      { valueTrees = [] }
  in
  readArrayWithSession des0 session


{-| Read array without a known model a priori.
-}
readArrayWithSession :
  DeserializationPoint
  -> ObjectReadSession
  -> ( DeserializationPoint, ObjectReadSession, Maybe ValueTreeId )
readArrayWithSession des0 session =
  let
    ( des1, objModelId ) =
      possiblyReadDescriptions des0 False

    ( des2, isNull ) =
      checkNull des1

    shouldReadBasedOnModel =
      case objModelId of
        Just objModelId ->
          let
            objModel =
              getObjectModelById des2.models objModelId
          in
          if objModel.dataType == TArray then
            True
          else
            False

        Nothing ->
          False
  in
  if shouldReadBasedOnModel then
    readArrayWithSessionAndModel des2 session (sure objModelId)
  else
    let
      ( isPrimitive, elementType, n ) =
        peekArray des2
    in
    if isPrimitive then
      let
        ( des3, array ) =
          readPrimitiveArrayByType des2 elementType
      in
      -- TODO create ValueTree here!
      ( des3, session, Nothing )
    else if elementType == TUnknown then
      let
        ( des3, n ) =
          beginTypedArray des2 TUnknown False
      in
      -- TODO create ValueTree here!
      ( des0, session, Nothing )
    else
      -- TODO create ValueTree here!
      -- readArrayByType des2 elementType
      ( des0, session, Nothing )


readArrayWithSessionAndModel :
  DeserializationPoint
  -> ObjectReadSession
  -> ObjectModelNodeId
  -> ( DeserializationPoint, ObjectReadSession, Maybe ValueTreeId )
readArrayWithSessionAndModel des0 session0 objModelId =
  let
    ( des1, isNull ) =
      checkNull des0

    objModel =
      getObjectModelById des0.models objModelId
  in
  if isNull then
    ( des1, session0, Nothing )
  else if objModel.isSubTypePrimitive then
    ( des1, session0, Nothing )
  else
    ( des1, session0, Nothing )


possiblyReadDescriptions :
  DeserializationPoint
  -> Bool
  -> ( DeserializationPoint, Maybe ObjectModelNodeId )
possiblyReadDescriptions des0 force =
  let
    ( des1, isOk ) =
      checkIfHasDescription des0 force
  in
  if not isOk then
    ( des1, Nothing )
  else
    let
      ( des2, descrCount ) =
        readRawInt des1
    in
    if descrCount > 0 then
      let
        ( des3, lastModelId ) =
          iterateFoldl
            (\( des, lastModelId ) idx ->
              let
                ( newDes, objModelId ) =
                  readDataDescription des
              in
              ( newDes, Just objModelId )
            )
            ( des2, Nothing )
            0
            (descrCount - 1)
      in
      ( des3, lastModelId )
    else
      let
        des3 =
          checkType des2 TDescriptionRef

        ( des4, objModelId ) =
          readRawInt des3
      in
      ( des4, Just objModelId )


checkIfHasDescription :
  DeserializationPoint
  -> Bool
  -> ( DeserializationPoint, Bool )
checkIfHasDescription des0 force =
  if force then
    let
      des1 =
        checkType des0 TMultipleDescriptions
    in
    ( des1, True )
  else if not <| peekType des0 TMultipleDescriptions then
    ( des0, False )
  else
    ( des0, True )


{-| Read array of non-primitives (can contain nulls).
-}
readArrayByType :
  DeserializationPoint
  -> DataType
  -> ( DeserializationPoint, AValueList )
readArrayByType des0 arrayElType =
  case arrayElType of
    TBoolean ->
      readBooleanArray des0

    TByte ->
      readByteArray des0

    TShort ->
      readShortArray des0

    TInt ->
      readIntArray des0

    -- TODO: readLongArray
    TFloat ->
      readFloatArray des0

    TDouble ->
      readDoubleArray des0

    _ ->
      intentionalCrash ( des0, ANonPrimitiveArray [] ) ("unknown primitive array type: " ++ toString arrayElType)


readNonPrimitiveArrayByType :
  DeserializationPoint
  -> DataType
  -> (DeserializationPoint -> ( DeserializationPoint, a ))
  -> (a -> AValue)
  -> ( DeserializationPoint, AValueList )
readNonPrimitiveArrayByType des0 theType readValue packValue =
  let
    ( des1, arr ) =
      readArrayByType_ des0 theType readValue packValue
  in
  ( des1, ANonPrimitiveArray arr )


readArrayByType_ :
  DeserializationPoint
  -> DataType
  -> (DeserializationPoint -> ( DeserializationPoint, a ))
  -> (a -> AValue)
  -> ( DeserializationPoint, List AValue )
readArrayByType_ des0 theType readValue packValue =
  let
    ( des1, n ) =
      beginTypedArray des0 theType False

    ( des2, reversedArr ) =
      iterateFoldl
        (\( des0, arr ) idx ->
          let
            ( des1, isTheType ) =
              expectTypeOrNull des0 theType
          in
          if isTheType then
            let
              ( des2, val ) =
                readValue des1
            in
            ( des2, packValue val :: arr )
          else
            ( des1, AReference Nothing :: arr )
        )
        ( des1, [] )
        0
        (n - 1)

    arr =
      --TODO: avoid reversing list. Maybe change List to Array?
      reversedArr |> List.reverse
  in
  ( des2, arr )


readBooleanArray : DeserializationPoint -> ( DeserializationPoint, AValueList )
readBooleanArray des0 =
  readNonPrimitiveArrayByType des0 TBoolean readRawBoolean ABool


readByteArray : DeserializationPoint -> ( DeserializationPoint, AValueList )
readByteArray des0 =
  readNonPrimitiveArrayByType des0 TByte readRawByte AInt


readShortArray : DeserializationPoint -> ( DeserializationPoint, AValueList )
readShortArray des0 =
  readNonPrimitiveArrayByType des0 TShort readRawShort AInt


readIntArray : DeserializationPoint -> ( DeserializationPoint, AValueList )
readIntArray des0 =
  readNonPrimitiveArrayByType des0 TInt readRawInt AInt


readFloatArray : DeserializationPoint -> ( DeserializationPoint, AValueList )
readFloatArray des0 =
  readNonPrimitiveArrayByType des0 TFloat readRawFloat AFloat


readDoubleArray : DeserializationPoint -> ( DeserializationPoint, AValueList )
readDoubleArray des0 =
  readNonPrimitiveArrayByType des0 TDouble readRawFloat AFloat


readPrimitiveArrayByType :
  DeserializationPoint
  -> DataType
  -> ( DeserializationPoint, AValueList )
readPrimitiveArrayByType des0 arrayElType =
  let
    read =
      readPrimitiveArray des0

    pack t =
      repackValue (APrimitiveArray t)
  in
  case arrayElType of
    TBoolean ->
      read TBoolean readRawBoolean ABool
        |> pack TBoolean

    TByte ->
      read TByte readRawByte AInt
        |> pack TByte

    TShort ->
      read TShort readRawShort AInt
        |> pack TShort

    TInt ->
      read TInt readRawInt AInt
        |> pack TInt

    TFloat ->
      read TFloat readRawFloat AFloat
        |> pack TFloat

    TDouble ->
      read TDouble readRawDouble AFloat
        |> pack TDouble

    -- TODO: Long
    _ ->
      intentionalCrash ( des0, APrimitiveArray TUnknown [] ) ("unknown primitive array type: " ++ toString arrayElType)


readPrimitiveBooleanArray : DeserializationPoint -> ( DeserializationPoint, List Bool )
readPrimitiveBooleanArray des0 =
  readPrimitiveArray des0 TBoolean readRawBoolean identity


readPrimitiveArray :
  DeserializationPoint
  -> DataType
  -> (DeserializationPoint -> ( DeserializationPoint, a ))
  -> (a -> b)
  -> ( DeserializationPoint, List b )
readPrimitiveArray des0 theType readFunc packValue =
  let
    ( des1, n ) =
      beginTypedArray des0 theType True

    ( des2, arr ) =
      iterateFoldl
        (\( des, arr ) idx ->
          let
            ( newDes, val ) =
              readFunc des
          in
          ( newDes, packValue val :: arr )
        )
        ( des1, [] )
        0
        (n - 1)
  in
  -- TODO get rid of reversing a list
  ( des2, List.reverse arr )


readRawByType : DeserializationPoint -> DataType -> ( DeserializationPoint, AValue )
readRawByType des0 dataType =
  case dataType of
    TByte ->
      repackValue AInt (readRawByte des0)

    TShort ->
      repackValue AInt (readRawShort des0)

    TInt ->
      repackValue AInt (readRawInt des0)

    TString ->
      repackValue AString (readString des0)

    TBoolean ->
      repackValue ABool (readRawBoolean des0)

    TFloat ->
      repackValue AFloat (readRawFloat des0)

    TDouble ->
      repackValue AFloat (readRawDouble des0)

    -- TODO: Long
    _ ->
      intentionalCrash ( des0, AInt -1 ) ("type not supported: " ++ toString dataType)


rememberInSession : ObjectReadSession -> ValueTreeId -> Maybe ObjectModelNodeId -> ObjectReadSession
rememberInSession session id objModelId =
  { session | valueTrees = ( id, objModelId ) :: session.valueTrees }
