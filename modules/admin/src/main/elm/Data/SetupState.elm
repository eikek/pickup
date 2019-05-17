module Data.SetupState exposing (..)

import Json.Decode as Decode exposing (Decoder, int, string, float, bool)
import Json.Decode.Pipeline exposing (required, optional, hardcoded)

type alias SetupState =
    { exists: Bool
    , device: String
    }

setupDataDecoder: Decoder SetupState
setupDataDecoder =
    Decode.succeed SetupState
        |> required "exists" bool
        |> required "device" string
