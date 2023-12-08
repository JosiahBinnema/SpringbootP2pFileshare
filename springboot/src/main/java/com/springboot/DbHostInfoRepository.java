package com.springboot;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Interface for MongoDB operations. used in the DbHostInfoController
 */
public interface DbHostInfoRepository extends MongoRepository<DbHostInfo, String> {
    DbHostInfo findByFilename(String filename);
    DbHostInfo findByFilenameAndUsername(String filename, String username);
}
