module Util.Maybe exposing (..)

import Json.Encode as Encode

isEmpty: Maybe a -> Bool
isEmpty ma =
    Maybe.map (\_ -> False) ma |> Maybe.withDefault True

nonEmpty: Maybe a -> Bool
nonEmpty ma =
    not (isEmpty ma)

encode: (a -> Encode.Value) -> Maybe a -> Encode.Value
encode enc ma =
    Maybe.map enc ma
        |> Maybe.withDefault Encode.null
