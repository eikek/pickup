module Page.OutPeers.Data exposing (..)

import Http
import Data.Flags exposing (Flags)
import Data.OutPeer exposing (OutPeer)

type alias Model =
    { flags: Flags
    , peers : List OutPeer
    , mode: Mode
    , peer: OutPeer
    , updating: Bool
    }

emptyModel: Flags -> Model
emptyModel flags =
    { flags = flags
    , peers = []
    , mode = List
    , peer = Data.OutPeer.empty
    , updating = False
    }

type Msg
    = GetPeers
    | PeerListResp (Result Http.Error (List OutPeer))
    | SetMode Mode
    | SavePeer
    | SavePeerResp (Result Http.Error ())
    | SetPeerUri String
    | SetPeerDesc String
    | SetPeerEnable Bool
    | SetPeerSchedule String
    | EditPeer OutPeer
    | DeletePeer OutPeer
    | DeletePeerResp (Result Http.Error ())

type Mode
    = List
    | Add
