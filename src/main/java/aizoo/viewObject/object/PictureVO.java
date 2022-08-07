package aizoo.viewObject.object;

import aizoo.common.PictureType;
import org.springframework.web.multipart.MultipartFile;

/**
 * FileName:PictureVO
 * Description:用于截图上传
 */
public class PictureVO extends BaseVO{
//    前端图片文件命名规则为graphId_graphName.png
    private transient MultipartFile pictureFile;

    private PictureType pictureType;

    public MultipartFile getPictureFile() {
        return pictureFile;
    }

    public void setPictureFile(MultipartFile pictureFile) {
        this.pictureFile = pictureFile;
    }

    public PictureType getPictureType() {
        return pictureType;
    }

    public void setPictureType(PictureType pictureType) {
        this.pictureType = pictureType;
    }
}
