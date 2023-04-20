package com.rubyride.tripmanager.repository.mongo;

import com.rubyride.model.DataBlob;
import com.rubyride.model.DataType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DataBlobRepository extends MongoRepository<DataBlob, UUID> {
  List<DataBlob> findByReferencesContains(UUID id);

  List<DataBlob> findByType(DataType type);
}
