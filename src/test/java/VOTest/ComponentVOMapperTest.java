package VOTest;

import TestBase.MockitoTestBase;
import aizoo.domain.Component;
import aizoo.domain.Datatype;
import aizoo.domain.Namespace;
import aizoo.domain.User;
import aizoo.repository.*;
import aizoo.viewObject.object.ComponentVO;
import aizoo.viewObject.mapper.ComponentVOEntityMapper;
import aizoo.viewObject.object.NamespaceVO;
import aizoo.viewObject.object.UserVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

public class ComponentVOMapperTest extends MockitoTestBase{

    @MockBean
    private DatatypeDAO datatypeDAO;

    @MockBean
    private  ComponentDAO componentDAO;

    @MockBean
    private UserVO userVO;

    @MockBean
    private NamespaceVO namespaceVO;

    @MockBean
    private NamespaceDAO namespaceDAO;

    @MockBean
    private ComponentInputParameterDAO componentInputParameterDAO;

    @MockBean
    private ComponentOutputParameterDAO componentOutputParameterDAO;

    @MockBean
    private UserDAO userDAO;

    @Before
    public void beforeTest() {
        Datatype datatype = new Datatype();
        datatype.setName("aizoo.Tensor");
        datatype.setNamespace(new Namespace("aizoo.Tensor"));
        datatype.setPrivacy("public");
        datatype.setTitle("aizoo.Tensor Title");
        datatype.setUser(new User("aizoo"));
        Mockito.when(datatypeDAO.findByName("aizoo.Tensor")).thenReturn(datatype);
    }

    private String componentString() {
        return "{\n" +
                "    \"name\": \"测试2\",\n" +
                "    \"componentType\": \"DATASET\",\n" +
                "    \"forkFromUser\": \"abc\",\n" +
                "    \"user\": {\n" +
                "            \"id\": 1,\n" +
                "            \"username\": \"abc\",\n" +
                "            \"roles\":[ \"ROLE_USER\",\"ROLE_ADMIN\"]\n" +
                "    },\n" +
                "    \"namespace\": {\n" +
                "            \"id\": 1,\n" +
                "            \"privacy\": \"abc\",\n" +
                "            \"namespace\": \"aaa.bbb\",\n" +
                "            \"user\": {\n" +
                "               \"id\": 1,\n" +
                "               \"username\": \"abc\",\n" +
                "               \"roles\":[ \"ROLE_USER\",\"ROLE_ADMIN\"]\n" +
                "            }\n" +
                "    },\n" +
                "    \"inputs\": [\n" +
                "        {\n" +
                "            \"dataType\": \"aizoo.Tensor\",\n" +
                "            \"name\": \"input_ids\",\n" +
                "            \"description\": \"Bert input\",\n" +
                "            \"title\": \"input_ids\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"dataType\": \"aizoo.Tensor\",\n" +
                "            \"name\": \"attention_mask\",\n" +
                "            \"description\": \"Bert input\",\n" +
                "            \"title\": \"attention_mask\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"outputs\": [\n" +
                "        {\n" +
                "            \"dataType\": \"aizoo.Tensor\",\n" +
                "            \"name\": \"input_ids\",\n" +
                "            \"description\": \"Bert input\",\n" +
                "            \"title\": \"input_ids\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"dataType\": \"aizoo.Tensor\",\n" +
                "            \"name\": \"attention_mask\",\n" +
                "            \"description\": \"Bert input\",\n" +
                "            \"title\": \"attention_mask\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"privacy\": \"private\",\n" +
                "    \"needInitialize\": true,\n" +
                "    \"initMethods\": \"xavier_uniform\",\n" +
                "    \"description\": \"xxxxxx\",\n" +
                "    \"properties\": [\n" +
                "        {},\n" +
                "        {}\n" +
                "    ]\n" +
                "}";
    }

    public ComponentVO buildComponentVO() {
        ComponentVO componentVO = null;
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            componentVO = objectMapper.readValue(componentString(), ComponentVO.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return componentVO;
    }

    @Test
    public void mapperTest() {
        Component component0 = new Component();
        component0.setId(Long.valueOf(1));
        Component component = ComponentVOEntityMapper.MAPPER.componentVO2Component(buildComponentVO(), datatypeDAO,componentDAO,namespaceDAO,userDAO);
        component.setId(Long.valueOf(2));
        component0.setForkFrom(component);
        Optional<Component> optional=Optional.of(component);
        Mockito.when(componentDAO.findById(Long.valueOf(2))).thenReturn(optional);

        ComponentVO cvo = ComponentVOEntityMapper.MAPPER.component2ComponentVO(component0);
//        cvo.setNamespaceVO(namespaceVO);
        Component component1 = ComponentVOEntityMapper.MAPPER.componentVO2Component(cvo,datatypeDAO,componentDAO,namespaceDAO,userDAO);
        System.out.println(component0);
        System.out.println(cvo);
        System.out.println(component1);
    }
}
