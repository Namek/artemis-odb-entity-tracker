module Common exposing (..)

import Array exposing (Array)
import Task


intentionalCrash : a -> String -> a
intentionalCrash a str =
  let
    _ =
      Debug.crash str
  in
  a


elemIndex : Array a -> a -> Maybe Int
elemIndex arr expectedElem =
  let
    len =
      Array.length arr

    find idx =
      if idx < len then
        let
          elem =
            Array.get idx arr
        in
        case elem of
          Just val ->
            if val == expectedElem then
              Just idx
            else
              find (idx + 1)

          Nothing ->
            find (idx + 1)
      else
        Nothing
  in
  find 0


iterateFoldl : (a -> Int -> Maybe a) -> a -> Int -> Int -> a
iterateFoldl callback acc startIndex endIndex =
  if startIndex <= endIndex then
    case callback acc startIndex of
      Just newAcc ->
        iterateFoldl callback newAcc (startIndex + 1) endIndex

      Nothing ->
        acc
  else
    acc


send : msg -> Cmd msg
send msg =
  Task.succeed msg
    |> Task.perform identity


sure : Maybe a -> a
sure m =
  case m of
    Just val ->
      val

    Nothing ->
      Debug.crash "unexpected: a value should be here"
