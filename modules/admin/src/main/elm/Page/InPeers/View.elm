module Page.InPeers.View exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick, onInput, onCheck)

import Page exposing (Page(..))
import Data.InPeer exposing (InPeer)
import Page.InPeers.Data exposing (..)

view: Model -> Html Msg
view model =
    div [class "inpeers-page ui container"]
        [h1 [class "ui dividing header"]
            [text "Incoming Peers"
            ]
        ,p [][text "Active peers can push their data to this machine."
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
             [label [][text "Id"]
             ,input [type_ "text"
                    ,onInput SetPeerId
                    ,value model.peer.id
                    ][]
             ]
        ,div [class "field"]
             [label [][text "Public Key"]
             ,textarea [rows 8
                       ,onInput SetPeerPubkey
                       ][text model.peer.pubkey]
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
                ,Page.href InPeersPage
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
                  [th [class "collapsing"][text "Enabled"]
                  ,th [class "collapsing"][text "Id"]
                  ,th [class "collapsing"][text "Size"]
                  ,th [][text "Description"]
                  ,th [class "collapsing"][]
                  ]
              ]
        ,tbody []
            (List.map (peerRow model) model.peers)
        ]

peerRow: Model -> InPeer -> Html Msg
peerRow model peer =
    tr []
       [td [class "collapsing"]
           [let
               icon = if peer.enabled then "green check icon" else "red ban icon"
            in
                span [][i [class icon][]]
           ]
       ,td [class "collapsing"]
           [a [Page.href InPeersPage
              ,onClick (EditPeer peer)
              ]
              [text peer.id
              ]
           ]
       ,td []
           [text peer.sizeString
           ]
       ,td []
           [text peer.description
           ]
       ,td [class "collapsing"]
           [button [class "ui small button"
                   ,onClick (DeletePeer peer)
                   ]
                [text "Delete"
                ]
           ]
       ]
