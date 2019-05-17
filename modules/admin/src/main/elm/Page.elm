module Page exposing ( Page(..)
                     , href
                     , pageToString
                     , fromUrl
                     )

import Url exposing (Url)
import Url.Parser as Parser exposing ((</>), Parser, oneOf, s, string)
import Html exposing (Attribute)
import Html.Attributes as Attr

type Page
    = DevicePage
    | SetupPage
    | InPeersPage
    | OutPeersPage
    | BackupPage String


pageToString: Page -> String
pageToString page =
    case page of
        SetupPage -> "#/setup"
        DevicePage -> "#/device"
        InPeersPage -> "#/inpeers"
        OutPeersPage -> "#/outpeers"
        BackupPage id -> "#/outpeers/" ++ id

href: Page -> Attribute msg
href page =
    Attr.href (pageToString page)

parser: Parser (Page -> a) a
parser =
    oneOf
    [ Parser.map SetupPage Parser.top
    , Parser.map SetupPage (s "setup")
    , Parser.map DevicePage (s "device")
    , Parser.map InPeersPage (s "inpeers")
    , Parser.map BackupPage (s "outpeers" </> string)
    , Parser.map OutPeersPage (s "outpeers")
    ]

fromUrl : Url -> Maybe Page
fromUrl url =
    { url | path = Maybe.withDefault "" url.fragment, fragment = Nothing }
        |> Parser.parse parser
