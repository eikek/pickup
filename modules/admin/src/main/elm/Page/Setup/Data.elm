module Page.Setup.Data exposing (..)

import Http
import Browser.Navigation exposing (Key)
import Data.Flags exposing (Flags)
import Data.SetupState exposing (SetupState)
import Data.SetupData exposing (SetupData)

type alias Model =
    { flags: Flags
    , key: Key
    , state: Maybe SetupState
    , data: SetupData
    , setupRunning: Bool
    }

emptyModel: Flags -> Key -> Model
emptyModel flags key =
    { flags = flags
    , key = key
    , state = Nothing
    , data = Data.SetupData.empty
    , setupRunning = False
    }

type Msg
    = GetSetupState
    | SetupStateResp (Result Http.Error SetupState)
    | SetPassword String
    | RunSetup
    | SetupDone (Result Http.Error SetupState)
