package com.fuge;

/**
 * TODO.
 * <p>
 * User: sujie
 * Date: 2019/1/4
 * Time: 11:05 AM
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jfinal.plugin.IPlugin;
import com.jfinal.plugin.activerecord.Config;
import com.jfinal.plugin.activerecord.DbKit;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ProcessDefinition;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.datasource.DataSourceException;

import javax.sql.DataSource;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;


public class ActivitiPlugin implements IPlugin
{

    private static ProcessEngine processEngine = null;
    private static ProcessEngineConfiguration engineConfig = null;
    private boolean isStarted = false;
    private DataSource dataSource = null;

    private Config config;

    public ActivitiPlugin()
    {
        this.config = null;
        this.isStarted = false;
        this.config = DbKit.getConfig();
    }


    @Override
    public boolean start()
    {
        try
        {
            createProcessEngine();
        } catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean stop()
    {
        ProcessEngines.destroy();
        isStarted = false;
        return true;
    }


    private Boolean createProcessEngine() throws Exception
    {
        if (isStarted)
        {
            return true;
        }

        dataSource = DbKit.getConfig().getDataSource();
        if (dataSource == null)
        {
            throw new DataSourceException("DataSource cannot be null.");
        }
        engineConfig = ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration();
        engineConfig.setDataSource(dataSource).setDatabaseSchemaUpdate(
                ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE).setDbHistoryUsed(true);

        // 使用托管事务工厂
        engineConfig.setTransactionsExternallyManaged(true);
        
//        processEngineConfiguration.setHistory("full");

        processEngine = engineConfig.buildProcessEngine();

        ActivitiKit.setProcessEngine(processEngine);
        //        StandaloneProcessEngineConfiguration conf = (StandaloneProcessEngineConfiguration) ProcessEngineConfiguration
        //                .createStandaloneProcessEngineConfiguration();
        //        //        		conf.setDatabaseSchema("cwbase35_9999");
        //        //        conf.setDataSource(DbKit.getConfig().getDataSource())
        //        //                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
        //        //                .setDbHistoryUsed(true);
        //        //		conf.setTransactionsExternallyManaged(true); // 使用托管事务工厂
        //        conf.setTransactionFactory(new ActivitiTransactionFactory());
        //        ActivitiPlugin.processEngine = conf.buildProcessEngine();
        isStarted = true;
        //开启流程引擎
        System.out.println("流程引擎启动.......");

        /**
         * 部署流程定义
         * 以后可以拿出去
         * */
        ActivitiKit.deployProcess("bpmn/first-model.bpmn20.xml");

        //        		ProcessEngine pe = ProcessEngines.getDefaultProcessEngine();
//        processEngine.getRepositoryService()
//        		.createDeployment()
//        		.name("督察催办")
//                .addClasspathResource("first-model.bpmn20.xml")
////        		.addClasspathResource("/com/****/jbsf/oa/bpmn/Urge.bpmn")
////        		.addClasspathResource("/com/****/jbsf/oa/bpmn/Urge.png")
//        		.deploy();
//        convertToModel(ActivitiPlugin.processEngine,"Urge:4:17504");
//        createModel(ActivitiPlugin.processEngine);
        return isStarted;
    }

    // 开启流程服务引擎
//    public static ProcessEngine buildProcessEngine()
//    {
//        if (processEngine == null)
//            if (processEngineConfiguration != null)
//            {
//                processEngine = processEngineConfiguration.buildProcessEngine();
//            }
//        return processEngine;
//    }


    /**
     * 创建新模型
     *
     * @throws UnsupportedEncodingException
     */
    public void createModel(ProcessEngine pe) throws UnsupportedEncodingException
    {
        RepositoryService repositoryService = pe.getRepositoryService();
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode editorNode = objectMapper.createObjectNode();
        editorNode.put("id", "canvas");
        editorNode.put("resourceId", "canvas");
        ObjectNode stencilSetNode = objectMapper.createObjectNode();
        stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
        editorNode.put("stencilset", stencilSetNode);
        Model modelData = repositoryService.newModel();

        ObjectNode modelObjectNode = objectMapper.createObjectNode();
        modelObjectNode.put(ModelDataJsonConstants.MODEL_NAME, "模型名称");
        modelObjectNode.put(ModelDataJsonConstants.MODEL_REVISION, 1);
        String description = StringUtils.defaultString("模型描述信息");
        modelObjectNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, description);
        modelData.setMetaInfo(modelObjectNode.toString());
        modelData.setName("模型名称");
        modelData.setKey(StringUtils.defaultString("Urge"));

        repositoryService.saveModel(modelData);
        repositoryService.addModelEditorSource(modelData.getId(), editorNode.toString().getBytes("utf-8"));
    }

    /**
     * 流程定义转模型
     */
    public void convertToModel(ProcessEngine pe, String processDefinitionId) throws Exception
    {
        RepositoryService repositoryService = pe.getRepositoryService();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId).singleResult();
        InputStream bpmnStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(),
                processDefinition.getResourceName());
        XMLInputFactory xif = XMLInputFactory.newInstance();
        InputStreamReader in = new InputStreamReader(bpmnStream, "UTF-8");
        XMLStreamReader xtr = xif.createXMLStreamReader(in);
        BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(xtr);

        BpmnJsonConverter converter = new BpmnJsonConverter();
        ObjectNode modelNode = converter.convertToJson(bpmnModel);
        Model modelData = repositoryService.newModel();
        modelData.setKey(processDefinition.getKey());
        modelData.setName(processDefinition.getResourceName());
        modelData.setCategory(processDefinition.getDeploymentId());

        ObjectNode modelObjectNode = new ObjectMapper().createObjectNode();
        modelObjectNode.put(ModelDataJsonConstants.MODEL_NAME, processDefinition.getName());
        modelObjectNode.put(ModelDataJsonConstants.MODEL_REVISION, 1);
        modelObjectNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, processDefinition.getDescription());
        modelData.setMetaInfo(modelObjectNode.toString());

        repositoryService.saveModel(modelData);

        repositoryService.addModelEditorSource(modelData.getId(), modelNode.toString().getBytes("utf-8"));

    }
}