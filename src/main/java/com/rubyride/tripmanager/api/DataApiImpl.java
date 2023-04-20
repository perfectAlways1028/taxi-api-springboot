package com.rubyride.tripmanager.api;

import com.rubyride.model.DataBlob;
import com.rubyride.model.DataType;
import com.rubyride.tripmanager.repository.mongo.DataBlobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Service
public class DataApiImpl {
  private final DataBlobRepository dataRepository;

  @Autowired
  public DataApiImpl(final DataBlobRepository dataRepository) {
    this.dataRepository = dataRepository;
  }

  @PreAuthorize("@accessControl.canWriteData()")
  public ResponseEntity<Void> deleteData(final UUID dataId) {
    dataRepository.deleteById(dataId);

    return ResponseEntity.noContent()
        .build();
  }

  public ResponseEntity<DataBlob> downloadData(final UUID dataId) {
    return ResponseEntity.of(dataRepository.findById(dataId));
  }

  @PreAuthorize("@accessControl.canReadData(#dataType)")
  public ResponseEntity<List<DataBlob>> getDataByType(final DataType dataType) {
    return ResponseEntity.ok(dataRepository.findByType(dataType));
  }

  @PreAuthorize("@accessControl.canWriteData()")
  public ResponseEntity<DataBlob> uploadData(final DataBlob data) {
    dataRepository.insert(data.id(UUID.randomUUID()));

    return ResponseEntity.created(URI.create("/v1/data/" + data.getId().toString()))
        .body(data);
  }
}
