module Page.Device.Data exposing (..)

import Http
import Data.Flags exposing (Flags)
import Data.Device exposing (Device)

type alias Model =
    { flags: Flags
    , device: Device
    , tab: Tab
    , showPass: Bool
    }

emptyModel: Flags -> Model
emptyModel flags =
    { flags = flags
    , device = Data.Device.empty
    , tab = RemoteAccess
    , showPass = False
    }

type Msg
    = GetDevice
    | DeviceResp (Result Http.Error Device)
    | SetTab Tab
    | ToggleShowPass

type Tab
    = RemoteAccess
    | Encryption
    | PrivateAccessKey
    | PersonalAccess

activeTab: Model -> Tab -> Bool
activeTab model tab =
    model.tab == tab
