module Page.Backup.View exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick, onInput)
import Markdown

import Page exposing (Page(..))
import Util.Maybe
import Util.Duration
import Page.Backup.Data exposing (..)
import Data.OutPeer exposing (OutPeer)
import Data.OutData exposing (OutData)

view: Model -> Html Msg
view model =
    div []
        [h1 [class "ui header"]
            [(runningIcon model.peer)
            ,div [class "content"]
                 [text "Backup to: "
                 ,code [][text model.peer.remoteUri
                         ]
                 ]
            ]
        , div []
              [button [classList [("ui primary button", True)
                                 ,("disabled", Util.Maybe.nonEmpty model.peer.runTime)
                                 ]
                      ,onClick RunBackup
                      ]
                   [text "Backup Now"
                   ]
              ]
        ,(runningTime model.peer)
        ,h3 [class "ui header"]
            [text "Properties"]
        ,(infoTable model.peer)
        ,h3 [class "ui header"]
            [text "Last Run"
            ]
        ,(lastRun "backup" model.backupOut)
        ,(lastRunOut model)
        ,h1 [class "ui header"]
            [i [class "iu download icon"][]
            ,text "Restore"
            ]
        ,(restoreView model)
        ]

lastRunOut: Model -> Html Msg
lastRunOut model =
    div [class "container"]
        [div [class "ui top attached tabular menu"]
             [a [classList [("item", True)
                           ,("active", model.backupOutTab == Stdout)
                           ]
                ,onClick GetBackupStdOut
                ,Page.href (BackupPage model.peer.id)
                ]
                  [text "Stdout"
                  ]
             ,a [classList [("item", True)
                           ,("active", model.backupOutTab == Stderr)
                           ]
                ,onClick GetBackupStdErr
                ,Page.href (BackupPage model.peer.id)
                ]
                [text "Stderr"
                ]
             ]
        ,div [classList [("ui bottom attached tab segment", True)
                        ,("active", model.backupOutTab == Stdout)
                        ]
             ]
            [pre [class "script-output"]
                 [code []
                      [ text model.stdoutBackup
                      ]
                 ]
            ]
        ,div [classList [("ui bottom attached tab segment", True)
                        ,("active", model.backupOutTab == Stderr)
                        ]
             ]
            [pre [class "script-output"]
                 [code []
                      [ text model.stderrBackup
                      ]
                 ]
            ]
        ]

lastRun: String -> OutData -> Html Msg
lastRun what out =
    if out == Data.OutData.empty then
        div [class "ui info message"]
            [text ("The " ++ what ++ " has not been run.")
            ]
    else table [class "ui very basic segment table"]
        [ tbody []
            [ tr []
                 [td [class "collapsing"]
                     [text "Started"
                     ]
                 ,td []
                     [text out.runAt
                     ]
                 ]
            , tr []
                [td [class "collapsing"]
                    [text "Running Time"
                    ]
                ,td []
                    [Util.Duration.toHuman out.runTime |> text
                    ]
                ]
            , tr []
                [td [class "collpasing"]
                    [text "Success"
                    ]
                ,td []
                    [if out.success then
                         i [class "ui green thumbs up outline icon"][]
                     else
                         div [class "ui basic label"]
                             [i [class "ui red bolt icon"][]
                             ,String.fromInt out.returnCode |> text
                             ]
                    ]
                ]
            ,tr []
                [td [class "collapsing"]
                    [text "Overall runs"
                    ]
                ,td []
                    [String.fromInt out.runCount |> text
                    ]
                ]
            ,tr []
                [td [class "collapsing"]
                    [text "Successes"
                    ]
                ,td []
                    [String.fromInt out.runSuccess |> text
                    ]
                ]
            ]
        ]

runningTime: OutPeer -> Html Msg
runningTime peer =
    case peer.runTime of
        Just ms ->
            div [class "ui info message"]
                [text "This process is running for "
                ,Util.Duration.toHuman ms |> text
                ]
        Nothing ->
            div [class "invisible"][]

runningIcon: OutPeer -> Html Msg
runningIcon peer =
    case peer.runTime of
        Just _ ->
            i [class "ui orange loading cog icon"][]
        Nothing ->
            i [class "ui upload icon"][]

infoTable: OutPeer -> Html Msg
infoTable peer =
    table [class "ui very basic segment table"]
        [ tbody []
            [ tr []
                 [td [class "collapsing"]
                     [text "Enabled"
                     ]
                 ,td []
                     [if peer.enabled then
                          i[class "green check icon"][]
                      else
                          i[class "red ban icon"][]
                     ]
                 ]
            , tr []
                 [td [class "collapsing"]
                     [text "Schedule"
                     ]
                 ,td []
                     [case peer.schedule of
                          Just s ->
                              div []
                                  [div [class "ui blue basic label"]
                                       [text s.schedule
                                       ]
                                  ,text " next on "
                                  ,code [][text s.nextRun]
                                  ]
                          Nothing ->
                              div [][text "Not scheduled"]
                     ]
                 ]
            , tr []
                 [td [class "collapsing"]
                     [text "Description"
                     ]
                 ,td []
                     [Markdown.toHtml [] peer.description
                     ]
                 ]
            ]
        ]

restoreView: Model -> Html Msg
restoreView model =
    div [class "container"]
        [div [class "ui segment"]
             [div [class "ui fluid form"]
                  [div [class "field"]
                       [label [][text "Days back:"]
                       ,input [type_ "text"
                              ,onInput SetDaysBack
                              ][]
                       ]
                  ,button [classList [("ui primary button", True)
                                     ,("disabled", Util.Maybe.nonEmpty model.peer.runTime)
                                     ]
                          ,onClick RunRestore
                          ,Maybe.map String.fromInt model.daysBack
                              |> Maybe.withDefault ""
                              |> value
                          ]
                      [text "Restore Now"
                      ]
                  ]
             ]
        ,h3 [class "ui header"]
            [text "Last Run"
            ]
        ,(lastRun "restore" model.restoreOut)
        ,div [class "ui top attached tabular menu"]
             [a [classList [("item", True)
                           ,("active", model.restoreOutTab == Stdout)
                           ]
                ,onClick GetRestoreStdOut
                ,Page.href (BackupPage model.peer.id)
                ]
                  [text "Stdout"
                  ]
             ,a [classList [("item", True)
                           ,("active", model.restoreOutTab == Stderr)
                           ]
                ,onClick GetRestoreStdErr
                ,Page.href (BackupPage model.peer.id)
                ]
                [text "Stderr"
                ]
             ]
        ,div [classList [("ui bottom attached tab segment", True)
                        ,("active", model.restoreOutTab == Stdout)
                        ]
             ]
            [pre [class "script-output"]
                 [code []
                      [ text model.stdoutRestore
                      ]
                 ]
            ]
        ,div [classList [("ui bottom attached tab segment", True)
                        ,("active", model.restoreOutTab == Stderr)
                        ]
             ]
            [pre [class "script-output"]
                 [code []
                      [ text model.stderrRestore
                      ]
                 ]
            ]
        ]
