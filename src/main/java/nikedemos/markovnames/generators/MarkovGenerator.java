package nikedemos.markovnames.generators;

import nikedemos.markovnames.MarkovDictionary;
import noppes.npcs.shared.common.util.LogWriter;

public class MarkovGenerator {

   protected static final MarkovGenerator[] GENERATOR = new MarkovGenerator[10];
   public MarkovDictionary markov;
   public String name;

   public MarkovGenerator() { this(3); }

   public MarkovGenerator(int ignoredSeqlen) { }

   public String fetch(int gender) {
      return this.stylize(this.markov.generateWord());
   }

   public String fetch() { return this.fetch(0); }

   public static String fetch(int dictionary, int gender) {
      try { return GENERATOR[dictionary].fetch(gender); } catch (Exception e) { LogWriter.error(e); }
      return "Noppes";
   }

   public String stylize(String str) { return str; }

   public String feminize(String element, boolean flag) { return element; }

   public static void load() {
      GENERATOR[0] = new MarkovRoman(3);
      GENERATOR[1] = new MarkovJapanese(4);
      GENERATOR[2] = new MarkovSlavic(3);
      GENERATOR[3] = new MarkovWelsh(3);
      GENERATOR[4] = new MarkovSaami(3);
      GENERATOR[5] = new MarkovOldNorse(4);
      GENERATOR[6] = new MarkovAncientGreek(3);
      GENERATOR[7] = new MarkovAztec(3);
      GENERATOR[8] = new MarkovCustomNPCsClassic(3);
      GENERATOR[9] = new MarkovSpanish(3);
   }

}
