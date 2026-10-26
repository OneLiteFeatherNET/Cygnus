package net.onelitefeather.cygnus.setup.listener.dialog;

import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.setup.dialog.AtmosphereDialogs;
import net.onelitefeather.cygnus.setup.dialog.AuthorDialogs;
import net.onelitefeather.cygnus.setup.dialog.CreekRouteDialogs;
import net.onelitefeather.cygnus.setup.dialog.PagePoolDialogs;
import net.onelitefeather.cygnus.setup.dialog.MapDialogs;
import net.onelitefeather.cygnus.setup.event.dialog.DialogContext;
import net.onelitefeather.cygnus.setup.event.dialog.DialogRequestEvent;
import net.onelitefeather.cygnus.setup.event.dialog.DialogTarget;
import net.onelitefeather.cygnus.setup.dialog.settings.SettingsDialog;
import net.onelitefeather.cygnus.setup.dialog.settings.SettingsForms;

import java.util.function.Consumer;

public class DialogRequestListener implements Consumer<DialogRequestEvent> {

    @Override
    public void accept(DialogRequestEvent event) {
        DialogTarget target = event.getTarget();
        Player player = event.getPlayer();
        DialogContext context = event.getContext();

        switch (target) {
            case CREATE_NAME -> MapDialogs.openNameCreateDialog(player);
            case CREEK_ROUTE_NAME -> CreekRouteDialogs.openNameDialog(player);
            case PAGE_POOL_NAME -> PagePoolDialogs.openNameDialog(player);
            case CREEK_ROUTE_PAUSE -> {
                if (context instanceof DialogContext.CreekPauseContext pauseContext) {
                    CreekRouteDialogs.openPauseDialog(player, pauseContext);
                }
            }
            case UPDATE_NAME -> {
                if (context instanceof DialogContext.NameContext(String name)) {
                    MapDialogs.openNameUpdateDialog(player, name);
                }
            }
            case CREATE_AUTHORS -> AuthorDialogs.openAuthorRequestDialog(player);
            case ATMOSPHERE_PRESET -> AtmosphereDialogs.openPresetDialog(
                    player,
                    context instanceof DialogContext.AtmosphereContext
            );
            case ATMOSPHERE_VALUES -> {
                if (context instanceof DialogContext.AtmosphereContext(var atmosphere)) {
                    AtmosphereDialogs.openValueDialog(player, atmosphere);
                }
            }
            case PAGE_SETTINGS -> {
                if (context instanceof DialogContext.SettingsValues(var values)) {
                    SettingsDialog.open(player, SettingsForms.PAGES, values);
                }
            }
            case CREEK_SETTINGS -> {
                if (context instanceof DialogContext.SettingsValues(var values)) {
                    SettingsDialog.open(player, SettingsForms.CREEK, values);
                }
            }
            case AUTHOR_INPUT -> {
                if (context instanceof DialogContext.AuthorAmount authorAmount) {
                    AuthorDialogs.openAuthorInput(player, authorAmount.amount());
                }
            }
            default -> {
                // Nothing to do here
            }
        }
    }
}
