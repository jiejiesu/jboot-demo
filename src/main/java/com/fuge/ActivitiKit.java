package com.fuge;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;

/**
 * TODO.
 * <p>
 * User: sujie
 * Date: 2019/1/14
 * Time: 3:08 PM
 */
public class ActivitiKit
{
    private static ProcessEngine processEngine = null;

    public static ProcessEngine getPrecessEngine()
    {
        return processEngine;
    }

    public static void setProcessEngine(ProcessEngine pe)
    {
        processEngine = pe;
    }

    public static void deployProcess(String classpathResource)
    {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        repositoryService.createDeployment().addClasspathResource(classpathResource).deploy();
    }

    public static ProcessInstance startProcessInstanceByKey(String processKey)
    {
        RuntimeService runtimeService = processEngine.getRuntimeService();

        return runtimeService.startProcessInstanceByKey(processKey);
    }

    public static Task getTask(String instanceId)
    {
        TaskService taskService = processEngine.getTaskService();

        return taskService.createTaskQuery().processInstanceId(instanceId).singleResult();
    }

    public static Task completeTask(Task task)
    {
        if (task == null)
        {
            return null;
        }
        TaskService taskService = processEngine.getTaskService();
        System.out.println("task.getName() = " + task.getName());
        taskService.complete(task.getId());

        return task;
    }
}
