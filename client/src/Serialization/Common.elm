module Serialization.Common exposing (..)

import Array exposing (Array)


type alias LongContainer =
  ( Int, Int )


type alias BitVector =
  Array Bool


integerSize : number
integerSize =
  32


defaultNewBufferSize : Int
defaultNewBufferSize =
  102400


intBitsToFloat : Int -> Float
intBitsToFloat int =
  Native.Serialization.intBitsToFloat int


floatToIntBits : Float -> Int
floatToIntBits float =
  Native.Serialization.floatToIntBits float


intBitsToDouble : Int -> Int -> Float
intBitsToDouble int1 int2 =
  Native.Serialization.intBitsToDouble int1 int2


intBitsToLong : Int -> Int -> Int
intBitsToLong int1 int2 =
  Native.Serialization.intBitsToLong int1 int2


repackValue : (a -> b) -> ( c, a ) -> ( c, b )
repackValue packFunc readFunc =
  let
    ( arg0, val ) =
      readFunc
  in
  ( arg0, packFunc val )
