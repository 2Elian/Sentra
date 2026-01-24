package com.sentra.knowledge.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sentra.knowledge.entity.KnowledgeBase;
import com.sentra.knowledge.mapper.KnowledgeBaseMapper;
import com.sentra.knowledge.service.IKnowledgeBaseService;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements IKnowledgeBaseService {
}
