package com.fuge.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;


public class InitProcessEngine extends HttpServlet {
    private static final long serialVersionUID = 715456159702221404L;

    @Override
    public void init(ServletConfig config) throws ServletException {
//        StandaloneProcessEngineConfiguration conf = (StandaloneProcessEngineConfiguration) ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration();
//        //		conf.setDatabaseSchema("root");
//        conf.setDataSource(DbKit.getConfig().getDataSource()).setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE).setDbHistoryUsed(true);
//        conf.setTransactionFactory(new ActivitiTransactionFactory());
//        ActivitiKit conf.buildProcessEngine();
        //自启动完成

        //部署流程定义
//        ProcessEngine pe = ProcessEngines.getDefaultProcessEngine();
//        pe.getRepositoryService()
//                .createDeployment()
//                .name("督察催办")
//                .addClasspathResource("/com/pansoft/jbsf/oa/bpmn/Urge.bpmn")
//                .addClasspathResource("/com/pansoft/jbsf/oa/bpmn/Urge.png")
//                .deploy();
    }

}
