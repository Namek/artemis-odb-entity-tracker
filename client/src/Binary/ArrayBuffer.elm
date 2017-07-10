module Binary.ArrayBuffer exposing
  ( ArrayBuffer
  , new
  , byteLength
  , Uint8Array
  , stringToBufferArrayWithOffset
  , stringToBufferArray
  , asUint8Array
  , getByte
  , setByte
  , bytesToString
  , bytesToDebugString
  )

{-|
Low-level bindings to the ArrayBuffer API

https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/ArrayBuffer

@docs ArrayBuffer, new, byteLength, transfer, clone
-}
import Native.Binary.ArrayBuffer

import Char

{-|
A value representing a handle to a raw binary data buffer. This excludes views such as typed array objects or data views.
-}
type ArrayBuffer = ArrayBuffer

{-|
Creates a new buffer with the given length in bytes
-}
new : Int -> ArrayBuffer
new size = Native.Binary.ArrayBuffer._new size

{-|
Returns the length in bytes of an buffer
-}
byteLength : ArrayBuffer -> Int
byteLength buffer = Native.Binary.ArrayBuffer.byteLength buffer

type Uint8Array = Uint8Array

stringToBufferArrayWithOffset : String -> Int -> Int -> ArrayBuffer
stringToBufferArrayWithOffset str idxStart idxEnd = Native.Binary.ArrayBuffer.stringToBufferArray str idxStart idxEnd

stringToBufferArray : String -> ArrayBuffer
stringToBufferArray str = Native.Binary.ArrayBuffer.stringToBufferArray str

asUint8Array : ArrayBuffer -> Uint8Array
asUint8Array buffer = Native.Binary.ArrayBuffer.asUint8Array buffer

getByte : Uint8Array -> Int -> Int
getByte buffer index = Native.Binary.ArrayBuffer.getByte buffer index

setByte : Uint8Array -> Int -> Int -> Uint8Array
setByte buffer index value = Native.Binary.ArrayBuffer.setByte buffer index value

bytesToString : ArrayBuffer -> String
bytesToString buf =
  let
    len =
      byteLength buf

    arr =
      asUint8Array buf

    rec : Int -> String -> String
    rec idx acc_str =
      if idx < len then
        let
          char = String.fromChar (Char.fromCode (getByte arr idx))
        in
          rec (idx + 1) (acc_str ++ char)
      else
        acc_str
  in
    rec 0 ""

    
bytesToDebugString : ArrayBuffer -> String
bytesToDebugString buf =
  let
    len =
      byteLength buf

    arr =
      asUint8Array buf

    rec : Int -> String -> String
    rec idx acc_str =
      if idx < len then
        rec (idx + 1) (acc_str ++ " " ++ ((getByte arr idx) |> toString))
      else
        acc_str
  in
    rec 0 ""
