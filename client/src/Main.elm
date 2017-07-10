module Main exposing (..)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Task
import Binary.ArrayBuffer exposing (ArrayBuffer, stringToBufferArray, byteLength, getByte, asUint8Array, bytesToDebugString)
import WebSocket
import WebSocket.LowLevel exposing (MessageData(..))
import Common exposing (send)
import Constants exposing (..)
import ObjectModelNode exposing (..)
import Serialization exposing (..)


main =
  Html.program
    { init = init
    , view = view
    , update = update
    , subscriptions = subscriptions
    }


-- MODEL


type alias Model =
  { input : String
  , messages : List String
  , modelNodes : List ObjectModelNode
  }


init : ( Model, Cmd Msg )
init =
  ( Model "" [] [ defaultModelNode ], Cmd.none )


-- UPDATE


type Msg
  = Input String
  | Send
  | NewNetworkMessage MessageData
  | Msg_Unknown
  | Msg_OnAddedSystem Int String (Maybe BitVector) (Maybe BitVector) (Maybe BitVector)


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
  let
    { input, messages, modelNodes } =
      model
  in
    case msg of
      Input newInput ->
        { model | input = newInput } ! []

      Send ->
        ( { model | input = "" }
        , WebSocket.send websocketUrl (ArrayBuffer (stringToBufferArray input))
        )

      NewNetworkMessage (String str) ->
        { model | messages = (str :: messages) } ! []

      NewNetworkMessage (ArrayBuffer bytes) ->
        let
          packet =
            deserializePacket bytes

          cmd =
            send packet
        in
          ( { model | messages = (bytes |> bytesToDebugString) :: messages }, cmd )

      Msg_Unknown ->
        model ! []

      Msg_OnAddedSystem index name allTypes oneTypes notTypes ->
        { model | messages = ((index |> toString) ++ ": " ++ name) :: messages } ! []


deserializePacket : ArrayBuffer -> Msg
deserializePacket bytes =
  let
    ( des0, packetType ) =
      beginDeserialization bytes
        |> readRawByte
  in
    if packetType == type_AddedEntitySystem then
      let
        ( des1, index ) =
          readInt des0

        ( des2, name ) =
          readString des1

        ( des3, allTypes ) =
          readBitVector des2

        ( des4, oneTypes ) =
          readBitVector des3

        ( des5, notTypes ) =
          readBitVector des4
      in
        Msg_OnAddedSystem index (Maybe.withDefault "" name) allTypes oneTypes notTypes
    else
      Msg_Unknown


-- SUBSCRIPTIONS


subscriptions : Model -> Sub Msg
subscriptions model =
  WebSocket.listen websocketUrl NewNetworkMessage


-- VIEW


view : Model -> Html Msg
view model =
  div []
    [ div [] (List.map viewMessage model.messages)
    , input [ onInput Input, value model.input ] []
    , button [ onClick Send ] [ text "Send" ]
    ]


viewMessage : String -> Html msg
viewMessage msg =
  div [] [ text msg ]
