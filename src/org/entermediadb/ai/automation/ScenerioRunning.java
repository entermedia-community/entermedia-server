package org.entermediadb.ai.automation;

import java.util.Collection;
import java.util.List;

public class ScenerioRunning
{
    protected List<Agent> fieldAgents;
    public List<Agent> getAgents() 
    {
        return fieldAgents;
    }

    public void setAgents(List<Agent> inAgents  ) {
        fieldAgents = inAgents;
    }
   
    protected int fieldCurrentStep;
    public int getCurrentStep() {
        return fieldCurrentStep;        
    }
    public void setCurrentStep(int inCurrentStep) {
        fieldCurrentStep = inCurrentStep;
    }

}
