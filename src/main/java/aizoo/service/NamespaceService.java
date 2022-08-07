package aizoo.service;

import aizoo.Client;
import aizoo.domain.*;
import aizoo.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;

@Service("NamespaceService")
public class NamespaceService {

    @Value("${file.path}")
    String filePath;

    @Autowired
    Client client;

    @Autowired
    NamespaceDAO namespaceDAO;

    @Autowired
    private ServiceDAO serviceDAO;

    @Autowired
    UserDAO userDAO;

    @Autowired
    ComponentDAO componentDAO;

    @Autowired
    GraphDAO graphDAO;

    @Autowired
    DatasourceDAO datasourceDAO;

    @Autowired
    ValidationService validationService;

    @Autowired
    ForkEditionDAO forkEditionDAO;

    // 在IDE控制台打印日志
    private static final Logger logger = LoggerFactory.getLogger(NamespaceService.class);

    /**
     * 添加命名空间
     *
     * @param user      需要添加命名空间的用户
     * @param privacy   添加的命名空间的privacy
     * @param namespace 添加的命名空间的namespace
     * @return String类型，若已存在则返回"命名空间已存在"，若外层存在私有但本层选择公开则返回"先公开上层目录或者将当前层改为私有"，成功添加则返回"SUCCESS"
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String addNamespace(User user, String privacy, String namespace) {
        logger.info("Start add Namespace");
        logger.info("user: {},privacy: {},namespace: {}", user, privacy, namespace);

        //1. 判断是否已经存在这个命名空间，存在则返回“命名空间已存在”
        if (namespaceDAO.existsByNamespace(namespace))
            return "命名空间已存在";

        //targetUser为根据user的Id找到的user实体，如果没找到则抛出异常
        //User user = userDAO.findByUsername(username);
        String username = user.getUsername();
        User targetUser = userDAO.findById(user.getId()).orElseThrow(() -> new EntityNotFoundException());

        // 先将 namespace 结构进行拆分，得到nsArray，再分级建立命名空间，并创建文件夹
        String[] nsArray = namespace.split("\\.");
        //ns为每层namespace，初始为username
        StringBuilder ns = new StringBuilder();
        ns.append(username);

        //2. 判断外层的命名空间是否已经创建，若没有则自动创建
        for (int i = 1; i < nsArray.length; i++) {
            //ns为下一级命名空间
            ns.append(".").append(nsArray[i]);
            //如果不存在，则创建该命名空间
            if (namespaceDAO.existsByNamespace(ns.toString()) == false) {
                Namespace namespaceEntity = new Namespace(ns.toString(), targetUser);

                //如果外层有private的命名空间,则内层必须为私有,否则内层权限由用户决定
                if (!canModifyPrivacy(ns.toString(), "namespace")) {
                    //若外层存在private但本层选择公开，返回提示“先公开上册目录或者将当前层改为私有”
                    if (!privacy.equals("private")) {
                        return "先公开上层目录或者将当前层改为私有";
                    }
                }
                //将命名空间访问权限设置为privacy并保存命名空间
                namespaceEntity.setPrivacy(privacy);
                namespaceDAO.save(namespaceEntity);
            }
        }

        //3. 将namespace中的"."替换成文件分隔符，得到namespaceDir
        String namespaceDir = namespace.replaceAll("\\.", Matcher.quoteReplacement(File.separator));
        //将文件路径与namespaceDir连接得到命名空间的路径namespacePath
        Path namespacePath = Paths.get(filePath, namespaceDir);

        //如果命名空间目录不存在，则创建该目录
        File file = new File(namespacePath.toString());
        if (!file.exists())
            file.mkdirs();
        logger.info("End add Namespace");

        return "SUCCESS";
    }

    /**
     * 获取fork的命名空间
     *
     * @param user           用户
     * @param sourceUsername 组件原username
     * @param namespace      组件原namespace
     * @param isFirstAtom    当前组件是否为单次fork的原子组件
     * @return String类型，为fork的命名空间
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String getForkNamespace(User user, String sourceUsername, String namespace, boolean isFirstAtom) {
        logger.info("Start get Fork Namespace");
        logger.info("user: {},sourceUsername: {},namespace: {},isFirstAtom: {}", user, sourceUsername, namespace, isFirstAtom);

        //User user = userDAO.findByUsername(username);
        String username = user.getUsername();

        //1. 将该user的forkEdition的eidtion增加1
        ForkEdition forkEdition = addEdition(user);

        //获取更新后的edition
        long edition = forkEdition.getEdition();
        //将 namespace 结构进行拆分，得到nsArray
        String[] nsArray = namespace.split("\\.");

        //2. forkEditionName为“fork_" + sourceUsername + edition
        String forkEditionName = "fork_";
        forkEditionName += sourceUsername;  //创建要在namespace中间插入的变量
        forkEditionName = forkEditionName + edition;

        //将namespace的username改为新的username
        nsArray[0] = username;

        //3. 将username和forkEditionName替换到原有namespace中，得到forkNamespace
        String forkNamespace = "";
        for (int i = 0; i < 2; i++) {
            if (i != 0)
                forkNamespace += ".";
            forkNamespace += nsArray[i];
        }
        forkNamespace += ".";
        forkNamespace += forkEditionName;
        for (int i = 3; i < nsArray.length; i++) {
            forkNamespace += ".";
            forkNamespace += nsArray[i];
        }

        logger.info("得到的forkNamespace为: {}", forkNamespace);
        logger.info("End get Fork Namespace");
        //返回结果
        return forkNamespace;
    }

    /**
     * 注册fork的命名空间
     *
     * @param sourceId    component或service的Id
     * @param isFirstAtom 当前组件是否为单次fork的原子组件
     * @param user        用户
     * @param type        区分是component还是service
     * @return String类型
     */
    public String forkRegisterNamespace(long sourceId, boolean isFirstAtom, User user, String type) {
        logger.info("Start register Fork Namespace");
        logger.info("sourceId: {},isFirstAtom: {},user: {},type: {}", sourceId, isFirstAtom, user, type);
        try {
            String privacy = "private";
            String forkNamespace = "";

            //1. 获取组件的username和namespace，调用getForkNamespace生成fork的命名空间
            if (type.equals("COMPONENT")) {
                //component
                Component component = componentDAO.findById(sourceId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceId)));
                String sourceUsername = component.getUser().getUsername();
                String namespace = component.getNamespace().getNamespace();
                forkNamespace = getForkNamespace(user, sourceUsername, namespace, isFirstAtom);
            } else if (type.equals("SERVICE")) {
                //service
                aizoo.domain.Service service = serviceDAO.findById(sourceId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceId)));
                String sourceUsername = service.getUser().getUsername();
                String namespace = service.getNamespace().getNamespace();
                forkNamespace = getForkNamespace(user, sourceUsername, namespace, isFirstAtom);
            } else if (type.equals("DATASOURCE")) {
                //service
                Datasource datasource = datasourceDAO.findById(sourceId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceId)));
                String sourceUsername = datasource.getUser().getUsername();
                String namespace = datasource.getNamespace().getNamespace();
                forkNamespace = getForkNamespace(user, sourceUsername, namespace, isFirstAtom);
            }
            //2. 添加命名空间
            addNamespace(user, privacy, forkNamespace);
            logger.info("Registered forkNamespace: {}", forkNamespace);
            logger.info("End register Fork Namespace");

            //返回fork的命名空间
            return forkNamespace;
        } catch (Exception e) {
            logger.error("Fail to register Fork Namespace,sourceId: {},错误信息为: ", sourceId, e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * fork时将edition加1
     *
     * @param user 需要增加edition的用户
     * @return ForkEdition类型，为更新后的forkEdition
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ForkEdition addEdition(User user) {
        logger.info("Start add Edition");
        logger.info("user: {}", user.toString());

        //获取user的forkEdition
        ForkEdition forkEdition = user.getForkEdition();

        //若该user的forkEdition为空，则新建一个edition为1的forkEdition，并返回
        if (forkEdition == null) {
            forkEdition = new ForkEdition(1);
            forkEdition.setUser(user);
            forkEditionDAO.save(forkEdition);
            return forkEdition;
        }
        //若该user的forkEdition不为空，则将edition加1并保存
        forkEdition.setEdition(forkEdition.getEdition() + 1);
        forkEditionDAO.save(forkEdition);
        logger.info("新添加的Edition信息: {}", forkEdition.toString());
        logger.info("End add Edition");
        //返回结果
        return forkEdition;
    }

    /**
     * 修改privacy
     *
     * @param namespace 需要修改的namespace
     * @param type      判断需要修改privacy的是namespace，component，graph，datasource中哪种类型
     * @param privacy   新的privacy
     * @param id        需要修改的component，graph或datasource的Id
     * @return String类型，成功或失败提示
     */
    @Transactional
    public String modifyPrivacy(String namespace, String type, String privacy, Long id) {
        logger.info("Start modify Privacy");
        logger.info("namespace: {},type: {},privacy: {},id: {}", namespace, type, privacy, id);

        //若需要把privacy修改为public
        if (privacy.equals("public")) {
            //若外层命名空间有private，返回提示
            if (!canModifyPrivacy(namespace, type)) {
                if ("namespace".equals(type)) {
                    logger.info("先公开上层目录或者将当前层改为私有");
                    logger.info("End modify Privacy");
                    return "先公开上层目录或者将当前层改为私有";
                }
                if ("component".equals(type)) {
                    logger.info("该组件所在命名空间为私有，无法公开该组件，请先公开命名空间！");
                    logger.info("End modify Privacy");
                    return "该组件所在命名空间为私有，无法公开该组件，请先公开命名空间！";
                }
                if ("graph".equals(type)) {
                    logger.info("该组件所在命名空间为私有，无法公开该组件，请先公开命名空间！");
                    logger.info("End modify Privacy");
                    return "该组件所在命名空间为私有，无法公开该组件，请先公开命名空间！";
                }
                if ("datasource".equals(type)) {
                    logger.info("该组件所在命名空间为私有，无法公开该组件，请先公开命名空间！");
                    logger.info("End modify Privacy");
                    return "该数据资源所在命名空间为私有，无法公开，请先公开命名空间！";
                }
            }
        }
        //当外层都是公开或需要将权限改为私有时，可以修改
        if ("namespace".equals(type)) {
            Namespace namespaceEntity = namespaceDAO.findByNamespace(namespace);
            namespaceEntity.setPrivacy(privacy);
            namespaceDAO.save(namespaceEntity);
            //外层修改privacy为private时，可能影响到内层
            if (privacy.equals("private")) {
                modifyOuterPrivacy(namespace);
            }
        } else if ("component".equals(type)) {
            //修改componen的权限
            Component component = componentDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
            component.setPrivacy(privacy);
            componentDAO.save(component);

        } else if ("graph".equals(type)) {
            Graph graph = graphDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
            //分别修改graph的component和servive的权限
            if (graph.getComponent() != null) {
                Component component = graph.getComponent();
                component.setPrivacy(privacy);
                componentDAO.save(component);
            }
            if (graph.getService() != null) {
                aizoo.domain.Service service = graph.getService();
                service.setPrivacy(privacy);
                serviceDAO.save(service);
            }
        } else if ("datasource".equals(type)) {
            //修改datasource的权限
            Datasource datasource = datasourceDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
            datasource.setPrivacy(privacy);
            datasourceDAO.save(datasource);
        }
        logger.info("SUCCESS");
        logger.info("End modify Privacy");

        return "SUCCESS";
    }

    /**
     * 判断内层privacy是否可以修改
     * 当外层有private时，内层只能为private（修改会失败）
     * 当外层全为public时，内层可以随意修改
     *
     * @param namespace 唯一表示某个命名空间或者控件
     * @param type      区分是处理namespace还是component的权限
     * @return boolean类型，若外层命名空间全为public则返回true，否则返回false
     */
    public boolean canModifyPrivacy(String namespace, String type) {
        logger.info("Start Verifying If Can Modify Privacy");
        logger.info("namespace: {},type: {}", namespace, type);

        //如果是处理namespace的权限，令namespace为外层命名空间
        if ("namespace".equals(type)) {
            namespace = namespace.substring(0, namespace.lastIndexOf('.'));
        }

        //将 namespace 结构进行拆分，得到nsArray
        //temp初始为最外层命名空间
        String[] nsArray = namespace.split("\\.");
        String temp = nsArray[0];
        Namespace namespaceEntity;

        //逐层判断是否为public
        for (int i = 1; i < nsArray.length; i++) {
            temp += "." + nsArray[i];

            //根据temp找到namespace实体
            namespaceEntity = namespaceDAO.findByNamespace(temp);
            if (namespaceEntity == null) continue;

            //如果该层不为public，则返回false
            if (!namespaceEntity.getPrivacy().equals("public")) {
                logger.info("can Not Modify Privacy");
                logger.info("End Verifying If Can Modify Privacy");
                return false;
            }
        }
        logger.info("can Modify Privacy");
        logger.info("End Verifying If Can Modify Privacy");

        return true;  //外层全为public
    }

    /**
     * 当外层的命名空间由公开变为私有后，需要将其内层的所有命名空间和组件都变为私有
     *
     * @param namespace 需要修改的namespace
     */
    @Transactional
    public void modifyOuterPrivacy(String namespace) {
        logger.info("Start modify Outer Privacy");
        logger.info("namespace: {}", namespace);

        //1. 修改命名空间中component的privacy
        modifyComponentPrivacy(namespace);

        //将 namespace 结构进行拆分，得到nsArray
        String[] nsArray = namespace.split("\\.");

        //根据username和namespace获取内层的namespace
        List<Namespace> namespaceList = namespaceDAO.findByUserUsernameAndNamespaceLike(nsArray[0], namespace + ".%");

        //2. 若namespace为public，则将其改为privacy，并修改该命名空间的component的privacy为private
        for (Namespace ns : namespaceList) {
            if (ns.getPrivacy().equals("public")) {
                ns.setPrivacy("private");
                namespaceDAO.save(ns);
                modifyComponentPrivacy(ns.getNamespace());
            }
        }
        logger.info("End modify Outer Privacy");
    }

    /**
     * 修改命名空间中component的privacy
     *
     * @param namespace 需要修改的namespace
     */
    public void modifyComponentPrivacy(String namespace) {
        logger.info("Start modify Component Privacy");
        logger.info("namespace: {}", namespace);

        //根据namespace获取需要修改的component列表
        List<Component> components = componentDAO.findByNamespaceNamespace(namespace);
        if (components != null) {
            //若component的privacy为public，则修改为private
            for (Component component : components) {
                if (component.getPrivacy().equals("public")) {
                    component.setPrivacy("private");
                    componentDAO.save(component);
                }
            }
        }
        logger.info("End modify Component Privacy");
    }

    /**
     * 根据username和type获得namespace列表
     *
     * @param username 需要查询的namespace对应的username
     * @param type     需要获取的namespace类型
     * @return List<Namespace>类型
     */
    public List<Namespace> getNamespaceListByType(String username, String type) {
        logger.info("Start getNamespaceList By Type");
        logger.info("username: {},type: {}", username, type);

        //1. sort为按照namespace升序排序
        Sort sort = Sort.by(Sort.Direction.ASC, "namespace");
        List<Namespace> namespaceList;
        //type为all时，对命名空间{username}.datasource, {username}.service, {username}.checkPoint进行模糊查询
        if (type.equals("all"))
            namespaceList = namespaceDAO.findByUserUsernameAndNamespaceNotLikeAndNamespaceNotLikeAndNamespaceNotLike(username, username + ".datasource%", username + ".service%", username + ".checkPoint.%", sort);
        else //否则对命名空间{username}.{type}进行模糊查询
            namespaceList = namespaceDAO.findByUserUsernameAndNamespaceLike(username, username + "." + type + "%", sort);

        logger.info("获取到的NamespaceList为: {}", namespaceList.toString());
        logger.info("End getNamespaceList By Type");

        //2. 返回namespaceList剔除不合法命名空间后的结果
        return getFilterNamespaceList(namespaceList);
    }

    /**
     * 将不合法的命名空间，即长度小于4的命名空间剔除
     *
     * @param namespaceList 需要剔除不合法命名空间的namespace列表
     * @return List<Namespace>类型，剔除不合法命名空间后得到的namespace列表
     */
    public List<Namespace> getFilterNamespaceList(List<Namespace> namespaceList) {
        logger.info("Start get Filter NamespaceList");
        logger.info("namespaceList: {}", namespaceList.toString());

        //finalNamespaceList为最终的namespace列表，初始为空
        List<Namespace> finalNamespaceList = new ArrayList<>();

        //对namespaceList中每个namespace进行判断，若长度大于4则加入finalNamespaceList
        for (Namespace namespace : namespaceList) {
            String name = namespace.getNamespace();
            String[] nsArray = name.split("\\.");
            if (nsArray.length >= 4)
                finalNamespaceList.add(namespace);
        }
        logger.info("得到的FilterNamespaceList为: {}", finalNamespaceList.toString());
        logger.info("End get Filter NamespaceList");
        //返回结果
        return finalNamespaceList;
    }

    /**
     * 返回当前命名空间的所有上层命名空间
     *
     * @param namespace 需要返回所有上层命名空间的namespace
     * @return List类型，namespace的所有上层命名空间
     */
    public List getPrivacyEditList(String namespace) {
        logger.info("Start get Privacy EditList");
        logger.info("namespace: {}", namespace);

        //1. 将 namespace 结构进行拆分，得到nsArray
        String[] nsArray = namespace.split("\\.");
        List<Namespace> namespaceList = new ArrayList<>();
        //temp初始为username
        String temp = nsArray[0];
        Namespace namespaceEntity;

        //2. 将外层namespace逐层加入namespaceList
        for (int i = 1; i < nsArray.length; i++) {
            temp += "." + nsArray[i];
            namespaceEntity = namespaceDAO.findByNamespace(temp);
            if (namespaceEntity == null) continue;
            namespaceList.add(namespaceEntity);
        }
        logger.info("得到的PrivacyEditList为: {}", namespaceList.toString());
        logger.info("End get Privacy EditList");
        //返回获取的命名空间列表
        return namespaceList;
    }

    /**
     * 将type对应的组件/命名空间privacy设置为公开
     *
     * @param namespace 需要公开的内容所在的命名空间
     * @param type      需要公开的类型为"namespace","component"或者"graph"
     * @param id        component或graph的Id
     */
    @Transactional
    public void privacyEditPublic(String namespace, String type, Long id) {
        logger.info("Start Edit Privacy Is Public");
        logger.info("namespace: {},type: {},id: {}", namespace, type, id);

        if ("namespace".equals(type)) {
            modifyOuterPrivacyPublic(namespace);
        } else if ("component".equals(type)) {

            //将component设置为公开，再将所在的命名空间及外层命名空间设置为公开
            Component component = componentDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
            component.setPrivacy("public");
            componentDAO.save(component);
            modifyOuterPrivacyPublic(namespace);
        } else if ("graph".equals(type)) {

            //将graph的component和service分别设置为公开
            Graph graph = graphDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
            if (graph.getComponent() != null) {
                Component component = graph.getComponent();
                component.setPrivacy("public");
                componentDAO.save(component);
            }
            if (graph.getService() != null) {
                aizoo.domain.Service service = graph.getService();
                service.setPrivacy("public");
                serviceDAO.save(service);
            }

            //将graph所在的命名空间及外层命名空间设置为公开
            modifyOuterPrivacyPublic(namespace);
        }
        logger.info("End Edit Privacy Is Public");
    }

    /**
     * 将namespace及其外层的命名空间一键公开
     *
     * @param namespace 需要一键公开的namespace
     */
    @Transactional
    public void modifyOuterPrivacyPublic(String namespace) {
        logger.info("Start modify Outer Privacy Public");
        logger.info("namespace: {}", namespace);

        //1. 将 namespace 结构进行拆分，得到nsArray
        String[] nsArray = namespace.split("\\.");
        //temp为每层namespace，初始为username
        String temp = nsArray[0];
        Namespace namespaceEntity;

        //2. 逐层对namespace进行判断，若privacy为private，则修改为public
        for (int i = 1; i < nsArray.length; i++) {
            //temp每次更新为下一层命名空间
            temp += "." + nsArray[i];
            namespaceEntity = namespaceDAO.findByNamespace(temp);
            if (namespaceEntity == null) continue;
            if (!namespaceEntity.getPrivacy().equals("public")) {
                namespaceEntity.setPrivacy("public");
                namespaceDAO.save(namespaceEntity);
            } else
                break;
        }
        logger.info("End modify Outer Privacy Public");
    }
}
