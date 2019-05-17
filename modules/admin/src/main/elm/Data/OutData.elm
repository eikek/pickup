module Data.OutData exposing (..)

import Json.Decode as Decode exposing (Decoder, int, string, float, bool)
import Json.Decode.Pipeline exposing (required, optional, hardcoded)

type alias OutData =
    { runAt: String
    , runTime: Int
    , returnCode: Int
    , success: Bool
    , runCount: Int
    , runSuccess: Int
    }

empty: OutData
empty =
    { runAt = ""
    , runTime = 0
    , returnCode = 0
    , success = False
    , runCount = 0
    , runSuccess = 0
    }

outDataDecoder: Decode.Decoder OutData
outDataDecoder =
    Decode.succeed OutData
        |> required "runAt" string
        |> required "runTime" int
        |> required "returnCode" int
        |> required "success" bool
        |> required "runCount" int
        |> required "runSuccess" int
