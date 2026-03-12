package org.entermediadb.ai.automation;

import org.apache.velocity.tools.config.Data;
import org.entermediadb.ai.BaseAiManager;

public class BaseAgent  extends BaseAiManager {

    //Calls functions as he wants
    protected Data fieldAgentData;
    public Data getAgentData() {
        return fieldAgentData;
    }   
    public void setAgentData(Data inAgentData) {
        fieldAgentData = inAgentData;
    }

}   