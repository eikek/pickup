module Data.SshRemote exposing (..)

import Json.Decode as Decode exposing (Decoder, int, string, float, bool)
import Json.Decode.Pipeline exposing (required, optional, hardcoded)

type alias SshRemote =
    { host: String
    , bindPort: Int
    }

empty: SshRemote
empty =
    { host = ""
    , bindPort = 0
    }

sshRemoteDecoder: Decoder SshRemote
sshRemoteDecoder =
    Decode.succeed SshRemote
        |> required "host" string
        |> required "port" int
