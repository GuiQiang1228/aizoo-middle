package aizoo.repository;

import aizoo.domain.Component;
import aizoo.common.ComponentType;
import aizoo.domain.Namespace;
import aizoo.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.List;

@Repository
public interface ComponentDAO extends JpaRepository<Component, Long>, PagingAndSortingRepository<Component, Long> {

    Component findByUserUsernameAndForkFrom(String username, Component forkfrom);

    Component findByNameAndComponentType(String componentName, ComponentType componentType);

    List<Component> findByComponentTypeAndUserUsernameAndPathIsNotNull(ComponentType componentType, String username);

    List<Component> findByComponentTypeAndUserUsernameNotAndPrivacyAndPathIsNotNull(ComponentType componentType, String username, String privacy);

    Page<Component> findByUserUsernameAndComposed(String username, boolean composed, Pageable pageable);

    Page<Component> findByComposedAndForkFromIsNull(boolean composed, Pageable pageable);

    List<Component> findByUserUsername(String username);

    List<Component> findByNamespaceNamespace(String namespace);

    //    模糊查询
    List<Component> findByComponentTypeAndPrivacyAndUserUsernameNotAndTitleLikeAndPathIsNotNull(ComponentType componentType, String privacy, String username, String title);

    List<Component> findByPrivacyAndUserUsernameNotAndTitleLikeAndPathIsNotNullAndComponentTypeIn(String privacy, String username, String title, List<ComponentType> componentTypes);

    List<Component> findByComponentTypeAndUserUsernameAndTitleLikeAndPathIsNotNull(ComponentType componentType, String username, String title);

    List<Component> findByUserUsernameAndTitleLikeAndPathIsNotNullAndComponentTypeIn(String username, String title, List<ComponentType> componentTypes);

    //    查询的为公开组件，含系统公开，其他用户公开的且当前用户未fork过的组件,并且除MODULE PARAMETER类型
    List<Component> findByComponentTypeIsNotInAndPrivacyAndUserUsernameNotAndTitleLikeAndPathIsNotNull(List<ComponentType> componentTypes, String privacy, String username, String title);

    List<Component> findByComponentTypeInAndPrivacyAndUserUsernameNotAndTitleLikeAndPathIsNotNull(List<ComponentType> componentTypes, String privacy, String username, String title);

    List<Component> findByComponentTypeInAndPrivacyAndUserUsernameAndTitleLikeAndPathIsNotNull(List<ComponentType> componentTypes, String privacy, String username, String title);

    List<Component> findByComponentTypeIsNotInAndPrivacyAndUserUsernameAndTitleLikeAndPathIsNotNull(List<ComponentType> componentTypes, String privacy, String username, String title);

    //    根据用户和forkFrom判断该组件是否被当前用户fork过
    boolean existsByForkFromAndUserUsername(Component forkFrom, String username);

    List<Component> findByPrivacy(String privacy);

    boolean existsByNamespaceNamespace(String namespace);

    boolean existsByNameAndUserUsername(String name, String username);

    boolean existsByComponentVersionAndNameAndUserUsername(String componentVersion, String name, String username);

    List<Component> findByNameAndNamespaceNamespace(String name, String namespace);//找到已发布的复合组件的版本以及已上传的组件版本

    Page<Component> findByComposed(boolean composed, Pageable pageable);

    int countByUserUsernameAndReleased(String username, boolean released);

    List<Component> findByComponentType(ComponentType componentType);

    List<Component> findByIdIn(List<Long> componentIds);

    @Query(value = "SELECT component.* FROM component JOIN namespace ON component.namespace_id=namespace.id JOIN user ON namespace.user_id=user.id WHERE IF(?1 != '',namespace.namespace like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',component.component_type LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',component.privacy LIKE CONCAT('%',?3,'%'),1=1) AND IF(?4 !='',component.description LIKE CONCAT('%',?4,'%'),1=1) AND IF(?5 !='',component.update_time>=?5 ,1=1) AND IF(?6 !='',component.update_time<=?6,1=1) AND IF(?7 !='',component.name LIKE CONCAT('%',?7,'%'),1=1) AND user.username=?8 AND component.composed = false ORDER BY component.create_time DESC",
            countQuery = "SELECT COUNT(1) FROM component  JOIN namespace ON component.namespace_id=namespace.id JOIN user ON namespace.user_id=user.id WHERE IF(?1 != '',namespace.namespace like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',component.component_type LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',component.privacy LIKE CONCAT('%',?3,'%'),1=1) AND IF(?4 !='',component.description LIKE CONCAT('%',?4,'%'),1=1) AND IF(?5 !='',component.update_time>=?5 ,1=1) AND IF(?6 !='',component.update_time<=?6,1=1) AND IF(?7 !='',component.name LIKE CONCAT('%',?7,'%'),1=1) AND user.username=?8 AND component.composed = false ORDER BY component.create_time DESC",
            nativeQuery = true)
    Page<Component> searchComponent(String namespace, String type, String privacy, String desc, String startUpdateTime, String endUpdateTime, String name, String userName, Pageable pageable);

    @Query(value = "SELECT component.* FROM component JOIN namespace ON component.namespace_id=namespace.id JOIN user ON namespace.user_id=user.id WHERE IF(?1 != '',namespace.namespace like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',component.component_type LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',component.privacy LIKE CONCAT('%',?3,'%'),1=1) AND IF(?4 !='',component.description LIKE CONCAT('%',?4,'%'),1=1) AND IF(?5 !='',component.update_time>=?5 ,1=1) AND IF(?6 !='',component.update_time<=?6,1=1) AND IF(?7 !='',component.name LIKE CONCAT('%',?7,'%'),1=1) AND IF(?8 !='',user.username=?8,1=1)AND component.composed = false ORDER BY component.create_time DESC",
            countQuery = "SELECT COUNT(1) FROM component  JOIN namespace ON component.namespace_id=namespace.id JOIN user ON namespace.user_id=user.id WHERE IF(?1 != '',namespace.namespace like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',component.component_type LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',component.privacy LIKE CONCAT('%',?3,'%'),1=1) AND IF(?4 !='',component.description LIKE CONCAT('%',?4,'%'),1=1) AND IF(?5 !='',component.update_time>=?5 ,1=1) AND IF(?6 !='',component.update_time<=?6,1=1) AND IF(?7 !='',component.name LIKE CONCAT('%',?7,'%'),1=1) AND IF(?8 !='',user.username=?8,1=1) AND component.composed = false ORDER BY component.create_time DESC",
            nativeQuery = true)
    Page<Component> adminSearchComponent(String namespace, String type, String privacy, String desc, String startUpdateTime, String endUpdateTime, String name, String owner, Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = "update component set component.description = ?2 where component.id = ?1", nativeQuery = true)
    void updateDesc(long id, String desc);
}
