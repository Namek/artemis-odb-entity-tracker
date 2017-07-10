module ValueTree exposing (ValueTree)


type alias ValueTree =
  { id : Int
  , values : List a
  , parentId : Maybe Int
  , modelId : Maybe Int
  }
