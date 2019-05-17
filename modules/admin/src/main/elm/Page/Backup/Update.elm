module Page.Backup.Update exposing (update)

import Api
import Util.Maybe
import Page.Backup.Data exposing (..)

update: Msg -> Model -> (Model, Cmd Msg)
update msg model =
    case msg of
        GetPeer id ->
            (model, Cmd.batch [getPeer model id, getBackupOut model id, getRestoreOut model id])

        GetPeerResp (Ok peer) ->
            let mm = {model | peer = peer} in
            (mm, getLater mm)
        GetPeerResp (Err err) ->
            (model, Cmd.none)

        GetBackupOutResp (Ok out) ->
            ({model | backupOut = out}, Cmd.none)
        GetBackupOutResp (Err err) ->
            let x = Debug.log "err" err
            in
            (model, Cmd.none)

        RunBackup ->
            let
                running = Util.Maybe.nonEmpty model.peer.runTime
                empty = model.peer.id == ""
            in
                if (running || empty) then (model, Cmd.none)
                else ({model|backupOutTab = Init}, Api.runBackup model.flags.apiBase model.peer.id RunBackupResp)

        RunBackupResp (Ok _) ->
            (model, getPeer model model.peer.id)

        RunBackupResp (Err err) ->
            (model, getPeer model model.peer.id)

        GetBackupStdOut ->
            ({model|backupOutTab = Stdout}, Api.getBackupStdout model.flags.apiBase model.peer.id BackupStdOutResp)
        GetBackupStdErr ->
            ({model|backupOutTab = Stderr}, Api.getBackupStderr model.flags.apiBase model.peer.id BackupStdErrResp)

        BackupStdOutResp (Ok s) ->
            ({model | stdoutBackup = s}, Cmd.none)
        BackupStdOutResp (Err err) ->
            ({model | stdoutBackup = ""}, Cmd.none)

        BackupStdErrResp (Ok s) ->
            ({model | stderrBackup = s}, Cmd.none)
        BackupStdErrResp (Err err) ->
            ({model | stderrBackup = ""}, Cmd.none)


        GetRestoreOutResp (Ok out) ->
            ({model | restoreOut = out}, Cmd.none)
        GetRestoreOutResp (Err err) ->
            let x = Debug.log "err" err
            in
            (model, Cmd.none)
        RunRestore ->
            let
                running = Util.Maybe.nonEmpty model.peer.runTime
                empty = model.peer.id == ""
            in
                if (running || empty) then (model, Cmd.none)
                else ({model|restoreOutTab = Init}, Api.runRestore model.flags.apiBase model.peer.id model.daysBack RunRestoreResp)
        RunRestoreResp (Ok _) ->
            (model, getPeer model model.peer.id)

        RunRestoreResp (Err err) ->
            (model, getPeer model model.peer.id)
        GetRestoreStdOut ->
            ({model|restoreOutTab = Stdout}, Api.getRestoreStdout model.flags.apiBase model.peer.id RestoreStdOutResp)
        GetRestoreStdErr ->
            ({model|restoreOutTab = Stderr}, Api.getRestoreStderr model.flags.apiBase model.peer.id RestoreStdErrResp)

        RestoreStdOutResp (Ok s) ->
            ({model | stdoutRestore = s}, Cmd.none)
        RestoreStdOutResp (Err err) ->
            ({model | stdoutRestore = ""}, Cmd.none)

        RestoreStdErrResp (Ok s) ->
            ({model | stderrRestore = s}, Cmd.none)
        RestoreStdErrResp (Err err) ->
            ({model | stderrRestore = ""}, Cmd.none)

        SetDaysBack ds ->
            if ds == "" then ({model | daysBack = Nothing}, Cmd.none)
            else ({model| daysBack = String.toInt ds}, Cmd.none)

getBackupOut: Model -> String -> Cmd Msg
getBackupOut model id =
    Api.getOutPeerBackupRun model.flags.apiBase id GetBackupOutResp

getRestoreOut: Model -> String -> Cmd Msg
getRestoreOut model id =
    Api.getOutPeerRestoreRun model.flags.apiBase id GetRestoreOutResp

getPeer: Model -> String -> Cmd Msg
getPeer model id =
    Api.getOutPeer model.flags.apiBase id GetPeerResp

getLater: Model -> Cmd Msg
getLater model =
    case model.peer.runTime of
        Just _ ->
            Api.getOutPeerLater 800 model.flags.apiBase model.peer.id GetPeerResp
        Nothing ->
            Cmd.batch
                [getBackupOut model model.peer.id
                ,getRestoreOut model model.peer.id
                ]
