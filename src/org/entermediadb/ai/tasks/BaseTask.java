package org.entermediadb.ai.tasks;

import java.util.Collection;

public class BaseTask implements SmartTask
{
    //This will be the base class for all tasks. It will have a reference to the manager and the media archive. It will also have a method to execute the task. The manager will be responsible for scheduling and executing the tasks.
    protected SmartProcessManager fieldSmartTaskManager;
    public SmartProcessManager getSmartTaskManager() 
    {
        return fieldSmartTaskManager;
    }

    public void setSmartTaskManager(SmartProcessManager inSmartTaskManager) 
    {
        fieldSmartTaskManager = inSmartTaskManager;
    }

    TaskStep fieldCurrentStep;

    public TaskStep getCurrentStep() {
        return fieldCurrentStep;
    }

    public void setCurrentStep(TaskStep inCurrentStep) {
        fieldCurrentStep = inCurrentStep;
    }

    Collection<TaskStep> fieldPreviousSteps;

    public Collection<TaskStep> getPreviousSteps() {
        return fieldPreviousSteps;
    }

    public void setPreviousSteps(Collection<TaskStep> inPreviousSteps) {
        fieldPreviousSteps = inPreviousSteps;
    }

    Collection<TaskStep> fieldFutureSteps;

    public Collection<TaskStep> getFutureSteps() {
        return fieldFutureSteps;
    }

    public void setFutureSteps(Collection<TaskStep> inFutureSteps) {
        fieldFutureSteps = inFutureSteps;
    }

}
