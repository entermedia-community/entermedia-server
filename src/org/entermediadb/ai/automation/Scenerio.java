package org.entermediadb.ai.automation;

import java.util.Collection;

import org.entermediadb.ai.Agent;
import org.openedit.data.BaseData;

public class Scenerio extends BaseData
{

    Collection<ScenerioRunning> fieldRunningSenerios;

    public Collection<ScenerioRunning> getRunningSenerios() {
        return fieldRunningSenerios;
    }

    Collection<Agent> fieldSkills;
    public Collection<Agent> getAgents() {
        return fieldSkills;    
    }



}
