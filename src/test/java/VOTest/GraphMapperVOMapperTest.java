package VOTest;

import TestBase.MockitoTestBase;
import aizoo.domain.Graph;
import aizoo.repository.*;
import aizoo.utils.DAOUtil;
import aizoo.viewObject.mapper.GraphVOEntityMapper;
import aizoo.viewObject.object.GraphVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

public class GraphMapperVOMapperTest extends MockitoTestBase {

    @MockBean
    private GraphVO graphVO;

    @MockBean
    private GraphDAO graphDAO;

    @MockBean
    private DatasourceDAO datasourceDAO;

    @MockBean
    private DatatypeDAO datatypeDAO;

    @MockBean
    private ComponentDAO componentDAO;

    @MockBean
    private NamespaceDAO namespaceDAO;

    @MockBean
    private UserDAO userDAO;

    @MockBean
    private DAOUtil daoUtil;

    private String graphString(){
        return "{\n" +
                "    \"id\": 130,\n" +
                "    \"name\": \"Graphl3\",\n" +
                "    \"graphKey\": \"MODEL-df571656-0a0b-4aa2-9add-4b8f8b0b5b5b\",\n" +
                " \"linkList\": [\n" +
                "    {\n" +
                "      \"source\": \"endpoint-node-d54697d2f0474c578a64133a1e5528e5-output-1\",\n" +
                "      \"target\": \"endpoint-node-c83e110203544dafa590584e5560035d-input-1\",\n" +
                "      \"id\": \"bac52541-e2be-4de8-a419-7e1f1e4414dd\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"nodeList\": [\n" +
                "    {\n" +
                "      \"id\": \"node-d54697d2f0474c578a64133a1e5528e5\",\n" +
                "      \"variable\": \"test_1\",\n" +
                "      \"component\": {\n" +
                "          \"id\": 18,\n" +
                "          \"updateTime\": \"2020-11-03\",\n" +
                "          \"privacy\": \"private\",\n" +
                "          \"name\": \"test\",\n" +
                "          \"title\": \"测试2_1\",\n" +
                "          \"username\":\"super\",\n" +
                "          \"description\": \"xxxxxx\",\n" +
                "          \"properties\": [\n" +
                "                    {\n" +
                "                        \"dataType\":\"aizoo.dict\",\n" +
                "                        \"name\":\"attr\",\n" +
                "                        \"description\":\"attr\",\n" +
                "                        \"title\":\"attr\",\n" +
                "                        \"value\":\n" +
                "                        {\n" +
                "                                \"aggregator\":\"cls\",\n" +
                "                                \"pretrained_weights\":\"bert-base-chinese\",\n" +
                "                                \"output_layer\":13,\n" +
                "                                \"init\": null\n" +
                "                        }\n" +
                "                    }\n" +
                "                    ],\n" +
                "           \"needInitialize\": true,\n" +
                "           \"initMethods\": \"xavier_uniform\",\n" +
                "           \"forkFromUser\": null,\n" +
                "           \"componentType\": \"DATASET\",\n" +
                "        \"inputs\": [\n" +
                "                {\n" +
                "                    \"name\":\"input_ids\",\n" +
                "                    \"title\":\"input_ids\",\n" +
                "                    \"description\":\"Bert input\",\n" +
                "                    \"dataType\":\"aizoo.Tensor\"\n" +
                "                    },\n" +
                "                 {\n" +
                "                    \"name\":\"attention_mask_input\",\n" +
                "                    \"title\":\"attention_mask_input\",\n" +
                "                    \"description\":\"Bert input\",\n" +
                "                    \"dataType\":\"aizoo.Tensor\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"attention_mask_input\",\n" +
                "                    \"description\": \"Bert input\",\n" +
                "                    \"title\": \"attention_mask_input\",\n" +
                "                    \"dataType\":\"aizoo.Tensor\"\n" +
                "                     }\n" +
                "                    ],\n" +
                "    \"outputs\": [\n" +
                "                {\n" +
                "                    \"name\": \"output_ids\",\n" +
                "                    \"description\": \"Bert output\",\n" +
                "                    \"title\": \"output_ids\",\n" +
                "                    \"dataType\":\"aizoo.Tensor\"\n" +
                "                    },\n" +
                "                {\n" +
                "                    \"name\": \"attention_mask_output\",\n" +
                "                    \"description\": \"Bert output\",\n" +
                "                    \"title\": \"attention_mask_output\",\n" +
                "                    \"dataType\":\"aizoo.Tensor\"\n" +
                "                    }\n" +
                "                ],\n" +
                "    \"released\": false,\n" +
                "    \"composed\": false,\n" +
                "    \"namespace\": \"aizoo.xxhx\"\n" +
                "    },\n" +
                "    \"componentType\": \"DATASET\",\n" +
                "    \"datasource\":null,\n" +
                "    \"saveOutput\":[],\n" +
                "    \"exposedOutput\":[]\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"node-c83e110203544dafa590584e5560035d\",\n" +
                "      \"variable\": \"test_2\",\n" +
                "      \"component\": null,\n" +
                "    \"datasource\":{\n" +
                "        \"id\": 123,\n" +
                "        \"updateTime\": \"2020-11-03\",\n" +
                "        \"privacy\":\"private\",\n" +
                "        \"name\":\"datasource\",\n" +
                "        \"title\":\"123\",\n" +
                "        \"description\":\"XX\",\n" +
                "        \"username\":\"super\",\n" +
                "        \"namespace\":\"super.datasource.datasource1\",\n" +
                "        \"componentType\": \"DATASOURCE\",\n" +
                "        \"outputs\":[{\n" +
                "            \"name\":\"super-DATASOURCE-d1150191-df14-4474-b45d-728aa86a649e\",\n" +
                "            \"title\":\"XX\",\n" +
                "            \"description\": \"datasource input\",\n" +
                "            \"dataType\": \"aizoo.Tensor\"\n" +
                "        }\n" +
                "        ]\n" +
                "    },\n" +
                "    \"componentType\": \"DATASOURCE\",\n" +
                "    \"saveOutput\":[],\n" +
                "    \"exposedOutput\":[]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"originJson\": {\n" +
                "    \"elements\": {\n" +
                "      \"nodes\": [\n" +
                "        {\n" +
                "          \"data\": {\n" +
                "            \"id\": \"node-d54697d2f0474c578a64133a1e5528e5\",\n" +
                "            \"component\": {\n" +
                "              \"id\": 18,\n" +
                "              \"privacy\": \"private\",\n" +
                "              \"name\": \"test\",\n" +
                "              \"title\": \"测试2\",\n" +
                "              \"description\": \"xxxxxx\",\n" +
                "              \"properties\": \"[{},{}]\",\n" +
                "              \"needInitialize\": true,\n" +
                "              \"initMethods\": \"xavier_uniform\",\n" +
                "              \"forkFromComponentId\": null,\n" +
                "              \"componentType\": \"DATASET\",\n" +
                "              \"inputs\": [\n" +
                "                {\n" +
                "                  \"name\": \"input_ids\",\n" +
                "                  \"description\": \"Bert input\",\n" +
                "                  \"title\": \"input_ids\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"name\": \"attention_mask_input\",\n" +
                "                  \"description\": \"Bert input\",\n" +
                "                  \"title\": \"attention_mask_input\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"outputs\": [\n" +
                "                {\n" +
                "                  \"name\": \"output_ids\",\n" +
                "                  \"description\": \"Bert output\",\n" +
                "                  \"title\": \"output_ids\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"name\": \"attention_mask_output\",\n" +
                "                  \"description\": \"Bert output\",\n" +
                "                  \"title\": \"attention_mask_output\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"released\": false,\n" +
                "              \"composed\": false,\n" +
                "              \"user\": {\n" +
                "                \"id\": 2,\n" +
                "                \"username\": \"super\",\n" +
                "                \"roles\": [\n" +
                "                  \"ROLE_USER\",\n" +
                "                  \"ROLE_ADMIN\"\n" +
                "                ]\n" +
                "              },\n" +
                "              \"namespace\": {\n" +
                "                \"id\": 1,\n" +
                "                \"privacy\": \"\",\n" +
                "                \"namespace\": \"aizoo.xxhx\",\n" +
                "                \"user\": {\n" +
                "                  \"id\": 2,\n" +
                "                  \"username\": \"super\",\n" +
                "                  \"roles\": [\n" +
                "                    \"ROLE_USER\",\n" +
                "                    \"ROLE_ADMIN\"\n" +
                "                  ]\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          },\n" +
                "          \"position\": {\n" +
                "            \"x\": 132,\n" +
                "            \"y\": 77\n" +
                "          },\n" +
                "          \"group\": \"nodes\",\n" +
                "          \"removed\": false,\n" +
                "          \"selected\": false,\n" +
                "          \"selectable\": true,\n" +
                "          \"locked\": false,\n" +
                "          \"grabbable\": true,\n" +
                "          \"pannable\": false,\n" +
                "          \"classes\": \"compoundNode\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"data\": {\n" +
                "            \"id\": \"endpoint-node-d54697d2f0474c578a64133a1e5528e5-output-0\",\n" +
                "            \"parent\": \"node-d54697d2f0474c578a64133a1e5528e5\",\n" +
                "            \"name\": \"output_ids\",\n" +
                "            \"description\": \"Bert output\",\n" +
                "            \"title\": \"output_ids\"\n" +
                "          },\n" +
                "          \"position\": {\n" +
                "            \"x\": 82,\n" +
                "            \"y\": 97\n" +
                "          },\n" +
                "          \"group\": \"nodes\",\n" +
                "          \"removed\": false,\n" +
                "          \"selected\": false,\n" +
                "          \"selectable\": true,\n" +
                "          \"locked\": false,\n" +
                "          \"grabbable\": true,\n" +
                "          \"pannable\": false,\n" +
                "          \"classes\": \"sourceEndpoint\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"data\": {\n" +
                "            \"id\": \"endpoint-node-d54697d2f0474c578a64133a1e5528e5-output-1\",\n" +
                "            \"parent\": \"node-d54697d2f0474c578a64133a1e5528e5\",\n" +
                "            \"name\": \"attention_mask_output\",\n" +
                "            \"description\": \"Bert output\",\n" +
                "            \"title\": \"attention_mask_output\"\n" +
                "          },\n" +
                "          \"position\": {\n" +
                "            \"x\": 182,\n" +
                "            \"y\": 97\n" +
                "          },\n" +
                "          \"group\": \"nodes\",\n" +
                "          \"removed\": false,\n" +
                "          \"selected\": false,\n" +
                "          \"selectable\": true,\n" +
                "          \"locked\": false,\n" +
                "          \"grabbable\": true,\n" +
                "          \"pannable\": false,\n" +
                "          \"classes\": \"sourceEndpoint eh-preview-active\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"data\": {\n" +
                "            \"id\": \"endpoint-node-d54697d2f0474c578a64133a1e5528e5-input-0\",\n" +
                "            \"parent\": \"node-d54697d2f0474c578a64133a1e5528e5\",\n" +
                "            \"name\": \"input_ids\",\n" +
                "            \"description\": \"Bert input\",\n" +
                "            \"title\": \"input_ids\"\n" +
                "          },\n" +
                "          \"position\": {\n" +
                "            \"x\": 82,\n" +
                "            \"y\": 57\n" +
                "          },\n" +
                "          \"group\": \"nodes\",\n" +
                "          \"removed\": false,\n" +
                "          \"selected\": false,\n" +
                "          \"selectable\": true,\n" +
                "          \"locked\": false,\n" +
                "          \"grabbable\": true,\n" +
                "          \"pannable\": false,\n" +
                "          \"classes\": \"targetEndpoint\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"data\": {\n" +
                "            \"id\": \"endpoint-node-d54697d2f0474c578a64133a1e5528e5-input-1\",\n" +
                "            \"parent\": \"node-d54697d2f0474c578a64133a1e5528e5\",\n" +
                "            \"name\": \"attention_mask_input\",\n" +
                "            \"description\": \"Bert input\",\n" +
                "            \"title\": \"attention_mask_input\"\n" +
                "          },\n" +
                "          \"position\": {\n" +
                "            \"x\": 182,\n" +
                "            \"y\": 57\n" +
                "          },\n" +
                "          \"group\": \"nodes\",\n" +
                "          \"removed\": false,\n" +
                "          \"selected\": false,\n" +
                "          \"selectable\": true,\n" +
                "          \"locked\": false,\n" +
                "          \"grabbable\": true,\n" +
                "          \"pannable\": false,\n" +
                "          \"classes\": \"targetEndpoint\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"data\": {\n" +
                "            \"id\": \"node-c83e110203544dafa590584e5560035d\",\n" +
                "            \"component\": {\n" +
                "              \"id\": 18,\n" +
                "              \"privacy\": \"private\",\n" +
                "              \"name\": \"test\",\n" +
                "              \"title\": \"测试2\",\n" +
                "              \"description\": \"xxxxxx\",\n" +
                "              \"properties\": \"[{},{}]\",\n" +
                "              \"needInitialize\": true,\n" +
                "              \"initMethods\": \"xavier_uniform\",\n" +
                "              \"forkFromComponentId\": null,\n" +
                "              \"componentType\": \"DATASET\",\n" +
                "              \"inputs\": [\n" +
                "                {\n" +
                "                  \"name\": \"input_ids\",\n" +
                "                  \"description\": \"Bert input\",\n" +
                "                  \"title\": \"input_ids\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"name\": \"attention_mask_input\",\n" +
                "                  \"description\": \"Bert input\",\n" +
                "                  \"title\": \"attention_mask_input\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"outputs\": [\n" +
                "                {\n" +
                "                  \"name\": \"output_ids\",\n" +
                "                  \"description\": \"Bert output\",\n" +
                "                  \"title\": \"output_ids\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"name\": \"attention_mask_output\",\n" +
                "                  \"description\": \"Bert output\",\n" +
                "                  \"title\": \"attention_mask_output\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"released\": false,\n" +
                "              \"composed\": false,\n" +
                "              \"user\": {\n" +
                "                \"id\": 2,\n" +
                "                \"username\": \"super\",\n" +
                "                \"roles\": [\n" +
                "                  \"ROLE_USER\",\n" +
                "                  \"ROLE_ADMIN\"\n" +
                "                ]\n" +
                "              },\n" +
                "              \"namespace\": {\n" +
                "                \"id\": 1,\n" +
                "                \"privacy\": \"\",\n" +
                "                \"namespace\": \"aizoo.xxhx\",\n" +
                "                \"user\": {\n" +
                "                  \"id\": 2,\n" +
                "                  \"username\": \"super\",\n" +
                "                  \"roles\": [\n" +
                "                    \"ROLE_USER\",\n" +
                "                    \"ROLE_ADMIN\"\n" +
                "                  ]\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          },\n" +
                "          \"position\": {\n" +
                "            \"x\": 174,\n" +
                "            \"y\": 217\n" +
                "          },\n" +
                "          \"group\": \"nodes\",\n" +
                "          \"removed\": false,\n" +
                "          \"selected\": false,\n" +
                "          \"selectable\": true,\n" +
                "          \"locked\": false,\n" +
                "          \"grabbable\": true,\n" +
                "          \"pannable\": false,\n" +
                "          \"classes\": \"compoundNode\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"data\": {\n" +
                "            \"id\": \"endpoint-node-c83e110203544dafa590584e5560035d-output-0\",\n" +
                "            \"parent\": \"node-c83e110203544dafa590584e5560035d\",\n" +
                "            \"name\": \"output_ids\",\n" +
                "            \"description\": \"Bert output\",\n" +
                "            \"title\": \"output_ids\"\n" +
                "          },\n" +
                "          \"position\": {\n" +
                "            \"x\": 124,\n" +
                "            \"y\": 237\n" +
                "          },\n" +
                "          \"group\": \"nodes\",\n" +
                "          \"removed\": false,\n" +
                "          \"selected\": false,\n" +
                "          \"selectable\": true,\n" +
                "          \"locked\": false,\n" +
                "          \"grabbable\": true,\n" +
                "          \"pannable\": false,\n" +
                "          \"classes\": \"sourceEndpoint\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"data\": {\n" +
                "            \"id\": \"endpoint-node-c83e110203544dafa590584e5560035d-output-1\",\n" +
                "            \"parent\": \"node-c83e110203544dafa590584e5560035d\",\n" +
                "            \"name\": \"attention_mask_output\",\n" +
                "            \"description\": \"Bert output\",\n" +
                "            \"title\": \"attention_mask_output\"\n" +
                "          },\n" +
                "          \"position\": {\n" +
                "            \"x\": 224,\n" +
                "            \"y\": 237\n" +
                "          },\n" +
                "          \"group\": \"nodes\",\n" +
                "          \"removed\": false,\n" +
                "          \"selected\": false,\n" +
                "          \"selectable\": true,\n" +
                "          \"locked\": false,\n" +
                "          \"grabbable\": true,\n" +
                "          \"pannable\": false,\n" +
                "          \"classes\": \"sourceEndpoint\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"data\": {\n" +
                "            \"id\": \"endpoint-node-c83e110203544dafa590584e5560035d-input-0\",\n" +
                "            \"parent\": \"node-c83e110203544dafa590584e5560035d\",\n" +
                "            \"name\": \"input_ids\",\n" +
                "            \"description\": \"Bert input\",\n" +
                "            \"title\": \"input_ids\"\n" +
                "          },\n" +
                "          \"position\": {\n" +
                "            \"x\": 124,\n" +
                "            \"y\": 197\n" +
                "          },\n" +
                "          \"group\": \"nodes\",\n" +
                "          \"removed\": false,\n" +
                "          \"selected\": false,\n" +
                "          \"selectable\": true,\n" +
                "          \"locked\": false,\n" +
                "          \"grabbable\": true,\n" +
                "          \"pannable\": false,\n" +
                "          \"classes\": \"targetEndpoint\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"data\": {\n" +
                "            \"id\": \"endpoint-node-c83e110203544dafa590584e5560035d-input-1\",\n" +
                "            \"parent\": \"node-c83e110203544dafa590584e5560035d\",\n" +
                "            \"name\": \"attention_mask_input\",\n" +
                "            \"description\": \"Bert input\",\n" +
                "            \"title\": \"attention_mask_input\"\n" +
                "          },\n" +
                "          \"position\": {\n" +
                "            \"x\": 224,\n" +
                "            \"y\": 197\n" +
                "          },\n" +
                "          \"group\": \"nodes\",\n" +
                "          \"removed\": false,\n" +
                "          \"selected\": false,\n" +
                "          \"selectable\": true,\n" +
                "          \"locked\": false,\n" +
                "          \"grabbable\": true,\n" +
                "          \"pannable\": false,\n" +
                "          \"classes\": \"targetEndpoint eh-preview-active\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"edges\": [\n" +
                "        {\n" +
                "          \"data\": {\n" +
                "            \"source\": \"endpoint-node-d54697d2f0474c578a64133a1e5528e5-output-1\",\n" +
                "            \"target\": \"endpoint-node-c83e110203544dafa590584e5560035d-input-1\",\n" +
                "            \"id\": \"bac52541-e2be-4de8-a419-7e1f1e4414dd\"\n" +
                "          },\n" +
                "          \"position\": {\n" +
                "            \"x\": 0,\n" +
                "            \"y\": 0\n" +
                "          },\n" +
                "          \"group\": \"edges\",\n" +
                "          \"removed\": false,\n" +
                "          \"selected\": false,\n" +
                "          \"selectable\": true,\n" +
                "          \"locked\": false,\n" +
                "          \"grabbable\": true,\n" +
                "          \"pannable\": true,\n" +
                "          \"classes\": \"\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"style\": [\n" +
                "      {\n" +
                "        \"selector\": \".compoundNode\",\n" +
                "        \"style\": {\n" +
                "          \"label\": \"fn\",\n" +
                "          \"min-width\": \"fn\",\n" +
                "          \"text-valign\": \"center\",\n" +
                "          \"shape\": \"round-rectangle\",\n" +
                "          \"compound-sizing-wrt-labels\": \"exclude\",\n" +
                "          \"padding\": \"-5px\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"selector\": \".sourceEndpoint\",\n" +
                "        \"style\": {\n" +
                "          \"width\": \"10px\",\n" +
                "          \"height\": \"10px\",\n" +
                "          \"font-size\": \"10px\",\n" +
                "          \"background-fit\": \"cover\",\n" +
                "          \"label\": \"fn\",\n" +
                "          \"background-image\": \"data:image/png;base64,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\",\n" +
                "          \"text-valign\": \"bottom\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"selector\": \".targetEndpoint\",\n" +
                "        \"style\": {\n" +
                "          \"width\": \"10px\",\n" +
                "          \"height\": \"10px\",\n" +
                "          \"font-size\": \"10px\",\n" +
                "          \"background-fit\": \"cover\",\n" +
                "          \"label\": \"fn\",\n" +
                "          \"background-image\": \"data:image/png;base64,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\",\n" +
                "          \"text-valign\": \"top\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"selector\": \"edge\",\n" +
                "        \"style\": {\n" +
                "          \"curve-style\": \"bezier\",\n" +
                "          \"target-arrow-shape\": \"triangle\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"selector\": \".eh-handle\",\n" +
                "        \"style\": {\n" +
                "          \"background-color\": \"rgb(255,0,0)\",\n" +
                "          \"width\": \"12px\",\n" +
                "          \"height\": \"12px\",\n" +
                "          \"shape\": \"ellipse\",\n" +
                "          \"overlay-opacity\": \"0\",\n" +
                "          \"border-width\": \"12px\",\n" +
                "          \"border-opacity\": \"0\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"selector\": \".eh-hover\",\n" +
                "        \"style\": {\n" +
                "          \"background-color\": \"rgb(255,0,0)\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"selector\": \".eh-source\",\n" +
                "        \"style\": {\n" +
                "          \"border-width\": \"2px\",\n" +
                "          \"border-color\": \"rgb(255,0,0)\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"selector\": \".eh-target\",\n" +
                "        \"style\": {\n" +
                "          \"border-width\": \"2px\",\n" +
                "          \"border-color\": \"rgb(255,0,0)\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"selector\": \".eh-preview, .eh-ghost-edge\",\n" +
                "        \"style\": {\n" +
                "          \"background-color\": \"rgb(255,0,0)\",\n" +
                "          \"line-color\": \"rgb(255,0,0)\",\n" +
                "          \"target-arrow-color\": \"rgb(255,0,0)\",\n" +
                "          \"source-arrow-color\": \"rgb(255,0,0)\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"selector\": \".eh-ghost-edge.eh-preview-active\",\n" +
                "        \"style\": {\n" +
                "          \"opacity\": \"0\"\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"data\": {},\n" +
                "    \"zoomingEnabled\": true,\n" +
                "    \"userZoomingEnabled\": true,\n" +
                "    \"zoom\": 1,\n" +
                "    \"minZoom\": 1e-50,\n" +
                "    \"maxZoom\": 1e+50,\n" +
                "    \"panningEnabled\": true,\n" +
                "    \"userPanningEnabled\": true,\n" +
                "    \"pan\": {\n" +
                "      \"x\": 0,\n" +
                "      \"y\": 0\n" +
                "    },\n" +
                "    \"boxSelectionEnabled\": true,\n" +
                "    \"renderer\": {\n" +
                "      \"name\": \"canvas\"\n" +
                "    }\n" +
                "  },\n" +
                "    \"graphType\": \"COMPONENT\",\n" +
                "    \"componentType\": \"DATASET\"\n" +
                "}\n";
    }

    public GraphVO buildGraphVO() {
        GraphVO graphVO = null;
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            graphVO = objectMapper.readValue(graphString(), GraphVO.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return graphVO;
    }

    @Test
    public void mapperTest() {
        Graph graph0 = new Graph();
        graph0.setId(Long.valueOf(1));
        Graph graph = GraphVOEntityMapper.MAPPER.graphVO2Graph(buildGraphVO(),graphDAO,componentDAO,daoUtil,datatypeDAO);
        graph.setId(Long.valueOf(2));
        Optional<Graph> optional=Optional.of(graph);
        Mockito.when(graphDAO.findById(Long.valueOf(2))).thenReturn(optional);

        GraphVO graphVO = GraphVOEntityMapper.MAPPER.graph2GraphVO(graph0);
        Graph graph1 = GraphVOEntityMapper.MAPPER.graphVO2Graph(graphVO,graphDAO,componentDAO,daoUtil,datatypeDAO);
        System.out.println(graph0);
        System.out.println(graphVO);
        System.out.println(graph1);
    }
}
