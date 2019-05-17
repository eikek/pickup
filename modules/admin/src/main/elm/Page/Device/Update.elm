module Page.Device.Update exposing (update)

import Api
import Page.Device.Data exposing (..)

update: Msg -> Model -> (Model, Cmd Msg)
update msg model =
    case msg of
        GetDevice ->
            (model, Api.deviceData model.flags.apiBase DeviceResp)
            
        DeviceResp (Ok dev) ->
            ( {model|device = dev}, Cmd.none )

        DeviceResp (Err err) ->
            let x = Debug.log "Err:" err
            in
            (model, Cmd.none)

        SetTab tab ->
            ({model | tab = tab }, Cmd.none)

        ToggleShowPass ->
            ({model | showPass = (not model.showPass) }, Cmd.none)

