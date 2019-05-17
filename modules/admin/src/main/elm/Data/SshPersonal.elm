module Data.SshPersonal exposing (..)

import Json.Decode as Decode exposing (Decoder, int, string, float, bool)
import Json.Decode.Pipeline exposing (required, optional, hardcoded)

type alias SshPersonal =
    { host: String
    , bindPort: Int
    , user: String
    , enabled: Bool
    }

empty: SshPersonal
empty =
    { host = ""
    , bindPort = 0
    , user = ""
    , enabled = False
    }

sshPersonalDecoder: Decoder SshPersonal
sshPersonalDecoder =
    Decode.succeed SshPersonal
        |> required "host" string
        |> required "port" int
        |> required "user" string
        |> required "enabled" bool
