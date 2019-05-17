module Data.Device exposing (..)

import Json.Decode as Decode exposing (Decoder, int, string, float, bool)
import Json.Decode.Pipeline exposing (required, optional, hardcoded)

import Data.SshPersonal exposing (..)
import Data.SshRemote exposing (..)

type alias Device =
    { id: String
    , password: String
    , sshPublicKey: String
    , sshPrivateKey: String
    , sshPersonal: SshPersonal
    , sshRemote: SshRemote
    }

empty: Device
empty =
    { id = ""
    , password = ""
    , sshPublicKey = ""
    , sshPrivateKey = ""
    , sshPersonal = Data.SshPersonal.empty
    , sshRemote = Data.SshRemote.empty
    }

deviceDecoder: Decoder Device
deviceDecoder =
    Decode.succeed Device
        |> required "id" string
        |> required "password" string
        |> required "sshPublicKey" string
        |> required "sshPrivateKey" string
        |> required "sshPersonal" sshPersonalDecoder
        |> required "sshRemote" sshRemoteDecoder
