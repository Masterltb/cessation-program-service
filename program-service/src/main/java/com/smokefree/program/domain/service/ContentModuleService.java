package com.smokefree.program.domain.service;



import com.smokefree.program.web.dto.module.ContentModuleCreateReq;
import com.smokefree.program.web.dto.module.ContentModuleRes;
import com.smokefree.program.web.dto.module.ContentModuleUpdateReq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

import java.util.UUID;

public interface ContentModuleService {

    ContentModuleRes create(ContentModuleCreateReq req);

    ContentModuleRes update(UUID id, ContentModuleUpdateReq req);

    void delete(UUID id);

    ContentModuleRes getOne(UUID id);

    ContentModuleRes getLatestByCode(String code, String lang);

    List<ContentModuleRes> listVersions(String code, String lang);

    Page<ContentModuleRes> search(String codeKeyword, String lang, Pageable pageable);
}
