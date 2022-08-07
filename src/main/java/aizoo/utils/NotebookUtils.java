package aizoo.utils;

import aizoo.domain.Namespace;
import aizoo.service.ComponentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.regex.Matcher;

public class NotebookUtils  {

    private static final Logger logger = LoggerFactory.getLogger(ComponentService.class);

    /**
     * 用于组织打开原子组件源文件的url的方法
     * @param namespace 原子组件的namespace
     * @param filename  原子组件名字
     * @return
     */
    public static String getComponentNotebookPath (Namespace namespace, String filename) throws Exception {
        if (namespace == null) {
            logger.error("{} namespace is null", filename);
            throw new Exception();
        }
        String namespacePath = namespace.getNamespace().replaceAll("\\.", Matcher.quoteReplacement(File.separator));
        return Paths.get("notebook", "edit", namespacePath, filename).toString();
    }
}
