module Page.InPeers.Data exposing (..)

import Http
import Data.Flags exposing (Flags)
import Data.InPeer exposing (InPeer)

type alias Model =
    { flags: Flags
    , peers : List InPeer
    , mode: Mode
    , peer: InPeer
    , updating: Bool
    }

emptyModel: Flags -> Model
emptyModel flags =
    { flags = flags
    , peers = []
    , mode = List
    , peer = Data.InPeer.empty
    , updating = False
    }

type Msg
    = GetPeers
    | PeerListResp (Result Http.Error (List InPeer))
    | SetMode Mode
    | SavePeer
    | SavePeerResp (Result Http.Error ())
    | SetPeerId String
    | SetPeerPubkey String
    | SetPeerDesc String
    | SetPeerEnable Bool
    | EditPeer InPeer
    | DeletePeer InPeer
    | DeletePeerResp (Result Http.Error ())

type Mode
    = List
    | Add
