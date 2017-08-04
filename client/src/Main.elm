module Main exposing (..)

import Array exposing (Array)
import Binary.ArrayBuffer exposing (ArrayBuffer, asUint8Array, byteLength, bytesToDebugString, getByte, stringToBufferArray)
import Common exposing (send, sure)
import Constants exposing (..)
import Dict exposing (Dict)
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import List.Extra
import Serialization.Common exposing (..)
import Serialization.Deserializer exposing (..)
import Serialization.ObjectModelNode exposing (..)
import Serialization.Serializer exposing (..)
import Serialization.ValueTree exposing (..)
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
  , javaObjects : JavaObjects
  , objModelNodes : List ObjectModelNode
  , valueTrees : List ValueTree
  , entities : Dict Int EntityInfo
  , systems : List EntitySystemInfo
  , managers : List EntityManagerInfo
  , componentTypes : Array ComponentTypeInfo
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
  , objModelId : ObjectModelNodeId
  }


init : ( Model, Cmd Msg )
init =
  ( Model ""
      []
      (JavaObjects
        Dict.empty
        0
      )
      []
      []
      Dict.empty
      []
      []
      Array.empty
  , Cmd.none
  )


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
  | OnWebsocketOpen String
  | OnWebsocketClose String
  | NewNetworkMessage MessageData
  | Msg_Unknown
  | Msg_OnAddedSystem Int String (Maybe BitVector) (Maybe BitVector) (Maybe BitVector)
  | Msg_OnAddedManager String
  | Msg_OnAddedComponentType Int String ObjectModelNodeId
  | Msg_OnUpdatedEntitySystem Int Int Int
  | Msg_OnAddedEntity EntityId BitVector
  | Msg_OnDeletedEntity EntityId
  | Msg_OnUpdatedComponentState EntityId Int
  | Request_ComponentState EntityId Int


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
  let
    { input, messages, javaObjects, objModelNodes, valueTrees, componentTypes } =
      model
  in
  case msg of
    Input newInput ->
      { model | input = newInput } ! []

    Send ->
      ( { model | input = "" }
      , WebSocket.send websocketUrl (ArrayBuffer (stringToBufferArray input) 0 (String.length input))
      )

    OnWebsocketOpen url ->
      let
        _ =
          Debug.log "websocket open" url
      in
      model ! []

    OnWebsocketClose url ->
      let
        _ =
          Debug.log "websocket close" url
      in
      model ! []

    NewNetworkMessage (String str) ->
      { model | messages = str :: messages } ! []

    NewNetworkMessage (ArrayBuffer bytes offset len) ->
      let
        ( des, packet ) =
          deserializePacket javaObjects objModelNodes valueTrees componentTypes bytes

        cmd =
          send packet
      in
      ( { model
          | messages = (bytes |> bytesToDebugString) :: messages
          , javaObjects = des.objects
          , objModelNodes = des.models
          , valueTrees = valueTrees
        }
      , cmd
      )

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
        newComponentType : ComponentTypeInfo
        newComponentType =
          { name = name, index = index, objModelId = objModelId }
      in
      { model | componentTypes = Array.push newComponentType model.componentTypes } ! []

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

    Msg_OnUpdatedComponentState entityId componentIndex ->
      -- TODO
      model ! []

    Request_ComponentState entityId componentIndex ->
      let
        ser =
          beginSerialization
            |> addRawByte type_RequestComponentState
            |> addInt entityId
            |> addInt componentIndex
      in
      model
        ! [ WebSocket.send websocketUrl (ArrayBuffer ser.buf 0 ser.pos)
          ]


