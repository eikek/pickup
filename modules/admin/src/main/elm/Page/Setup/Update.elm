module Page.Setup.Update exposing (update)

import Browser.Navigation as Nav
import Api
import Page exposing (Page(..))
import Page.Setup.Data exposing (..)

update: Msg -> Model -> (Model, Cmd Msg)
update msg model =
    case msg of
        GetSetupState ->
            ( model, Api.setupState model.flags.apiBase SetupStateResp )

        SetupStateResp (Ok state) ->
            if state.exists then (model, gotoPage model)
            else ({model|state = Just state}, Cmd.none)

        SetupStateResp (Err err) ->
            ( model, Cmd.none )

        SetPassword pw ->
            let
                data = model.data
                ndata = {data|password = pw}
            in
                ({model|data = ndata}, Cmd.none)

        RunSetup ->
            ({model|setupRunning = True}, Api.setup model.flags.apiBase model.data SetupDone)

        SetupDone (Ok state) ->
            ({model|setupRunning = False}, gotoPage model)

        SetupDone (Err err) ->
            ({model|setupRunning = False}, Cmd.none)

gotoPage: Model -> Cmd msg
gotoPage model =
    Nav.pushUrl model.key (Page.pageToString DevicePage)
