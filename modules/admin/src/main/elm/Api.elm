module Api exposing (..)

import Process
import Task
import Http
import Json.Decode as Decode

import Data.SetupState exposing (..)
import Data.SetupData exposing (..)
import Data.Device exposing (..)
import Data.InPeer exposing (..)
import Data.OutPeer exposing (..)
import Data.OutData exposing (..)

setupState: String -> ((Result Http.Error SetupState) -> msg) -> Cmd msg
setupState baseurl receive =
    Http.get
        { url = baseurl ++ "/setup"
        , expect = Http.expectJson receive setupDataDecoder
        }

setup: String -> SetupData -> ((Result Http.Error SetupState) -> msg) -> Cmd msg
setup baseurl data receive =
    Http.post
        { url = baseurl ++ "/setup"
        , body = Http.jsonBody (Data.SetupData.setupDataEncode data)
        , expect = Http.expectJson receive setupDataDecoder
        }

deviceData: String -> ((Result Http.Error Device) -> msg) -> Cmd msg
deviceData baseurl receive =
    Http.get
        { url = baseurl ++ "/device"
        , expect = Http.expectJson receive deviceDecoder
        }

getInPeers: String -> ((Result Http.Error (List InPeer)) -> msg) -> Cmd msg
getInPeers baseurl receive =
    Http.get
        { url = baseurl ++ "/peers/in"
        , expect = Http.expectJson receive inPeerListDecoder
        }

getOutPeers: String -> ((Result Http.Error (List OutPeer)) -> msg) -> Cmd msg
getOutPeers baseurl receive =
    Http.get
        { url = baseurl ++ "/peers/out"
        , expect = Http.expectJson receive outPeerListDecoder
        }

getOutPeersLater: Float -> String -> ((Result Http.Error (List OutPeer)) -> msg) -> Cmd msg
getOutPeersLater delay baseurl receive =
    getLater receive
        { url = baseurl ++ "/peers/out"
        , decoder = outPeerListDecoder
        , delay = delay
        }

getOutPeer: String -> String -> ((Result Http.Error OutPeer) -> msg) -> Cmd msg
getOutPeer baseurl id receive =
    Http.get
        { url = baseurl ++ "/peers/out/" ++ id
        , expect = Http.expectJson receive outPeerDecoder
        }

getOutPeerBackupRun: String -> String -> ((Result Http.Error OutData) -> msg) -> Cmd msg
getOutPeerBackupRun baseurl id receive =
    Http.get
        { url = baseurl ++ "/peers/out/" ++ id ++ "/backuprun"
        , expect = Http.expectJson receive outDataDecoder
        }

getOutPeerRestoreRun: String -> String -> ((Result Http.Error OutData) -> msg) -> Cmd msg
getOutPeerRestoreRun baseurl id receive =
    Http.get
        { url = baseurl ++ "/peers/out/" ++ id ++ "/restorerun"
        , expect = Http.expectJson receive outDataDecoder
        }

getBackupStdout: String -> String -> ((Result Http.Error String) -> msg) -> Cmd msg
getBackupStdout baseurl id receive =
    Http.get
        { url = (baseurl ++ "/peers/out/" ++ id ++ "/backuprun/stdout")
        , expect = Http.expectString receive
        }

getBackupStderr: String -> String -> ((Result Http.Error String) -> msg) -> Cmd msg
getBackupStderr baseurl id receive =
    Http.get
        { url = (baseurl ++ "/peers/out/" ++ id ++ "/backuprun/stderr")
        , expect = Http.expectString receive
        }

getRestoreStdout: String -> String -> ((Result Http.Error String) -> msg) -> Cmd msg
getRestoreStdout baseurl id receive =
    Http.get
        { url = (baseurl ++ "/peers/out/" ++ id ++ "/restorerun/stdout")
        , expect = Http.expectString receive
        }

