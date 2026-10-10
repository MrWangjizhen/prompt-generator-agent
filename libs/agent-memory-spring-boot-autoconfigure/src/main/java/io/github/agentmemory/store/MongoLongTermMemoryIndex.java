package io.github.agentmemory.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.github.agentmemory.LongTermMemory;
import io.github.agentmemory.LongTermMemoryIndex;
import io.github.agentmemory.LongTermMemoryPage;
import io.github.agentmemory.LongTermMemoryQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * 基于 MongoDB 的长期记忆元数据索引。
 */
public class MongoLongTermMemoryIndex implements LongTermMemoryIndex {

    private static final String COLLECTION = "agent_memory_long_term_index";

    private final MongoOperations mongoOperations;

    public MongoLongTermMemoryIndex(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void save(LongTermMemory memory) {
        mongoOperations.save(memory, COLLECTION);
    }

    @Override
    public LongTermMemoryPage search(LongTermMemoryQuery query) {
        LongTermMemoryQuery normalized = query.normalized();
        Query mongoQuery = buildQuery(normalized)
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .with(PageRequest.of(normalized.page(), normalized.size()));
        long total = mongoOperations.count(buildQuery(normalized), LongTermMemory.class, COLLECTION);
        List<LongTermMemory> content = mongoOperations.find(mongoQuery, LongTermMemory.class, COLLECTION);
        return new LongTermMemoryPage(content, normalized.page(), normalized.size(), total);
    }

    @Override
    public Optional<LongTermMemory> findById(String userId, String memoryId) {
        Query query = Query.query(Criteria.where("userId").is(userId).and("id").is(memoryId));
        return Optional.ofNullable(mongoOperations.findOne(query, LongTermMemory.class, COLLECTION));
    }

    @Override
    public void deleteById(String userId, String memoryId) {
        Query query = Query.query(Criteria.where("userId").is(userId).and("id").is(memoryId));
        mongoOperations.remove(query, LongTermMemory.class, COLLECTION);
    }

    @Override
    public void delete(LongTermMemoryQuery query) {
        mongoOperations.remove(buildQuery(query.normalized()), LongTermMemory.class, COLLECTION);
    }

    @Override
    public void deleteByUserId(String userId) {
        mongoOperations.remove(Query.query(Criteria.where("userId").is(userId)), LongTermMemory.class, COLLECTION);
    }

    private static Query buildQuery(LongTermMemoryQuery query) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("userId").is(query.userId()));
        if (query.createdFrom() != null || query.createdTo() != null) {
            Criteria createdAt = Criteria.where("createdAt");
            if (query.createdFrom() != null) {
                createdAt = createdAt.gte(query.createdFrom());
            }
            if (query.createdTo() != null) {
                createdAt = createdAt.lte(query.createdTo());
            }
            criteria.add(createdAt);
        }
        if (query.conversationType() != null) {
            criteria.add(Criteria.where("conversationType").is(query.conversationType()));
        }
        if (query.minImportance() != null) {
            criteria.add(Criteria.where("importance").gte(query.minImportance()));
        }
        if (!query.topics().isEmpty()) {
            criteria.add(Criteria.where("topics").in(query.topics()));
        }
        return Query.query(new Criteria().andOperator(criteria));
    }
}
