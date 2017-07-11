module Main exposing (..)

import Binary.ArrayBuffer exposing (ArrayBuffer, asUint8Array, byteLength, bytesToDebugString, getByte, stringToBufferArray)
import Common exposing (send, sure)
import Constants exposing (..)
import Dict exposing (Dict)
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import List.Extra
import ObjectModelNode exposing (..)
import Serialization exposing (..)
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
  , entities : Dict Int EntityInfo
  , systems : List EntitySystemInfo
  , managers : List EntityManagerInfo
  , componentTypes : List ComponentTypeInfo
  }


type alias EntityId =
  Int


type alias EntityInfo =
  { id : EntityId
  , components : BitVector
  }


type alias EntitySystemInfo =
  { name : String
  , index : Int
  , allTypes : Maybe BitVector
  , oneTypes : Maybe BitVector
  , notTypes : Maybe BitVector
  , entitiesCount : Int
  , maxEntitiesCount : Int
  }


type alias EntityManagerInfo =
  { name : String }


type alias ComponentTypeInfo =
  { name : String
  , index : Int
  , objModelId : Int
  }


init : ( Model, Cmd Msg )
init =
  ( Model "" [] [ defaultModelNode ] Dict.empty [] [] [], Cmd.none )


createEntitySystemInfo : String -> Int -> Maybe BitVector -> Maybe BitVector -> Maybe BitVector -> EntitySystemInfo
createEntitySystemInfo name index allTypes oneTypes notTypes =
  { name = name, index = index, allTypes = allTypes, oneTypes = oneTypes, notTypes = notTypes, entitiesCount = 0, maxEntitiesCount = 0 }


updateSystemStats : List EntitySystemInfo -> Int -> Int -> Int -> List EntitySystemInfo
updateSystemStats systems index entitiesCount maxEntitiesCount =
  let
    updateFunc s =
      { s | entitiesCount = entitiesCount, maxEntitiesCount = maxEntitiesCount }
  in
  List.Extra.updateAt index updateFunc systems |> sure



-- UPDATE


type Msg
  = Input String
  | Send
  | NewNetworkMessage MessageData
  | Msg_Unknown
  | Msg_OnAddedSystem Int String (Maybe BitVector) (Maybe BitVector) (Maybe BitVector)
  | Msg_OnAddedManager String
  | Msg_OnAddedComponentType Int String ObjectModelNodeId
  | Msg_OnUpdatedEntitySystem Int Int Int
  | Msg_OnAddedEntity EntityId BitVector
  | Msg_OnDeletedEntity EntityId


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
        newSystemInfo =
          createEntitySystemInfo name index allTypes oneTypes notTypes
      in
      { model | systems = newSystemInfo :: model.systems |> List.sortBy .index } ! []

    Msg_OnAddedManager name ->
      let
        newManager =
          { name = name }
      in
      { model | managers = newManager :: model.managers } ! []

    Msg_OnAddedComponentType index name objModelId ->
      let
        newComponentType =
          { name = name, index = index, objModelId = objModelId }
      in
      { model | componentTypes = newComponentType :: model.componentTypes |> List.sortBy .index } ! []

    Msg_OnUpdatedEntitySystem index entitiesCount maxEntitiesCount ->
      let
        newSystems =
          updateSystemStats model.systems index entitiesCount maxEntitiesCount
      in
      { model | systems = newSystems } ! []

    Msg_OnAddedEntity id components ->
      let
        newEntity =
          { id = id, components = components }
      in
      { model | entities = Dict.insert id newEntity model.entities } ! []

    Msg_OnDeletedEntity id ->
      { model | entities = Dict.remove id model.entities } ! []


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
    Msg_OnAddedSystem index (sure name) allTypes oneTypes notTypes
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
  else if packetType == type_AddedEntity then
    let
      ( des1, id ) =
        readInt des0

      ( des2, components ) =
        readBitVector des1
    in
    Msg_OnAddedEntity id (sure components)
  else if packetType == type_DeletedEntity then
    let
      ( des1, id ) =
        readInt des0
    in
    Msg_OnDeletedEntity id
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
    [ h2 [] [ text "Systems" ]
    , div [] (List.map viewSystem model.systems)
    , h2 [] [ text "Managers" ]
    , div [] (List.map viewManager model.managers)
    , h2 [] [ text "Component types" ]
    , div [] (List.map viewComponentType model.componentTypes)
    , h2 [] [ text "Entities" ]
    , div [] (Dict.foldr viewEntity [] model.entities)
    , h2 [] [ text "Debug messages" ]
    , div [] (List.map viewMessage model.messages)
    , input [ onInput Input, value model.input ] []
    , button [ onClick Send ] [ text "Send" ]
    ]


viewSystem : EntitySystemInfo -> Html msg
viewSystem system =
  let
    aText =
      (system.index |> toString)
        ++ ": "
        ++ system.name
        ++ " all("
        ++ maybeBitsToString system.allTypes
        ++ ")"
        ++ " one("
        ++ maybeBitsToString system.oneTypes
        ++ ")"
        ++ " not("
        ++ maybeBitsToString system.notTypes
        ++ ")"
  in
  div [] [ text aText ]


viewManager : EntityManagerInfo -> Html msg
viewManager manager =
  div [] [ text manager.name ]


viewComponentType : ComponentTypeInfo -> Html msg
viewComponentType cmp =
  div [] [ text (toString cmp.index ++ ": " ++ cmp.name) ]


maybeBitsToString : Maybe BitVector -> String
maybeBitsToString bits =
  case bits of
    Just bits ->
      bitVectorToDebugString bits

    Nothing ->
      ""


viewEntity : EntityId -> EntityInfo -> List (Html msg) -> List (Html msg)
viewEntity id entity divs =
  div [] [ text (toString entity.id ++ ": " ++ bitVectorToDebugString entity.components) ] :: divs


viewMessage : String -> Html msg
viewMessage msg =
  div [] [ text msg ]
