package com.asilvorcarp;

import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.event.TickHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;

public class InitHandler implements IInitializationHandler {
    @Override
    public void registerModHandlers() {
        RenderHandler renderer = RenderHandler.getInstance();
        RenderEventHandler.getInstance().registerGameOverlayRenderer((IRenderer) renderer);
        RenderEventHandler.getInstance().registerTooltipLastRenderer((IRenderer) renderer);
        RenderEventHandler.getInstance().registerWorldLastRenderer((IRenderer) renderer);

        TickHandler.getInstance().registerClientTickHandler(new ClientTickHandler());

//        KeyCallbacks.init();
    }
}