module Constants exposing (..)


websocketUrl =
  "ws://localhost:8025/actions"


-- Tracker events


( type_AddedEntitySystem, type_AddedManager, type_AddedComponentType, type_UpdatedEntitySystem, type_AddedEntity, type_DeletedEntity, type_UpdatedComponentState ) =
  ( 60, 61, 63, 64, 68, 73, 104 )


-- UI requests


( type_SetSystemState, type_SetManagerState, type_RequestComponentState, type_SetComponentFieldValue ) =
  ( 90, 94, 103, 113 )
