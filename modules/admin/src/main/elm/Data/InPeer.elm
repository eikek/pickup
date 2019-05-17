module Data.InPeer exposing (..)

import Json.Decode as Decode exposing (Decoder, int, string, float, bool)
import Json.Decode.Pipeline exposing (required, optional, hardcoded)
import Json.Encode as Encode

type alias InPeer =
    { id: String
    , pubkey: String
    , description: String
    , enabled : Bool
    , sizeBytes: Int
    , sizeString: String
    }

empty: InPeer
empty =
    { id = ""
    , pubkey = ""
    , description = ""
    , enabled = False
    , sizeBytes = 0
    , sizeString = ""
    }

encode: InPeer -> Encode.Value
encode peer =
    Encode.object
        [ ("id", Encode.string peer.id)
        , ("pubkey", Encode.string peer.pubkey)
        , ("description", Encode.string peer.description)
        , ("enabled", Encode.bool peer.enabled)
        , ("connections", Encode.int 0)
        , ("lastConnection", Encode.string "")
        , ("sizeBytes", Encode.int peer.sizeBytes)
        , ("sizeString", Encode.string peer.sizeString)
        ]


inPeerDecoder: Decoder InPeer
inPeerDecoder =
    Decode.succeed InPeer
        |> required "id" string
        |> required "pubkey" string
        |> required "description" string
        |> required "enabled" bool
        |> required "sizeBytes" int
        |> required "sizeString" string

inPeerListDecoder: Decoder (List InPeer)
inPeerListDecoder =
    Decode.list inPeerDecoder
