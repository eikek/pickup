module Data.ScheduleData exposing (..)

import Json.Decode as Decode exposing (Decoder, int, string, float, bool)
import Json.Decode.Pipeline exposing (required, optional, hardcoded)
import Json.Encode as Encode

type alias ScheduleData =
    { schedule: String
    , nextRun: String
    , nextRunMillis: Int
    }

ofSchedule: String -> ScheduleData
ofSchedule schedule =
    { schedule = schedule
    , nextRun = ""
    , nextRunMillis = 0
    }

scheduleDataDecoder: Decode.Decoder ScheduleData
scheduleDataDecoder =
    Decode.succeed ScheduleData
        |> required "schedule" string
        |> required "nextRun" string
        |> required "nextRunMillis" int

scheduleDataEncode: ScheduleData -> Encode.Value
scheduleDataEncode sd =
    Encode.object
        [ ("schedule", Encode.string sd.schedule)
        , ("nextRun", Encode.string sd.nextRun)
        , ("nextRunMillis", Encode.int sd.nextRunMillis)
        ]
