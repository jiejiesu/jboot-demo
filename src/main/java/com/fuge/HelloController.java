package com.fuge;

import io.jboot.app.JbootApp;
import io.jboot.web.controller.JbootController;
import io.jboot.web.controller.annotation.RequestMapping;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;

/**
 * TODO.
 * <p>
 * User: sujie
 * Date: 2019-01-21
 * Time: 14:31
 */
@RequestMapping("/h")
public class HelloController extends JbootController
{

    public void index(){
        renderText("hello su jie f dfsfsafs");
    }

    public void start()
    {
        ProcessInstance pi = ActivitiKit.startProcessInstanceByKey("firstProcess");
        renderJson(pi.getId());

    }

    public void getTaskName()
    {
        String processInstanceId = getPara("id");

        Task testTask = ActivitiKit.getTask(processInstanceId);

        renderJson(testTask == null ? null : testTask.getName());
    }


    public void completeTask()
    {
        String processInstanceId = getPara("id");

        Task testTask = ActivitiKit.getTask(processInstanceId);

        testTask = ActivitiKit.completeTask(testTask);

        renderJson(testTask == null ? null : testTask.getName());
    }

    public static void main(String[] args){

        JbootApp.run(args);
//        JbootApplication.run(args);
    }
}
