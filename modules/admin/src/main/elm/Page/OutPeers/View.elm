module Page.OutPeers.View exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick, onInput, onCheck)

import Util.Maybe
import Page exposing (Page(..))
import Data.OutPeer exposing (OutPeer)
import Page.OutPeers.Data exposing (..)

view: Model -> Html Msg
view model =
    div [class "inpeers-page ui container"]
        [h1 [class "ui dividing header"]
            [text "Outgoing Peers"
            ]
        ,p [][text "You can push to active peers your data."
             ]
        ,case model.mode of
             List ->
                 (peersTable model)
             Add ->
                 (addPeerForm model)
        ]

addPeerForm: Model -> Html Msg
addPeerForm model =
    div [class "ui form"]
        [div [class "field"]
             [label [][text "Remote URI"]
             ,input [type_ "text"
                    ,onInput SetPeerUri
                    ,value model.peer.remoteUri
                    ][]
             ]
        ,div [class "field"]
             [label [][text "Schedule"]
             ,input [type_ "text"
                    ,onInput SetPeerSchedule
                    ,Maybe.map .schedule model.peer.schedule |> Maybe.withDefault "" |> value
                    ][]
             ]
        ,div [class "field"]
             [label [][text "Description"]
             ,textarea [rows 4
                       ,onInput SetPeerDesc
                       ]
                 [text model.peer.description]
             ]
        ,div [class "inline field"]
             [div [class "ui checkbox"]
                  [input [type_ "checkbox"
                         ,checked model.peer.enabled
                         ,onCheck SetPeerEnable
                         ][]
                  ,label [][text "Enabled"]
                  ]
             ]
        ,button [classList [("ui primary button", True)
                           ,("disabled loading", model.updating)
                           ]
                ,onClick SavePeer
                ,Page.href OutPeersPage
                ]
            [text "Save"
            ]
        ,button [classList [("ui secondary button", True)
                           ,("disabled loading", model.updating)
                           ]
                ,onClick (SetMode List)
                ]
            [text "Cancel"
            ]
        ]

peersTable: Model -> Html Msg
peersTable model =
    table [class "ui table"]
        [thead []
              [tr []
                  [th [colspan 5]
                      [button [class "ui button"
                              ,onClick (SetMode Add)
                              ]
                           [text "Add"
                           ]
                      ]
                  ]
              ,tr []
                  [th [class "collapsing"][]
                  ,th [class "collapsing"][text "Remote Uri"]
                  ,th [class "collapsing"][text "Schedule"]
                  ,th [][text "Description"]
                  ,th [][text "Actions"]
                  ]
              ]
        ,tbody []
            (List.map (peerRow model) model.peers)
        ]

peerRow: Model -> OutPeer -> Html Msg
peerRow model peer =
    tr []
       [td [class "collapsing"]
           [let
               icon = if peer.enabled then "green check icon" else "red ban icon"
            in
                span [][i [class icon][]]
           ,case peer.runTime of
                Just ms ->
                    span [][i [class "orange loading cog icon"][]]
                Nothing ->
                    span [class "invisible"][]
           ]
       ,td [class "collapsing"]
           [a [Page.href (BackupPage peer.id)
              ]
              [text peer.remoteUri
              ]
           ]
       ,td [class "collapsing"]
           [div [class "ui blue basic label"]
                [Maybe.map .schedule peer.schedule |> Maybe.withDefault "-" |> text
                ]
           ]
       ,td []
           [text peer.description
           ]
       ,td [class "collapsing"]
           [button [class "ui small primary button"
                   ,onClick (EditPeer peer)
                   ]
                [text "Edit"
                ]
           ,button [class "ui small danger button"
                   ,onClick (DeletePeer peer)
                   ]
               [text "Delete"
               ]
           ]
       ]
