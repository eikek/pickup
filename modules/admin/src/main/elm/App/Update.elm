module App.Update exposing (update, initPage)

import Api
import Browser exposing (UrlRequest(..))
import Browser.Navigation as Nav
import Url
import App.Data exposing (..)
import Page exposing (Page(..))
import Page.Setup.Data
import Page.Setup.Update
import Page.Device.Data
import Page.Device.Update
import Page.InPeers.Data
import Page.InPeers.Update
import Page.OutPeers.Data
import Page.OutPeers.Update
import Page.Backup.Data
import Page.Backup.Update

update: Msg -> Model -> (Model, Cmd Msg)
update msg model =
    case msg of
        SetupMsg lm ->
            updateSetup lm model
        DeviceMsg lm ->
            updateDevice lm model
        InPeersMsg lm ->
            updateInPeers lm model
        OutPeersMsg lm ->
            updateOutPeers lm model
        BackupMsg lm ->
            updateBackup lm model

        SetPage p ->
            ( {model | page = p }
            , Cmd.none
            )

        NavRequest req ->
            case req of
                Internal url ->
                    let
                        isCurrent =
                            Page.fromUrl url |>
                            Maybe.map (\p -> p == model.page) |>
                            Maybe.withDefault True
                    in
                        ( model
                        , if isCurrent then Cmd.none else Nav.pushUrl model.key (Url.toString url)
                        )

                External url ->
                    ( model
                    , Nav.load url
                    )

        NavChange url ->
            let
                page = Page.fromUrl url |> Maybe.withDefault SetupPage
                (m, c) = initPage model page
            in
                ( { m | page = page }, c )

updateSetup: Page.Setup.Data.Msg -> Model -> (Model, Cmd Msg)
updateSetup lmsg model =
    let
        (lm, lc) = Page.Setup.Update.update lmsg model.setupModel
    in
        ( {model | setupModel = lm }
        , Cmd.map SetupMsg lc
        )

updateDevice: Page.Device.Data.Msg -> Model -> (Model, Cmd Msg)
updateDevice lmsg model =
    let
        (lm, lc) = Page.Device.Update.update lmsg model.deviceModel
    in
        ( {model | deviceModel = lm }
        , Cmd.map DeviceMsg lc
        )

updateInPeers: Page.InPeers.Data.Msg -> Model -> (Model, Cmd Msg)
updateInPeers lmsg model =
    let
        (lm, lc) = Page.InPeers.Update.update lmsg model.inPeersModel
    in
        ( {model | inPeersModel = lm }
        , Cmd.map InPeersMsg lc
        )

updateOutPeers: Page.OutPeers.Data.Msg -> Model -> (Model, Cmd Msg)
updateOutPeers lmsg model =
    let
        (lm, lc) = Page.OutPeers.Update.update lmsg model.outPeersModel
    in
        ( {model | outPeersModel = lm }
        , Cmd.map OutPeersMsg lc
        )

updateBackup: Page.Backup.Data.Msg -> Model -> (Model, Cmd Msg)
updateBackup lmsg model =
    let
        (lm, lc) = Page.Backup.Update.update lmsg model.backupModel
    in
        ( {model | backupModel = lm }
        , Cmd.map BackupMsg lc
        )

initPage: Model -> Page -> (Model, Cmd Msg)
initPage model page =
    case page of
        DevicePage ->
            updateDevice Page.Device.Data.GetDevice model
        SetupPage ->
            updateSetup Page.Setup.Data.GetSetupState model
        InPeersPage ->
            updateInPeers Page.InPeers.Data.GetPeers model
        OutPeersPage ->
            updateOutPeers Page.OutPeers.Data.GetPeers model
        BackupPage id ->
            updateBackup (Page.Backup.Data.GetPeer id) model
