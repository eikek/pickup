module Page.Setup.View exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick, onInput)
import Markdown

import Data.SetupState exposing (SetupState)
import Page exposing (Page(..))
import Page.Setup.Data exposing (..)

view: Model -> Html Msg
view model =
    div [class "setup-page"]
        [ case model.state of
              Just state ->
                  setupForm state model
              Nothing ->
                  loading
        ]


setupForm: SetupState -> Model -> Html Msg
setupForm state model =
    div [class "ui text container"]
        [h1 [class "ui header"]
            [text "Setup"
            ]
        ,div [classList [("ui error message", True)
                        ,("invisible", not state.exists)]]
            [text "The setup is already completed."
            ]
        ,setupMessage
        ,div [class "ui segment"]
             [div [class "ui fluid form"]
                  [div [class "field"]
                       [label [][text "Password*"]
                       ,input [type_ "password"
                              ,onInput SetPassword
                              ]
                            []
                       ]
                  ,div []
                      [button [classList [("ui primary button", True)
                                         ,("disabled loading", model.setupRunning)
                                         ]
                              ,onClick RunSetup
                              ]
                           [text "Proceed"
                           ]
                      ]
                  ]
             ]
        ]

setupMessage: Html Msg
setupMessage =
    Markdown.toHtml [] """
Pickup needs some setup first.

Pickup will encrypt your backup data before sending it to a peer. For
this it requires a password. It should be a new and good password, it
is recommended to use a password generator, for example
[dicepass](https://dicepass.org/). Keep the password somewhere else,
because it is required to read the backup data.

**Note: the password is stored in plain text in the pickup
  database. When doing incremental backups, the passphrase is
  required.**
"""

loading: Html Msg
loading =
    div [class "ui basic loading segment"]
        [
        ]
