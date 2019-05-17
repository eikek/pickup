module Data.OutPeer exposing (..)

import Json.Decode as Decode exposing (Decoder, int, string, float, bool)
import Json.Decode.Pipeline exposing (required, optional, hardcoded)
import Json.Encode as Encode
import Util.Maybe
import Data.ScheduleData exposing (ScheduleData)

type alias OutPeer =
    { id: String
    , remoteUri: String
    , schedule: Maybe ScheduleData
    , description: String
    , enabled : Bool
    , runTime: Maybe Int
    }

empty: OutPeer
empty =
    { id = ""
    , remoteUri = ""
    , schedule = Nothing
    , description = ""
    , enabled = False
    , runTime = Nothing
    }

encode: OutPeer -> Encode.Value
encode peer =
    Encode.object
        [ ("id", Encode.string peer.id)
        , ("remoteUri", Encode.string peer.remoteUri)
        , ("schedule", Util.Maybe.encode Data.ScheduleData.scheduleDataEncode peer.schedule)
        , ("description", Encode.string peer.description)
        , ("enabled", Encode.bool peer.enabled)
        , ("runningTime", Util.Maybe.encode Encode.int peer.runTime)
        , ("connections", Encode.int 0)
        , ("lastConnection", Encode.string "")
        ]


outPeerDecoder: Decoder OutPeer
outPeerDecoder =
    Decode.succeed OutPeer
        |> required "id" string
        |> required "remoteUri" string
        |> required "schedule" (Decode.maybe Data.ScheduleData.scheduleDataDecoder)
        |> required "description" string
        |> required "enabled" bool
        |> required "runningTime" (Decode.maybe int)

outPeerListDecoder: Decoder (List OutPeer)
outPeerListDecoder =
    Decode.list outPeerDecoder