getRestoreStderr: String -> String -> ((Result Http.Error String) -> msg) -> Cmd msg
getRestoreStderr baseurl id receive =
    Http.get
        { url = (baseurl ++ "/peers/out/" ++ id ++ "/restorerun/stderr")
        , expect = Http.expectString receive
        }

getOutPeerLater: Float -> String -> String -> ((Result Http.Error OutPeer) -> msg) -> Cmd msg
getOutPeerLater delay baseurl id receive =
    getLater receive
        { url = baseurl ++ "/peers/out/" ++ id
        , decoder = outPeerDecoder
        , delay = delay
        }

updateInPeer: String -> InPeer -> ((Result Http.Error ()) -> msg) -> Cmd msg
updateInPeer baseurl peer receive =
    Http.post
        { url = baseurl ++ "/peers/in"
        , body = Http.jsonBody (Data.InPeer.encode peer)
        , expect = Http.expectWhatever receive
        }

updateOutPeer: String -> OutPeer -> ((Result Http.Error ()) -> msg) -> Cmd msg
updateOutPeer baseurl peer receive =
    Http.post
        { url = baseurl ++ "/peers/out"
        , body = Http.jsonBody (Data.OutPeer.encode peer)
        , expect = Http.expectWhatever receive
        }

runBackup: String -> String -> ((Result Http.Error ()) -> msg) -> Cmd msg
runBackup baseurl id receive =
    Http.post
        { url = baseurl ++ "/peers/out/" ++ id ++ "/runBackup"
        , body = Http.emptyBody
        , expect = Http.expectWhatever receive
        }

runRestore: String -> String -> Maybe Int -> ((Result Http.Error ()) -> msg) -> Cmd msg
runRestore baseurl id daysBack receive =
    Http.post
        { url =
              let
                  base = baseurl ++ "/peers/out/" ++ id ++ "/runRestore"
              in
                  case daysBack of
                      Just n -> base ++ "?daysBack=" ++ (String.fromInt n)
                      Nothing -> base
        , body = Http.emptyBody
        , expect = Http.expectWhatever receive
        }

deleteOutPeer: String -> String -> ((Result Http.Error ()) -> msg) -> Cmd msg
deleteOutPeer baseurl id receive =
    delete
        { url = baseurl ++ "/peers/out/" ++ id
        , expect = Http.expectWhatever receive
        }

deleteInPeer: String -> String -> ((Result Http.Error ()) -> msg) -> Cmd msg
deleteInPeer baseurl id receive =
    delete
        { url = baseurl ++ "/peers/in/" ++ id
        , expect = Http.expectWhatever receive
        }


delete: { url: String
        , expect: Http.Expect msg
        } -> Cmd msg
delete r =
    Http.request
        { url = r.url
        , body = Http.emptyBody
        , expect = r.expect
        , method = "DELETE"
        , headers = []
        , timeout = Nothing
        , tracker = Nothing
        }


getLater: ((Result Http.Error a) -> msg) ->
          { url: String
          , decoder: Decode.Decoder a
          , delay: Float
          } -> Cmd msg
getLater receive r =
    let
        decoder: Http.Response String -> Result Http.Error a
        decoder resp =
            case resp of
                Http.BadUrl_ u -> (Err (Http.BadUrl u))
                Http.Timeout_ -> (Err Http.Timeout)
                Http.NetworkError_ -> (Err Http.NetworkError)
                Http.BadStatus_ meta body -> (Err (Http.BadStatus meta.statusCode))
                Http.GoodStatus_ meta body ->
                    case (Decode.decodeString r.decoder body) of
                        Err err -> Err (Http.BadBody (Decode.errorToString err))
                        Ok a -> Ok a
    in
    Process.sleep r.delay
        |> Task.andThen (\_ -> Http.task
                             { method = "GET"
                             , headers = []
                             , url = r.url
                             , body = Http.emptyBody
                             , resolver = Http.stringResolver decoder
                             , timeout = Nothing
                             }
                        )
        |> Task.attempt receive
