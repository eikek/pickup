module Page.Backup.Data exposing (..)

import Http
import Data.Flags exposing (Flags)
import Data.OutPeer exposing (OutPeer)
import Data.OutData exposing (OutData)

type alias Model =
    { flags: Flags
    , peer: OutPeer
    , backupOut: OutData
    , restoreOut: OutData
    , stdoutBackup: String
    , stderrBackup: String
    , stdoutRestore: String
    , stderrRestore: String
    , backupOutTab: OutView
    , restoreOutTab: OutView
    , daysBack: Maybe Int
    }

emptyModel: Flags -> Model
emptyModel flags =
    { flags = flags
    , peer = Data.OutPeer.empty
    , backupOut = Data.OutData.empty
    , restoreOut = Data.OutData.empty
    , stdoutBackup = ""
    , stderrBackup = ""
    , stdoutRestore = ""
    , stderrRestore = ""
    , backupOutTab = Init
    , restoreOutTab = Init
    , daysBack = Nothing
    }

type Msg
    = GetPeer String
    | GetPeerResp (Result Http.Error OutPeer)
    | GetBackupOutResp (Result Http.Error OutData)
    | GetRestoreOutResp (Result Http.Error OutData)
    | RunBackup
    | RunBackupResp (Result Http.Error ())
    | GetBackupStdOut
    | GetBackupStdErr
    | BackupStdOutResp (Result Http.Error String)
    | BackupStdErrResp (Result Http.Error String)
    | RunRestore
    | RunRestoreResp (Result Http.Error ())
    | GetRestoreStdOut
    | GetRestoreStdErr
    | RestoreStdOutResp (Result Http.Error String)
    | RestoreStdErrResp (Result Http.Error String)
    | SetDaysBack String

type OutView = Stdout | Stderr | Init
