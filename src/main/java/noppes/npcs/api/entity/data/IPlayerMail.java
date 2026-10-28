package noppes.npcs.api.entity.data;

import noppes.npcs.api.IContainer;
import noppes.npcs.api.interfaces.ParamName;
import noppes.npcs.api.handler.data.IQuest;

import java.util.List;

public interface IPlayerMail {

   String getSender();

   void setSender(@ParamName("sender") String sender);

   String getSubject();

   void setSubject(@ParamName("subject") String subject);

   List<String> getText();

   void setText(@ParamName("pages") String ... pages);

   IQuest getQuest();

   void setQuest(@ParamName("id") int id);

   IContainer getContainer();

   // New from Unofficial (BetaZavr)
   int getMoney();

   int getDonat();

   void setDonat(@ParamName("money") int money);

   int getRansom();

   void setMoney(@ParamName("money") int money);

   void setRansom(@ParamName("money") int money);

}
