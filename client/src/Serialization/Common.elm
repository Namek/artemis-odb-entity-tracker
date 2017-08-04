module Serialization.Common exposing (..)

import Array exposing (Array)


type alias LongContainer =
  ( Int, Int )


type alias BitVector =
  Array Bool


integerSize : number
integerSize =
  32
