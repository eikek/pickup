module Page.OutPeers.Update exposing (update)

import Api
import Util.Maybe
import Page.OutPeers.Data exposing (..)
import Data.OutPeer
import Data.ScheduleData

update: Msg -> Model -> (Model, Cmd Msg)
update msg model =
    case msg of
        GetPeers ->
            (model, getAll model)

        PeerListResp (Ok list) ->
            let
                mm = {model | peers = list}
            in
                (mm, getLater mm)

        PeerListResp (Err err) ->
            (model, getLater model)

        SetMode mode ->
            ({model| mode = mode, peer = Data.OutPeer.empty}, Cmd.none)

        SavePeer ->
            ({model|updating = True}, Api.updateOutPeer model.flags.apiBase model.peer SavePeerResp)

        SavePeerResp (Ok ()) ->
            ({model|updating = False, mode = List}, getAll model)

        SavePeerResp (Err err) ->
            ({model|updating = False}, Cmd.none)

        SetPeerUri uri ->
            let
                p = model.peer
                np = {p|remoteUri = uri}
            in
                ({model|peer = np}, Cmd.none)

        SetPeerDesc str ->
            let
                p = model.peer
                np = {p|description = str}
            in
                ({model|peer = np}, Cmd.none)

        SetPeerSchedule str ->
            let
                p = model.peer
                sc = case model.peer.schedule of
                         Just s ->
                             {s|schedule = str}
                         Nothing ->
                             Data.ScheduleData.ofSchedule str
                np = {p|schedule = if str == "" then Nothing else Just sc}
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
            (model, Api.deleteOutPeer model.flags.apiBase peer.id DeletePeerResp)

        DeletePeerResp (Ok _) ->
            (model, getAll model)
        DeletePeerResp (Err _) ->
            (model, Cmd.none)

getLater: Model -> Cmd Msg
getLater model =
    let
        running =
            List.map .runTime model.peers
                |> List.any Util.Maybe.nonEmpty
        load =
            Api.getOutPeersLater 800 model.flags.apiBase PeerListResp
    in
        if running then load else Cmd.none



getAll: Model -> Cmd Msg
getAll model =
    Api.getOutPeers model.flags.apiBase PeerListResp
