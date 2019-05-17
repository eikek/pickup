module Data.SetupData exposing (..)

import Json.Decode as Decode exposing (Decoder, int, string, float, bool)
import Json.Decode.Pipeline exposing (required, optional, hardcoded)
import Json.Encode as Encode
import Util.Maybe

type alias SetupData =
    { password: String
    }

empty: SetupData
empty =
    { password = ""
    }

setupDataEncode: SetupData -> Encode.Value
setupDataEncode sd =
    Encode.object
        [ ("password", Encode.string sd.password)
        ]
