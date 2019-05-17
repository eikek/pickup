module App.View exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)

import App.Data exposing (..)
import Page exposing (Page(..))
import Page.Setup.View
import Page.Device.View
import Page.InPeers.View
import Page.OutPeers.View
import Page.Backup.View

view: Model -> Html Msg
view model =
    div [class "default-layout"]
        [ div [class "ui fixed top sticky attached large menu black-bg"]
              [div [class "ui fluid container"]
                   ([ a [class "header item narrow-item"]
                         [i [classList [("cog icon", True)
                                       ]]
                              []
                         ,text model.flags.appName]
                   ] ++ (pageItems model [InPeersPage, OutPeersPage]))
              ]
        , div [ class "ui container main-content" ]
            [ (case model.page of
                   DevicePage ->
                       viewDevice model
                   SetupPage ->
                       viewSetup model
                   InPeersPage ->
                       viewInPeers model
                   OutPeersPage ->
                       viewOutPeers model
                   BackupPage id ->
                       viewBackup model
              )
            ]
        ]

pageItems: Model -> (List Page) -> List (Html Msg)
pageItems model pages =
    List.map
        (\page ->
             a [classList [("active", model.page == page)
                            ,("item", True)
                            ]
               ,Page.href page
               ]
               [pageToString page |> text
               ]
        )
        pages

pageToString: Page -> String
pageToString page =
    case page of
        DevicePage -> "Device"
        SetupPage -> "Setup"
        InPeersPage -> "Incoming"
        OutPeersPage -> "Outgoing"
        BackupPage id -> "Backup"

viewOutPeers: Model -> Html Msg
viewOutPeers model =
    Html.map OutPeersMsg (Page.OutPeers.View.view model.outPeersModel)

viewInPeers: Model -> Html Msg
viewInPeers model =
    Html.map InPeersMsg (Page.InPeers.View.view model.inPeersModel)

viewSetup: Model -> Html Msg
viewSetup model =
    Html.map SetupMsg (Page.Setup.View.view model.setupModel)

viewDevice: Model -> Html Msg
viewDevice model =
    Html.map DeviceMsg (Page.Device.View.view model.deviceModel)

viewBackup: Model -> Html Msg
viewBackup model =
    Html.map BackupMsg (Page.Backup.View.view model.backupModel)