deserializePacket : JavaObjects -> List ObjectModelNode -> List ValueTree -> Array ComponentTypeInfo -> ArrayBuffer -> ( DeserializationPoint, Msg )
deserializePacket objects objModelNodes valueTrees componentTypes bytes =
  let
    ( des0, packetType ) =
      beginDeserialization objects objModelNodes valueTrees bytes
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
    ( des5, Msg_OnAddedSystem index (sure name) allTypes oneTypes notTypes )
  else if packetType == type_AddedManager then
    let
      name =
        Tuple.second (readString des0) |> sure
    in
    ( des0, Msg_OnAddedManager name )
  else if packetType == type_AddedComponentType then
    let
      ( des1, index ) =
        readInt des0

      ( des2, name ) =
        readString des1

      ( des3, objModelId ) =
        readDataDescription des2
    in
    ( des3, Msg_OnAddedComponentType index (sure name) objModelId )
  else if packetType == type_AddedEntity then
    let
      ( des1, id ) =
        readInt des0

      ( des2, components ) =
        readBitVector des1
    in
    ( des2, Msg_OnAddedEntity id (sure components) )
  else if packetType == type_UpdatedEntitySystem then
    let
      ( des1, index ) =
        readInt des0

      ( des2, entitiesCount ) =
        readInt des1

      ( des3, maxEntitiesCount ) =
        readInt des2
    in
    ( des3, Msg_OnUpdatedEntitySystem index entitiesCount maxEntitiesCount )
  else if packetType == type_DeletedEntity then
    let
      ( des1, id ) =
        readInt des0
    in
    ( des1, Msg_OnDeletedEntity id )
  else if packetType == type_UpdatedComponentState then
    let
      ( des1, entityId ) =
        readInt des0

      ( des2, componentIndex ) =
        readInt des1

      componentTypeInfo =
        Array.get componentIndex componentTypes
          |> sure

      ( des3, _, valueTreeId ) =
        readObjectWithModel des0 componentTypeInfo.objModelId

      e =
        Debug.log "valueTreeId" valueTreeId

      -- TODO do something with the valueTree!
    in
    ( des3, Msg_OnUpdatedComponentState entityId componentIndex )
  else
    Debug.log ("unknown msg " ++ toString packetType) ( des0, Msg_Unknown )



-- SUBSCRIPTIONS


subscriptions : Model -> Sub Msg
subscriptions model =
  Sub.batch
    [ WebSocket.listen websocketUrl NewNetworkMessage
    , WebSocket.onOpen OnWebsocketOpen
    , WebSocket.onClose OnWebsocketClose
    ]



-- VIEW


view : Model -> Html Msg
view model =
  let
    componentTypes =
      Array.toList model.componentTypes

    componentTypeCount =
      List.length componentTypes
  in
  div []
    [ h2 [] [ text "Systems" ]
    , div [] (List.map viewSystem model.systems)
    , h2 [] [ text "Managers" ]
    , div [] (List.map viewManager model.managers)
    , h2 [] [ text "Component types" ]
    , div [] (List.map viewComponentType componentTypes)
    , h2 [] [ text "Entities" ]
    , table []
        [ thead [] [ viewEntitiesHeader componentTypes ]
        , tbody [] (Dict.foldr (viewEntityRow componentTypeCount) [] model.entities)
        ]
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


viewEntitiesHeader : List ComponentTypeInfo -> Html msg
viewEntitiesHeader componentTypes =
  let
    componentColumns =
      List.map (\t -> td [] [ text <| toString t.name ]) componentTypes
  in
  tr [] (td [] [ text "id" ] :: componentColumns)


viewEntityRow : Int -> EntityId -> EntityInfo -> List (Html Msg) -> List (Html Msg)
viewEntityRow componentTypesCount id entity rows =
  let
    idxToBool : Int -> Bool
    idxToBool idx =
      case Array.get idx entity.components of
        Just bool ->
          bool

        Nothing ->
          False

    idCell =
      td [] [ text <| toString id ]

    componentCell cmpIdx =
      let
        hasComponent =
          idxToBool cmpIdx
      in
      td
        (if hasComponent then
          [ onClick (Request_ComponentState id cmpIdx) ]
         else
          []
        )
        [ text (hasComponent |> toString) ]

    componentCells =
      Common.iterateFoldl (\acc idx -> componentCell idx :: acc) [] 0 (componentTypesCount - 1)
        |> List.reverse

    row =
      tr [] (idCell :: componentCells)
  in
  row :: rows


viewMessage : String -> Html msg
viewMessage msg =
  div [] [ text msg ]
