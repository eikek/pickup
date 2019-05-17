module Page.Device.View exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick)
import Markdown

import Util.List
import Page exposing (Page(..))
import Page.Device.Data exposing (..)

view: Model -> Html Msg
view model =
    div [class "device-page ui grid container"]
        [div [class "sixteen wide column"]
             [h2 [class "ui header"]
                 [i [class "settings icon"][]
                 ,div [class "content"]
                      [text "Pickup Device Settings"
                      ,div [class "sub header"]
                           [code [][text model.device.id]
                           ]
                      ]
                 ]
             ]
        ,div [class "four wide colum"]
             [ (sideMenu model)
             ]
        , case model.tab of
              RemoteAccess ->
                  remoteSsh model
              Encryption ->
                  encryption model
              PrivateAccessKey ->
                  privateAccess model
              PersonalAccess ->
                  personalAccess model
        ]

sideMenu: Model -> Html Msg
sideMenu model =
    div [class "ui vertical secondary menu"]
        [ a [classList [("item", True)
                       ,("active", activeTab model RemoteAccess)
                       ]
            ,Page.href DevicePage
            ,onClick (SetTab RemoteAccess)
            ]
            [text "Remote Access"
            ]
        ,a [classList [("item", True)
                       ,("active", activeTab model Encryption)
                       ]
            ,Page.href DevicePage
            ,onClick (SetTab Encryption)
            ]
            [text "Encryption"
            ]
        ,a [classList [("item", True)
                       ,("active", activeTab model PrivateAccessKey)
                       ]
            ,Page.href DevicePage
            ,onClick (SetTab PrivateAccessKey)
            ]
            [text "Private Access"
            ]
        ,a [classList [("item", True)
                       ,("active", activeTab model PersonalAccess)
                       ]
            ,Page.href DevicePage
            ,onClick (SetTab PersonalAccess)
            ]
            [text "Personal Endpoint"
            ]
        ]

personalAccess: Model -> Html Msg
personalAccess model =
    let ssh = model.device.sshPersonal 
    in
    div [class "twelve wide column"]
        [h3 [class "ui header"]
            [text "Personal Endpoint"
            ]
        ,case ssh.enabled of
             True ->
                 div []
                     [p [][text "This is a SFTP endpoint where you can upload the data to backup."]
                     ,div [class "ui list"]
                          [div [class "item"]
                               [i [class "orange upload icon"][]
                               ,div [class "content"]
                                    [text "Endpoint URI"
                                    ,div [class "description"]
                                         [div [class "verbatim"]
                                              [text (ssh.user ++ "@" ++ ssh.host ++ ":" ++ (String.fromInt ssh.bindPort))
                                              ]
                                         ]
                                    ]
                               ]
                          ]
                     ]
             False ->
                 div [class "ui info message"]
                     [text "The personal SFTP endpoint is disabled."
                     ]
        ]
privateAccess: Model -> Html Msg
privateAccess model =
    div [class "twelve wide column"]
        [h3 [class "ui header"]
            [text "Private Access"
            ]
        ,p [][text "This key is required when authenticating to other peers that have you in their incoming peers. It is also required to push via the “personal” SFTP if this is enabled."
             ,text "Keep this private and never share it with anyone!"
             ]
        ,p [][text "You can store this in "
             ,code [][text "~/.ssh/pickup_rsa"]
             ,text " when copying data via cron or systemd timers and sftp."
             ]
        ,pre []
             [text model.device.sshPrivateKey
             ]
        ]

encryption: Model -> Html Msg
encryption model =
    div [class "twelve wide column"]
        [h3 [class "ui header"]
            [text "Encryption"
            ]
        ,p [][text """
                    This password is used to encrypt the backup data,
                    before it is send to a peer. So for restoring, it
                    is required to read your backups!
                    """
             ]
        ,div [class "ui container"] <|
             [h5 [class "ui header"]
                 [text "Password"
                 ,div [class "sub header"]
                      [text "The password is used to encrypt (and decrypt) the data."
                      ]
                 ]
             ,pre []
                  [ text (if model.showPass then model.device.password else "*************")
                  ]
             , button [class "ui button"
                      ,onClick ToggleShowPass
                      ]
                  [text (if model.showPass then "Hide" else "Show")
                  ]
             ]
        ]

remoteSsh: Model -> Html Msg
remoteSsh model =
    div [class "twelve wide column"]
        [h3 [class "ui header"]
            [text "Remote Access"
            ]
        ,p [][text "If you want to push your backup to another peer, you must provide this information."
             ]
        ,div [class "ui list"]
             [div [class "item"]
                  [i [class "green user outline icon"][]
                  ,div [class "content"]
                       [text "Id"
                       ,div [class "description"]
                            [div [class "verbatim"][text model.device.id]
                            ]
                       ]
                  ]
             ,div [class "item"]
                  [i [class "green id badge outline icon"][]
                  ,div [class "content"]
                       [text "Public Key"
                       ,div [class "description"]
                            [div [class "verbatim"][text model.device.sshPublicKey]
                            ]
                       ]
                  ]
             ]
        ,p [][text "If you want to allow other to push their backups to this machine, you'll need to add them in your “Incoming Peers” list. And additionally provide your remote URL to them."
             ]
        ,div [class "ui list"]
             [div [class "item"]
                  [i [class "blue download icon"][]
                  ,div [class "content"]
                       [text "Remote URL"
                       ,div [class "description"]
                            [div [class "verbatim"]
                                 [text (model.device.sshRemote.host ++ ":" ++ (String.fromInt model.device.sshRemote.bindPort))
                                 ]
                            ]
                       ]
                  ]
             ]
        ]
