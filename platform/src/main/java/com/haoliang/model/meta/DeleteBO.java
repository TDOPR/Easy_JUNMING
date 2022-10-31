package com.haoliang.model.meta;

import lombok.Data;

import java.util.List;

/**
 * @author Dominick Li
 * @description 元数据模型通用删除
 **/
@Data
public class DeleteBO {

    /**
     * 元数据Id
     */
    private Long metaId;

    /**
     * 记录的Id
     */
    private List idList;

}
