package noppes.npcs.controllers.scripts;

import javax.script.ScriptEngine;
import java.util.Map;

@SuppressWarnings("unused")
public interface IScriptExecutor {

    void initialize(String language, Map<String, Object> globals);

    ScriptEngine getEngine();

    void setScript(String fullScriptCode);

    String run(IScriptHandler handler, String functionName, Object event);

    boolean isErrored();

    boolean isInit();

    boolean isUnknownFunction(String functionName);

    void close();

    void setErrored(boolean isErrored);

    void setInit(boolean isInit);

}
