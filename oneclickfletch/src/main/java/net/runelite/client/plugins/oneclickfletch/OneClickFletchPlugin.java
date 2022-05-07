package net.runelite.client.plugins.oneclickfletch;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.queries.BankItemQuery;
import net.runelite.api.queries.GameObjectQuery;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.pf4j.Extension;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Extension
@PluginDescriptor(
        name = "One Click Fletch",
        description = "Left click fletch",
        tags = {"one", "click", "oneclick", "fletch"}
)

@Slf4j
public class OneClickFletchPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private OneClickFletchConfig config;

    @Provides
    OneClickFletchConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(OneClickFletchConfig.class);
    }
    private final List<Integer> banks = List.of(19051); // barb assault for now

    private final int KNIFE = ItemID.KNIFE;
    private final int MAPLE_LOGS = ItemID.MAPLE_LOGS;
    private final int MAPLE_LONGBOW_U = ItemID.MAPLE_LONGBOW_U;
    private final int MAPLE_LONGBOW_CHAT_OPTION_ID = 17694736;
    private final int CLOSE_BANK_ID = 786434;

    private Boolean inMenu = false;

    @Subscribe
    private void onClientTick(ClientTick event) {
        if (
                this.client.getLocalPlayer() == null ||
                        this.client.getGameState() != GameState.LOGGED_IN
        )
            return;
        else {
            String text = "<col=00ff00>One Click Fletching";
            client.insertMenuItem(text, "", MenuAction.UNKNOWN.getId(), 0, 0, 0, true);
            client.setTempMenuEntry(Arrays.stream(client.getMenuEntries()).filter(x -> x.getOption().equals(text)).findFirst().orElse(null));
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (event.getMenuOption().equals("<col=00ff00>One Click Fletching")) {
            fletchLoop(event);
        }
    }

    private void logger(String event){
        log.warn(event);
    }

    private void fletchLoop(MenuOptionClicked event) {
        logger("got event");
        if (isFletching(event)) {
            logger("we are currently fletching, no op");
            // no op wait for fletching to be done
            event.consume();
        }
        else  {
            logger("not fletching");
            // if in bank either deposit logs, withdraw logs, or close bank
            if (inBank()) {
                logger("in bank");
                if (isLogsInInvetory(event)) {
                    logger("logs detected, close bank");
                    closeBank(event);
                }
                else if (isBowsInInvetory(event)){
                    logger("bows detected, deposit bows");
                    depositBows(event);
                }
                else {
                    logger("no logs or bows detected, withdraw logs");
                    withdrawLogs(event);
                }
            }
            // if not in bank, start fletching or open bank to deposit
            else {
                logger("not in bank");
                if (isLogsInInvetory(event) && !inMenu) {
                    logger("logs in inventory, start fletching");
                    useKnifeOnLog(event);
                }
                else if (isBowsInInvetory(event)) {
                    logger("bows in inventory, open bank");
                    openBank(event);
                }
                else if (!isLogsInInvetory(event)) {
                    logger("no logs in inventory, open bank");
                    openBank(event);
                }
                else {
                    logger("starting to fletch");
                    startFletch(event);
                }
            }
        }
    }

    private void startFletch(MenuOptionClicked event) {
        event.setMenuEntry(createMenuEntry(
                1,
                MenuAction.CC_OP,
                -1,
                MAPLE_LONGBOW_CHAT_OPTION_ID,
                false));
        inMenu = false;
    }

    private Boolean inBank() {
        return client.getItemContainer(InventoryID.BANK) != null;
    }

    private Boolean isLogsInInvetory(MenuOptionClicked event) {
        Widget inventory = client.getWidget(WidgetInfo.INVENTORY.getId());
        Widget bankInventory = client.getWidget(WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER.getId());

        if (inventory!=null && !inventory.isHidden() && inventory.getDynamicChildren()!=null) {
            List<Widget> inventoryItems = Arrays.asList(client.getWidget(WidgetInfo.INVENTORY.getId()).getDynamicChildren());
            return (int) inventoryItems.stream().filter(item -> item.getItemId() == MAPLE_LOGS).count() == 27;
        }
        else if (bankInventory!=null && !bankInventory.isHidden() && bankInventory.getDynamicChildren()!=null) {
            List<Widget> inventoryItems = Arrays.asList(Objects.requireNonNull(client.getWidget(WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER.getId())).getDynamicChildren());
            return (int) inventoryItems.stream().filter(item -> item.getItemId() == MAPLE_LOGS).count() == 27;
        }
        else {
            return false;
        }
    }

    private Boolean isBowsInInvetory(MenuOptionClicked event) {
        Widget inventory = client.getWidget(WidgetInfo.INVENTORY.getId());
        Widget bankInventory = client.getWidget(WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER.getId());

        if (inventory!=null && !inventory.isHidden() && inventory.getDynamicChildren()!=null) {
            List<Widget> inventoryItems = Arrays.asList(client.getWidget(WidgetInfo.INVENTORY.getId()).getDynamicChildren());
            return (int) inventoryItems.stream().filter(item -> item.getItemId() == MAPLE_LONGBOW_U).count() == 27;
        }
        else if (bankInventory!=null && !bankInventory.isHidden() && bankInventory.getDynamicChildren()!=null) {
            List<Widget> inventoryItems = Arrays.asList(Objects.requireNonNull(client.getWidget(WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER.getId())).getDynamicChildren());
            return (int) inventoryItems.stream().filter(item -> item.getItemId() == MAPLE_LONGBOW_U).count() == 27;
        }
        else {
            return false;
        }
    }

    private Boolean isFletching(MenuOptionClicked event) {
        return client.getLocalPlayer().getAnimation() == AnimationID.FLETCHING_BOW_CUTTING;
    }

    private void useKnifeOnLog(MenuOptionClicked event) {
        // set menu entry to use knife on log
        Widget knife = getInventoryItem(KNIFE);
        Widget logs = getInventoryItem(ItemID.MAPLE_LOGS);

        if (knife == null || logs == null) return;

        client.setSelectedSpellWidget(WidgetInfo.INVENTORY.getId());
        client.setSelectedSpellChildIndex(knife.getIndex());
        client.setSelectedSpellItemId(knife.getItemId());

        event.setMenuEntry(createMenuEntry(0, MenuAction.WIDGET_TARGET_ON_WIDGET, logs.getIndex(), 9764864, true));
        inMenu = true;
    }

    private void openBank(MenuOptionClicked event) {
        GameObject BANK = new GameObjectQuery().idEquals(19051).result(client).nearestTo(client.getLocalPlayer());
        event.setMenuEntry(createMenuEntry(BANK.getId(), MenuAction.GAME_OBJECT_FIRST_OPTION, getLocation(BANK).getX(), getLocation(BANK).getY(), false));
    }

    public void closeBank(MenuOptionClicked event) {
        event.setMenuEntry(createMenuEntry(1, MenuAction.CC_OP, 11, CLOSE_BANK_ID, false)); //close bank
    }
    private void depositBows(MenuOptionClicked event) {
        event.setMenuEntry(createMenuEntry(
                8,
                MenuAction.CC_OP_LOW_PRIORITY,
                getInventoryItem(MAPLE_LONGBOW_U).getIndex(),
                983043,
                false));
    }

    private void withdrawLogs(MenuOptionClicked event) {
        event.setMenuEntry(createMenuEntry(
                1,
                MenuAction.CC_OP,
                getBankIndex(ItemID.MAPLE_LOGS),
                786445,
                true));
    }

    private Point getLocation(TileObject tileObject) {
        if (tileObject == null) {
            return new Point(0, 0);
        }
        if (tileObject instanceof GameObject) {
            return ((GameObject) tileObject).getSceneMinLocation();
        }
        return new Point(tileObject.getLocalLocation().getSceneX(), tileObject.getLocalLocation().getSceneY());
    }

    Widget getInventoryItem(int id) {
        Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
        Widget bankInventoryWidget = client.getWidget(WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER);
        if (inventoryWidget!=null && !inventoryWidget.isHidden())
        {
            return getWidgetItem(inventoryWidget,id);
        }
        else if (bankInventoryWidget!=null && !bankInventoryWidget.isHidden())
        {
            return getWidgetItem(bankInventoryWidget,id);
        }
        else {
            return null;
        }
    }

    Widget getWidgetItem(Widget widget, int id) {
        for (Widget item : widget.getDynamicChildren())
        {
            if (item.getItemId() == id)
            {
                return item;
            }
        }
        return null;
    }

    public MenuEntry createMenuEntry(int identifier, MenuAction type, int param0, int param1, boolean forceLeftClick) {
        return client.createMenuEntry(0).setOption("").setTarget("").setIdentifier(identifier).setType(type)
                .setParam0(param0).setParam1(param1).setForceLeftClick(forceLeftClick);
    }

    private int getBankIndex(int id){
        WidgetItem bankItem = new BankItemQuery()
                .idEquals(id)
                .result(client)
                .first();
        if (bankItem == null) return -1;
        return bankItem.getWidget().getIndex();
    }
}