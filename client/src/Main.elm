module Main exposing (..)

import Binary.ArrayBuffer exposing (ArrayBuffer, asUint8Array, byteLength, bytesToDebugString, getByte, stringToBufferArray)
import Common exposing (send, sure)
import Constants exposing (..)
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import ObjectModelNode exposing (..)
import Serialization exposing (..)
import Task
import WebSocket
import WebSocket.LowLevel exposing (MessageData(..))


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
  | Msg_OnAddedManager String
  | Msg_OnAddedComponentType Int String ObjectModelNodeId


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
      { model | messages = str :: messages } ! []

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
      let
        msg =
          (index |> toString)
            ++ ": "
            ++ name
            ++ " all("
            ++ maybeBitsToString allTypes
            ++ ")"
            ++ " one("
            ++ maybeBitsToString oneTypes
            ++ ")"
            ++ " not("
            ++ maybeBitsToString notTypes
            ++ ")"
      in
      { model | messages = msg :: messages } ! []

    Msg_OnAddedManager name ->
      { model | messages = ("manager: " ++ name) :: messages } ! []

    Msg_OnAddedComponentType index name objModelId ->
      { model | messages = ("component type: " ++ toString index ++ ": " ++ name) :: messages } ! []


maybeBitsToString : Maybe BitVector -> String
maybeBitsToString bits =
  case bits of
    Just bits ->
      bitVectorToDebugString bits

    Nothing ->
      ""


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
  else if packetType == type_AddedManager then
    let
      name =
        Tuple.second (readString des0) |> sure
    in
    Msg_OnAddedManager name
  else if packetType == type_AddedComponentType then
    let
      ( des1, index ) =
        readInt des0

      ( des2, name ) =
        readString des1

      ( des3, objModelId ) =
        readDataDescription des2
    in
    Msg_OnAddedComponentType index (sure name) objModelId
  else
    Debug.log ("unknown msg: " ++ toString packetType) Msg_Unknown



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
