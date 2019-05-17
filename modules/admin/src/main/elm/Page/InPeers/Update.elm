module Page.InPeers.Update exposing (update)

import Api
import Page.InPeers.Data exposing (..)
import Data.InPeer

update: Msg -> Model -> (Model, Cmd Msg)
update msg model =
    case msg of
        GetPeers ->
            (model, getAll model)

        PeerListResp (Ok list) ->
            ({model | peers = list}, Cmd.none)

        PeerListResp (Err err) ->
            (model, Cmd.none)

        SetMode mode ->
            ({model| mode = mode, peer = Data.InPeer.empty }, Cmd.none)

        SavePeer ->
            ({model|updating = True}, Api.updateInPeer model.flags.apiBase model.peer SavePeerResp)

        SavePeerResp (Ok ()) ->
            ({model|updating = False, mode = List}, getAll model)

        SavePeerResp (Err err) ->
            ({model|updating = False}, Cmd.none)

        SetPeerId id ->
            let
                p = model.peer
                np = {p|id = id}
            in
                ({model|peer = np}, Cmd.none)

        SetPeerPubkey pk ->
            let
                p = model.peer
                np = {p|pubkey = pk}
            in
                ({model|peer = np}, Cmd.none)

        SetPeerDesc str ->
            let
                p = model.peer
                np = {p|description = str}
            in
                ({model|peer = np}, Cmd.none)

        SetPeerEnable flag ->
            let
                p = model.peer
                np = {p|enabled = flag}
            in
                ({model|peer = np}, Cmd.none)

        EditPeer peer ->
            ({model | peer = peer, mode = Add}, Cmd.none)

        DeletePeer peer ->
            (model, Api.deleteInPeer model.flags.apiBase peer.id DeletePeerResp)

        DeletePeerResp (Ok _) ->
            (model, getAll model)
        DeletePeerResp (Err _) ->
            (model, Cmd.none)

getAll: Model -> Cmd Msg
getAll model =
    Api.getInPeers model.flags.apiBase PeerListResp
