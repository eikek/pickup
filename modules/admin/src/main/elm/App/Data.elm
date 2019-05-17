module App.Data exposing (..)

import Browser exposing (UrlRequest)
import Browser.Navigation exposing (Key)
import Url exposing (Url)
import Http
import Data.Flags exposing (Flags)
import Page exposing (Page(..))
import Page.Setup.Data
import Page.Device.Data
import Page.InPeers.Data
import Page.OutPeers.Data
import Page.Backup.Data

type alias Model =
    { flags: Flags
    , key: Key
    , page: Page
    , setupModel: Page.Setup.Data.Model
    , deviceModel: Page.Device.Data.Model
    , inPeersModel: Page.InPeers.Data.Model
    , outPeersModel: Page.OutPeers.Data.Model
    , backupModel: Page.Backup.Data.Model
    }

init: Key -> Url -> Flags -> Model
init key url flags =
    let
        page = Page.fromUrl url |> Maybe.withDefault SetupPage
    in
        { flags = flags
        , key = key
        , page = page
        , setupModel = Page.Setup.Data.emptyModel flags key
        , deviceModel = Page.Device.Data.emptyModel flags
        , inPeersModel = Page.InPeers.Data.emptyModel flags
        , outPeersModel = Page.OutPeers.Data.emptyModel flags
        , backupModel = Page.Backup.Data.emptyModel flags
        }

type Msg
    = NavRequest UrlRequest
    | NavChange Url
    | SetupMsg Page.Setup.Data.Msg
    | DeviceMsg Page.Device.Data.Msg
    | InPeersMsg Page.InPeers.Data.Msg
    | OutPeersMsg Page.OutPeers.Data.Msg
    | BackupMsg Page.Backup.Data.Msg
    | SetPage Page
