package noppes.npcs.api.event;

import noppes.npcs.api.handler.IFactionHandler;
import noppes.npcs.api.handler.IRecipeHandler;

public class HandlerEvent {

   public static class FactionsLoadedEvent extends CustomNPCsEvent {
      public final IFactionHandler handler;

      public FactionsLoadedEvent(IFactionHandler handlerIn) { handler = handlerIn; }
   }

   public static class RecipesLoadedEvent extends CustomNPCsEvent {
      public final IRecipeHandler handler;

      public RecipesLoadedEvent(IRecipeHandler handlerIn) { handler = handlerIn; }
   }

}
